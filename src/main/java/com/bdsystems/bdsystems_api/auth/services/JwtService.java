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

@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  @Value("${security.jwt.issuer}") private String issuer;

  public JwtService(JwtEncoder jwtEncoder) {
    this.jwtEncoder = jwtEncoder;

  }

  public String generateToken(User user) {

    Instant now = Instant.now();

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .subject(user.getId().toString())
        .claim(
            "tenant_id",
            user.getCompany().getId().toString()
        )
        .claim(
            "role",
            user.getRole().name()
        )
        .build();

    JwsHeader header = JwsHeader.with(
        MacAlgorithm.HS256
    ).build();

    return jwtEncoder.encode(
        JwtEncoderParameters.from(header, claims)
    ).getTokenValue();
  }
}
