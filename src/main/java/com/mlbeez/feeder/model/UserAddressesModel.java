package com.mlbeez.feeder.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.mlbeez.feeder.config.AddressTypeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "\"Addresses\"", schema = "public")
@Getter
@Setter
public class UserAddressesModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"Id\"")
    private Long id;

    @Column(name = "\"AddressLine1\"")
    private String addressLine1;

    @Column(name = "\"AddressLine2\"")
    private String addressLine2;

    @Column(name = "\"CityName\"")
    private String cityName;

    @Column(name = "\"CreatedBy\"", insertable = false, updatable = false) // Prevent duplicate mapping
    private String createdBy;

    @Column(name = "\"ZipCode\"")
    private String zipCode;

    @Column(name = "\"StateName\"")
    private String stateName;

    @Column(name = "\"CountryName\"")
    private String countryName;


    @Convert(converter = AddressTypeConverter.class)
    @Column(name = "\"TypeOfProperty\"")
    private AddressType typeOfProperty;

    @Column(name = "\"IsDefault\"")
    private boolean isDefault;

    @Column(name = "\"IsSameMailingAddress\"")
    private boolean isSameMailingAddress;

    // Many-to-one relationship with UserResponseBaseModel
    @ManyToOne
    @JoinColumn(name = "\"CreatedBy\"", referencedColumnName = "\"Id\"")
    @JsonBackReference
    private UserResponseBaseModel user;
}