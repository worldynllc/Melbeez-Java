package com.mlbeez.feeder.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "\"AspNetUserRoles\"", schema = "public")
@Getter
@Setter
public class AspNetUserRole {

    @Id
    @Column(name = "\"UserId\"")
    private String userId;

    @Column(name = "\"RoleId\"")
    private String roleId;

}
