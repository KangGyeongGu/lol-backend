package com.lol.backend.modules.shop.repo;

import com.lol.backend.modules.shop.entity.Algorithm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlgorithmRepository extends JpaRepository<Algorithm, UUID> {
    List<Algorithm> findByIsActiveTrue();
}
