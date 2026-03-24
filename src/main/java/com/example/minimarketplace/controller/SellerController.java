package com.example.minimarketplace.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    @GetMapping("/dashboard")
    public String dashboard() { return "seller/dashboard"; }
}
