package com.example.minimarketplace.controller;

import com.example.minimarketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerController {

    private final UserService userService;

    @GetMapping("/orders")
    public String orders() { return "buyer/orders"; }

    @GetMapping("/seller-request")
    public String sellerRequestForm() { return "buyer/seller-request"; }

    @PostMapping("/seller-request")
    public String submitSellerRequest(@RequestParam(defaultValue = "") String note,
                                      Authentication auth, RedirectAttributes ra) {
        try   { userService.requestSellerRole(auth.getName(), note);
                ra.addFlashAttribute("successMsg", "Request submitted! Admin will review it soon."); }
        catch (RuntimeException e) { ra.addFlashAttribute("errorMsg", e.getMessage()); }
        return "redirect:/buyer/seller-request";
    }
}
