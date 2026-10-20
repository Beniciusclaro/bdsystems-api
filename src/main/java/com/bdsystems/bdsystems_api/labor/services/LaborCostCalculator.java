package com.bdsystems.bdsystems_api.labor.services;

import com.bdsystems.bdsystems_api.employee.domains.entities.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class LaborCostCalculator {
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  public BigDecimal hourlyCost(PaymentMethod method,
    BigDecimal payment,
    BigDecimal productiveHours,
    BigDecimal workDays,
    BigDecimal chargesPercent,
    BigDecimal benefits,
    BigDecimal otherCosts) {
    BigDecimal base = switch (method) {
      case MONTHLY -> payment;
      case WEEKLY -> payment.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12),
        6,
        RoundingMode.HALF_UP);
      case DAILY -> payment.multiply(workDays);
      case HOURLY -> payment.multiply(productiveHours);
    };
    BigDecimal charged = base.multiply(BigDecimal.ONE.add(chargesPercent.divide(HUNDRED,
      8,
      RoundingMode.HALF_UP)));
    return charged.add(benefits).add(otherCosts).divide(productiveHours,
      4,
      RoundingMode.HALF_UP);
  }
}
