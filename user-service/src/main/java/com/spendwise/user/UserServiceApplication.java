package com.spendwise.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(UserServiceApplication.class, args);
        String password = ctx.getEnvironment().getProperty("spring.datasource.password");
        System.out.println("### RESOLVED DATASOURCE PASSWORD: [" + password + "] length=" + (password == null ? "null" : password.length()));
    }
}