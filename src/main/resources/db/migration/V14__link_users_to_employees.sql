ALTER TABLE users
  ALTER COLUMN address_id DROP NOT NULL;

ALTER TABLE employees
  ALTER COLUMN phone_number DROP NOT NULL,
  ALTER COLUMN address_id DROP NOT NULL,
  ALTER COLUMN position DROP NOT NULL;

ALTER TABLE employees
  DROP CONSTRAINT employees_name_key,
  DROP CONSTRAINT employees_phone_number_key;

ALTER TABLE users
  ADD COLUMN employee_id UUID;

DO
$$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM users registered_user
      JOIN employees employee
        ON employee.company_id = registered_user.company_id
          AND lower(employee.email) = lower(registered_user.email)
    GROUP BY registered_user.id
    HAVING count(employee.id) > 1
  ) THEN
    RAISE EXCEPTION 'Cannot link users to employees: ambiguous company and email match';
  END IF;
END
$$;

INSERT INTO employees (
  id,
  name,
  email,
  active,
  created_date,
  company_id,
  status,
  enable
)
SELECT (md5(registered_user.id::text || ':employee'))::uuid,
       registered_user.name,
       registered_user.email,
       TRUE,
       registered_user.created_date,
       registered_user.company_id,
       'ACTIVE',
       FALSE
FROM users registered_user
WHERE NOT EXISTS (
  SELECT 1
  FROM employees employee
  WHERE employee.company_id = registered_user.company_id
    AND lower(employee.email) = lower(registered_user.email)
);

UPDATE users registered_user
SET employee_id = employee.id
FROM employees employee
WHERE employee.company_id = registered_user.company_id
  AND lower(employee.email) = lower(registered_user.email);

ALTER TABLE company
  ADD CONSTRAINT uk_company_name UNIQUE (name);

ALTER TABLE employees
  ADD CONSTRAINT uk_employees_id_company UNIQUE (id, company_id);

ALTER TABLE users
  ADD CONSTRAINT fk_users_employee
    FOREIGN KEY (employee_id) REFERENCES employees (id),
  ADD CONSTRAINT fk_users_employee_company
    FOREIGN KEY (employee_id, company_id) REFERENCES employees (id, company_id),
  ADD CONSTRAINT uk_users_employee UNIQUE (employee_id),
  ALTER COLUMN employee_id SET NOT NULL;
