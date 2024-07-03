package com.mlbeez.feeder.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWarrentyRequest {
    private String vendor;
    private String productName;
    private Float monthlyPrice;
    private Float annualPrice;
    private Integer discount;
    private String termsConditions;
    private String createdBy;
    private String updatedBy;
}
