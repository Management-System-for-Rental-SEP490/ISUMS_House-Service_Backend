package com.isums.houseservice.domains.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTerminatedEvent {
    private UUID contractId;
    private UUID houseId;
    private UUID tenantId;
    private String messageId;
}