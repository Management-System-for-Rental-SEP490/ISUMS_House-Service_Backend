package com.isums.houseservice.domains.entities;

import com.isums.houseservice.domains.emuns.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "installed_devices",
        indexes = {
                @Index(name = "idx_installed_device_thing", columnList = "thing_name"),
                @Index(name = "idx_installed_device_house", columnList = "house_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class InstalledDevice {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "thing_name", nullable = false, unique = true)
    private String thingName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "health_status", nullable = false)
    private String healthStatus = "uknown";

    @CreationTimestamp
    @Column(name = "installed_at")
    private Instant installedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", insertable = false, updatable = false)
    private House house;
}
