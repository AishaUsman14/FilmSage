package com.filmsage.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminViewController {
    
    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }
} 