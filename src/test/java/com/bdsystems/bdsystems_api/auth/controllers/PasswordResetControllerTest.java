package com.bdsystems.bdsystems_api.auth.controllers;

import com.bdsystems.bdsystems_api.auth.domains.dtos.UserRequest;
import com.bdsystems.bdsystems_api.auth.domains.entities.PasswordResetToken;
import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import com.bdsystems.bdsystems_api.auth.repositories.PasswordResetTokenRepository;
import com.bdsystems.bdsystems_api.auth.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PasswordResetControllerTest.MailTestConfiguration.class)
class PasswordResetControllerTest {
  private static final String OLD_PASSWORD = "old-password";
  private static final String NEW_PASSWORD = "new-password";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordResetTokenRepository resetTokenRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private TestMailSender mailSender;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void resetMailSender() {
    mailSender.reset();
  }

  @Test
  void activeAccountReceivesHashedOneHourTokenAndCanResetPasswordOnce() throws Exception {
    User user = register("Password Reset Flow Company",
      "password.reset.flow@example.com");

    requestReset(user.getEmail())
      .andExpect(status().isNoContent());

    assertEquals(1,
      mailSender.sendCount());
    String token = tokenFromLatestMessage();
    PasswordResetToken storedToken = resetTokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
      .orElseThrow();
    assertNotEquals(token,
      storedToken.getTokenHash());
    assertEquals(sha256(token),
      storedToken.getTokenHash());
    assertEquals(3600,
      storedToken.getExpiresAt().getEpochSecond() - storedToken.getCreatedAt().getEpochSecond());

    confirmReset(token,
      NEW_PASSWORD)
      .andExpect(status().isNoContent());

    User updatedUser = userRepository.findById(user.getId()).orElseThrow();
    assertFalse(passwordEncoder.matches(OLD_PASSWORD,
      updatedUser.getPassword()));
    assertTrue(passwordEncoder.matches(NEW_PASSWORD,
      updatedUser.getPassword()));
    login(user.getEmail(),
      OLD_PASSWORD)
      .andExpect(status().isUnauthorized());
    login(user.getEmail(),
      NEW_PASSWORD)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token").isNotEmpty());
    confirmReset(token,
      "another-password")
      .andExpect(status().isBadRequest());
  }

  @Test
  void requestIsNeutralForUnknownInactiveAndSmtpFailure() throws Exception {
    long initialTokenCount = resetTokenRepository.count();
    requestReset("unknown.password.reset@example.com")
      .andExpect(status().isNoContent());
    assertEquals(0,
      mailSender.sendCount());
    assertEquals(initialTokenCount,
      resetTokenRepository.count());

    User inactiveUser = register("Inactive Password Reset Company",
      "inactive.password.reset@example.com");
    inactiveUser.deactivate();
    userRepository.save(inactiveUser);
    requestReset(inactiveUser.getEmail())
      .andExpect(status().isNoContent());
    assertEquals(0,
      mailSender.sendCount());
    assertEquals(initialTokenCount,
      resetTokenRepository.count());

    User activeUser = register("SMTP Password Reset Company",
      "smtp.password.reset@example.com");
    mailSender.failOnSend();
    requestReset(activeUser.getEmail())
      .andExpect(status().isNoContent());
    assertEquals(1,
      mailSender.sendCount());
    assertTrue(resetTokenRepository.findFirstByUserIdOrderByCreatedAtDesc(activeUser.getId()).isPresent());
  }

