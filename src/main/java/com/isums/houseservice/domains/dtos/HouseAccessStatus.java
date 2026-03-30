package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.AccessStatus;

import java.time.Instant;
import java.util.UUID;

public record HouseAccessStatus(
        UUID houseId,
        String houseName,
        String address,

        Instant handoverDate,

        AccessStatus accessStatus,

        String reason,

        boolean hasUnpaidInvoice,

        UUID pendingInvoiceId
) {
}