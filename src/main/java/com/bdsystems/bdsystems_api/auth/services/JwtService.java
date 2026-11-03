package com.bdsystems.bdsystems_api.auth.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;

@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final Duration accessTokenDuration;
  @Value("${security.jwt.issuer}")
  private String issuer;

  public JwtService(JwtEncoder jwtEncoder,
    @Value("${security.jwt.access-token-duration}") Duration accessTokenDuration) {
    this.jwtEncoder = jwtEncoder;
    this.accessTokenDuration = accessTokenDuration;

  }

  public String generateToken(User user) {

    Instant now = Instant.now();

    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer(issuer)
      .issuedAt(now)
      .expiresAt(now.plus(accessTokenDuration))
      .subject(user.getId().toString())
      .claim(
        "tenant_id",
        user.getCompany().getId().toString()
      )
      .claim(
        "role",
        user.getRole().name()
      )
      .claim(
        "employee_id",
        user.getEmployee().getId().toString()
      )
      .build();

    JwsHeader header = JwsHeader.with(
      MacAlgorithm.HS256
    ).build();

    return jwtEncoder.encode(
      JwtEncoderParameters.from(header,
        claims)
    ).getTokenValue();
  }

  public long expiresInSeconds() {
    return accessTokenDuration.toSeconds();
  }
}
