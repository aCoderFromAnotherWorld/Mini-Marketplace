package com.example.minimarketplace.controller;

import com.example.minimarketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        if (isLoggedIn(auth)) {
            model.addAttribute("username", auth.getName());
            model.addAttribute("role", primaryRole(auth));
            try { model.addAttribute("user", userService.findByUsername(auth.getName())); }
            catch (Exception ignored) {}
        }
        return "home";
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         Authentication auth, Model model) {
        model.addAttribute("searchQuery", q);
        if (isLoggedIn(auth)) {
            model.addAttribute("username", auth.getName());
            model.addAttribute("role", primaryRole(auth));
        }
        return "home";
    }

    private boolean isLoggedIn(Authentication a) {
        return a != null && a.isAuthenticated() && !"anonymousUser".equals(a.getPrincipal());
    }

    private String primaryRole(Authentication a) {
        return a.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(r -> r.startsWith("ROLE_"))
            .min((x, y) -> rank(x) - rank(y))
            .map(r -> r.replace("ROLE_", ""))
            .orElse("USER");
    }

    private int rank(String r) {
        return switch (r) {
            case "ROLE_ADMIN"  -> 1;
            case "ROLE_SELLER" -> 2;
            case "ROLE_BUYER"  -> 3;
            default            -> 99;
        };
    }
}
