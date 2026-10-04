package com.bdsystems.bdsystems_api.auth.exceptions;

public class EmailAlreadyRegisteredException extends RuntimeException {

  public EmailAlreadyRegisteredException(String email) {
    super("E-mail já cadastrado: " + email);
  }
}