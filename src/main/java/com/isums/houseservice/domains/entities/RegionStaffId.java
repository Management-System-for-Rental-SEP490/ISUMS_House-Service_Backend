package com.isums.houseservice.domains.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionStaffId implements Serializable {

    private UUID regionId;
    private UUID staffId;
}