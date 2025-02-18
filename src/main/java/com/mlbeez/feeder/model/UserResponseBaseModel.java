package com.mlbeez.feeder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "\"AspNetUsers\"", schema = "public")
@Getter
@Setter
public class UserResponseBaseModel {
    @Id
    @Column(name = "\"Id\"")
    private String id;

    @Column(name = "\"UserName\"")
    private String username;

    @Column(name = "\"FirstName\"")
    private String firstname;

    @Column(name = "\"LastName\"")
    private String lastname;

    @Column(name = "\"Email\"")
    private String email;

    @Column(name = "\"EmailConfirmed\"")
    private boolean emailConfirmed;

    @Column(name = "\"PhoneNumber\"")
    private String phoneNumber;

    @Column(name = "\"PhoneNumberConfirmed\"")
    private boolean phoneNumberConfirmed;

    @Column(name = "\"PasswordHash\"")
    private String passwordHash;

    @Column(name = "\"LockoutEnd\"")
    private LocalDateTime lockoutEnd;

    @CreationTimestamp
    @Column(name = "\"CreatedDate\"")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    @Column(name = "\"IsDeleted\"")
    private boolean isDeleted;

    @Column(name = "\"IsUserBlockedByAdmin\"")
    private Boolean isUserBlockedByAdmin;

    @Column(name = "\"IsPermanentLockOut\"")
    private Boolean isPermanentLockOut;

    @Column(name = "\"IsVerifiedByAdmin\"")
    private Boolean isVerifiedByAdmin;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserAddressesModel> userAddresses;


}
