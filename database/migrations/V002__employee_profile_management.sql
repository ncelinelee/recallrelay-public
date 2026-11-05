USE recallrelay;

-- Existing RecallRelay v1.2 DB upgrade:
-- 1) employee display name
-- 2) soft-delete timestamp so historical Case / Interaction links remain intact
ALTER TABLE employees
  ADD COLUMN employee_name VARCHAR(120) NULL AFTER employee_login_id,
  ADD COLUMN deleted_at TIMESTAMP NULL AFTER must_change_password;

UPDATE employees
SET employee_name = CASE employee_login_id
  WHEN 'hadmin' THEN 'Helen Admin'
  WHEN 'jlee' THEN 'Jordan Lee'
  WHEN 'maria' THEN 'Maria Park'
  WHEN 'parts01' THEN 'Jordan Porter'
  WHEN 'advisor01' THEN 'Sarah Lee'
  WHEN 'tech01' THEN 'Kevin Adams'
  WHEN 'tech02' THEN 'Alex Kim'
  ELSE employee_login_id
END
WHERE employee_name IS NULL OR TRIM(employee_name) = '';

ALTER TABLE employees
  MODIFY COLUMN employee_name VARCHAR(120) NOT NULL;

CREATE INDEX idx_employees_scope_deleted
  ON employees (organization_id, dealership_id, deleted_at);
