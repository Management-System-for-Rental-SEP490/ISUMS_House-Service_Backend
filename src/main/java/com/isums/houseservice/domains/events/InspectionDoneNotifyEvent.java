package com.isums.houseservice.domains.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionDoneNotifyEvent {
    private UUID contractId;
    private UUID inspectionId;
    private UUID managerId;
    private UUID houseId;
    private UUID tenantId;
    private Long deductionAmount;
    private Instant effectiveAt;
    private String messageId;
}
