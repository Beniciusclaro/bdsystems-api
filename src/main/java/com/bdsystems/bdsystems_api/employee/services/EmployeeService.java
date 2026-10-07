package com.bdsystems.bdsystems_api.employee.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.AddressRepository;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.employee.domains.dtos.EmployeeRequest;
import com.bdsystems.bdsystems_api.employee.domains.dtos.EmployeeResponse;
import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class EmployeeService {

  private final EmployeeRepository employeeRepository;
  private final CompanyRepository companyRepository;
  private final AddressRepository addressRepository;

  public EmployeeService(
      EmployeeRepository employeeRepository,
      CompanyRepository companyRepository,
      AddressRepository addressRepository
  ) {
    this.employeeRepository = employeeRepository;
    this.companyRepository = companyRepository;
    this.addressRepository = addressRepository;
  }

  @Transactional
  public EmployeeResponse create(EmployeeRequest request, UUID companyId) {
    if (employeeRepository.existsByCompanyIdAndEmployeeNumber(
        companyId,
        request.employeeNumber()
    )) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Employee number already registered for this company"
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
        request.status()
    );

    return toResponse(employeeRepository.save(employee));
  }

  private EmployeeResponse toResponse(Employee employee) {
    var address = employee.getAddress();

    return new EmployeeResponse(
        employee.getId(),
        employee.getFullName(),
        employee.getEmail(),
        employee.getPhoneNumber(),
        employee.getDocumentNumber(),
        employee.getBirthDate(),
        new EmployeeResponse.AddressResponse(
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
        employee.getStatus()
    );
  }
}
