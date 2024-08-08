package com.example.demo.service;

import com.example.demo.entity.Account;

import com.example.demo.exception.AuthException;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.GlobalException;

import com.example.demo.model.EmailDetail;
import com.example.demo.model.Request.*;
import com.example.demo.model.Response.AccountResponse;
import com.example.demo.repository.AuthenticationRepository;
import com.example.demo.utils.AccountUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class AuthenticationService {

    @Autowired
    private AuthenticationRepository authenticationRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;


    @Autowired
    AccountUtils accountUtils;


    private static final Logger logger = Logger.getLogger(AuthenticationService.class.getName());

    @Transactional
    public Account register(RegisterRequest registerRequest) {
        Account account = new Account();
        account.setName(registerRequest.getName());
        account.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        account.setPhone(registerRequest.getPhone());
        account.setEmail(registerRequest.getEmail());

        account.setEnable(false);
        account.setVerificationCode(UUID.randomUUID().toString());

        authenticationRepository.save(account);

        try {
            account = authenticationRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            throw new AuthException("Duplicate");
        }

//        EmailDetail emailDetail = new EmailDetail();
//        emailDetail.setRecipient(registerRequest.getEmail());
//        emailDetail.setSubject("Verify your registration");
//        emailDetail.setName(registerRequest.getName());
//        String verifyURL = "http://booking88.online/api/verify?code="+account.getVerificationCode();
//        emailDetail.setLink(verifyURL);
//        emailDetail.setButtonValue("Verify Email");
//        emailService.sendMailTemplate(emailDetail);
        return account;
    }

    public boolean verify(String verificationCode) {
        Account account = authenticationRepository.findByVerificationCode(verificationCode);
        if (account == null || account.isEnable()) {
            return false;
        } else {
            account.setEnable(true);
//            account.setStatus(AccoutStatus.ACTIVE);
            authenticationRepository.save(account);
            return true;
        }
    }

    public AccountResponse login(LoginRequest loginRequest) {
        var account = authenticationRepository.findByEmail(loginRequest.getEmail());
        if (account == null) {
            throw new AuthException("Account not found with email: " + loginRequest.getEmail());
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), account.getPassword())) {
            throw new AuthException("Wrong Id Or Password");
        }

//        if (account.getStatus().equals(AccoutStatus.DELETED)) {
//            throw new AuthException("Account deleted");
//        }

        // Check if the account is verified
        if (!account.isEnable()) {
            throw new AuthException("Account not verified. Please check your email to verify your account.");
        }

        String token = tokenService.generateToken(account);
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getId());
        accountResponse.setEmail(account.getEmail());
        accountResponse.setToken(token);

        accountResponse.setName(account.getName());
        accountResponse.setPhone(account.getPhone());


        return accountResponse;
    }


    public AccountResponse loginGoogle(LoginGoogleRequest loginGoogleRequest) {
        AccountResponse accountResponse = new AccountResponse();
        try {
            FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(loginGoogleRequest.getToken());
            String email = firebaseToken.getEmail();
            Account account = authenticationRepository.findByEmail(email);

            if (account == null) {
                account = new Account();
                account.setName(firebaseToken.getName());
                account.setEmail(firebaseToken.getEmail());


                account = authenticationRepository.save(account);

            }


            accountResponse.setId(account.getId());
            accountResponse.setName(account.getName());
            accountResponse.setEmail(account.getEmail());

            String token = tokenService.generateToken(account);
            accountResponse.setToken(token);

        } catch (FirebaseAuthException e) {
            logger.severe("Firebase authentication error: " + e.getMessage());
            throw new BadRequestException("Invalid Google token");
        }

        return accountResponse;
    }






    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        Account account = authenticationRepository.findByEmail(forgotPasswordRequest.getEmail());
        if (account == null) {
            throw new BadRequestException("Account not found");
        }

        EmailDetail emailDetail = new EmailDetail();
        emailDetail.setRecipient(forgotPasswordRequest.getEmail());
        emailDetail.setSubject("Reset Password for account " + forgotPasswordRequest.getEmail() + "!!!");
        emailDetail.setMsgBody(""); // You might want to add a meaningful message here
        emailDetail.setButtonValue("Reset Password");
        emailDetail.setLink("http://booking88.online/reset-password?token=" + tokenService.generateToken(account));
        emailDetail.setName(account.getName());

        Runnable r = new Runnable() {
            @Override
            public void run() {
                emailService.sendMailTemplateForgot(emailDetail);
            }
        };

        new Thread(r).start();
    }


    public int resetPassword(ResetPasswordRequest resetPasswordRequest) {
        Account user = authenticationRepository.findByEmail(resetPasswordRequest.getEmail());

        if (user == null) {
            throw new GlobalException("Not found email");
        }
        String token = tokenService.generateToken(user);
        // Check if the token matches
        if (!token.equals(resetPasswordRequest.getToken())) {
            throw new GlobalException("Invalid token");
        }else {
            user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
            authenticationRepository.save(user);
            return 1;
        }

    }
//    public Account deleteAccount(long id) {
//        Account account = authenticationRepository.findById(id).orElseThrow(() -> new AuthException("Can not find account"));;
//        account.setStatus(AccoutStatus.DELETED);
//        return authenticationRepository.save(account);
//    }


    public Account findById(Long id) {
        Account account = authenticationRepository.findById(id).orElse(null);
        if (account == null) {
            throw new RuntimeException("Account not found with id: " + id);
        }
        return account;
    }
}
