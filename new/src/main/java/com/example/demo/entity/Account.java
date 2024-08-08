package com.example.demo.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;



@Entity
@Data
public class Account {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   long id;
   String name;

   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   String password;

   @Column(unique = true)
   String phone;

   @Column(unique = true)
   String email;



   private boolean enable;

   private String verificationCode;

}
