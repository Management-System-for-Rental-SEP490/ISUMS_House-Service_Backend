package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.HouseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HouseDto {
    private String id;
    private String userRentalId;
    private String name;
    private String address;
    private String ward;
    private String commune;
    private String city;
    private String description;
    private HouseStatus status;
}
