package com.example.onlinestore.service;

import com.example.onlinestore.model.*;
import com.example.onlinestore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    public Order createOrderFromCart(ShoppingCartService cartService, String customerName,
                                     String customerEmail, String customerAddress) {

        List<CartItem> cartItems = cartService.getCartItems();

        System.out.println("Creating order for: " + customerName);
        System.out.println("Cart items count: " + cartItems.size());

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Корзина пуста");
        }

        // Создаем заказ
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setCustomerEmail(customerEmail);
        order.setCustomerAddress(customerAddress);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Добавляем товары в заказ и обновляем остатки
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            Integer quantity = cartItem.getQuantity();

            System.out.println("Processing product: " + product.getName() + ", quantity: " + quantity);

            // Проверяем доступное количество
            if (product.getStockQuantity() < quantity) {
                throw new IllegalStateException("Недостаточно товара: " + product.getName() +
                        ". Доступно: " + product.getStockQuantity() +
                        ", запрошено: " + quantity);
            }

            // Обновляем остатки
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productService.saveProduct(product);

            // Создаем позицию заказа
            OrderItem orderItem = new OrderItem(product, quantity);
            order.addItem(orderItem);

            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        order.setTotalAmount(totalAmount);

        // Сохраняем заказ и очищаем корзину
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart();

        System.out.println("Order created successfully with ID: " + savedOrder.getId());

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден: " + id));
    }

    public void updateOrderStatus(Long id, OrderStatus status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        orderRepository.save(order);
    }

    public List<Order> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmailOrderByOrderDateDesc(email);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByOrderDateDesc(status);
    }

    public void cancelOrder(Long id) {
        Order order = getOrderById(id);

        // Возвращаем товары на склад
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productService.saveProduct(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public Long getTotalOrdersCount() {
        return orderRepository.count();
    }

    public BigDecimal getTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED ||
                        order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Order> getRecentOrders(int count) {
        return orderRepository.findTop5ByOrderByOrderDateDesc();
    }
}