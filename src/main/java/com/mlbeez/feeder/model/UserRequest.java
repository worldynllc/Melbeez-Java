package com.mlbeez.feeder.model;

//import java.text.SimpleDateFormat;
//import java.util.Date;
import java.util.List;
import java.util.UUID;

public class UserRequest {

    private List<User> users;

    // Getters and Setters
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

//        public String getAlt_email() {
//            return alt_email;
//        }
//
//        public void setAlt_email(String alt_email) {
//            this.alt_email = alt_email;
//        }
//
//        public String getProperty_type() {
//            return property_type;
//        }
//
//        public void setProperty_type(String property_type) {
//            this.property_type = property_type;
//        }
//
//        public String getRes_type() {
//            return res_type;
//        }
//
//        public void setRes_type(String res_type) {
//            this.res_type = res_type;
//        }

        private String first_name;
        private String last_name;
//        private String birthday;
//        private String alt_email;
        private String address;
        private String city;
//        private String street;
        private String zip;
//        private String apt;
        private String state;
//        private String property_type;
//        private String res_type;

        // Getters and Setters

//
//        public String getBirthday() {
//            return birthday;
//        }
//
//        public void setBirthday(String birthday) {
//            try {
//                SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM dd, yyyy");
//                Date date = inputFormat.parse(birthday);
//                SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy");
//                this.birthday = outputFormat.format(date);
//            } catch (Exception e) {
//                // Handle parsing error
//                this.birthday = birthday;  // Fallback to the original value if parsing fails
//            }
//        }


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

//        public String getStreet() {
//            return street;
//        }
//
//        public void setStreet(String street) {
//            this.street = street;
//        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }

//        public String getApt() {
//            return apt;
//        }
//
//        public void setApt(String apt) {
//            this.apt = apt;
//        }
//
        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

    }
}