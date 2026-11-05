package com.recallrelay.casework;

import com.recallrelay.casework.dto.RecallCaseDetailResponse;
import com.recallrelay.casework.dto.RecallCaseDetailResponse.ActivityResponse;
import com.recallrelay.casework.dto.RecallCaseDetailResponse.AppointmentResponse;
import com.recallrelay.casework.dto.RecallCaseDetailResponse.CustomerResponse;
import com.recallrelay.casework.dto.RecallCaseDetailResponse.DealershipResponse;
import com.recallrelay.casework.dto.RecallCaseDetailResponse.OrganizationResponse;
import com.recallrelay.casework.dto.RecallCaseDetailResponse.PartResponse;
import com.recallrelay.casework.dto.RecallCaseDetailResponse.RecallResponse;
import com.recallrelay.casework.dto.RecallCaseDetailResponse.VehicleResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecallCaseQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final CaseAccessGuard accessGuard;

    public RecallCaseQueryService(JdbcTemplate jdbcTemplate, CaseAccessGuard accessGuard) {
        this.accessGuard = accessGuard;
        this.jdbcTemplate = jdbcTemplate;
    }

    public RecallCaseDetailResponse getCaseDetail(Long caseId) {
        accessGuard.requireRead(caseId);

        CaseCore core;

        try {
            core = jdbcTemplate.queryForObject(
                    """
                    SELECT
                        rc.id AS case_id,
                        rc.case_status,
                        rc.ownership_valid,
                        rc.service_area_valid,
                        rc.phone_valid,
                        rc.requested_service_date,

                        o.id AS organization_id,
                        o.organization_code,
                        o.name AS organization_name,

                        d.id AS dealership_id,
                        d.name AS dealership_name,

                        c.id AS customer_id,
                        c.customer_no,
                        c.name AS customer_name,
                        c.phone AS customer_phone,

                        v.id AS vehicle_id,
                        v.vin,

                        r.id AS recall_campaign_id,
                        r.recall_no,
                        r.campaign_type,
                        r.description AS recall_description,
                        r.applicable_vehicle,
                        r.campaign_date,
                        r.campaign_status,
                        r.severity,
                        r.safety_impact,

                        dc.operational_priority

                    FROM recall_cases rc

                    JOIN dealerships d
                      ON d.id = rc.dealership_id

                    JOIN organizations o
                      ON o.id = d.organization_id

                    JOIN vehicles v
                      ON v.id = rc.vehicle_id

                    JOIN customers c
                      ON c.id = v.customer_id

                    JOIN recall_campaigns r
                      ON r.id = rc.recall_campaign_id

                    LEFT JOIN dealership_campaigns dc
                      ON dc.dealership_id = rc.dealership_id
                     AND dc.recall_campaign_id = rc.recall_campaign_id

                    WHERE rc.id = ?
                    """,
                    (rs, rowNum) -> new CaseCore(
                            rs.getLong("case_id"),
                            rs.getString("case_status"),
                            rs.getBoolean("ownership_valid"),
                            rs.getBoolean("service_area_valid"),
                            rs.getBoolean("phone_valid"),
                            toLocalDate(rs.getDate("requested_service_date")),

                            new OrganizationResponse(
                                    rs.getLong("organization_id"),
                                    rs.getString("organization_code"),
                                    rs.getString("organization_name")
                            ),

                            new DealershipResponse(
                                    rs.getLong("dealership_id"),
                                    rs.getString("dealership_name")
                            ),

                            new CustomerResponse(
                                    rs.getLong("customer_id"),
                                    rs.getString("customer_no"),
                                    rs.getString("customer_name"),
                                    rs.getString("customer_phone")
                            ),

                            new VehicleResponse(
                                    rs.getLong("vehicle_id"),
                                    rs.getString("vin")
                            ),

                            new RecallResponse(
                                    rs.getLong("recall_campaign_id"),
                                    rs.getString("recall_no"),
                                    rs.getString("campaign_type"),
                                    rs.getString("recall_description"),
                                    rs.getString("applicable_vehicle"),
                                    toLocalDate(rs.getDate("campaign_date")),
                                    rs.getString("campaign_status"),
                                    rs.getString("severity"),
                                    rs.getString("safety_impact")
                            ),

                            rs.getString("operational_priority")
                    ),
                    caseId
            );

        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Recall case not found: " + caseId
            );
        }

        List<PartResponse> parts = jdbcTemplate.query(
                """
                SELECT
                    id,
                    part_no,
                    part_name,
                    status,
                    order_date,
                    order_no,
                    eta,
                    arrived_at,
                    updated_at
                FROM case_parts
                WHERE recall_case_id = ?
                ORDER BY id
                """,
                (rs, rowNum) -> new PartResponse(
                        rs.getLong("id"),
                        rs.getString("part_no"),
                        rs.getString("part_name"),
                        rs.getString("status"),
                        toLocalDate(rs.getDate("order_date")),
                        rs.getString("order_no"),
                        toLocalDate(rs.getDate("eta")),
                        toLocalDateTime(rs.getTimestamp("arrived_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))
                ),
                caseId
        );

        List<AppointmentResponse> appointments = jdbcTemplate.query(
                """
                SELECT
                    a.id,
                    a.scheduled_at,
                    a.appointment_status,
                    a.technician_id,
                    technician.employee_name AS technician_name,
                    a.service_status,
                    a.additional_service_requested,
                    a.service_note,
                    a.service_advisor_id,
                    advisor.employee_name AS service_advisor_name,
                    a.handoff_status,
                    a.updated_at
                FROM appointments a

                LEFT JOIN employees technician
                  ON technician.id = a.technician_id

                LEFT JOIN employees advisor
                  ON advisor.id = a.service_advisor_id

                WHERE a.recall_case_id = ?
                """,
                (rs, rowNum) -> new AppointmentResponse(
                        rs.getLong("id"),
                        toLocalDateTime(rs.getTimestamp("scheduled_at")),
                        rs.getString("appointment_status"),
                        getNullableLong(rs, "technician_id"),
                        rs.getString("technician_name"),
                        rs.getString("service_status"),
                        rs.getBoolean("additional_service_requested"),
                        rs.getString("service_note"),
                        getNullableLong(rs, "service_advisor_id"),
                        rs.getString("service_advisor_name"),
                        rs.getString("handoff_status"),
                        toLocalDateTime(rs.getTimestamp("updated_at"))
                ),
                caseId
        );

        List<ActivityResponse> activities = jdbcTemplate.query(
                """
                SELECT
                    ca.id,
                    ca.employee_id,
                    e.employee_name,
                    ca.activity_type,
                    ca.direction,
                    ca.channel,
                    ca.reason,
                    ca.summary,
                    ca.outcome,
                    ca.follow_up,
                    ca.callback_due_at,
                    ca.callback_completed_at,
                    ca.created_at
                FROM case_activities ca

                JOIN employees e
                  ON e.id = ca.employee_id

                WHERE ca.recall_case_id = ?
                ORDER BY ca.created_at ASC
                """,
                (rs, rowNum) -> new ActivityResponse(
                        rs.getLong("id"),
                        rs.getLong("employee_id"),
                        rs.getString("employee_name"),
                        rs.getString("activity_type"),
                        rs.getString("direction"),
                        rs.getString("channel"),
                        rs.getString("reason"),
                        rs.getString("summary"),
                        rs.getString("outcome"),
                        rs.getString("follow_up"),
                        toLocalDateTime(rs.getTimestamp("callback_due_at")),
                        toLocalDateTime(rs.getTimestamp("callback_completed_at")),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                caseId
        );

        AppointmentResponse appointment =
                appointments.isEmpty() ? null : appointments.get(0);

        return new RecallCaseDetailResponse(
                core.caseId(),
                core.caseStatus(),
                core.ownershipValid(),
                core.serviceAreaValid(),
                core.phoneValid(),
                core.requestedServiceDate(),
                core.organization(),
                core.dealership(),
                core.customer(),
                core.vehicle(),
                core.recall(),
                core.operationalPriority(),
                parts,
                appointment,
                activities
        );
    }

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static Long getNullableLong(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record CaseCore(
            Long caseId,
            String caseStatus,
            Boolean ownershipValid,
            Boolean serviceAreaValid,
            Boolean phoneValid,
            LocalDate requestedServiceDate,
            OrganizationResponse organization,
            DealershipResponse dealership,
            CustomerResponse customer,
            VehicleResponse vehicle,
            RecallResponse recall,
            String operationalPriority
    ) {}
}
