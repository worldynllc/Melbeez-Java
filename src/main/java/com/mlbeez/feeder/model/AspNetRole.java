package com.mlbeez.feeder.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "\"AspNetRoles\"",schema = "public")
@Getter
@Setter
public class AspNetRole {

    @Id
    @Column(name = "\"Id\"")
    private String id;

    @Column(name = "\"Name\"")
    private String name;

    @Column(name = "\"NormalizedName\"")
    private String normalizedName;

}
