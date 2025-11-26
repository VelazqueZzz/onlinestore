package com.example.onlinestore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YooKassaPaymentResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("amount")
    private Amount amount;

    @JsonProperty("confirmation")
    private Confirmation confirmation;

    @JsonProperty("description")
    private String description;

    @JsonProperty("metadata")
    private Metadata metadata;

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Amount getAmount() { return amount; }
    public void setAmount(Amount amount) { this.amount = amount; }

    public Confirmation getConfirmation() { return confirmation; }
    public void setConfirmation(Confirmation confirmation) { this.confirmation = confirmation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }

    // Вложенные классы
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

    public static class Confirmation {
        @JsonProperty("confirmation_url")
        private String confirmationUrl;

        @JsonProperty("type")
        private String type;

        public String getConfirmationUrl() { return confirmationUrl; }
        public void setConfirmationUrl(String confirmationUrl) { this.confirmationUrl = confirmationUrl; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class Metadata {
        @JsonProperty("order_id")
        private Long orderId;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }
}