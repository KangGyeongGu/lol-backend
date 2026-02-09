package com.lol.backend.modules.catalog.controller;

import com.lol.backend.common.dto.ApiResponse;
import com.lol.backend.common.util.RequestContextHolder;
import com.lol.backend.modules.catalog.dto.ListOfAlgorithmsResponse;
import com.lol.backend.modules.catalog.dto.ListOfItemsResponse;
import com.lol.backend.modules.catalog.dto.ListOfSpellsResponse;
import com.lol.backend.modules.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/algorithms")
    public ApiResponse<ListOfAlgorithmsResponse> getAlgorithms() {
        ListOfAlgorithmsResponse response = catalogService.getAlgorithms();
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @GetMapping("/items")
    public ApiResponse<ListOfItemsResponse> getItems() {
        ListOfItemsResponse response = catalogService.getItems();
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @GetMapping("/spells")
    public ApiResponse<ListOfSpellsResponse> getSpells() {
        ListOfSpellsResponse response = catalogService.getSpells();
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }
}
