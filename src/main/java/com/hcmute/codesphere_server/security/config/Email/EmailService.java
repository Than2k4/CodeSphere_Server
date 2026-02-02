package com.hcmute.codesphere_server.security.config.Email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email text đơn giản (không HTML, không file đính kèm)
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }

    /**
     * Gửi email HTML (ví dụ template đẹp, có tag <h1>, <p>, ...)
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email HTML", e);
        }
    }

    /**
     * Hàm gửi email custom, nếu bạn muốn tự tạo MimeMessage bên ngoài rồi đưa vào
     */
    @Async
    public void sendEmail(MimeMessage email) {
        mailSender.send(email);
    }
}
