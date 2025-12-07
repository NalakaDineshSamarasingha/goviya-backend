package com.nalaka.goviya.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private String password;
    private String provice;
    private String district;
    private String city;
    private String phone;
    private String optionalPhone;
    private String[] harvestTypes;
    private double harvestArea;

    public User() {
    }

    public User(String firstName, String lastName, String email, String password, String provice, String district, String city, String phone, String optionalPhone,String[] harvestTypes, double harvestArea){
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.provice = provice;
        this.district = district;
        this.city = city;
        this.phone = phone;
        this.optionalPhone = optionalPhone;
        this.harvestArea = harvestArea;
        this.harvestTypes = harvestTypes;
    }

    public User(String firstName, String lastName, String email, String password, String provice, String district, String city, String phone,String[] harvestTypes, double harvestArea){
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.provice = provice;
        this.district = district;
        this.city = city;
        this.phone = phone;
        this.harvestArea = harvestArea;
        this.harvestTypes = harvestTypes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProvice() { return provice; }
    public void setProvice(String provice) { this.provice = provice; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOptionalPhone() { return optionalPhone; }
    public void setOptionalPhone(String optionalPhone) { this.optionalPhone = optionalPhone; }

    public String[] getHarvestTypes() { return harvestTypes; }
    public void setHarvestTypes(String[] harvestTypes) { this.harvestTypes = harvestTypes; }

    public double getHarvestArea() { return harvestArea; }
    public void setHarvestArea(double harvestArea) { this.harvestArea = harvestArea; }
}
