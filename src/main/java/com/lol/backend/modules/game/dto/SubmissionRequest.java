package com.lol.backend.modules.game.dto;

import com.lol.backend.modules.user.entity.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 코드 제출 요청 DTO.
 * OPENAPI SubmissionRequest 스키마 참조.
 */
public record SubmissionRequest(
        @NotNull(message = "language는 필수입니다")
        Language language,

        @NotBlank(message = "sourceCode는 필수입니다")
        String sourceCode
) {
}
