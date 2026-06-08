package com.example.platform.user.service;

import com.example.platform.user.domain.VerificationCode;

public interface UserVerificationCodeSender {

    void send(VerificationCode verificationCode);
}
