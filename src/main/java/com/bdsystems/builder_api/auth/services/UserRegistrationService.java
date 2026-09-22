package com.bdsystems.builder_api.auth.services;

import com.bdsystems.builder_api.auth.domains.dtos.UserRequest;
import com.bdsystems.builder_api.auth.domains.entities.Address;
import com.bdsystems.builder_api.auth.domains.entities.Company;
import com.bdsystems.builder_api.auth.domains.entities.Role;
import com.bdsystems.builder_api.auth.domains.entities.User;
import com.bdsystems.builder_api.auth.repositories.AddressRepository;
import com.bdsystems.builder_api.auth.repositories.CompanyRepository;
import com.bdsystems.builder_api.auth.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRegistrationService implements UserDetailsService {

  private final AddressRepository addressRepository;
  private final CompanyRepository companyRepository;
  private final UserRepository userRepository;

  @Transactional
  public User createUser(UserRequest userRequest) {
    Company company;
    if (userRequest.company().id() != null){
       company = companyRepository.findById(userRequest.company().id())
          .orElseThrow();
    }else {
      company = companyRepository.save(
          new Company(
              userRequest.company().name(),
              addressRepository.save(

                  new Address(
                      userRequest.company().address().street(),
                      userRequest.company().address().city(),
                      userRequest.company().address().state(),
                      userRequest.company().address().zipcode()
                  ))
          ));
    }

    Address savedAddress = addressRepository.save(new Address(
        userRequest.address().street(),
        userRequest.address().city(),
        userRequest.address().state(),
        userRequest.address().zipcode()
    ));

    User user = new User(
        null,
        userRequest.name(),
        userRequest.email(),
        userRequest.password(),
        userRequest.role() != null ? Role.valueOf(userRequest.role()) : Role.USER,
        savedAddress,
        company
    );

    return userRepository.save(user);
  }

  @Override
  public UserDetails loadUserByUsername(@NonNull String email)
      throws UsernameNotFoundException {

    User user = userRepository.findByEmail(email)
        .orElseThrow(() ->
            new UsernameNotFoundException(
                "User not found: " + email
            )
        );

    return org.springframework.security.core.userdetails.User
        .withUsername(user.getEmail())
        .password(user.getPassword())
        .authorities("USER")
        .build();
  }
}
