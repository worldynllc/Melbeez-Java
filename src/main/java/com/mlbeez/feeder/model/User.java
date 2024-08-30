package com.mlbeez.feeder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "bigint")
    private Long id;

    private String userId;

    private String customerId;

    private String firstName;

    private String lastName;
    private String cityName;

    private String stateName;
    private String zipCode;

    private String addressLine1;
    private String phoneNumber;

    private String email;

    private String userName;


}
