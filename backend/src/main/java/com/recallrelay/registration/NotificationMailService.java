package com.recallrelay.registration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationMailService {
    private static final Logger log = LoggerFactory.getLogger(NotificationMailService.class);
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String from;

    public NotificationMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:no-reply@recallrelay.local}") String from
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailEnabled = mailEnabled;
        this.from = from;
    }

    public void sendVerificationCode(String to, String code) {
        send(to, "RecallRelay company email verification", "Your RecallRelay verification code is: " + code + "\n\nThis code expires in 10 minutes.");
    }

    public void sendPasswordReset(String to, String resetUrl) {
        send(to, "RecallRelay password reset", "Use this one-time link to reset your RecallRelay password:\n" + resetUrl + "\n\nThe link expires in 30 minutes.");
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("MAIL_DISABLED recipient={} subject={} body={}", to, subject, body.replace('\n', ' '));
            return;
        }
        if (mailSender == null) throw new IllegalStateException("Mail is enabled but JavaMailSender is unavailable.");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
