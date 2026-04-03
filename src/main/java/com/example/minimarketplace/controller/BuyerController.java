package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.BuyerDashboardData;
import com.example.minimarketplace.service.BuyerDashboardService;
import com.example.minimarketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Controller
@RequestMapping("/buyer")
@PreAuthorize("hasRole('BUYER')")
@RequiredArgsConstructor
public class BuyerController {

    private final BuyerDashboardService buyerDashboardService;
    private final UserService userService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "") String seller,
                            @RequestParam(defaultValue = "featured") String sort,
                            @RequestParam(defaultValue = "grid") String view,
                            Authentication auth,
                            Model model) {
        populateBuyerDashboard(auth, model, q, seller, sort);
        model.addAttribute("catalogQuery", q);
        model.addAttribute("catalogSeller", seller);
        model.addAttribute("catalogSort", normalizeCatalogSort(sort));
        model.addAttribute("catalogView", normalizeCatalogView(view));
        return "buyer/dashboard";
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "false") boolean downloadsOnly,
                         Authentication auth,
                         Model model) {
        populateBuyerDashboard(auth, model, "", "", "featured");
        model.addAttribute("orders", buyerDashboardService.loadOrders(auth.getName(), q, downloadsOnly));
        model.addAttribute("orderQuery", q);
        model.addAttribute("downloadsOnly", downloadsOnly);
        return "buyer/orders";
    }

    @PostMapping("/purchase")
    public String purchase(@RequestParam Long productId,
                           @RequestParam(defaultValue = "1") Integer quantity,
                           @RequestParam(defaultValue = "/buyer/dashboard") String returnTo,
                           Authentication auth,
                           RedirectAttributes ra) {
        try {
            buyerDashboardService.purchaseProduct(auth.getName(), productId, quantity);
            ra.addFlashAttribute("successMsg", "Purchase completed. Your order is now in your library.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:" + sanitizeBuyerRoute(returnTo);
    }

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

    @GetMapping("/products/{id}/image")
    @ResponseBody
    public ResponseEntity<byte[]> productImage(@PathVariable Long id) {
        var image = buyerDashboardService.getProductImage(id);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)))
            .contentType(resolveMediaType(image.contentType()))
            .body(image.data());
    }

    @GetMapping("/orders/{id}/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadOrderAsset(@PathVariable Long id, Authentication auth) {
        var asset = buyerDashboardService.getOrderAsset(auth.getName(), id);
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(asset.filename(), StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .contentType(resolveMediaType(asset.contentType()))
            .body(asset.data());
    }

    private void populateBuyerDashboard(Authentication auth, Model model, String query, String seller, String sort) {
        BuyerDashboardData data = buyerDashboardService.loadDashboard(auth.getName(), query, seller, sort);
        model.addAttribute("buyer", data.buyer());
        model.addAttribute("analytics", data.analytics());
        model.addAttribute("marketplacePicks", data.marketplacePicks());
        model.addAttribute("recentOrders", data.recentOrders());
        model.addAttribute("latestSellerRequest", data.latestSellerRequest());
        model.addAttribute("sellerOptions", data.sellerOptions());
        model.addAttribute("downloadReadyOrders", data.downloadReadyOrders());
    }

    private String sanitizeBuyerRoute(String route) {
        return "/buyer/orders".equals(route) ? "/buyer/orders" : "/buyer/dashboard";
    }

    private String normalizeCatalogSort(String sort) {
        return switch (sort) {
            case "newest", "priceAsc", "priceDesc", "name" -> sort;
            default -> "featured";
        };
    }

    private String normalizeCatalogView(String view) {
        return "list".equalsIgnoreCase(view) ? "list" : "grid";
    }

    private MediaType resolveMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
