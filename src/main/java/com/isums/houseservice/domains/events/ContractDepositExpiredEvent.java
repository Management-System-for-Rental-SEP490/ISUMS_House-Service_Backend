package com.isums.houseservice.domains.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDepositExpiredEvent {
    private String messageId;
    private UUID contractId;
    private UUID tenantId;
    private UUID houseId;
    private UUID landlordId;
}
