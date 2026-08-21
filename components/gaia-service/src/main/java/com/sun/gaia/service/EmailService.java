package com.sun.gaia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private static final ObjectMapper MAPPER = new ObjectMapper();

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

  /**
   * Sends a password-reset link by email.
   *
   * @param toEmail the recipient address
   * @param resetLink the reset link to include
   */
  public void sendPasswordResetEmail(String toEmail, String resetLink) {
    String body =
        "Click the link below to reset your password:\n\n" + resetLink +
        "\n\nThis link expires in 15 minutes.";
    sendEmail(toEmail, "Password Reset", body);
  }

  /**
   * Sends an account-reactivation link by email.
   *
   * @param toEmail the recipient address
   * @param reactivationLink the reactivation link to include
   */
  public void sendReactivationEmail(String toEmail, String reactivationLink) {
    String body =
        "Click the link below to reactivate your account:\n\n" + reactivationLink +
        "\n\nThis link expires in 15 minutes.";
    sendEmail(toEmail, "Account Reactivation", body);
  }

  /**
   * Sends a notification that private notes were shared.
   *
   * @param toEmail the recipient address
   * @param textTitle the text title
   * @param sharerName the sharer display name
   */
  public void sendShareNotesEmail(String toEmail, String textTitle, String sharerName) {
    String body = sharerName + " shared their private notes on \"" + textTitle + "\" with you.";
    sendEmail(toEmail, "Notes shared with you", body);
  }

  /**
   * Sends a plain-text email via Gmail XOAUTH2.
   *
   * @param toEmail the recipient address
   * @param subject the message subject
   * @param body the message body
   */
  private void sendEmail(String toEmail, String subject, String body) {
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
      message.setSubject(subject);
      message.setText(body);

      Transport.send(message);
      logger.info("Email '{}' sent to {}", subject, toEmail);
    } catch (Exception e) {
      logger.error("Failed to send email '{}' to {}", subject, toEmail, e);
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

    int status = conn.getResponseCode();
    if (status >= 400) {
      throw new RuntimeException("OAuth token refresh failed (" + status + "): " + readErrorBody(conn));
    }

    JsonNode json = MAPPER.readTree(conn.getInputStream());
    JsonNode tokenNode = json.get("access_token");
    if (tokenNode == null) {
      throw new RuntimeException("OAuth token refresh response had no access_token: " + json);
    }
    String token = tokenNode.asText();
    int expiresIn = json.get("expires_in").asInt();

    cachedAccessToken = token;
    tokenExpiresAt = now + (expiresIn * 1000L);

    logger.info("Refreshed Gmail OAuth2 access token, expires in {} seconds", expiresIn);
    return cachedAccessToken;
  }

  /**
   * Reads the error stream of a failed token request.
   *
   * @param conn the failed connection
   * @return the Google error body, or the status line when unreadable
   */
  private static String readErrorBody(HttpURLConnection conn) {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
      StringBuilder body = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        body.append(line);
      }
      return body.toString();
    } catch (Exception e) {
      return "no error body";
    }
  }
}
