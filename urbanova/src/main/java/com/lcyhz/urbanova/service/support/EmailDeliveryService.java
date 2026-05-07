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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

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
        sendRequiredTextEmail(email, "Urbanova registration verification code", """
                Your Urbanova registration verification code is %s.

                The code expires at %s.

                If you did not request this code, you can ignore this email.
                """.formatted(code, expiresAt));
    }

    public boolean sendBookingConfirmationEmail(String email,
                                                String bookingRef,
                                                String scooterId,
                                                LocalDateTime startAt,
                                                LocalDateTime endAt,
                                                BigDecimal finalPrice) {
        if (isBlank(email)) {
            return false;
        }
        String body = """
                Your Urbanova booking has been confirmed.

                Booking reference: %s
                Scooter ID: %s
                Planned start: %s
                Planned end: %s
                Total amount: GBP %s

                Thank you for booking with Urbanova.
                """.formatted(
                defaultText(bookingRef),
                defaultText(scooterId),
                defaultText(startAt),
                defaultText(endAt),
                finalPrice == null ? "0.00" : finalPrice.toPlainString()
        );
        return sendBestEffortTextEmail(email, "Urbanova booking confirmation", body, "booking confirmation");
    }

    public boolean sendIssueSubmissionEmail(String email,
                                            String issueId,
                                            String issueType,
                                            String title,
                                            String priority,
                                            String scooterId,
                                            String bookingId,
                                            LocalDateTime createdAt) {
        if (isBlank(email)) {
            return false;
        }

        StringBuilder body = new StringBuilder();
        body.append("We received your Urbanova issue submission.\n\n")
                .append("Issue ID: ").append(defaultText(issueId)).append('\n')
                .append("Issue type: ").append(defaultText(issueType)).append('\n')
                .append("Title: ").append(defaultText(title)).append('\n')
                .append("Priority: ").append(defaultText(priority)).append('\n');
        appendOptionalLine(body, "Scooter ID", scooterId);
        appendOptionalLine(body, "Booking ID", bookingId);
        body.append("Submitted at: ").append(defaultText(createdAt)).append("\n\n")
                .append("Our team will review your submission and update you when needed.");
        return sendBestEffortTextEmail(email, "Urbanova issue submission received", body.toString(), "issue submission");
    }

    private void sendRequiredTextEmail(String email, String subject, String body) {
        sendTextEmail(email, subject, body);
    }

    private boolean sendBestEffortTextEmail(String email, String subject, String body, String context) {
        try {
            sendTextEmail(email, subject, body);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Failed to send {} email to {}: {}", context, email, ex.getMessage());
            return false;
        }
    }

    private void sendTextEmail(String email, String subject, String body) {
        if (mailOAuth2TokenService.isEnabled()) {
            if ("graph".equalsIgnoreCase(oauth2Transport)) {
                sendTextEmailViaGraph(email, subject, body);
            } else {
                sendTextEmailViaOAuth2(email, subject, body);
            }
            return;
        }
        sendTextEmailViaSpring(email, subject, body);
    }

    private void sendTextEmailViaSpring(String email, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || isBlank(mailHost) || isBlank(fromAddress)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_NOT_CONFIGURED,
                    "Email delivery is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void sendTextEmailViaOAuth2(String email, String subject, String body) {
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
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());
            message.setText(body, StandardCharsets.UTF_8.name());

            try (Transport transport = session.getTransport("smtp")) {
                transport.connect(mailHost, mailPort, mailUsername, accessToken);
                transport.sendMessage(message, message.getAllRecipients());
            }
        } catch (MessagingException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_FAILED,
                    "Failed to send email through Outlook OAuth2 SMTP: " + ex.getMessage());
        }
    }

    private void sendTextEmailViaGraph(String email, String subject, String body) {
        if (isBlank(fromAddress) || isBlank(graphSendMailUrl)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE.value(), ErrorCodes.EMAIL_DELIVERY_NOT_CONFIGURED,
                    "Microsoft Graph email delivery is missing sender address or sendMail URL");
        }

        String accessToken = mailOAuth2TokenService.getAccessToken();
        Map<String, Object> payload = Map.of(
                "message", Map.of(
                        "subject", subject,
                        "body", Map.of(
                                "contentType", "Text",
                                "content", body
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

    private void appendOptionalLine(StringBuilder builder, String label, String value) {
        if (!isBlank(value)) {
            builder.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private String defaultText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
