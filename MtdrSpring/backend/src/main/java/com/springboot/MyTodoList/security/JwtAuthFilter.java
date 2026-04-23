package com.springboot.MyTodoList.security;

import com.springboot.MyTodoList.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");
    final String jwtToken;
    final String userEmail;

    System.out.println("PATH: " + request.getRequestURI());
    System.out.println("AUTH HEADER: " + authHeader);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        System.out.println("No Bearer token found");
        filterChain.doFilter(request, response);
        return;
    }

    jwtToken = authHeader.substring(7);

    try {
        userEmail = jwtService.extractEmail(jwtToken);
        System.out.println("EMAIL FROM TOKEN: " + userEmail);
    } catch (Exception e) {
        System.out.println("TOKEN ERROR: " + e.getMessage());
        filterChain.doFilter(request, response);
        return;
    }

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        System.out.println("USERDETAILS USERNAME: " + userDetails.getUsername());

        if (jwtService.isTokenValid(jwtToken)) {
            System.out.println("TOKEN VALID");
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            System.out.println("AUTHENTICATION SET");
        } else {
            System.out.println("TOKEN INVALID");
        }
    }

    filterChain.doFilter(request, response);
}
}