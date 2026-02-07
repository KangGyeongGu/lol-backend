package com.lol.backend.modules.shop.repo;

import com.lol.backend.modules.shop.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findByIsActiveTrue();
    Optional<Item> findByName(String name);
}
