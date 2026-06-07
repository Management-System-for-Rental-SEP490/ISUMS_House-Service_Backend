package com.isums.houseservice.domains.entities;

import common.i18n.TranslationMap;
import common.i18n.TranslationMapConverter;
import com.isums.houseservice.domains.emuns.BookingState;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.emuns.HouseStructure;
import jakarta.persistence.*;

import java.math.BigDecimal;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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

    @Column(name = "address_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap addressTranslations;

    @Column(columnDefinition = "text")
    private String ward;

    @Column(name = "ward_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap wardTranslations;

    @Column(columnDefinition = "text")
    private String commune;

    @Column(name = "commune_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap communeTranslations;

    @Column(columnDefinition = "text")
    private String city;

    @Column(name = "city_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap cityTranslations;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap descriptionTranslations;

    @Column(name = "number_of_floors")
    private Integer numberOfFloors;

    @Column(name = "area_m2", precision = 10, scale = 2)
    private BigDecimal areaM2;

    @Enumerated(EnumType.STRING)
    @Column(name = "structure", length = 32)
    private HouseStructure structure;

    @Column(name = "land_cert_number", length = 128)
    private String landCertNumber;

    @Column(name = "land_cert_issue_date")
    private java.time.LocalDate landCertIssueDate;

    @Column(name = "land_cert_issuer")
    private String landCertIssuer;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_state", length = 32)
    @Builder.Default
    private BookingState bookingState = BookingState.NONE;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
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

