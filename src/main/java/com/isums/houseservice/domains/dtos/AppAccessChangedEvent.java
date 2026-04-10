package com.isums.houseservice.domains.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppAccessChangedEvent {
    private UUID tenantId;
    private UUID houseId;
    private UUID contractId;
    private boolean restricted;
    private String reason;
    private String messageId;
}
