package com.example.minimarketplace.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isSeller = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"));
        boolean isBuyer = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_BUYER"));

        model.addAttribute("username", authentication.getName());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isSeller", isSeller);
        model.addAttribute("isBuyer", isBuyer);

        if (isAdmin) return "dashboard-admin";
        if (isSeller) return "dashboard-seller";
        if (isBuyer) return "dashboard-buyer";

        return "dashboard";
    }
}
