package com.bdsystems.bdsystems_api.auth.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.PasswordResetToken;
import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import com.bdsystems.bdsystems_api.auth.repositories.PasswordResetTokenRepository;
import com.bdsystems.bdsystems_api.auth.repositories.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
public class PasswordResetService {
  private static final Duration TOKEN_VALIDITY = Duration.ofHours(1);
  private static final int TOKEN_BYTES = 32;

  private final PasswordResetTokenRepository resetTokenRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JavaMailSender mailSender;
  private final SecureRandom secureRandom;
  private final Clock clock;
  private final String frontendBaseUrl;
  private final String mailFrom;

  public PasswordResetService(
    PasswordResetTokenRepository resetTokenRepository,
    UserRepository userRepository,
    PasswordEncoder passwordEncoder,
    JavaMailSender mailSender,
    @Value("${app.frontend.base-url}") String frontendBaseUrl,
    @Value("${app.mail.from}") String mailFrom
  ) {
    this.resetTokenRepository = resetTokenRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.mailSender = mailSender;
    this.frontendBaseUrl = frontendBaseUrl;
    this.mailFrom = mailFrom;
    this.secureRandom = new SecureRandom();
    this.clock = Clock.systemUTC();
  }

  @Transactional
  public void request(String email) {
    userRepository.findByEmailAndActiveTrue(email).ifPresent(this::createAndSend);
  }

  @Transactional
  public void confirm(String token,
    String password) {
    Instant now = clock.instant();
    PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(hashToken(token))
      .orElseThrow(this::invalidToken);
    if (resetToken.getConsumedAt() != null) {
      throw invalidToken();
    }
    if (!resetToken.getExpiresAt().isAfter(now)) {
      throw expiredToken();
    }

    User user = resetToken.getUser();
    user.updatePassword(passwordEncoder.encode(password));
    resetToken.consume(now);
    resetTokenRepository.invalidateUnconsumedByUserId(user.getId(),
      now);
    userRepository.save(user);
  }

  private void createAndSend(User user) {
    Instant now = clock.instant();
    resetTokenRepository.invalidateUnconsumedByUserId(user.getId(),
      now);
    String token = generateToken();
    resetTokenRepository.save(new PasswordResetToken(user,
      hashToken(token),
      now.plus(TOKEN_VALIDITY),
      now));
    try {
      sendEmail(user,
        token);
    } catch (MailException | MessagingException exception) {
      log.error("Password reset email delivery failed ({})",
        exception.getClass().getSimpleName());
    }
  }

  private void sendEmail(User user,
    String token) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message,
      StandardCharsets.UTF_8.name());
    helper.setFrom(mailFrom);
    helper.setTo(user.getEmail());
    helper.setSubject("Redefinição de senha do BD Systems");
    helper.setText("Redefina a sua senha em: " + resetUrl(token),
      false);
    mailSender.send(message);
  }

  private String resetUrl(String token) {
    return frontendBaseUrl.replaceAll("/+$",
      "") + "/reset-password?token=" + token;
  }

  private String generateToken() {
    byte[] tokenBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  private String hashToken(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable",
        exception);
    }
  }

  private ResponseStatusException invalidToken() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST,
      "Password reset token is invalid or already used");
  }

  private ResponseStatusException expiredToken() {
    return new ResponseStatusException(HttpStatus.GONE,
      "Password reset token has expired");
  }
}
