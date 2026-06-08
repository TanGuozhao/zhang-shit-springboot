package com.example.platform.user.service;

import com.example.platform.user.domain.VerificationCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingUserVerificationCodeSender implements UserVerificationCodeSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingUserVerificationCodeSender.class);

    @Override
    public void send(VerificationCode verificationCode) {
        if (verificationCode == null) {
            return;
        }
        log.info(
                "placeholder verification delivery, scene={}, account={}, contact={}, code={}, expiresAt={}",
                verificationCode.scene(),
                verificationCode.account(),
                verificationCode.contact(),
                verificationCode.code(),
                verificationCode.expiresAt()
        );
    }
}
