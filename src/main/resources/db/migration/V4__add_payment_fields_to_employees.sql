ALTER TABLE employees
  ADD COLUMN payment_method VARCHAR(32),
    ADD COLUMN payment_amount NUMERIC(38, 2);
