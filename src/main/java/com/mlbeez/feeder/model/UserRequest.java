package com.mlbeez.feeder.model;
import java.util.List;
import java.util.UUID;

public class UserRequest {

    private List<User> users;

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public static class User {


        public List<UUID> getProduct_price_ids() {
            return product_price_ids;
        }

        public void setProduct_price_ids(List<UUID> product_price_ids) {
            this.product_price_ids = product_price_ids;
        }

        private List<UUID> product_price_ids;
        private String phone;

        public boolean isIs_primary() {
            return is_primary;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        private String email;

        public void setIs_primary(boolean is_primary) {
            this.is_primary = is_primary;
        }

        private boolean is_primary;
        private Profile profile;


        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }


        public Profile getProfile() {
            return profile;
        }

        public void setProfile(Profile profile) {
            this.profile = profile;
        }
    }

    public static class Profile {

        public String getFirst_name() {
            return first_name;
        }

        public void setFirst_name(String first_name) {
            this.first_name = first_name;
        }

        public String getLast_name() {
            return last_name;
        }

        public void setLast_name(String last_name) {
            this.last_name = last_name;
        }

        private String first_name;
        private String last_name;

        private String address;
        private String city;

        private String zip;

        private String state;


        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

    }
}