package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.ProductForm;
import com.example.minimarketplace.dto.SaleForm;
import com.example.minimarketplace.dto.SellerDashboardData;
import com.example.minimarketplace.service.SellerDashboardService;
import jakarta.validation.Valid;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/seller")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class SellerController {

    private final SellerDashboardService sellerDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        SellerDashboardData data = sellerDashboardService.loadDashboard(auth.getName());

        model.addAttribute("products", data.products());
        model.addAttribute("recentSales", data.recentSales());
        model.addAttribute("analytics", data.analytics());
        model.addAttribute("dailyRevenue", data.dailyRevenue());

        if (!model.containsAttribute("newProductForm")) {
            model.addAttribute("newProductForm", new ProductForm());
        }
        if (!model.containsAttribute("saleForm")) {
            model.addAttribute("saleForm", new SaleForm());
        }
        if (!model.containsAttribute("editProductForm")) {
            model.addAttribute("editProductForm", new ProductForm());
        }

        return "seller/dashboard";
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute("newProductForm") ProductForm form,
                                BindingResult bindingResult,
                                @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(name = "assetFile", required = false) MultipartFile assetFile,
                                Authentication auth,
                                RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMsg", firstValidationError(bindingResult));
            ra.addFlashAttribute("newProductForm", form);
            return "redirect:/seller/dashboard";
        }

        try {
            sellerDashboardService.createProduct(auth.getName(), form, imageFile, assetFile);
            ra.addFlashAttribute("successMsg", "Product created successfully.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
            ra.addFlashAttribute("newProductForm", form);
        }
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/products/{id}/update")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("editProductForm") ProductForm form,
                                BindingResult bindingResult,
                                @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(name = "assetFile", required = false) MultipartFile assetFile,
                                Authentication auth,
                                RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMsg", firstValidationError(bindingResult));
            return "redirect:/seller/dashboard";
        }

        try {
            sellerDashboardService.updateProduct(auth.getName(), id, form, imageFile, assetFile);
            ra.addFlashAttribute("successMsg", "Product updated.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            sellerDashboardService.deleteProduct(auth.getName(), id);
            ra.addFlashAttribute("successMsg", "Product deleted.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/sales")
    public String recordSale(@Valid @ModelAttribute("saleForm") SaleForm form,
                             BindingResult bindingResult,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMsg", firstValidationError(bindingResult));
            ra.addFlashAttribute("saleForm", form);
            return "redirect:/seller/dashboard";
        }

        try {
            sellerDashboardService.recordSale(auth.getName(), form);
            ra.addFlashAttribute("successMsg", "Sale recorded successfully.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
            ra.addFlashAttribute("saleForm", form);
        }

        return "redirect:/seller/dashboard";
    }

    @GetMapping("/products/{id}/image")
    @ResponseBody
    public ResponseEntity<byte[]> productImage(@PathVariable Long id, Authentication auth) {
        var image = sellerDashboardService.getProductImage(auth.getName(), id);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)))
            .contentType(resolveMediaType(image.contentType()))
            .body(image.data());
    }

    @GetMapping("/products/{id}/asset")
    @ResponseBody
    public ResponseEntity<byte[]> productAsset(@PathVariable Long id, Authentication auth) {
        var asset = sellerDashboardService.getProductAsset(auth.getName(), id);
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

    private String firstValidationError(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Please check your input.")
            .orElse("Please check your input.");
    }

    private MediaType resolveMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
