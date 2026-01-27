package com.isums.houseservice.domains.entities;

import com.isums.houseservice.domains.emuns.HouseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
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

    private UUID userRentalId;

    @Column(columnDefinition = "text")
    private String name;

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

    @Enumerated(EnumType.STRING)
    private HouseStatus status;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FunctionalArea> functionalAreas = new java.util.ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    public void addFunctionalArea(FunctionalArea area) {
        functionalAreas.add(area);
        area.setHouse(this);
    }

    public void removeFunctionalArea(FunctionalArea area) {
        functionalAreas.remove(area);
        area.setHouse(null);
    }
}
