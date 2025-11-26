package com.example.onlinestore.controller;

import com.example.onlinestore.model.Order;
import com.example.onlinestore.model.OrderStatus; // Добавляем этот импорт
import com.example.onlinestore.service.OrderService;
import com.example.onlinestore.service.ShoppingCartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private ShoppingCartService cartService;

    @Autowired
    private OrderService orderService;

    // Форма оформления заказа - ДОЛЖЕН БЫТЬ GET
    @GetMapping("/checkout")
    public String showCheckoutForm(Model model) {
        System.out.println("=== CHECKOUT GET called ===");
        System.out.println("Cart items: " + cartService.getCartItems().size());
        System.out.println("Cart total: " + cartService.getTotalPrice());

        if (cartService.getCartItems().isEmpty()) {
            System.out.println("Cart is empty, redirecting to cart");
            return "redirect:/cart";
        }

        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        model.addAttribute("cartItemsCount", cartService.getTotalItems());

        // Если форма еще не была заполнена, создаем новую
        if (!model.containsAttribute("orderForm")) {
            model.addAttribute("orderForm", new OrderForm());
        }

        return "order/checkout";
    }

    // Обработка оформления заказа - ДОЛЖЕН БЫТЬ POST
    @PostMapping("/checkout")
    public String processCheckout(@Valid @ModelAttribute("orderForm") OrderForm orderForm,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        System.out.println("=== CHECKOUT POST called ===");
        System.out.println("Customer: " + orderForm.getCustomerName());
        System.out.println("Email: " + orderForm.getCustomerEmail());

        if (cartService.getCartItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Корзина пуста");
            return "redirect:/cart";
        }

        if (result.hasErrors()) {
            System.out.println("Form has errors: " + result.getAllErrors());
            model.addAttribute("cartItems", cartService.getCartItems());
            model.addAttribute("totalPrice", cartService.getTotalPrice());
            model.addAttribute("cartItemsCount", cartService.getTotalItems());
            return "order/checkout";
        }

        try {
            Order order = orderService.createOrderFromCart(
                    cartService,
                    orderForm.getCustomerName(),
                    orderForm.getCustomerEmail(),
                    orderForm.getCustomerAddress()
            );

            System.out.println("Order created successfully: " + order.getId());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Заказ №" + order.getId() + " успешно оформлен!");
            return "redirect:/order/success/" + order.getId();

        } catch (Exception e) {
            System.out.println("Error creating order: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("errorMessage", "Ошибка при оформлении заказа: " + e.getMessage());
            model.addAttribute("cartItems", cartService.getCartItems());
            model.addAttribute("totalPrice", cartService.getTotalPrice());
            model.addAttribute("cartItemsCount", cartService.getTotalItems());
            return "order/checkout";
        }
    }

    // Страница успешного оформления заказа
    @GetMapping("/success/{orderId}")
    public String orderSuccess(@PathVariable Long orderId, Model model) {
        try {
            Order order = orderService.getOrderById(orderId);
            model.addAttribute("order", order);
            model.addAttribute("cartItemsCount", cartService.getTotalItems());
            return "order/success";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Заказ не найден: " + e.getMessage());
            return "redirect:/";
        }
    }

    // Выбор способа оплаты
    @GetMapping("/payment/{orderId}")
    public String selectPaymentMethod(@PathVariable Long orderId, Model model) {
        try {
            Order order = orderService.getOrderById(orderId);
            model.addAttribute("order", order);
            model.addAttribute("cartItemsCount", cartService.getTotalItems());
            return "order/payment-method";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Заказ не найден: " + e.getMessage());
            return "redirect:/";
        }
    }

    // Список всех заказов (для админки)
    @GetMapping("/admin")
    public String orderList(Model model) {
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        model.addAttribute("cartItemsCount", cartService.getTotalItems());
        return "admin/orders";
    }

    // Детали конкретного заказа (для админки)
    @GetMapping("/admin/{id}")
    public String orderDetails(@PathVariable Long id, Model model) {
        try {
            Order order = orderService.getOrderById(id);
            model.addAttribute("order", order);
            model.addAttribute("cartItemsCount", cartService.getTotalItems());
            model.addAttribute("orderStatuses", OrderStatus.values()); // Используем OrderStatus
            return "admin/order-details";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Заказ не найден: " + e.getMessage());
            return "redirect:/order/admin";
        }
    }

    // Изменение статуса заказа (для админки)
    @PostMapping("/admin/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status, // Используем OrderStatus
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Статус заказа обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении статуса: " + e.getMessage());
        }
        return "redirect:/order/admin/" + id;
    }

    // Удаление заказа (для админки)
    @PostMapping("/admin/{id}/delete")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // В реальном приложении лучше использовать мягкое удаление
            orderService.cancelOrder(id); // Используем cancel вместо delete
            redirectAttributes.addFlashAttribute("successMessage", "Заказ отменен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при отмене заказа: " + e.getMessage());
        }
        return "redirect:/order/admin";
    }

    // Повторение заказа
    @PostMapping("/repeat/{orderId}")
    public String repeatOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order originalOrder = orderService.getOrderById(orderId);

            // Добавляем товары из заказа в корзину
            originalOrder.getItems().forEach(item -> {
                cartService.addProduct(item.getProduct(), item.getQuantity());
            });

            redirectAttributes.addFlashAttribute("successMessage",
                    "Товары из заказа №" + orderId + " добавлены в корзину");
            return "redirect:/cart";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при повторении заказа: " + e.getMessage());
            return "redirect:/order/admin";
        }
    }

    // История заказов для пользователя (по email)
    @GetMapping("/history")
    public String orderHistory(@RequestParam(required = false) String email, Model model) {
        if (email != null && !email.trim().isEmpty()) {
            List<Order> userOrders = orderService.getOrdersByEmail(email);
            model.addAttribute("userOrders", userOrders);
            model.addAttribute("searchedEmail", email);
        }
        model.addAttribute("cartItemsCount", cartService.getTotalItems());
        return "order/history";
    }

    // DTO для формы заказа
    public static class OrderForm {
        private String customerName;
        private String customerEmail;
        private String customerAddress;
        private String customerPhone;
        private String notes;

        // Геттеры и сеттеры
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

        public String getCustomerAddress() { return customerAddress; }
        public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}