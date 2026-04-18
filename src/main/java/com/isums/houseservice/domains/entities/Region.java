package com.isums.houseservice.domains.entities;

import common.i18n.TranslationMap;
import common.i18n.TranslationMapConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "regions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String name;

    @Column(name = "name_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap nameTranslations;

    private String description;

    @Column(name = "description_translations", columnDefinition = "text")
    @Convert(converter = TranslationMapConverter.class)
    private TranslationMap descriptionTranslations;
    @Column(nullable = false)
    private UUID managerId;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegionStaff> staffs = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<House> houses = new ArrayList<>();
}
