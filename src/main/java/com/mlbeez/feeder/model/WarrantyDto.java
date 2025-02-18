package com.mlbeez.feeder.model;

import lombok.Getter;
import lombok.Setter;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;


@Getter
@Setter
public class WarrantyDto implements Serializable {

    private Long id;

    @Serial
    private static final long serialVersionUID = 1L;

    private String vendor;
    private String warrantyId;
    private String name;
    private String productId;
    private String monthlyPrice;
    private String status;
    private String annualPrice;
    private Integer discount;
    private String planName;
    private List<String> product_price_ids;
    private String other_Details;
    private String planDescription;
    private String updated_by;
    private String pictureLink;
}
