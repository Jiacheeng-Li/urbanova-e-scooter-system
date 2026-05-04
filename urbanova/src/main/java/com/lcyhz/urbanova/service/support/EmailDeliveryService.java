package com.lcyhz.urbanova.service.support;

import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailDeliveryService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailOAuth2TokenService mailOAuth2TokenService;
    private final RestClient restClient;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from-address:}")
    private String fromAddress;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean startTlsEnabled;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean startTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.localhost:localhost}")
    private String smtpLocalhost;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:10000}")
    private int smtpConnectionTimeoutMs;

    @Value("${spring.mail.properties.mail.smtp.timeout:10000}")
    private int smtpReadTimeoutMs;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:10000}")
    private int smtpWriteTimeoutMs;

    @Value("${app.mail.oauth2.transport:smtp}")
    private String oauth2Transport;

    @Value("${app.mail.graph.send-mail-url:https://graph.microsoft.com/v1.0/me/sendMail}")
    private String graphSendMailUrl;

    @Value("${app.mail.graph.save-to-sent-items:true}")
    private boolean graphSaveToSentItems;

    public EmailDeliveryService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                MailOAuth2TokenService mailOAuth2TokenService) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailOAuth2TokenService = mailOAuth2TokenService;
        this.restClient = RestClient.builder().build();
    }

    public void sendRegistrationVerificationCode(String email, String code, LocalDateTime expiresAt) {
        if (mailOAuth2TokenService.isEnabled()) {
            if ("graph".equalsIgnoreCase(oauth2Transport)) {
                sendRegistrationVerificationCodeViaGraph(email, code, expiresAt);
            } else {
                sendRegistrationVerificationCodeViaOAuth2(email, code, expiresAt);
            }
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || isBlank(mailHost) || isBlank(fromAddress)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_NOT_CONFIGURED,
                    "Email delivery is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Urbanova registration verification code");
        message.setText("""
                Your Urbanova registration verification code is %s.

                The code expires at %s.

                If you did not request this code, you can ignore this email.
                """.formatted(code, expiresAt));
        mailSender.send(message);
    }

    private void sendRegistrationVerificationCodeViaOAuth2(String email, String code, LocalDateTime expiresAt) {
        if (isBlank(mailHost) || isBlank(fromAddress) || isBlank(mailUsername)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_NOT_CONFIGURED,
                    "Outlook OAuth2 email delivery is missing SMTP host, username, or from address");
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.host", mailHost);
        properties.put("mail.smtp.port", String.valueOf(mailPort));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        properties.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));
        properties.put("mail.smtp.starttls.required", String.valueOf(startTlsRequired));
        properties.put("mail.smtp.ssl.checkserveridentity", "true");
        properties.put("mail.smtp.localhost", smtpLocalhost);
        properties.put("mail.smtp.connectiontimeout", String.valueOf(smtpConnectionTimeoutMs));
        properties.put("mail.smtp.timeout", String.valueOf(smtpReadTimeoutMs));
        properties.put("mail.smtp.writetimeout", String.valueOf(smtpWriteTimeoutMs));

        Session session = Session.getInstance(properties);
        String accessToken = mailOAuth2TokenService.getAccessToken();

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Urbanova registration verification code", StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());
            message.setText("""
                    Your Urbanova registration verification code is %s.

                    The code expires at %s.

                    If you did not request this code, you can ignore this email.
                    """.formatted(code, expiresAt), StandardCharsets.UTF_8.name());

            try (Transport transport = session.getTransport("smtp")) {
                transport.connect(mailHost, mailPort, mailUsername, accessToken);
                transport.sendMessage(message, message.getAllRecipients());
            }
        } catch (MessagingException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_FAILED,
                    "Failed to send email through Outlook OAuth2 SMTP: " + ex.getMessage());
        }
    }

    private void sendRegistrationVerificationCodeViaGraph(String email, String code, LocalDateTime expiresAt) {
        if (isBlank(fromAddress) || isBlank(graphSendMailUrl)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_NOT_CONFIGURED,
                    "Microsoft Graph email delivery is missing sender address or sendMail URL");
        }

        String accessToken = mailOAuth2TokenService.getAccessToken();
        Map<String, Object> payload = Map.of(
                "message", Map.of(
                        "subject", "Urbanova registration verification code",
                        "body", Map.of(
                                "contentType", "Text",
                                "content", """
                                        Your Urbanova registration verification code is %s.

                                        The code expires at %s.

                                        If you did not request this code, you can ignore this email.
                                        """.formatted(code, expiresAt)
                        ),
                        "toRecipients", List.of(Map.of(
                                "emailAddress", Map.of("address", email)
                        ))
                ),
                "saveToSentItems", graphSaveToSentItems
        );

        try {
            restClient.post()
                    .uri(graphSendMailUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_FAILED,
                    "Failed to send email through Microsoft Graph sendMail: " + ex.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
