package com.mlbeez.feeder.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.ZonedDateTime;

@Entity
@Table(name ="InsurancePayments")
@Getter
@Setter
public class InsurancePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "bigint")
    private Long id;

    private String subscriptionId;

    private Long amount;


    private String currency;

    private String invoiceId    ;

    private String customer;

    private String productId;

    private String userId;

    private String email;

    private String name;

    private String phoneNumber;

    private String warrantyId;

    private String mode;

    private String subscriptionMode;

    private String default_payment_method;

    private String chargeRequest_status;

    private String subscription_Status;

    private String invoice_status;



    @CreationTimestamp
    @Column(name = "created_at",columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ssXXX")
    private ZonedDateTime createdAt;


}