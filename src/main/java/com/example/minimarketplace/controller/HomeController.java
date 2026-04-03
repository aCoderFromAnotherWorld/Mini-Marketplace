package com.example.minimarketplace.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        if (isLoggedIn(auth)) {
            return "redirect:" + resolveLandingRoute(auth, null);
        }
        return "home";
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         Authentication auth, Model model) {
        if (isLoggedIn(auth)) {
            return "redirect:" + resolveLandingRoute(auth, q);
        }
        model.addAttribute("searchQuery", q);
        return "home";
    }

    private boolean isLoggedIn(Authentication a) {
        return a != null && a.isAuthenticated() && !"anonymousUser".equals(a.getPrincipal());
    }

    private String resolveLandingRoute(Authentication authentication, String searchQuery) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "/admin/dashboard";
        }
        if (hasRole(authentication, "ROLE_SELLER")) {
            return "/seller/dashboard";
        }
        if (hasRole(authentication, "ROLE_BUYER")) {
            if (searchQuery != null && !searchQuery.isBlank()) {
                return "/buyer/dashboard?q="
                    + UriUtils.encodeQueryParam(searchQuery.trim(), StandardCharsets.UTF_8);
            }
            return "/buyer/dashboard";
        }
        return "/";
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role::equals);
    }
}
