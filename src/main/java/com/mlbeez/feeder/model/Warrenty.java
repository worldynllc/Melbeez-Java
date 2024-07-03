package com.mlbeez.feeder.model;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "warrenty")
public class Warrenty {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Use GenerationType.IDENTITY for auto-generated IDs
    @Column(columnDefinition = "bigint")
    private Long id;

    private String vendor;
    private String productName;
    private Float monthlyPrice;
    private Float annualPrice;

//    private String img;
//
//    private String img_Link;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

//    public String getImg_Link() {
//        return img_Link;
//    }
//
//    public void setImg_Link(String img_Link) {
//        this.img_Link = img_Link;
//    }

    private int discount;
//    private String picture;

    private String terms_conditions;

    private String created_by;

    private String updated_by;

    @CreationTimestamp
    private LocalDateTime createdAt;


//    public String getImg() {
//        return img;
//    }
//
//    public void setImg(String img) {
//        this.img = img;
//    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Float getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(Float monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public Float getAnnualPrice() {
        return annualPrice;
    }

    public void setAnnualPrice(Float annualPrice) {
        this.annualPrice = annualPrice;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

//    public String getPicture() {
//        return picture;
//    }
//
//    public void setPicture(String picture) {
//        this.picture = picture;
//    }

    public String getTerms_conditions() {
        return terms_conditions;
    }

    public void setTerms_conditions(String terms_conditions) {
        this.terms_conditions = terms_conditions;
    }

    public String getCreated_by() {
        return created_by;
    }

    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    public String getUpdated_by() {
        return updated_by;
    }

    public void setUpdated_by(String updated_by) {
        this.updated_by = updated_by;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


}
