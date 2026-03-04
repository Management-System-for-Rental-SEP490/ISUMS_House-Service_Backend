package com.isums.houseservice.domains.entities;

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

    private String description;
    @Column(nullable = false)
    private UUID managerId;

    @ElementCollection
    @CollectionTable(name = "region_staff", joinColumns = @JoinColumn(name = "region_id"))
    @Column(name = "staff_id")
    private List<UUID> technicalStaffIds = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<House> houses = new ArrayList<>();
}
