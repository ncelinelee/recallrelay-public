package com.recallrelay.casework.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RecallCaseDetailResponse(
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
        String operationalPriority,
        List<PartResponse> parts,
        AppointmentResponse appointment,
        List<ActivityResponse> activities
) {

    public record OrganizationResponse(
            Long organizationId,
            String organizationCode,
            String name
    ) {}

    public record DealershipResponse(
            Long dealershipId,
            String name
    ) {}

    public record CustomerResponse(
            Long customerId,
            String customerNo,
            String name,
            String phone
    ) {}

    public record VehicleResponse(
            Long vehicleId,
            String vin
    ) {}

    public record RecallResponse(
            Long recallCampaignId,
            String recallNo,
            String campaignType,
            String description,
            String applicableVehicle,
            LocalDate campaignDate,
            String campaignStatus,
            String severity,
            String safetyImpact
    ) {}

    public record PartResponse(
            Long partId,
            String partNo,
            String partName,
            String status,
            LocalDate orderDate,
            String orderNo,
            LocalDate eta,
            LocalDateTime arrivedAt,
            LocalDateTime updatedAt
    ) {}

    public record AppointmentResponse(
            Long appointmentId,
            LocalDateTime scheduledAt,
            String appointmentStatus,
            Long technicianId,
            String technicianName,
            String serviceStatus,
            Boolean additionalServiceRequested,
            String serviceNote,
            Long serviceAdvisorId,
            String serviceAdvisorName,
            String handoffStatus,
            LocalDateTime updatedAt
    ) {}

    public record ActivityResponse(
            Long activityId,
            Long employeeId,
            String employeeName,
            String activityType,
            String direction,
            String channel,
            String reason,
            String summary,
            String outcome,
            String followUp,
            LocalDateTime callbackDueAt,
            LocalDateTime callbackCompletedAt,
            LocalDateTime createdAt
    ) {}
}
