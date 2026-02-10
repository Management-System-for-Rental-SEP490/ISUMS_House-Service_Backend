package com.isums.houseservice.domains.entities;

import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "functionalAreas")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FunctionalArea {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @Column(columnDefinition = "text")
    private String name;

    @Enumerated(EnumType.STRING)
    private AreaType areaType;

    private String floorNo;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    private FuctionalAreaStatus status;

    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
