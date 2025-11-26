package com.example.onlinestore.controller;

import com.example.onlinestore.service.OrderService;
import com.example.onlinestore.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

@Controller
public class AuthController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        try {
            // Безопасное получение статистики
            Long totalProducts = (long) productService.getAllProducts().size();
            Long totalOrders = orderService.getTotalOrdersCount();
            BigDecimal totalRevenue = orderService.getTotalRevenue();
            Long pendingOrders = orderService.getPendingOrdersCount();
            var recentOrders = orderService.getRecentOrders(5);

            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("recentOrders", recentOrders);

        } catch (Exception e) {
            System.err.println("Error in admin dashboard: " + e.getMessage());
            // В случае ошибки устанавливаем значения по умолчанию
            model.addAttribute("totalProducts", 0);
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("pendingOrders", 0);
            model.addAttribute("recentOrders", java.util.Collections.emptyList());
        }

        return "admin/dashboard";
    }
}