  @Test
  void newerRequestInvalidatesPreviousTokenAndExpiredTokenDoesNotChangePassword() throws Exception {
    User user = register("Invalidated Password Reset Company",
      "invalidated.password.reset@example.com");
    requestReset(user.getEmail()).andExpect(status().isNoContent());
    String previousToken = tokenFromLatestMessage();
    requestReset(user.getEmail()).andExpect(status().isNoContent());
    String replacementToken = tokenFromLatestMessage();
    assertNotEquals(previousToken,
      replacementToken);

    confirmReset(previousToken,
      NEW_PASSWORD)
      .andExpect(status().isBadRequest());
    User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
    assertTrue(passwordEncoder.matches(OLD_PASSWORD,
      unchangedUser.getPassword()));

    String expiredRawToken = "expired-password-reset-token";
    Instant now = Instant.now();
    resetTokenRepository.save(new PasswordResetToken(user,
      sha256(expiredRawToken),
      now.minusSeconds(1),
      now.minusSeconds(3601)));
    confirmReset(expiredRawToken,
      NEW_PASSWORD)
      .andExpect(status().isGone());
    unchangedUser = userRepository.findById(user.getId()).orElseThrow();
    assertTrue(passwordEncoder.matches(OLD_PASSWORD,
      unchangedUser.getPassword()));

    confirmReset("unknown-token",
      NEW_PASSWORD)
      .andExpect(status().isBadRequest());
  }

  @Test
  void validatesEightCharacterPasswordsAndResetPayloads() throws Exception {
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(userRequest("Short Registration Company",
          "short.registration@example.com",
          "short"))))
      .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/auth/invitations/accept")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"token":"invitation-token","password":"short"}
          """))
      .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/auth/password-reset/request")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"email":"not-an-email"}
          """))
      .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/auth/password-reset/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"token":"reset-token","password":"short"}
          """))
      .andExpect(status().isBadRequest());
  }

  private User register(String companyName,
    String email) throws Exception {
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(userRequest(companyName,
          email,
          OLD_PASSWORD))))
      .andExpect(status().isCreated());
    return userRepository.findByEmail(email).orElseThrow();
  }

  private UserRequest userRequest(String companyName,
    String email,
    String password) {
    return new UserRequest("Password Reset User",
      email,
      password,
      new UserRequest.CompanyRequest(companyName,
        new UserRequest.AddressRequest("Main Street",
          "Lisbon",
          "Lisbon",
          "1000-001")));
  }

  private org.springframework.test.web.servlet.ResultActions requestReset(String email) throws Exception {
    return mockMvc.perform(post("/api/auth/password-reset/request")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""
        {"email":"%s"}
        """.formatted(email)));
  }

  private org.springframework.test.web.servlet.ResultActions confirmReset(String token,
    String password) throws Exception {
    return mockMvc.perform(post("/api/auth/password-reset/confirm")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""
        {"token":"%s","password":"%s"}
        """.formatted(token,
        password)));
  }

  private org.springframework.test.web.servlet.ResultActions login(String email,
    String password) throws Exception {
    return mockMvc.perform(post("/api/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""
        {"email":"%s","password":"%s"}
        """.formatted(email,
        password)));
  }

  private String tokenFromLatestMessage() throws Exception {
    MimeMessage latestMessage = mailSender.latestMessage();
    assertNotNull(latestMessage);
    String content = latestMessage.getContent().toString();
    return content.substring(content.indexOf("token=") + "token=".length()).trim();
  }

  private String sha256(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
      .digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  @TestConfiguration
  static class MailTestConfiguration {

    @Bean
    @Primary
    TestMailSender passwordResetTestMailSender() {
      return new TestMailSender();
    }
  }

  static class TestMailSender extends JavaMailSenderImpl {
    private final AtomicReference<MimeMessage> latestMessage = new AtomicReference<>();
    private final AtomicInteger sendCount = new AtomicInteger();
    private boolean failing;

    @Override
    public MimeMessage createMimeMessage() {
      return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Override
    public void send(MimeMessage message) {
      latestMessage.set(message);
      sendCount.incrementAndGet();
      if (failing) {
        throw new MailSendException("SMTP unavailable");
      }
    }

    void reset() {
      latestMessage.set(null);
      sendCount.set(0);
      failing = false;
    }

    void failOnSend() {
      failing = true;
    }

    MimeMessage latestMessage() {
      return latestMessage.get();
    }

    int sendCount() {
      return sendCount.get();
    }
  }
}
