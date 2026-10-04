package com.bdsystems.bdsystems_api.auth.services;

import com.bdsystems.bdsystems_api.auth.domains.dtos.LoginRequest;
import com.bdsystems.bdsystems_api.auth.domains.dtos.LoginResponse;
import com.bdsystems.bdsystems_api.auth.domains.dtos.UserRequest;
import com.bdsystems.bdsystems_api.auth.domains.dtos.UserResponse;
import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.domains.entities.Role;
import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import com.bdsystems.bdsystems_api.auth.exceptions.EmailAlreadyRegisteredException;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.auth.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

  private final CompanyRepository companyRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AddressRepository addressRepository;

  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthenticationService(
      CompanyRepository companyRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AddressRepository addressRepository, JwtService jwtService, AuthenticationManager authenticationManager
  ) {
    this.companyRepository = companyRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.addressRepository = addressRepository;
    this.jwtService = jwtService;
    this.authenticationManager = authenticationManager;
  }

  @Transactional
  public UserResponse register(
      UserRequest request
  ) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyRegisteredException(request.email());
    }

    Company company = companyRepository.findByName(request.company().name())
        .orElseGet(() -> companyRepository.save(
            new Company(
                request.company().name(),
                addressRepository.save(
                    new Address(
                        request.company().address().street(),
                        request.company().address().city(),
                        request.company().address().state(),
                        request.company().address().zipcode()
                    )
                ))));

    Address savedAddress = addressRepository.save(new Address(
        request.address().street(),
        request.address().city(),
        request.address().state(),
        request.address().zipcode()
    ));

    User user = new User(
        null,
        request.name(),
        request.email(),
        passwordEncoder.encode(request.password()),
        request.role() != null ? Role.valueOf(request.role()) : Role.USER,
        savedAddress,
        company
    );

    user = userRepository.save(user);

    String jwtToken = jwtService.generateToken(user);

    return new UserResponse(
        jwtToken
    );
  }

  public LoginResponse login(LoginRequest request) {

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.email(),
            request.password()
        )
    );

    User user = userRepository.findByEmail(
        request.email())
        .orElseThrow(() ->
            new UsernameNotFoundException("User not found") );
    String token = jwtService.generateToken(user);

    return new LoginResponse(token);
  }
}
