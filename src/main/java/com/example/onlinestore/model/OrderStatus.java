package com.example.onlinestore.model;

public enum OrderStatus {
    PENDING("Ожидает обработки"),
    PROCESSING("В обработке"),
    COMPLETED("Завершен"),
    CANCELLED("Отменен"),
    SHIPPED("Отправлен"),
    DELIVERED("Доставлен");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromString(String text) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}