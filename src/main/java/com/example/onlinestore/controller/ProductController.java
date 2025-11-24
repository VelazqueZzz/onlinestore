package com.example.onlinestore.controller;

import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
import com.example.onlinestore.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ShoppingCartService cartService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("cartItemsCount", cartService.getTotalItems());
        return "index";
    }

    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable Long id, Model model) {
        return productService.getProductById(id)
                .map(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("cartItemsCount", cartService.getTotalItems());
                    return "product-details";
                })
                .orElse("redirect:/"); // если товар не найден, редирект на главную
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam String keyword, Model model) {
        model.addAttribute("products", productService.searchProducts(keyword));
        model.addAttribute("cartItemsCount", cartService.getTotalItems());
        model.addAttribute("searchKeyword", keyword);
        return "index";
    }
}