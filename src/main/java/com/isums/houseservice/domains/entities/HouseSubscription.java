package com.isums.houseservice.domains.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "house_subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_house_subscriptions_user_house",
                columnNames = {"user_id", "house_id"}),
        indexes = {
                @Index(name = "idx_house_subscriptions_house", columnList = "house_id"),
                @Index(name = "idx_house_subscriptions_user", columnList = "user_id"),
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseSubscription {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "user_email", length = 320)
    private String userEmail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;
}
