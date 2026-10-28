package com.bdsystems.bdsystems_api.auth.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import com.bdsystems.bdsystems_api.auth.domains.entities.UserInvitation;
import com.bdsystems.bdsystems_api.auth.repositories.UserInvitationRepository;
import com.bdsystems.bdsystems_api.auth.repositories.UserRepository;
import com.bdsystems.bdsystems_api.employee.domains.dtos.UserAccessStatus;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

@Service
public class UserInvitationService {
  private static final Duration INVITATION_VALIDITY = Duration.ofHours(24);
  private static final int TOKEN_BYTES = 32;

  private final UserInvitationRepository invitationRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JavaMailSender mailSender;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Clock clock = Clock.systemUTC();
  private final String frontendBaseUrl;
  private final String mailFrom;

  public UserInvitationService(
    UserInvitationRepository invitationRepository,
    UserRepository userRepository,
    PasswordEncoder passwordEncoder,
    JavaMailSender mailSender,
    @Value("${app.frontend.base-url}") String frontendBaseUrl,
    @Value("${app.mail.from}") String mailFrom
  ) {
    this.invitationRepository = invitationRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.mailSender = mailSender;
    this.frontendBaseUrl = frontendBaseUrl;
    this.mailFrom = mailFrom;
  }

  public void createAndSend(User user) {
    Instant now = clock.instant();
    String token = generateToken();
    invitationRepository.save(new UserInvitation(user,
      hashToken(token),
      now.plus(INVITATION_VALIDITY),
      now));
    sendInvitation(user,
      token);
  }

  @Transactional
  public void resend(User user) {
    if (user.isActive()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
        "User access is already active");
    }
    Instant now = clock.instant();
    invitationRepository.invalidateUnconsumedByUserId(user.getId(),
      now);
    createAndSend(user);
  }

  @Transactional
  public void accept(String token,
    String password) {
    Instant now = clock.instant();
    UserInvitation invitation = invitationRepository.findByTokenHash(hashToken(token))
      .orElseThrow(() -> invalidInvitation());
    if (invitation.getConsumedAt() != null || invitation.getUser().isActive()) {
      throw invalidInvitation();
    }
    if (!invitation.getExpiresAt().isAfter(now)) {
      throw expiredInvitation();
    }

    User user = invitation.getUser();
    user.acceptInvitation(passwordEncoder.encode(password));
    invitation.consume(now);
    userRepository.save(user);
    invitationRepository.save(invitation);
  }

  public UserAccessStatus statusFor(User user) {
    if (user == null) {
      return UserAccessStatus.NONE;
    }
    if (user.isActive()) {
      return UserAccessStatus.ACTIVE;
    }
    return invitationRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
      .filter(invitation -> invitation.isPending(clock.instant()))
      .map(invitation -> UserAccessStatus.PENDING)
      .orElse(UserAccessStatus.EXPIRED);
  }

  private void sendInvitation(User user,
    String token) {
    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message,
        StandardCharsets.UTF_8.name());
      helper.setFrom(mailFrom);
      helper.setTo(user.getEmail());
      helper.setSubject("Convite para acessar o BD Systems");
      helper.setText("Defina a sua senha em: " + invitationUrl(token),
        false);
    } catch (jakarta.mail.MessagingException exception) {
      throw new IllegalStateException("Could not create invitation email",
        exception);
    }
    mailSender.send(message);
  }

  private String invitationUrl(String token) {
    return frontendBaseUrl.replaceAll("/+$",
      "") + "/accept-invitation?token=" + token;
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

  private ResponseStatusException invalidInvitation() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST,
      "Invitation is invalid or already used");
  }

  private ResponseStatusException expiredInvitation() {
    return new ResponseStatusException(HttpStatus.GONE,
      "Invitation has expired");
  }
}
