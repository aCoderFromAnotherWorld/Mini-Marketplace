package com.example.minimarketplace.controller;

import com.example.minimarketplace.entity.Product;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.Duration;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeController {

    private final UserService userService;
    private final ProductRepository productRepository;

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        populateUserContext(auth, model);
        populateCatalog(model, "");
        return "home";
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         Authentication auth, Model model) {
        populateUserContext(auth, model);
        populateCatalog(model, q);
        return "home";
    }

    @GetMapping("/products/{id}")
    public String productDetails(@PathVariable Long id, Authentication auth, Model model) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        populateUserContext(auth, model);
        model.addAttribute("product", product);
        return "products/detail";
    }

    @GetMapping("/products/{id}/image")
    @ResponseBody
    public ResponseEntity<byte[]> productImage(@PathVariable Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found."));

        if (product.getImageData() == null || product.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)))
            .contentType(resolveMediaType(product.getImageContentType()))
            .body(product.getImageData());
    }

    private void populateUserContext(Authentication auth, Model model) {
        if (!isLoggedIn(auth)) {
            return;
        }

        model.addAttribute("username", auth.getName());
        model.addAttribute("role", primaryRole(auth));
        try {
            model.addAttribute("user", userService.findByUsername(auth.getName()));
        } catch (Exception ignored) {
        }
    }

    private void populateCatalog(Model model, String query) {
        String searchQuery = query == null ? "" : query.trim();
        List<Product> products = searchQuery.isEmpty()
            ? productRepository.findAllByOrderByCreatedAtDesc()
            : productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(
                searchQuery,
                searchQuery
            );

        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("products", products);
    }

    private MediaType resolveMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
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
            case "ROLE_ADMIN" -> 1;
            case "ROLE_SELLER" -> 2;
            case "ROLE_BUYER" -> 3;
            default -> 99;
        };
    }
}
