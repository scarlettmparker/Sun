package com.sun.gaia.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  private final String host;
  private final int port;
  private final String oauthEndpoint;
  private final String clientId;
  private final String clientSecret;
  private final String refreshToken;
  private final String fromAddress;

  private volatile String cachedAccessToken;
  private volatile long tokenExpiresAt;

  public EmailService(
      @Value("${EMAIL_HOST:smtp.gmail.com}") String host,
      @Value("${EMAIL_PORT:465}") int port,
      @Value("${EMAIL_OAUTH:oauth2.googleapis.com}") String oauthEndpoint,
      @Value("${EMAIL_CLIENT_ID:}") String clientId,
      @Value("${EMAIL_CLIENT_SECRET:}") String clientSecret,
      @Value("${EMAIL_REFRESH_TOKEN:}") String refreshToken,
      @Value("${EMAIL_ADDRESS:}") String fromAddress) {
    this.host = host;
    this.port = port;
    this.oauthEndpoint = oauthEndpoint;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.refreshToken = refreshToken;
    this.fromAddress = fromAddress;
  }

  public void sendPasswordResetEmail(String toEmail, String resetLink) {
    try {
      String accessToken = getAccessToken();

      Properties props = new Properties();
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.port", String.valueOf(port));
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
      props.put("mail.smtp.auth", "true");

      Session session = Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(fromAddress, accessToken);
        }
      });

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(fromAddress));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
      message.setSubject("Password Reset");
      message.setText(
          "Click the link below to reset your password:\n\n" + resetLink +
          "\n\nThis link expires in 15 minutes.");

      Transport.send(message);
      logger.info("Password reset email sent to {}", toEmail);
    } catch (Exception e) {
      logger.error("Failed to send password reset email to {}", toEmail, e);
      throw new RuntimeException("Failed to send email", e);
    }
  }

  private synchronized String getAccessToken() throws Exception {
    long now = System.currentTimeMillis();
    if (cachedAccessToken != null && now < tokenExpiresAt - 60000) {
      return cachedAccessToken;
    }

    String urlStr = "https://" + oauthEndpoint + "/token";
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

    String body = "grant_type=refresh_token"
        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
        + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
        + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

    try (OutputStream os = conn.getOutputStream()) {
      os.write(body.getBytes(StandardCharsets.UTF_8));
    }

    StringBuilder response = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
    }

    String responseBody = response.toString();
    String token = extractJsonField(responseBody, "access_token");
    int expiresIn = Integer.parseInt(extractJsonField(responseBody, "expires_in"));

    cachedAccessToken = token;
    tokenExpiresAt = now + (expiresIn * 1000L);

    logger.info("Refreshed Gmail OAuth2 access token, expires in {} seconds", expiresIn);
    return cachedAccessToken;
  }

  private String extractJsonField(String json, String field) {
    String search = "\"" + field + "\":\"";
    int start = json.indexOf(search);
    if (start == -1) {
      throw new RuntimeException("Field not found in JSON: " + field);
    }
    start += search.length();
    int end = json.indexOf("\"", start);
    return json.substring(start, end);
  }
}
