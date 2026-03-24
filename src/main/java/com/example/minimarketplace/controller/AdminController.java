package com.example.minimarketplace.controller;

import com.example.minimarketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pendingRequests", userService.getPendingRequests());
        model.addAttribute("allUsers",        userService.findAll());
        return "admin/dashboard";
    }

    @PostMapping("/seller-requests/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        try   { userService.approveSellerRequest(id); ra.addFlashAttribute("successMsg", "Request approved."); }
        catch (RuntimeException e) { ra.addFlashAttribute("errorMsg", e.getMessage()); }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/seller-requests/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        try   { userService.rejectSellerRequest(id); ra.addFlashAttribute("successMsg", "Request rejected."); }
        catch (RuntimeException e) { ra.addFlashAttribute("errorMsg", e.getMessage()); }
        return "redirect:/admin/dashboard";
    }
}
