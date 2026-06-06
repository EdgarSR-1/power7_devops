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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

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

    logger.debug("PATH: {}", request.getRequestURI());
    logger.debug("AUTH HEADER: {}", authHeader);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        logger.debug("No Bearer token found");
        filterChain.doFilter(request, response);
        return;
    }

    jwtToken = authHeader.substring(7);

    try {
        userEmail = jwtService.extractEmail(jwtToken);
        logger.debug("EMAIL FROM TOKEN: {}", userEmail);
    } catch (Exception e) {
        logger.debug("TOKEN ERROR: {}", e.getMessage());
        filterChain.doFilter(request, response);
        return;
    }

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        logger.debug("USERDETAILS USERNAME: {}", userDetails.getUsername());

        if (jwtService.isTokenValid(jwtToken)) {
            logger.debug("TOKEN VALID");
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.debug("AUTHENTICATION SET");
        } else {
            logger.debug("TOKEN INVALID");
        }
    }

    filterChain.doFilter(request, response);
}
}
