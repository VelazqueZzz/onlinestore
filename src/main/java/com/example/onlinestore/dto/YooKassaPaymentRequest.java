package com.example.onlinestore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class YooKassaPaymentRequest {
    @JsonProperty("amount")
    private Amount amount;

    @JsonProperty("confirmation")
    private Confirmation confirmation;

    @JsonProperty("capture")
    private boolean capture = true;

    @JsonProperty("description")
    private String description;

    @JsonProperty("metadata")
    private Metadata metadata;

    // Конструкторы
    public YooKassaPaymentRequest() {}

    public YooKassaPaymentRequest(BigDecimal value, String currency, String description,
                                  String returnUrl, String confirmationType, Long orderId) {
        this.amount = new Amount(value, currency);
        this.confirmation = new Confirmation(confirmationType, returnUrl);
        this.description = description;
        this.metadata = new Metadata(orderId);
    }

    // Геттеры и сеттеры
    public Amount getAmount() { return amount; }
    public void setAmount(Amount amount) { this.amount = amount; }

    public Confirmation getConfirmation() { return confirmation; }
    public void setConfirmation(Confirmation confirmation) { this.confirmation = confirmation; }

    public boolean isCapture() { return capture; }
    public void setCapture(boolean capture) { this.capture = capture; }

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

        public Amount() {}

        public Amount(BigDecimal value, String currency) {
            // ЮKassa требует строковое представление с двумя знаками после запятой
            this.value = value.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            this.currency = currency;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class Confirmation {
        @JsonProperty("type")
        private String type;

        @JsonProperty("return_url")
        private String returnUrl;

        public Confirmation() {}

        public Confirmation(String type, String returnUrl) {
            this.type = type;
            this.returnUrl = returnUrl;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    }

    public static class Metadata {
        @JsonProperty("order_id")
        private Long orderId;

        public Metadata() {}

        public Metadata(Long orderId) {
            this.orderId = orderId;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }
}