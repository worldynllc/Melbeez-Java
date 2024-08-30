package com.mlbeez.feeder.model;


import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class UpdateWarrantyRequest {
    private String vendor;
    private String warrantyId;
    private String name;
    private Float monthlyPrice;
    private String status;
    private Float annualPrice;
    private Integer discount;
    private String planName;
    private String product_price_ids;
    private String other_Details;
    private String planDescription;
    private String updated_by;
}
