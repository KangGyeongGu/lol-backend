package com.lol.backend.modules.shop.service;

import com.lol.backend.modules.shop.dto.*;
import com.lol.backend.modules.shop.repo.AlgorithmRepository;
import com.lol.backend.modules.shop.repo.ItemRepository;
import com.lol.backend.modules.shop.repo.SpellRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final AlgorithmRepository algorithmRepository;
    private final ItemRepository itemRepository;
    private final SpellRepository spellRepository;

    public CatalogService(
            AlgorithmRepository algorithmRepository,
            ItemRepository itemRepository,
            SpellRepository spellRepository
    ) {
        this.algorithmRepository = algorithmRepository;
        this.itemRepository = itemRepository;
        this.spellRepository = spellRepository;
    }

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
