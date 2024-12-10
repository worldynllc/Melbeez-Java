package com.mlbeez.feeder.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_History")
@Getter
@Setter
public class Transactions {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "bigint")

    private Long id;

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

    private String phoneNumber;

    private String customerId;

    @CreationTimestamp
    @Column(name = "created_at",columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String chargeRequest_status;

    private String invoice_status;
    public TransactionDto toLogDTO() {

        TransactionDto dto = new TransactionDto();
        dto.setUserId(this.userId);
        dto.setProductName(this.productName);
        dto.setProductId(this.productId);
        dto.setPrice(this.price);
        dto.setTransactionId(this.transactionId);
        dto.setCard(this.card);
        dto.setEmail(this.email);
        dto.setPaymentMethod(this.paymentMethod);
        dto.setInterval(this.interval);
        dto.setReceiptUrl(this.receiptUrl);
        dto.setCustomerId(this.customerId);
        dto.setCreatedAt(this.createdAt);
        dto.setChargeRequestStatus(this.chargeRequest_status);
        dto.setInvoiceStatus(this.invoice_status);
        return dto;
    }
}
