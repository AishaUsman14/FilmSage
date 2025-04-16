package com.filmsage.controllers;

import com.filmsage.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    
    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             RedirectAttributes redirectAttributes) {
        
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Passwords do not match");
            return "redirect:/signup";
        }

        try {
            userService.registerNewUser(username, email, password);
            return "redirect:/login?registered";
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/signup";
        }
    }

    @GetMapping("/")
    public String home() {
        System.out.println("Home page requested");
        return "home";
    }
} 