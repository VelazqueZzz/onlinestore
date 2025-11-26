package com.example.onlinestore.repository;

import com.example.onlinestore.model.Order;
import com.example.onlinestore.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Найти все заказы, отсортированные по дате (новые сначала)
    List<Order> findAllByOrderByOrderDateDesc();

    // Найти заказы по email клиента
    List<Order> findByCustomerEmailOrderByOrderDateDesc(String customerEmail);

    // Найти заказы по статусу
    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status);

    // Найти последние N заказов
    List<Order> findTop5ByOrderByOrderDateDesc();

    // Найти заказы за определенный период
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Получить общее количество заказов по статусу
    Long countByStatus(OrderStatus status);
}