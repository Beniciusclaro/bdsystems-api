package com.bdsystems.bdsystems_api.labor.services;

import com.bdsystems.bdsystems_api.employee.domains.entities.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LaborCostCalculatorTest {
  private final LaborCostCalculator calculator = new LaborCostCalculator();

  @Test
  void calculatesMonthlyCostWithChargesAndBenefits() {
    assertEquals(new BigDecimal("15.0000"),
      calculator.hourlyCost(PaymentMethod.MONTHLY,
        new BigDecimal("1600"),
        new BigDecimal("140"),
        null,
        new BigDecimal("25"),
        new BigDecimal("50"),
        new BigDecimal("50")));
  }

  @Test
  void normalizesDailyAndHourlyPayments() {
    assertEquals(new BigDecimal("13.7500"),
      calculator.hourlyCost(PaymentMethod.DAILY,
        new BigDecimal("100"),
        new BigDecimal("160"),
        new BigDecimal("20"),
        new BigDecimal("10"),
        BigDecimal.ZERO,
        BigDecimal.ZERO));
    assertEquals(new BigDecimal("22.0000"),
      calculator.hourlyCost(PaymentMethod.HOURLY,
        new BigDecimal("20"),
        new BigDecimal("100"),
        null,
        new BigDecimal("10"),
        BigDecimal.ZERO,
        BigDecimal.ZERO));
  }
}
