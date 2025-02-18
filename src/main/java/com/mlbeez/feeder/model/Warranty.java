package com.mlbeez.feeder.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.hateoas.RepresentationModel;
import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;


@Entity
@Table(name = "warranty")
@Getter
@Setter
public class Warranty extends RepresentationModel<Warranty> implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "bigint")
    private Long id;

    private String userId;

    @Serial
    private static final long serialVersionUID = 1L;

    private String product_price_ids;

    public void setProduct_price_ids(String product_price_ids) {
        this.product_price_ids = product_price_ids;
    }

    public void setOther_Details(String other_Details) {
        this.other_Details = other_Details;
    }

    private String other_Details;

    private String vendor;

    @Column(unique = true)
    private String warrantyId;
    private String name;
    private String monthlyPrice;
    private String annualPrice;


    private String productId;

    public void setProductId(String productId) {
        this.productId = productId;
    }

    private String planName;

    private String planDescription;

    @PrePersist
    public void prePersist() {
        if (this.warrantyId == null) {
            this.warrantyId = generateWarrantyId();
        }
    }

    private String generateWarrantyId() {
        String uuid = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        return uuid.length() >= 8 ? uuid.substring(0, 8) : String.format("%08d", Integer.parseInt(uuid));
    }

    public void setWarrantyId(String warrantyId) {
        this.warrantyId = warrantyId;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    private String status = "Pending";

    private String picture;

    private String pictureLink;

    @UpdateTimestamp
    @Column(name = "updated_at",columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ssXXX")
    private ZonedDateTime updatedAt;

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public void setPlanDescription(String planDescription) {
        this.planDescription = planDescription;
    }

    public void setPictureName(String pictureName) {
        this.pictureName = pictureName;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPictureLink(String pictureLink) {
        this.pictureLink = pictureLink;
    }

    private String discount;
    private String pictureName;


    private String created_by;

    private String updated_by;

    @CreationTimestamp
    @Column(name = "created_at",columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ssXXX")
    private ZonedDateTime createdAt;


    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMonthlyPrice(String monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public void setAnnualPrice(String annualPrice) {
        this.annualPrice = annualPrice;
    }

    public void setDiscount(String discount) {
        this.discount = discount;
    }


    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    public void setUpdated_by(String updated_by) {
        this.updated_by = updated_by;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
