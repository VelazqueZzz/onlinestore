package com.example.onlinestore.model;

public enum PaymentStatus {
    PENDING("Ожидает оплаты"),
    WAITING_FOR_CAPTURE("Ожидает подтверждения"),
    SUCCEEDED("Оплачен"),
    CANCELED("Отменен");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentStatus fromString(String text) {
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
