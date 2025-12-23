package com.mlbeez.feeder.model;


import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class UpdateWarrantyRequest {
    private String vendor;
    private String warrantyId;
    private String name;
    private String monthlyPrice;
    private String status;
    private String annualPrice;
    private Integer discount;
    private String planName;
    @Column(name = "product_monthly_price_ids")
    private String productMonthlyPriceIds;
    @Column(name = "product_yearly_price_ids")
    private String productYearlyPriceIds;
    private String other_Details;
    private String planDescription;
    private String updated_by;
}
