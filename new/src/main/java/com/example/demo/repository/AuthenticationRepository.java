package com.example.demo.repository;

import com.example.demo.entity.Account;

import org.springframework.data.jpa.repository.JpaRepository;



public interface AuthenticationRepository extends JpaRepository<Account, Long>
{   // dua ra daatabase
    Account findByEmail(String email);


    Account findByVerificationCode(String code);

}
