-- RecallRelay v1.5 existing-database upgrade
-- Adds company-email self-registration, manager approval, manager one-time codes and password reset tokens.

USE recallrelay;

ALTER TABLE organizations
  ADD COLUMN IF NOT EXISTS email_domain VARCHAR(150) NULL AFTER name;

UPDATE organizations
SET email_domain = 'northstar.example'
WHERE organization_code = 'OHC-001-001' AND (email_domain IS NULL OR email_domain = '');

ALTER TABLE employees
  ADD COLUMN IF NOT EXISTS phone VARCHAR(40) NULL AFTER company_email,
  ADD COLUMN IF NOT EXISTS department ENUM('SERVICE','PARTS','HEAD_OFFICE') NULL AFTER phone;

ALTER TABLE employees
  MODIFY COLUMN role ENUM(
    'HEAD_OFFICE_ADMIN','HEAD_OFFICE_MANAGER','HEAD_OFFICE_STAFF','CALL_CENTER_AGENT',
    'SERVICE_MANAGER','APPOINTMENT_COORDINATOR','PARTS_SPECIALIST','SERVICE_ADVISOR','TECHNICIAN'
  ) NOT NULL;

UPDATE employees SET department = 'PARTS' WHERE role = 'PARTS_SPECIALIST' AND department IS NULL;
UPDATE employees SET department = 'HEAD_OFFICE' WHERE role IN ('HEAD_OFFICE_ADMIN','HEAD_OFFICE_MANAGER','HEAD_OFFICE_STAFF','CALL_CENTER_AGENT') AND department IS NULL;
UPDATE employees SET department = 'SERVICE' WHERE department IS NULL;

ALTER TABLE recall_cases
  ADD COLUMN IF NOT EXISTS requested_service_date DATE NULL COMMENT 'Customer requested service date; demand signal separate from actual appointment';

CREATE TABLE IF NOT EXISTS manager_activation_codes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  dealership_id BIGINT NULL,
  employee_login_id VARCHAR(80) NOT NULL,
  manager_role ENUM('SERVICE_MANAGER','HEAD_OFFICE_MANAGER') NOT NULL,
  code_hash CHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_manager_codes_lookup (organization_id, employee_login_id, manager_role, active),
  CONSTRAINT fk_manager_codes_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_manager_codes_dealership FOREIGN KEY (dealership_id) REFERENCES dealerships(id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS registration_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  dealership_id BIGINT NULL,
  employee_login_id VARCHAR(80) NOT NULL,
  employee_name VARCHAR(120) NOT NULL,
  company_email VARCHAR(150) NOT NULL,
  phone VARCHAR(40) NOT NULL,
  department ENUM('SERVICE','PARTS','HEAD_OFFICE') NOT NULL,
  requested_role ENUM('HEAD_OFFICE_STAFF','CALL_CENTER_AGENT','APPOINTMENT_COORDINATOR','PARTS_SPECIALIST','SERVICE_ADVISOR','TECHNICIAN') NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status ENUM('PENDING_EMAIL_VERIFICATION','PENDING_MANAGER_APPROVAL','APPROVED','REJECTED') NOT NULL,
  verification_code_hash CHAR(64) NULL,
  verification_expires_at DATETIME NULL,
  email_verified_at DATETIME NULL,
  approved_by_employee_id BIGINT NULL,
  approved_at DATETIME NULL,
  rejected_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_registration_pending (organization_id, status, dealership_id, department),
  KEY idx_registration_identity (organization_id, employee_login_id, company_email),
  CONSTRAINT fk_registration_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_registration_dealership FOREIGN KEY (dealership_id) REFERENCES dealerships(id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
  KEY idx_password_reset_employee (employee_id, expires_at, used_at),
  CONSTRAINT fk_password_reset_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- Issue manager activation codes privately; no shared defaults are seeded.
