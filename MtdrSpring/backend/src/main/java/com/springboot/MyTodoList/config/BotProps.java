package com.springboot.MyTodoList.config;

//import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
//import org.springframework.context.annotation.PropertySource;

@ConfigurationProperties(prefix = "telegram.bot")
@Validated
public class BotProps {
    @NotBlank
    private String name;
    @NotBlank
    private String token;

    public String getToken(){
		return token;
	}

    public String getName(){
		return name;
	}

    public void setToken(String tkn){
		token = tkn;
	}

    public void setName(String n){
		name = n;
	}
}