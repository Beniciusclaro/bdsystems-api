package com.bdsystems.bdsystems_api.employee.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.auth.repositories.UserRepository;
import com.bdsystems.bdsystems_api.auth.services.UserInvitationService;
import com.bdsystems.bdsystems_api.employee.domains.dtos.EmployeeRequest;
import com.bdsystems.bdsystems_api.employee.domains.dtos.EmployeeResponse;
import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class EmployeeService {

  private final EmployeeRepository employeeRepository;
  private final CompanyRepository companyRepository;
  private final AddressRepository addressRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserInvitationService invitationService;

  public EmployeeService(
    EmployeeRepository employeeRepository,
    CompanyRepository companyRepository,
    AddressRepository addressRepository,
    UserRepository userRepository,
    PasswordEncoder passwordEncoder,
    UserInvitationService invitationService
  ) {
    this.employeeRepository = employeeRepository;
    this.companyRepository = companyRepository;
    this.addressRepository = addressRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.invitationService = invitationService;
  }

  @Transactional
  public EmployeeResponse create(EmployeeRequest request,
    UUID companyId) {
    if (request.employeeNumber() != null && employeeRepository.existsByCompanyIdAndEmployeeNumber(
      companyId,
      request.employeeNumber()
    )) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Employee number already registered for this company"
      );
    }
    if (employeeRepository.existsByEmail(request.email())) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Employee email already registered"
      );
    }

    Company company = companyRepository.findById(companyId)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Company not found"
      ));

    EmployeeRequest.AddressRequest addressRequest = request.address();
    Address address = addressRepository.save(new Address(
      addressRequest.street(),
      addressRequest.city(),
      addressRequest.state(),
      addressRequest.zipCode()
    ));

    Employee employee = new Employee(
      request.fullName(),
      request.email(),
      request.phoneNumber(),
      request.documentNumber(),
      request.birthDate(),
      address,
      company,
      request.employeeNumber(),
      request.position(),
      request.department(),
      request.admissionDate(),
      request.terminationDate(),
      request.status(),
      request.paymentMethod(),
      request.paymentAmount()
    );

    employee = employeeRepository.save(employee);
    if (request.userRole() != null) {
      if (userRepository.existsByEmail(request.email())) {
        throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Email already registered"
        );
      }
      User user = userRepository.save(User.invited(
        request.fullName(),
        request.email(),
        passwordEncoder.encode(UUID.randomUUID().toString()),
        request.userRole(),
        company,
        employee
      ));
      invitationService.createAndSend(user);
    }

    return toResponse(employee);
  }

  @Transactional
  public EmployeeResponse update(UUID id,
    EmployeeRequest request,
    UUID companyId) {
    Employee employee = employeeRepository.findByIdAndCompanyIdAndEnableFalse(id,
        companyId)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Employee not found"
      ));

    if (request.employeeNumber() != null && employeeRepository.existsByCompanyIdAndEmployeeNumberAndIdNot(
      companyId,
      request.employeeNumber(),
      id
    )) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Employee number already registered for this company"
      );
    }
    if (employeeRepository.existsByEmailAndIdNot(
      request.email(),
      id
    )) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Employee email already registered"
      );
    }

    var linkedUser = userRepository.findByEmployeeId(id).orElse(null);
    if (linkedUser != null && userRepository.existsByEmailAndEmployeeIdNot(
      request.email(),
      id
    )) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Email already registered"
      );
    }
    if (linkedUser != null && linkedUser.isActive()
      && request.status() != EmployeeStatus.ACTIVE) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "An employee linked to an active user cannot be deactivated"
      );
    }

    EmployeeRequest.AddressRequest addressRequest = request.address();
    if (employee.getAddress() == null) {
      employee.updateAddress(addressRepository.save(new Address(
        addressRequest.street(),
        addressRequest.city(),
        addressRequest.state(),
        addressRequest.zipCode()
      )));
    } else {
      employee.getAddress().update(
        addressRequest.street(),
        addressRequest.city(),
        addressRequest.state(),
        addressRequest.zipCode()
      );
    }
    employee.updatePersonalDetails(
      request.fullName(),
      request.email(),
      request.phoneNumber(),
      request.documentNumber(),
      request.birthDate()
    );
    employee.updateEmploymentDetails(
      request.employeeNumber(),
      request.position(),
      request.department(),
      request.admissionDate(),
      request.terminationDate(),
      request.status(),
      request.paymentMethod(),
      request.paymentAmount()
    );
    if (linkedUser != null) {
      linkedUser.updatePersonalDetails(
        request.fullName(),
        request.email(),
        linkedUser.getPhoneNumber(),
        linkedUser.getDocumentNumber(),
        linkedUser.getBirthDate()
      );
      userRepository.save(linkedUser);
    }

    return toResponse(employeeRepository.save(employee));
  }

  @Transactional
  public void delete(UUID id,
    UUID companyId) {
    Employee employee = employeeRepository.findByIdAndCompanyIdAndEnableFalse(id,
        companyId)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Employee not found"
      ));
    if (userRepository.existsByEmployeeIdAndActiveTrue(id)) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "An employee linked to an active user cannot be deleted"
      );
    }
    employee.markAsDeleted();
    employeeRepository.save(employee);
  }

  @Transactional
  public Page<EmployeeResponse> findAll(
    UUID companyId,
    Pageable pageable,
    EmployeeStatus status,
    String position,
    String department
  ) {
    return employeeRepository.findByFilters(
        companyId,
        status,
        normalizeFilter(position),
        normalizeFilter(department),
        pageable
      )
      .map(this::toResponse);
  }

  @Transactional
  public void resendInvitation(UUID employeeId,
    UUID companyId) {
    Employee employee = employeeRepository.findByIdAndCompanyIdAndEnableFalse(employeeId,
        companyId)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Employee not found"
      ));
    User user = userRepository.findByEmployeeId(employee.getId())
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Employee does not have user access"
      ));
    invitationService.resend(user);
  }

  private String normalizeFilter(String value) {
    return value == null || value.isBlank() ? "" : value.trim();
  }

  private EmployeeResponse toResponse(Employee employee) {
    var address = employee.getAddress();
    User linkedUser = userRepository.findByEmployeeId(employee.getId()).orElse(null);

    return new EmployeeResponse(
      employee.getId(),
      employee.getFullName(),
      employee.getEmail(),
      employee.getPhoneNumber(),
      employee.getDocumentNumber(),
      employee.getBirthDate(),
      address == null ? null : new EmployeeResponse.AddressResponse(
        address.getStreet(),
        address.getCity(),
        address.getState(),
        address.getZipCode()
      ),
      employee.getCompany().getId(),
      employee.getEmployeeNumber(),
      employee.getPosition(),
      employee.getDepartment(),
      employee.getAdmissionDate(),
      employee.getTerminationDate(),
      employee.getStatus(),
      employee.getPaymentMethod(),
      employee.getPaymentAmount(),
      linkedUser == null ? null : linkedUser.getRole(),
      invitationService.statusFor(linkedUser)
    );
  }
}
