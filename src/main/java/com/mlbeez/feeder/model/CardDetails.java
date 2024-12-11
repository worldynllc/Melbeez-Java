package com.mlbeez.feeder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;


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

    private String userId;

    private String funding;

    private String card_Last4;

    private String customer;

    private String type;

    @Getter
    @CreationTimestamp
    @Column(name = "created_at",columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ssXXX")
    private ZonedDateTime createdAt;

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
