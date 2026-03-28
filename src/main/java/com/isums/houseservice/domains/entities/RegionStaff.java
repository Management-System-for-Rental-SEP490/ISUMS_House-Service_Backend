package com.isums.houseservice.domains.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "region_staff",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"region_id", "staff_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionStaff {

    @EmbeddedId
    private RegionStaffId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("regionId")
    @JoinColumn(name = "region_id")
    private Region region;
}