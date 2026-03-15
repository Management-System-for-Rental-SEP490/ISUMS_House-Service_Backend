package com.isums.houseservice.controllers;


import com.isums.houseservice.domains.dtos.FunctionalAreaDto.GetFunctionalAreasRequest;
import com.isums.houseservice.infrastructures.abstracts.FunctionalAreaService;
import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.CreateFunctionalAreaRequest;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.UpdateFunctionalAreaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses/functionalAreas")
@RequiredArgsConstructor
public class FunctionalAreaController {
    private final FunctionalAreaService functionalAreaService;

    @PostMapping
    public ApiResponse<FunctionalAreaDto> CreateArea(@RequestBody CreateFunctionalAreaRequest request){
        FunctionalAreaDto response = functionalAreaService.createArea(request);
        return ApiResponses.ok(response,"Create area successfully");
    }

    @GetMapping("/{houseId}")
    public  ApiResponse<List<FunctionalAreaDto>> GetAllAreas(@PathVariable UUID houseId){
        List<FunctionalAreaDto> response = functionalAreaService.getAllAreas(houseId);
        return  ApiResponses.ok(response,"Get all areas successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<FunctionalAreaDto> UpdateArea(@PathVariable UUID id, @RequestBody UpdateFunctionalAreaRequest request){
        FunctionalAreaDto response = functionalAreaService.updateArea(id,request);
        return  ApiResponses.ok(response,"Update successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> DeleteArea(@PathVariable UUID id){
        Boolean response = functionalAreaService.deleteArea(id);
        return ApiResponses.ok(response,"Delete successfully");
    }
}
