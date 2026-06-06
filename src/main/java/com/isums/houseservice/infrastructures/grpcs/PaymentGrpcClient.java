package com.isums.houseservice.infrastructures.grpcs;

import com.isums.houseservice.domains.dtos.InvoiceStatusDto;
import com.isums.paymentservice.grpc.InvoiceStatusRequest;
import com.isums.paymentservice.grpc.InvoiceStatusResponse;
import com.isums.paymentservice.grpc.PaymentServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentGrpcClient {

    private final PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub;

    public InvoiceStatusDto getInvoiceStatus(UUID houseId, UUID tenantId) {
        try {
            InvoiceStatusResponse res = paymentStub.getInvoiceStatus(
                    InvoiceStatusRequest.newBuilder()
                            .setHouseId(houseId.toString())
                            .setTenantId(tenantId.toString())
                            .build());

            return new InvoiceStatusDto(
                    res.getDepositPaid(),
                    res.getFirstRentPaid(),
                    res.getPendingInvoiceId().isBlank()
                            ? null : UUID.fromString(res.getPendingInvoiceId()));
        } catch (Exception e) {
            log.warn("[gRPC] getInvoiceStatus failed houseId={}: {}", houseId, e.getMessage());
            return new InvoiceStatusDto(false, false, null);
        }
    }
}
