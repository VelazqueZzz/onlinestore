package com.example.onlinestore.controller;

import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductService productService;

    // Страница со списком всех товаров
    @GetMapping("/products")
    public String adminProducts(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "admin/products";
    }

    // Форма добавления нового товара
    @GetMapping("/products/new")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "admin/add-product";
    }

    // Обработка добавления товара
    @PostMapping("/products/new")
    public String addProduct(@Valid @ModelAttribute("product") Product product,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "admin/add-product";
        }

        try {
            // Устанавливаем значение по умолчанию для изображения, если оно пустое
            if (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty()) {
                product.setImageUrl("https://via.placeholder.com/400");
            }

            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("successMessage", "Товар успешно добавлен!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Ошибка при добавлении товара: " + e.getMessage());
            return "admin/add-product";
        }
    }

    // Форма редактирования товара
    @GetMapping("/products/edit/{id}")
    public String showEditProductForm(@PathVariable Long id, Model model) {
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            model.addAttribute("product", product.get());
            return "admin/edit-product";
        } else {
            return "redirect:/admin/products";
        }
    }

    // Обработка редактирования товара
    @PostMapping("/products/edit/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") Product product,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "admin/edit-product";
        }

        try {
            // Проверяем существование товара
            Optional<Product> existingProduct = productService.getProductById(id);
            if (existingProduct.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Товар не найден!");
                return "redirect:/admin/products";
            }

            // Устанавливаем значение по умолчанию для изображения, если оно пустое
            if (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty()) {
                product.setImageUrl("https://via.placeholder.com/400");
            }

            // Сохраняем товар
            product.setId(id);
            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("successMessage", "Товар успешно обновлен!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Ошибка при обновлении товара: " + e.getMessage());
            return "admin/edit-product";
        }
    }

    // Удаление товара
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            if (productService.getProductById(id).isPresent()) {
                productService.deleteProduct(id);
                redirectAttributes.addFlashAttribute("successMessage", "Товар успешно удален!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Товар не найден!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении товара: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
}