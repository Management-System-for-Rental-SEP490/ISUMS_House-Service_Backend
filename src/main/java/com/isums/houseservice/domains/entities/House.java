package com.isums.houseservice.domains.entities;

import common.i18n.TranslationMap;
import common.i18n.TranslationMapConverter;
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

    @Column(name = "name_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap nameTranslations;

    @Column(columnDefinition = "text")
    private String address;

    @Column(columnDefinition = "text")
    private String ward;

    @Column(columnDefinition = "text")
    private String commune;

    @Column(columnDefinition = "text")
    private String city;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap descriptionTranslations;

    @Column(name = "number_of_floors")
    private Integer numberOfFloors;

    @Column(name = "payment_restricted")
    @Builder.Default
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
    @Builder.Default
    private List<FunctionalArea> functionalAreas = new java.util.ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HouseImage> houseImages = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "house")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
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
