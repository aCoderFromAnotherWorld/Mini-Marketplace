package com.example.minimarketplace.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication auth) {
        return redirectFor(auth);
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        if (hasRole(auth, "ROLE_BUYER") && q != null && !q.isBlank()) {
            redirectAttributes.addAttribute("q", q.trim());
        }
        return redirectFor(auth);
    }

    private String redirectFor(Authentication auth) {
        if (!isLoggedIn(auth)) {
            return "redirect:/auth/login";
        }
        if (hasRole(auth, "ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        if (hasRole(auth, "ROLE_SELLER")) {
            return "redirect:/seller/dashboard";
        }
        if (hasRole(auth, "ROLE_BUYER")) {
            return "redirect:/buyer/dashboard";
        }
        return "redirect:/auth/login";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private boolean hasRole(Authentication authentication, String role) {
        return isLoggedIn(authentication)
            && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
