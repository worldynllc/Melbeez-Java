package com.mlbeez.feeder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionDto {

    private String userId;
    private String productName;
    private String productId;
    private Long price;
    private String transactionId;
    private String card;
    private String email;
    private String paymentMethod;
    private String interval;
    private String receiptUrl;
    private String customerId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String chargeRequestStatus;
    private String invoiceStatus;
}
