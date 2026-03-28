package com.example.minimarketplace.controller;

import com.example.minimarketplace.config.SecurityConfig;
import com.example.minimarketplace.dto.RegisterRequest;
import com.example.minimarketplace.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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

    @GetMapping({"/login", "/login/"})
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String csrfError,
                            @RequestParam(required = false) String logout,
                            Authentication auth,
                            HttpSession session,
                            Model model) {
        if (isLoggedIn(auth)) return "redirect:/";
        String authErrorMessage = consumeAuthErrorMessage(session);
        if (error  != null) model.addAttribute("errorMsg",   "Invalid username or password.");
        if (authErrorMessage != null) {
            model.addAttribute("errorMsg", authErrorMessage);
        } else if (csrfError != null) {
            model.addAttribute("errorMsg", "Your session expired. Please try again.");
        }
        if (logout != null) model.addAttribute("successMsg", "You have been logged out.");
        return "auth/login";
    }

    /* ── Register ───────────────────────────────────────────── */

    @GetMapping({"/register", "/register/"})
    public String registerPage(@RequestParam(required = false) String csrfError,
                               Authentication auth,
                               HttpSession session,
                               Model model) {
        if (isLoggedIn(auth)) return "redirect:/";
        String authErrorMessage = consumeAuthErrorMessage(session);
        if (authErrorMessage != null) {
            model.addAttribute("errorMsg", authErrorMessage);
        } else if (csrfError != null) {
            model.addAttribute("errorMsg", "Your session expired. Please submit the form again.");
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping({"/register", "/register/"})
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest req,
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

    private String consumeAuthErrorMessage(HttpSession session) {
        Object value = session.getAttribute(SecurityConfig.AUTH_ERROR_MESSAGE_SESSION_KEY);
        session.removeAttribute(SecurityConfig.AUTH_ERROR_MESSAGE_SESSION_KEY);
        return value instanceof String message && !message.isBlank() ? message : null;
    }
}
