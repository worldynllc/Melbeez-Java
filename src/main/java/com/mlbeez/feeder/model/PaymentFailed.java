package com.mlbeez.feeder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payments_failed")
@Getter
@Setter
public class PaymentFailed {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "bigint")

    private Long id;

    private String email;

    private String name;

    private String customer;

    private String failure_code;

    private String reason;

    private String status;
}
