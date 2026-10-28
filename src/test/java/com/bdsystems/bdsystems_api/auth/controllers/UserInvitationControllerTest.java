package com.bdsystems.bdsystems_api.auth.controllers;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import com.bdsystems.bdsystems_api.auth.domains.entities.UserInvitation;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.auth.repositories.UserInvitationRepository;
import com.bdsystems.bdsystems_api.auth.repositories.UserRepository;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(UserInvitationControllerTest.MailTestConfiguration.class)
class UserInvitationControllerTest {
  private static final String ACCEPTED_PASSWORD = "invited-user-password";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AddressRepository addressRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private EmployeeRepository employeeRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserInvitationRepository invitationRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtEncoder jwtEncoder;

  @Autowired
  private TestMailSender mailSender;

  @BeforeEach
  void configureMailSender() {
    mailSender.reset();
  }

  @Test
  void createsPendingUserAcceptsInvitationAndAllowsLoginOnce() throws Exception {
    Company company = createCompany("Invitation Flow Company");

    mockMvc.perform(post("/api/employees")
        .header("Authorization",
          "Bearer " + tokenFor(company))
        .contentType(MediaType.APPLICATION_JSON)
        .content(employeeRequest("invite.flow@example.com",
          "USER")))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.userRole").value("USER"))
      .andExpect(jsonPath("$.userAccessStatus").value("PENDING"));

    User invitedUser = userRepository.findByEmail("invite.flow@example.com").orElseThrow();
    assertFalse(invitedUser.isActive());
    assertNotEquals(ACCEPTED_PASSWORD,
      invitedUser.getPassword());

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginRequest("invite.flow@example.com",
          ACCEPTED_PASSWORD)))
      .andExpect(status().isUnauthorized());

    String invitationToken = tokenFromLatestMessage();
    mockMvc.perform(post("/api/auth/invitations/accept")
        .contentType(MediaType.APPLICATION_JSON)
        .content(acceptanceRequest(invitationToken,
          ACCEPTED_PASSWORD)))
      .andExpect(status().isNoContent());

    User activeUser = userRepository.findByEmail("invite.flow@example.com").orElseThrow();
    assertTrue(activeUser.isActive());
    assertTrue(passwordEncoder.matches(ACCEPTED_PASSWORD,
      activeUser.getPassword()));

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginRequest("invite.flow@example.com",
          ACCEPTED_PASSWORD)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token").isNotEmpty());

    mockMvc.perform(post("/api/auth/invitations/accept")
        .contentType(MediaType.APPLICATION_JSON)
        .content(acceptanceRequest(invitationToken,
          ACCEPTED_PASSWORD)))
      .andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/employees")
        .header("Authorization",
          "Bearer " + tokenFor(company)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].userAccessStatus").value("ACTIVE"));
  }

  @Test
  void resendsOnlyWithinTenantAndInvalidatesPreviousInvitation() throws Exception {
    Company company = createCompany("Invitation Resend Company");
    Company anotherCompany = createCompany("Invitation Other Tenant");
    String response = mockMvc.perform(post("/api/employees")
        .header("Authorization",
          "Bearer " + tokenFor(company))
        .contentType(MediaType.APPLICATION_JSON)
        .content(employeeRequest("invite.resend@example.com",
          "MANAGER")))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();
    String employeeId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).path("id").asText();
    String previousToken = tokenFromLatestMessage();

    mockMvc.perform(post("/api/employees/{id}/user-invitation/resend",
        employeeId)
        .header("Authorization",
          "Bearer " + tokenFor(anotherCompany)))
      .andExpect(status().isNotFound());

    mockMvc.perform(post("/api/employees/{id}/user-invitation/resend",
        employeeId)
        .header("Authorization",
          "Bearer " + tokenFor(company)))
      .andExpect(status().isNoContent());
    String replacementToken = tokenFromLatestMessage();
    assertNotEquals(previousToken,
      replacementToken);

    mockMvc.perform(post("/api/auth/invitations/accept")
        .contentType(MediaType.APPLICATION_JSON)
        .content(acceptanceRequest(previousToken,
          ACCEPTED_PASSWORD)))
      .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/auth/invitations/accept")
        .contentType(MediaType.APPLICATION_JSON)
        .content(acceptanceRequest(replacementToken,
          ACCEPTED_PASSWORD)))
      .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/employees/{id}/user-invitation/resend",
        employeeId)
        .header("Authorization",
          "Bearer " + tokenFor(company)))
      .andExpect(status().isConflict());
  }

  @Test
  void reportsExpiredInvitationAndRejectsIt() throws Exception {
    Company company = createCompany("Expired Invitation Company");
    mockMvc.perform(post("/api/employees")
        .header("Authorization",
          "Bearer " + tokenFor(company))
        .contentType(MediaType.APPLICATION_JSON)
        .content(employeeRequest("invite.expired@example.com",
          "ADMIN")))
      .andExpect(status().isCreated());
    User invitedUser = userRepository.findByEmail("invite.expired@example.com").orElseThrow();
    Instant now = Instant.now();
    UserInvitation currentInvitation = invitationRepository.findFirstByUserIdOrderByCreatedAtDesc(invitedUser.getId())
      .orElseThrow();
    currentInvitation.consume(now);
    invitationRepository.save(currentInvitation);
    String expiredToken = "expired-invitation-token";
    invitationRepository.save(new UserInvitation(invitedUser,
      sha256(expiredToken),
      now.minusSeconds(1),
      now.plusSeconds(1)));

    mockMvc.perform(get("/api/employees")
        .header("Authorization",
          "Bearer " + tokenFor(company)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].userAccessStatus").value("EXPIRED"));

    mockMvc.perform(post("/api/auth/invitations/accept")
        .contentType(MediaType.APPLICATION_JSON)
        .content(acceptanceRequest(expiredToken,
          ACCEPTED_PASSWORD)))
      .andExpect(status().isGone());
  }

  @Test
  void rollsBackEmployeeUserAddressAndInvitationWhenSmtpFails() throws Exception {
    Company company = createCompany("SMTP Rollback Company");
    long employeeCount = employeeRepository.count();
    long userCount = userRepository.count();
    long addressCount = addressRepository.count();
    long invitationCount = invitationRepository.count();
    mailSender.failOnSend();

    mockMvc.perform(post("/api/employees")
        .header("Authorization",
          "Bearer " + tokenFor(company))
        .contentType(MediaType.APPLICATION_JSON)
        .content(employeeRequest("invite.smtp.failure@example.com",
          "USER")))
      .andExpect(status().is5xxServerError());

    assertEquals(employeeCount,
      employeeRepository.count());
    assertEquals(userCount,
      userRepository.count());
    assertEquals(addressCount,
      addressRepository.count());
    assertEquals(invitationCount,
      invitationRepository.count());
  }

  private Company createCompany(String name) {
    Address address = addressRepository.save(new Address("Main Street",
      "Lisbon",
      "Lisbon",
      "1000-001"));
    return companyRepository.save(new Company(name,
      address));
  }

  private String tokenFor(Company company) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer("bdsystems-api")
      .subject("invitation-test-user")
      .issuedAt(now)
      .expiresAt(now.plusSeconds(3600))
      .claim("tenant_id",
        company.getId().toString())
      .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header,
      claims)).getTokenValue();
  }

  private String tokenFromLatestMessage() throws Exception {
    String content = mailSender.latestMessage().getContent().toString();
    return content.substring(content.indexOf("token=") + "token=".length()).trim();
  }

  private String sha256(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
      .digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  private String employeeRequest(String email,
    String role) {
    return """
      {
        "fullName": "Invited Employee",
        "email": "%s",
        "phoneNumber": "+351900000000",
        "address": {
          "street": "Employee Street",
          "city": "Lisbon",
          "state": "Lisbon",
          "zipCode": "1000-002"
        },
        "employeeNumber": "%s",
        "position": "Engineer",
        "status": "ACTIVE",
        "userRole": "%s"
      }
      """.formatted(email,
      email,
      role);
  }

  private String acceptanceRequest(String token,
    String password) {
    return """
      {"token":"%s","password":"%s"}
      """.formatted(token,
      password);
  }

  private String loginRequest(String email,
    String password) {
    return """
      {"email":"%s","password":"%s"}
      """.formatted(email,
      password);
  }

  @TestConfiguration
  static class MailTestConfiguration {

    @Bean
    @Primary
    TestMailSender testMailSender() {
      return new TestMailSender();
    }
  }

  static class TestMailSender extends JavaMailSenderImpl {
    private final AtomicReference<MimeMessage> latestMessage = new AtomicReference<>();
    private boolean failing;

    @Override
    public MimeMessage createMimeMessage() {
      MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
      latestMessage.set(message);
      return message;
    }

    @Override
    public void send(MimeMessage message) {
      if (failing) {
        throw new MailSendException("SMTP unavailable");
      }
      latestMessage.set(message);
    }

    void reset() {
      failing = false;
      latestMessage.set(null);
    }

    void failOnSend() {
      failing = true;
    }

    MimeMessage latestMessage() {
      return latestMessage.get();
    }
  }
}
