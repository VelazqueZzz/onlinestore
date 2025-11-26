package com.example.onlinestore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YooKassaWebhookRequest {
    @JsonProperty("type")
    private String type;

    @JsonProperty("event")
    private String event;

    @JsonProperty("object")
    private PaymentObject object;

    // Геттеры и сеттеры
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public PaymentObject getObject() { return object; }
    public void setObject(PaymentObject object) { this.object = object; }

    public static class PaymentObject {
        @JsonProperty("id")
        private String id;

        @JsonProperty("status")
        private String status;

        @JsonProperty("amount")
        private Amount amount;

        @JsonProperty("description")
        private String description;

        @JsonProperty("metadata")
        private Metadata metadata;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Amount getAmount() { return amount; }
        public void setAmount(Amount amount) { this.amount = amount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Metadata getMetadata() { return metadata; }
        public void setMetadata(Metadata metadata) { this.metadata = metadata; }
    }

    public static class Amount {
        @JsonProperty("value")
        private String value;

        @JsonProperty("currency")
        private String currency;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class Metadata {
        @JsonProperty("order_id")
        private Long orderId;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }
}