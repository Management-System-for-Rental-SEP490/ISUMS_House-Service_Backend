package com.isums.houseservice.domains.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractCancelledByTenantEvent {
    private String messageId;
    private UUID contractId;
    private UUID houseId;
    private UUID tenantId;
    private String tenantName;
    private String reason;
    private Instant cancelledAt;
    private UUID initiatedByUserId;
}
