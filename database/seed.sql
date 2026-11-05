-- RecallRelay demo dataset

USE recallrelay;

-- Accounts stay disabled until configured privately.
SET @demo_hash = 'LOCAL_PASSWORD_NOT_CONFIGURED';

INSERT INTO organizations (id, organization_code, name, email_domain)
VALUES (1, 'OHC-001-001', 'Northstar Automotive Group', 'northstar.example');

INSERT INTO dealerships (id, organization_id, name, active) VALUES
  (12, 1, 'Downtown Subaru', TRUE),
  (13, 1, 'Northgate Subaru', TRUE),
  (14, 1, 'Riverside Subaru', TRUE);

INSERT INTO employees
(id, organization_id, dealership_id, employee_login_id, employee_name, company_email, phone, department, password_hash, role, active, must_change_password)
VALUES
  (1001, 1, NULL, 'hadmin', 'Helen Admin', 'admin@northstar.example', '(555) 100-1001', 'HEAD_OFFICE', @demo_hash, 'HEAD_OFFICE_ADMIN', FALSE, TRUE),
  (1010, 1, NULL, 'homgr', 'Morgan Hill', 'homgr@northstar.example', '(555) 100-1010', 'HEAD_OFFICE', @demo_hash, 'HEAD_OFFICE_MANAGER', FALSE, TRUE),
  (2041, 1, 12, 'jlee', 'Celine Lee', 'jlee@northstar.example', '(555) 204-1001', 'SERVICE', @demo_hash, 'SERVICE_MANAGER', FALSE, TRUE),
  (2201, 1, 12, 'maria', 'Maria Park', 'maria@northstar.example', '(555) 220-1001', 'SERVICE', @demo_hash, 'APPOINTMENT_COORDINATOR', FALSE, TRUE),
  (2301, 1, 12, 'parts01', 'Jordan Porter', 'parts01@northstar.example', '(555) 230-1001', 'PARTS', @demo_hash, 'PARTS_SPECIALIST', FALSE, TRUE),
  (2101, 1, 12, 'advisor01', 'Sarah Lee', 'advisor01@northstar.example', '(555) 210-1001', 'SERVICE', @demo_hash, 'SERVICE_ADVISOR', FALSE, TRUE),
  (3001, 1, 12, 'tech01', 'Kevin Adams', 'tech01@northstar.example', '(555) 300-1001', 'SERVICE', @demo_hash, 'TECHNICIAN', FALSE, TRUE),
  (3002, 1, 12, 'tech02', 'Alex Kim', 'tech02@northstar.example', '(555) 300-1002', 'SERVICE', @demo_hash, 'TECHNICIAN', FALSE, TRUE);

-- No predefined manager activation codes are published.

INSERT INTO recall_campaigns
(id, organization_id, recall_no, campaign_type, description, applicable_vehicle, campaign_date, campaign_status, severity, safety_impact)
VALUES
  (501, 1, 'WXR-2026-14', 'Safety Recall', 'Fuel Pump Relay', 'VIN-based target', '2026-08-01', 'ACTIVE', 'HIGH', 'Engine stall risk'),
  (502, 1, 'WQH-21-03', 'Safety Recall', 'Brake control module inspection', 'Selected 2020-2022 models', '2026-07-18', 'ACTIVE', 'MEDIUM', 'Extended stopping distance risk'),
  (503, 1, 'WQD-22-11', 'Service Campaign', 'Software calibration update', 'Selected 2019-2021 models', '2026-06-25', 'ACTIVE', 'LOW', 'Driveability improvement');

INSERT INTO dealership_campaigns (dealership_id, recall_campaign_id, operational_priority) VALUES
  (12, 501, 'P1'),
  (12, 502, 'P2'),
  (12, 503, 'P3'),
  (13, 501, 'P2');

INSERT INTO customers (id, organization_id, customer_no, name, phone) VALUES
  (10001, 1, 'CUST-000123', 'Amy Henderson', '(555) 123-8891'),
  (10002, 1, 'CUST-000124', 'Sarah Thompson', '(555) 220-1187'),
  (10003, 1, 'CUST-000125', 'David Lee', '(555) 741-5090');

INSERT INTO vehicles (id, organization_id, customer_id, vin) VALUES
  (11001, 1, 10001, 'JF2BTABC1M0000011'),
  (11002, 1, 10002, 'JF2SKABC2L0000022'),
  (11003, 1, 10003, '4S4WMABC3K0000033');

INSERT INTO recall_targets (id, recall_campaign_id, vin, matched_vehicle_id) VALUES
  (12001, 501, 'JF2BTABC1M0000011', 11001),
  (12002, 502, 'JF2SKABC2L0000022', 11002),
  (12003, 503, '4S4WMABC3K0000033', 11003),
  (12004, 501, 'JF2BTABC1M0000999', NULL);

INSERT INTO recall_cases
(id, dealership_id, recall_campaign_id, vehicle_id, case_status, ownership_valid, service_area_valid, phone_valid, requested_service_date)
VALUES
  (20881, 12, 501, 11001, 'APPOINTMENT_SCHEDULED', TRUE, TRUE, TRUE, '2026-09-03'),
  (20882, 12, 502, 11002, 'CUSTOMER_CONTACTED', TRUE, TRUE, TRUE, NULL),
  (20883, 12, 503, 11003, 'NEW', TRUE, TRUE, TRUE, '2026-08-22');

INSERT INTO case_parts
(id, recall_case_id, part_no, part_name, status, order_date, order_no, eta, arrived_at)
VALUES
  (31001, 20881, 'PR-10024', 'Fuel Pump Relay', 'IN_TRANSIT', '2026-08-14', 'PO-88410', '2026-09-02', NULL),
  (31002, 20882, 'BR-22031', 'Brake Control Module', 'ARRIVED', '2026-08-10', 'PO-88371', '2026-08-18', '2026-08-18 09:30:00'),
  (31003, 20883, 'SW-01002', 'Calibration Package', 'REQUESTED', NULL, NULL, NULL, NULL);

INSERT INTO appointments
(id, recall_case_id, scheduled_at, appointment_status, technician_id, service_status, additional_service_requested, service_note, service_advisor_id, handoff_status)
VALUES
  (41001, 20881, '2026-09-03 10:00:00', 'SCHEDULED', 3001, 'WAITING', FALSE, 'Recall appointment. Confirm part arrival before visit.', 2101, 'NOT_REQUIRED');

INSERT INTO case_activities
(id, recall_case_id, employee_id, activity_type, direction, channel, reason, summary, outcome, follow_up, callback_due_at, callback_completed_at, completes_callback_activity_id, created_at)
VALUES
  (51001, 20881, 2201, 'CONTACT', 'OUTBOUND', 'PHONE', 'RECALL_OUTREACH', 'Customer reached and recall appointment options discussed.', 'APPOINTMENT_SCHEDULED', 'Confirm part arrival before appointment.', NULL, NULL, NULL, '2026-08-19 10:00:00'),
  (51002, 20882, 2201, 'CONTACT', 'OUTBOUND', 'PHONE', 'RECALL_OUTREACH', 'Customer requested a callback after checking work schedule.', 'CALLBACK_REQUIRED', 'Call customer again.', '2026-08-21 14:00:00', NULL, NULL, '2026-08-19 11:20:00'),
  (51003, 20881, 2041, 'MEMO', NULL, NULL, 'INTERNAL', 'P1 case. Monitor ETA because appointment is already scheduled.', NULL, NULL, NULL, NULL, NULL, '2026-08-19 15:20:00');
