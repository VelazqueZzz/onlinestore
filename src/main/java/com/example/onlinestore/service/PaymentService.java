package com.example.onlinestore.service;

import com.example.onlinestore.dto.YooKassaPaymentRequest;
import com.example.onlinestore.dto.YooKassaPaymentResponse;
import com.example.onlinestore.model.Order;
import com.example.onlinestore.model.Payment;
import com.example.onlinestore.model.PaymentStatus; // Добавляем импорт
import com.example.onlinestore.model.OrderStatus; // Добавляем импорт
import com.example.onlinestore.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    @Value("${yookassa.shop-id:test_shop_id}")
    private String shopId;

    @Value("${yookassa.secret-key:test_secret_key}")
    private String secretKey;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final WebClient webClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    public PaymentService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.yookassa.ru/v3")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Payment createPayment(Order order) {
        try {
            // Проверяем, не существует ли уже платеж для этого заказа
            if (paymentRepository.existsByOrderId(order.getId())) {
                throw new IllegalStateException("Платеж для этого заказа уже существует");
            }

            // Создаем запрос к ЮKassa
            YooKassaPaymentRequest request = new YooKassaPaymentRequest(
                    order.getTotalAmount(),
                    "RUB",
                    "Оплата заказа №" + order.getId(),
                    baseUrl + "/payment/success",
                    "redirect",
                    order.getId()
            );

            // Вызываем API ЮKassa
            YooKassaPaymentResponse response = webClient.post()
                    .uri("/payments")
                    .header(HttpHeaders.AUTHORIZATION, getAuthHeader())
                    .header("Idempotence-Key", generateIdempotenceKey())
                    .body(Mono.just(request), YooKassaPaymentRequest.class)
                    .retrieve()
                    .bodyToMono(YooKassaPaymentResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Не удалось создать платеж в ЮKassa");
            }

            // Сохраняем платеж в БД
            Payment payment = new Payment(order, order.getTotalAmount(),
                    "Оплата заказа №" + order.getId());
            payment.setPaymentId(response.getId());
            payment.setConfirmationUrl(response.getConfirmation().getConfirmationUrl());
            payment.setStatus(PaymentStatus.PENDING);

            return paymentRepository.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания платежа: " + e.getMessage(), e);
        }
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElse(null);
    }

    public Payment getPaymentByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Платеж не найден: " + paymentId));
    }

    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        Payment payment = getPaymentByPaymentId(paymentId);
        payment.setStatus(status);
        paymentRepository.save(payment);

        // Обновляем статус заказа при успешной оплате
        if (status == PaymentStatus.SUCCEEDED) {
            Order order = payment.getOrder();
            orderService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        }
    }

    public boolean isPaymentCompleted(Long orderId) {
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        return payment.isPresent() && payment.get().getStatus() == PaymentStatus.SUCCEEDED;
    }

    public boolean isPaymentPending(Long orderId) {
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        return payment.isPresent() &&
                (payment.get().getStatus() == PaymentStatus.PENDING ||
                        payment.get().getStatus() == PaymentStatus.WAITING_FOR_CAPTURE);
    }

    private String getAuthHeader() {
        String auth = shopId + ":" + secretKey;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    private String generateIdempotenceKey() {
        return UUID.randomUUID().toString();
    }
}