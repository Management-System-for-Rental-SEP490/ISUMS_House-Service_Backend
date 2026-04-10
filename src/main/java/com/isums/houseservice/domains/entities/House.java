package com.isums.houseservice.domains.entities;

import com.isums.houseservice.domains.emuns.HouseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "houses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class House {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_rental_id")
    private UUID userRentalId;

    @Column(name = "tenant_group_id")
    private UUID tenantGroupId;

    @Column(columnDefinition = "text")
    private String name;

    @Column(columnDefinition = "text")
    private String address;

    @Column(columnDefinition = "text")
    private String commune;

    @Column(columnDefinition = "text")
    private String city;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "payment_restricted")
    private Boolean paymentRestricted = false;

    @Column(name = "handover_date")
    private Instant handoverDate;

    @Enumerated(EnumType.STRING)
    private HouseStatus status;

    @Column(name = "next_tenant_id")
    private UUID nextTenantId;

    @Column(name = "next_handover_date")
    private Instant nextHandoverDate;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FunctionalArea> functionalAreas = new java.util.ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HouseImage> houseImages = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "house")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<InstalledDevice> devices = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    public void addFunctionalArea(FunctionalArea area) {
        functionalAreas.add(area);
        area.setHouse(this);
    }

    public void removeFunctionalArea(FunctionalArea area) {
        functionalAreas.remove(area);
        area.setHouse(null);
    }
}
