package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.RegisterRequest;
import com.example.minimarketplace.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    /* ── Login ──────────────────────────────────────────────── */

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Authentication auth, Model model) {
        if (isLoggedIn(auth)) return "redirect:/";
        if (error  != null) model.addAttribute("errorMsg",   "Invalid username or password.");
        if (logout != null) model.addAttribute("successMsg", "You have been logged out.");
        return "auth/login";
    }

    /* ── Register ───────────────────────────────────────────── */

    @GetMapping("/register")
    public String registerPage(Authentication auth, Model model) {
        if (isLoggedIn(auth)) return "redirect:/";
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registerRequest") RegisterRequest req,
                           BindingResult br,
                           RedirectAttributes ra,
                           Model model) {
        if (br.hasErrors()) return "auth/register";
        try {
            userService.register(req);
            ra.addFlashAttribute("successMsg", "Account created! You can now log in.");
            return "redirect:/auth/login";
        } catch (RuntimeException ex) {
            log.warn("Registration failed: {}", ex.getMessage());
            model.addAttribute("errorMsg", ex.getMessage());
            return "auth/register";
        }
    }

    private boolean isLoggedIn(Authentication a) {
        return a != null && a.isAuthenticated() && !"anonymousUser".equals(a.getPrincipal());
    }
}
