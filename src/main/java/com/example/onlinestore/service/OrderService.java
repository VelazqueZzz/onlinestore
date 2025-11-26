package com.example.onlinestore.service;

import com.example.onlinestore.model.*;
import com.example.onlinestore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        try {
            return orderRepository.findAllByOrderByOrderDateDesc();
        } catch (Exception e) {
            System.err.println("Error getting all orders: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден: " + id));
    }

    public void updateOrderStatus(Long id, OrderStatus status) {
        try {
            Order order = getOrderById(id);
            order.setStatus(status);
            orderRepository.save(order);
            System.out.println("Order " + id + " status updated to: " + status);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при обновлении статуса заказа: " + e.getMessage(), e);
        }
    }

    public List<Order> getOrdersByEmail(String email) {
        try {
            return orderRepository.findByCustomerEmailOrderByOrderDateDesc(email);
        } catch (Exception e) {
            System.err.println("Error getting orders by email: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        try {
            return orderRepository.findByStatusOrderByOrderDateDesc(status);
        } catch (Exception e) {
            System.err.println("Error getting orders by status: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void cancelOrder(Long id) {
        try {
            Order order = getOrderById(id);

            // Возвращаем товары на склад
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productService.saveProduct(product);
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            System.out.println("Order " + id + " cancelled successfully");

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при отмене заказа: " + e.getMessage(), e);
        }
    }

    public Long getTotalOrdersCount() {
        try {
            return orderRepository.count();
        } catch (Exception e) {
            System.err.println("Error getting total orders count: " + e.getMessage());
            return 0L;
        }
    }

    public BigDecimal getTotalRevenue() {
        try {
            List<Order> allOrders = orderRepository.findAll();
            BigDecimal revenue = BigDecimal.ZERO;

            for (Order order : allOrders) {
                if (order.getStatus() == OrderStatus.COMPLETED ||
                        order.getStatus() == OrderStatus.DELIVERED ||
                        order.getStatus() == OrderStatus.PROCESSING) {
                    revenue = revenue.add(order.getTotalAmount());
                }
            }

            return revenue;
        } catch (Exception e) {
            System.err.println("Error calculating total revenue: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public List<Order> getRecentOrders(int count) {
        try {
            List<Order> allOrders = orderRepository.findAllByOrderByOrderDateDesc();
            return allOrders.stream()
                    .limit(count)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting recent orders: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Long getPendingOrdersCount() {
        try {
            List<Order> pendingOrders = orderRepository.findByStatusOrderByOrderDateDesc(OrderStatus.PENDING);
            return (long) pendingOrders.size();
        } catch (Exception e) {
            System.err.println("Error getting pending orders count: " + e.getMessage());
            return 0L;
        }
    }

    public BigDecimal getMonthlyRevenue() {
        try {
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            List<Order> allOrders = orderRepository.findAll();

            BigDecimal monthlyRevenue = BigDecimal.ZERO;
            for (Order order : allOrders) {
                if (order.getOrderDate().isAfter(startOfMonth) &&
                        (order.getStatus() == OrderStatus.COMPLETED ||
                                order.getStatus() == OrderStatus.DELIVERED ||
                                order.getStatus() == OrderStatus.PROCESSING)) {
                    monthlyRevenue = monthlyRevenue.add(order.getTotalAmount());
                }
            }

            return monthlyRevenue;
        } catch (Exception e) {
            System.err.println("Error calculating monthly revenue: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Order> allOrders = orderRepository.findAll();
            return allOrders.stream()
                    .filter(order -> !order.getOrderDate().isBefore(startDate) &&
                            !order.getOrderDate().isAfter(endDate))
                    .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting orders by date range: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void deleteOrder(Long id) {
        try {
            if (orderRepository.existsById(id)) {
                orderRepository.deleteById(id);
                System.out.println("Order " + id + " deleted successfully");
            } else {
                throw new IllegalArgumentException("Заказ с ID " + id + " не найден");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении заказа: " + e.getMessage(), e);
        }
    }

    public Order updateOrder(Order order) {
        try {
            if (!orderRepository.existsById(order.getId())) {
                throw new IllegalArgumentException("Заказ с ID " + order.getId() + " не найден");
            }
            return orderRepository.save(order);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при обновлении заказа: " + e.getMessage(), e);
        }
    }

    public boolean orderExists(Long id) {
        try {
            return orderRepository.existsById(id);
        } catch (Exception e) {
            System.err.println("Error checking if order exists: " + e.getMessage());
            return false;
        }
    }

    public List<Order> searchOrders(String searchTerm) {
        try {
            List<Order> allOrders = orderRepository.findAll();
            return allOrders.stream()
                    .filter(order ->
                            String.valueOf(order.getId()).contains(searchTerm) ||
                                    order.getCustomerName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                    order.getCustomerEmail().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                    order.getCustomerAddress().toLowerCase().contains(searchTerm.toLowerCase()))
                    .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error searching orders: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public OrderStatus getOrderStatus(Long id) {
        try {
            Order order = getOrderById(id);
            return order.getStatus();
        } catch (Exception e) {
            System.err.println("Error getting order status: " + e.getMessage());
            return OrderStatus.PENDING;
        }
    }

    public int getTotalItemsInOrder(Long orderId) {
        try {
            Order order = getOrderById(orderId);
            return order.getItems().stream()
                    .mapToInt(OrderItem::getQuantity)
                    .sum();
        } catch (Exception e) {
            System.err.println("Error getting total items in order: " + e.getMessage());
            return 0;
        }
    }

    public List<Order> getTodayOrders() {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

            return getOrdersByDateRange(startOfDay, endOfDay);
        } catch (Exception e) {
            System.err.println("Error getting today's orders: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}