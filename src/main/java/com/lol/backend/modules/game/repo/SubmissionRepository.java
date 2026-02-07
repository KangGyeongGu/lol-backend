package com.lol.backend.modules.game.repo;

import com.lol.backend.modules.game.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
}
