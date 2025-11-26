package com.example.onlinestore.controller;

import com.example.onlinestore.dto.YooKassaWebhookRequest;
import com.example.onlinestore.model.Order;
import com.example.onlinestore.model.Payment;
import com.example.onlinestore.model.PaymentStatus; // Добавляем импорт
import com.example.onlinestore.service.OrderService;
import com.example.onlinestore.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // Инициирование платежа
    @PostMapping("/create/{orderId}")
    public String createPayment(@PathVariable Long orderId,
                                RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.getOrderById(orderId);
            Payment payment = paymentService.createPayment(order);

            // Перенаправляем на страницу оплаты ЮKassa
            return "redirect:" + payment.getConfirmationUrl();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании платежа: " + e.getMessage());
            return "redirect:/order/success/" + orderId;
        }
    }

    // Страница успешной оплаты (callback от ЮKassa)
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam(required = false) String paymentId,
                                 Model model) {
        try {
            if (paymentId != null) {
                paymentService.updatePaymentStatus(paymentId, PaymentStatus.SUCCEEDED);
                model.addAttribute("paymentId", paymentId);
            }

            model.addAttribute("successMessage", "Оплата прошла успешно! Ваш заказ подтвержден.");
            return "payment/success";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Ошибка при обработке платежа: " + e.getMessage());
            return "payment/error";
        }
    }

    // Страница отмены оплаты
    @GetMapping("/cancel/{orderId}")
    public String paymentCancel(@PathVariable Long orderId, Model model) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment != null) {
            paymentService.updatePaymentStatus(payment.getPaymentId(), PaymentStatus.CANCELED);
        }

        model.addAttribute("orderId", orderId);
        model.addAttribute("message", "Оплата отменена. Вы можете попробовать снова.");
        return "payment/cancel";
    }

    // Вебхук для уведомлений от ЮKassa
    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(@RequestBody YooKassaWebhookRequest webhookRequest) {
        try {
            String paymentId = webhookRequest.getObject().getId();
            String status = webhookRequest.getObject().getStatus();

            // Обновляем статус платежа в зависимости от уведомления
            switch (status) {
                case "succeeded":
                    paymentService.updatePaymentStatus(paymentId, PaymentStatus.SUCCEEDED);
                    break;
                case "canceled":
                    paymentService.updatePaymentStatus(paymentId, PaymentStatus.CANCELED);
                    break;
                case "waiting_for_capture":
                    paymentService.updatePaymentStatus(paymentId, PaymentStatus.WAITING_FOR_CAPTURE);
                    break;
                default:
                    // Для других статусов можно добавить обработку
                    break;
            }

            return ResponseEntity.ok().body("OK");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Проверка статуса платежа
    @GetMapping("/status/{orderId}")
    @ResponseBody
    public PaymentStatus getPaymentStatus(@PathVariable Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return payment != null ? payment.getStatus() : PaymentStatus.PENDING;
    }

    // Страница информации о платеже
    @GetMapping("/info/{orderId}")
    public String paymentInfo(@PathVariable Long orderId, Model model) {
        try {
            Order order = orderService.getOrderById(orderId);
            Payment payment = paymentService.getPaymentByOrderId(orderId);

            model.addAttribute("order", order);
            model.addAttribute("payment", payment);
            model.addAttribute("cartItemsCount", 0); // Корзина пуста после заказа

            return "payment/info";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Ошибка: " + e.getMessage());
            return "redirect:/order/admin";
        }
    }
}