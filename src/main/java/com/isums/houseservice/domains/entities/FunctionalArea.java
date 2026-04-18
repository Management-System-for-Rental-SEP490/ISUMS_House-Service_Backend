package com.isums.houseservice.domains.entities;

import common.i18n.TranslationMap;
import common.i18n.TranslationMapConverter;
import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;
import jakarta.persistence.*;
import lombok.*;
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
    @ToString.Exclude
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @Column(columnDefinition = "text")
    private String name;

    @Column(name = "name_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap nameTranslations;

    @Enumerated(EnumType.STRING)
    private AreaType areaType;

    private String floorNo;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap descriptionTranslations;

    @Enumerated(EnumType.STRING)
    private FuctionalAreaStatus status;

    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
