package com.isums.houseservice.domains.dtos;

import java.util.UUID;

public record InvoiceStatusDto(
        boolean depositPaid,
        boolean firstRentPaid,
        UUID pendingInvoiceId
) {
}
