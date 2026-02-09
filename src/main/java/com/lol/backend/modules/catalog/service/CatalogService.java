package com.lol.backend.modules.catalog.service;

import com.lol.backend.modules.catalog.dto.*;
import com.lol.backend.modules.catalog.repo.AlgorithmRepository;
import com.lol.backend.modules.catalog.repo.ItemRepository;
import com.lol.backend.modules.catalog.repo.SpellRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final AlgorithmRepository algorithmRepository;
    private final ItemRepository itemRepository;
    private final SpellRepository spellRepository;

    @Transactional(readOnly = true)
    public ListOfAlgorithmsResponse getAlgorithms() {
        List<AlgorithmSummaryResponse> items = algorithmRepository.findByIsActiveTrue()
                .stream()
                .map(AlgorithmSummaryResponse::from)
                .toList();
        return new ListOfAlgorithmsResponse(items);
    }

    @Transactional(readOnly = true)
    public ListOfItemsResponse getItems() {
        List<ItemSummaryResponse> items = itemRepository.findByIsActiveTrue()
                .stream()
                .map(ItemSummaryResponse::from)
                .toList();
        return new ListOfItemsResponse(items);
    }

    @Transactional(readOnly = true)
    public ListOfSpellsResponse getSpells() {
        List<SpellSummaryResponse> items = spellRepository.findByIsActiveTrue()
                .stream()
                .map(SpellSummaryResponse::from)
                .toList();
        return new ListOfSpellsResponse(items);
    }
}
