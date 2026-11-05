-- RecallRelay v1.5 - MariaDB schema
-- 12-table ERD aligned with the technical specification and OpenAPI contract.
-- Calculated values intentionally NOT stored: partReadiness, recallOutreachEligibility, nextAction.

CREATE DATABASE IF NOT EXISTS recallrelay
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE recallrelay;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS case_activities;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS case_parts;
DROP TABLE IF EXISTS recall_cases;
DROP TABLE IF EXISTS recall_targets;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS dealership_campaigns;
DROP TABLE IF EXISTS recall_campaigns;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS registration_requests;
DROP TABLE IF EXISTS manager_activation_codes;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS dealerships;
DROP TABLE IF EXISTS organizations;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE organizations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_code VARCHAR(50) NOT NULL,
  name VARCHAR(150) NOT NULL,
  email_domain VARCHAR(150) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_organizations_code UNIQUE (organization_code)
) ENGINE=InnoDB;

CREATE TABLE dealerships (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  name VARCHAR(150) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_dealerships_org_active (organization_id, active),
  CONSTRAINT fk_dealerships_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE employees (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  dealership_id BIGINT NULL,
  employee_login_id VARCHAR(80) NOT NULL,
  employee_name VARCHAR(120) NOT NULL,
  company_email VARCHAR(150) NOT NULL,
  phone VARCHAR(40) NULL,
  department ENUM('SERVICE','PARTS','HEAD_OFFICE') NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM(
    'HEAD_OFFICE_ADMIN',
    'HEAD_OFFICE_MANAGER',
    'HEAD_OFFICE_STAFF',
    'CALL_CENTER_AGENT',
    'SERVICE_MANAGER',
    'APPOINTMENT_COORDINATOR',
    'PARTS_SPECIALIST',
    'SERVICE_ADVISOR',
    'TECHNICIAN'
  ) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
  deleted_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_employees_org_login UNIQUE (organization_id, employee_login_id),
  CONSTRAINT uq_employees_org_email UNIQUE (organization_id, company_email),
  KEY idx_employees_dealership_role (dealership_id, role, active),
  KEY idx_employees_scope_deleted (organization_id, dealership_id, deleted_at),
  CONSTRAINT fk_employees_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_employees_dealership
    FOREIGN KEY (dealership_id) REFERENCES dealerships(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE manager_activation_codes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  dealership_id BIGINT NULL,
  employee_login_id VARCHAR(80) NOT NULL,
  manager_role ENUM('SERVICE_MANAGER','HEAD_OFFICE_MANAGER') NOT NULL,
  code_hash CHAR(64) NOT NULL COMMENT 'SHA-256 hash of company-issued one-time manager code',
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_manager_codes_lookup (organization_id, employee_login_id, manager_role, active),
  CONSTRAINT fk_manager_codes_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_manager_codes_dealership FOREIGN KEY (dealership_id) REFERENCES dealerships(id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE registration_requests (
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

CREATE TABLE password_reset_tokens (
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

CREATE TABLE recall_campaigns (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  recall_no VARCHAR(80) NOT NULL,
  campaign_type VARCHAR(80) NOT NULL,
  description TEXT NULL,
  applicable_vehicle VARCHAR(255) NULL,
  campaign_date DATE NOT NULL,
  campaign_status VARCHAR(30) NOT NULL,
  severity ENUM('CRITICAL','HIGH','MEDIUM','LOW') NOT NULL,
  safety_impact TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_campaigns_org_recall UNIQUE (organization_id, recall_no),
  KEY idx_campaigns_org_status (organization_id, campaign_status),
  CONSTRAINT fk_campaigns_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE dealership_campaigns (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dealership_id BIGINT NOT NULL,
  recall_campaign_id BIGINT NOT NULL,
  operational_priority ENUM('P1','P2','P3') NOT NULL DEFAULT 'P2',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_dealership_campaign UNIQUE (dealership_id, recall_campaign_id),
  KEY idx_dealership_campaign_priority (dealership_id, operational_priority),
  CONSTRAINT fk_dealership_campaign_dealership
    FOREIGN KEY (dealership_id) REFERENCES dealerships(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_dealership_campaign_campaign
    FOREIGN KEY (recall_campaign_id) REFERENCES recall_campaigns(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE customers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  customer_no VARCHAR(80) NOT NULL,
  name VARCHAR(150) NOT NULL,
  phone VARCHAR(50) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_customers_org_customer_no UNIQUE (organization_id, customer_no),
  KEY idx_customers_org_name (organization_id, name),
  CONSTRAINT fk_customers_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE vehicles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  vin VARCHAR(17) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_vehicles_org_vin UNIQUE (organization_id, vin),
  KEY idx_vehicles_customer (customer_id),
  CONSTRAINT fk_vehicles_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_vehicles_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_vehicle_vin_len CHECK (CHAR_LENGTH(vin) = 17)
) ENGINE=InnoDB;

CREATE TABLE recall_targets (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recall_campaign_id BIGINT NOT NULL,
  vin VARCHAR(17) NOT NULL,
  matched_vehicle_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_targets_campaign_vin UNIQUE (recall_campaign_id, vin),
  KEY idx_recall_targets_vin (vin),
  KEY idx_recall_targets_matched_vehicle (matched_vehicle_id),
  CONSTRAINT fk_targets_campaign
    FOREIGN KEY (recall_campaign_id) REFERENCES recall_campaigns(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_targets_matched_vehicle
    FOREIGN KEY (matched_vehicle_id) REFERENCES vehicles(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_target_vin_len CHECK (CHAR_LENGTH(vin) = 17)
) ENGINE=InnoDB;

CREATE TABLE recall_cases (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dealership_id BIGINT NOT NULL,
  recall_campaign_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  case_status ENUM(
    'NEW',
    'CUSTOMER_CONTACTED',
    'APPOINTMENT_SCHEDULED',
    'SERVICE_COMPLETED',
    'CANCELLED'
  ) NOT NULL DEFAULT 'NEW',
  ownership_valid BOOLEAN NOT NULL DEFAULT TRUE,
  service_area_valid BOOLEAN NOT NULL DEFAULT TRUE,
  phone_valid BOOLEAN NOT NULL DEFAULT TRUE,
  requested_service_date DATE NULL COMMENT 'Customer requested service date; demand signal separate from actual appointment',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_cases_campaign_vehicle UNIQUE (recall_campaign_id, vehicle_id),
  KEY idx_cases_dealership_status (dealership_id, case_status),
  KEY idx_cases_vehicle (vehicle_id),
  CONSTRAINT fk_cases_dealership
    FOREIGN KEY (dealership_id) REFERENCES dealerships(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_cases_campaign
    FOREIGN KEY (recall_campaign_id) REFERENCES recall_campaigns(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_cases_vehicle
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE case_parts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recall_case_id BIGINT NOT NULL,
  part_no VARCHAR(100) NOT NULL,
  part_name VARCHAR(150) NULL,
  status ENUM('REQUESTED','ORDERED','BACKORDERED','IN_TRANSIT','ARRIVED') NOT NULL DEFAULT 'REQUESTED',
  order_date DATE NULL,
  order_no VARCHAR(100) NULL,
  eta DATE NULL,
  arrived_at TIMESTAMP NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_case_parts_case_part_no UNIQUE (recall_case_id, part_no),
  KEY idx_case_parts_part_no (part_no),
  KEY idx_case_parts_status_eta (status, eta),
  CONSTRAINT fk_case_parts_case
    FOREIGN KEY (recall_case_id) REFERENCES recall_cases(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE appointments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recall_case_id BIGINT NOT NULL,
  scheduled_at TIMESTAMP NOT NULL,
  appointment_status ENUM('SCHEDULED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  technician_id BIGINT NULL,
  service_status ENUM('SCHEDULED','CHECKED_IN','IN_PROGRESS','WAITING','COMPLETED') NOT NULL DEFAULT 'SCHEDULED',
  additional_service_requested BOOLEAN NOT NULL DEFAULT FALSE,
  service_note TEXT NULL,
  service_advisor_id BIGINT NULL,
  handoff_status VARCHAR(40) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_appointments_current_case UNIQUE (recall_case_id),
  KEY idx_appointments_technician_time (technician_id, scheduled_at),
  KEY idx_appointments_status_time (appointment_status, scheduled_at),
  CONSTRAINT fk_appointments_case
    FOREIGN KEY (recall_case_id) REFERENCES recall_cases(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_appointments_technician
    FOREIGN KEY (technician_id) REFERENCES employees(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_appointments_service_advisor
    FOREIGN KEY (service_advisor_id) REFERENCES employees(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE case_activities (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recall_case_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  activity_type VARCHAR(30) NOT NULL,
  direction VARCHAR(20) NULL,
  channel VARCHAR(30) NULL,
  reason VARCHAR(100) NULL,
  summary TEXT NOT NULL,
  outcome VARCHAR(80) NULL,
  follow_up TEXT NULL,
  callback_due_at TIMESTAMP NULL,
  callback_completed_at TIMESTAMP NULL,
  completes_callback_activity_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_case_activities_case_created (recall_case_id, created_at),
  KEY idx_case_activities_callback_due (callback_due_at, callback_completed_at),
  KEY idx_case_activities_employee (employee_id),
  CONSTRAINT fk_activities_case
    FOREIGN KEY (recall_case_id) REFERENCES recall_cases(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_activities_employee
    FOREIGN KEY (employee_id) REFERENCES employees(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_activities_callback_source
    FOREIGN KEY (completes_callback_activity_id) REFERENCES case_activities(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;
