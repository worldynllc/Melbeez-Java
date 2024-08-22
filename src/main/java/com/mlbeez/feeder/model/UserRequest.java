package com.mlbeez.feeder.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserRequest {

    private String email;
    private String phone;
    private boolean isPrimary;
    private List<UUID> productPriceIds;
    private Profile profile;

    @Getter
    @Setter
    public static class Profile {
        private String firstName;
        private String lastName;
        private String birthday;
        private String altEmail;
        private String address;
        private String city;
        private String street;
        private String zip;
        private String apt;
        private String state;
        private String propertyType;
        private String resType;
    }
}
