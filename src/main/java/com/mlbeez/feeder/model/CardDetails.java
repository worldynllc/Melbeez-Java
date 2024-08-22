package com.mlbeez.feeder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "card_details")
@Getter
@Setter
public class CardDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "bigint")
    private Long id;

    private String payment_methodId;

    private String email;

    private String name;

    private String cardBrand;

    private String country;

    private Long exp_month;

    private Long exp_year;

    private String funding;

    private String card_Last4;

    private String customer;

    private String type;

}
