package com.lol.backend.modules.shop.repo;

import com.lol.backend.modules.shop.entity.Spell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpellRepository extends JpaRepository<Spell, UUID> {
    List<Spell> findByIsActiveTrue();
    Optional<Spell> findByName(String name);
}
