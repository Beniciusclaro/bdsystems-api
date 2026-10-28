package com.bdsystems.bdsystems_api.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

  @Bean
  JavaMailSender javaMailSender(
    @Value("${spring.mail.host}") String host,
    @Value("${spring.mail.port}") int port,
    @Value("${spring.mail.username:}") String username,
    @Value("${spring.mail.password:}") String password,
    @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean authenticationEnabled,
    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnabled
  ) {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(host);
    mailSender.setPort(port);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    Properties mailProperties = mailSender.getJavaMailProperties();
    mailProperties.put("mail.smtp.auth",
      Boolean.toString(authenticationEnabled));
    mailProperties.put("mail.smtp.starttls.enable",
      Boolean.toString(startTlsEnabled));
    return mailSender;
  }
}
