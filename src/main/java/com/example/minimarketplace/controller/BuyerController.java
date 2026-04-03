package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.BuyerDashboardData;
import com.example.minimarketplace.service.BuyerDashboardService;
import com.example.minimarketplace.service.UserService;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/buyer")
@PreAuthorize("hasRole('BUYER')")
@RequiredArgsConstructor
public class BuyerController {

    private static final String CART_SESSION_KEY = "buyerCart";

    private final BuyerDashboardService buyerDashboardService;
    private final UserService userService;

    @ModelAttribute("cartItemCount")
    public int cartItemCount(HttpSession session) {
        return getCart(session).values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }

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

    @GetMapping("/cart")
    public String cart(Authentication auth, Model model, HttpSession session) {
        populateBuyerDashboard(auth, model, "", "", "featured");

        Map<Long, Integer> cart = getCart(session);
        var cartLines = buyerDashboardService.loadCart(auth.getName(), cart);
        BigDecimal subtotal = cartLines.stream()
            .map(BuyerDashboardService.CartLine::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        boolean hasBlockingIssue = cartLines.stream().anyMatch(line -> !line.purchasable());

        model.addAttribute("cartLines", cartLines);
        model.addAttribute("cartSubtotal", subtotal);
        model.addAttribute("cartHasBlockingIssue", hasBlockingIssue);
        model.addAttribute("cartUniqueItems", cartLines.size());
        return "buyer/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            @RequestParam(defaultValue = "/buyer/dashboard") String returnTo,
                            HttpSession session,
                            RedirectAttributes ra) {
        try {
            int normalizedQuantity = normalizeCartQuantity(quantity);
            Map<Long, Integer> cart = getCart(session);
            cart.merge(productId, normalizedQuantity, Integer::sum);
            session.setAttribute(CART_SESSION_KEY, cart);
            ra.addFlashAttribute("successMsg", "Added to cart.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:" + sanitizeBuyerRoute(returnTo);
    }

    @PostMapping("/cart/update")
    public String updateCartQuantity(@RequestParam Long productId,
                                     @RequestParam Integer quantity,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        Map<Long, Integer> cart = getCart(session);
        if (!cart.containsKey(productId)) {
            ra.addFlashAttribute("errorMsg", "That product is not in your cart.");
            return "redirect:/buyer/cart";
        }

        if (quantity == null || quantity < 1) {
            cart.remove(productId);
            ra.addFlashAttribute("successMsg", "Item removed from cart.");
        } else {
            cart.put(productId, quantity);
            ra.addFlashAttribute("successMsg", "Cart quantity updated.");
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/buyer/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Map<Long, Integer> cart = getCart(session);
        if (cart.remove(productId) != null) {
            ra.addFlashAttribute("successMsg", "Item removed from cart.");
        } else {
            ra.addFlashAttribute("errorMsg", "That product is not in your cart.");
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/buyer/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkoutCart(Authentication auth, HttpSession session, RedirectAttributes ra) {
        try {
            int purchasedItems = buyerDashboardService.checkoutCart(auth.getName(), getCart(session));
            session.setAttribute(CART_SESSION_KEY, new LinkedHashMap<Long, Integer>());
            ra.addFlashAttribute("successMsg", "Checkout complete for " + purchasedItems + " cart item(s).");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/buyer/cart";
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

    @GetMapping("/account")
    public String account(Authentication auth, Model model) {
        populateBuyerDashboard(auth, model, "", "", "featured");
        model.addAttribute("accountUser", userService.findByUsername(auth.getName()));
        return "buyer/account";
    }

    @PostMapping("/account/address")
    public String updateAddress(@RequestParam(defaultValue = "") String shippingAddress,
                                Authentication auth,
                                RedirectAttributes ra) {
        try {
            userService.updateShippingAddress(auth.getName(), shippingAddress);
            ra.addFlashAttribute("successMsg", "Shipping address updated.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/buyer/account";
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
    public String sellerRequestForm() {
        return "buyer/seller-request";
    }

    @PostMapping("/seller-request")
    public String submitSellerRequest(@RequestParam(defaultValue = "") String note,
                                      Authentication auth, RedirectAttributes ra) {
        try {
            userService.requestSellerRole(auth.getName(), note);
            ra.addFlashAttribute("successMsg", "Request submitted! Admin will review it soon.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
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
        return switch (route) {
            case "/buyer/cart", "/buyer/orders", "/buyer/account", "/buyer/seller-request" -> route;
            default -> "/buyer/dashboard";
        };
    }

    private int normalizeCartQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1.");
        }
        return quantity;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Object cartObject = session.getAttribute(CART_SESSION_KEY);
        if (!(cartObject instanceof Map<?, ?> rawMap)) {
            Map<Long, Integer> fresh = new LinkedHashMap<>();
            session.setAttribute(CART_SESSION_KEY, fresh);
            return fresh;
        }

        Map<Long, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof Long productId
                && entry.getValue() instanceof Integer quantity
                && quantity > 0) {
                normalized.put(productId, quantity);
            }
        }
        session.setAttribute(CART_SESSION_KEY, normalized);
        return normalized;
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
