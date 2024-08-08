package com.example.demo.utils;

import com.example.demo.entity.Account;
import com.example.demo.repository.AuthenticationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccountUtils {
    @Autowired
    AuthenticationRepository userRepository;

    public Account getCurrentUser(){
        String email=  SecurityContextHolder.getContext().getAuthentication().getName();
        Account user = userRepository.findByEmail(email);
        return user;
        }
}
