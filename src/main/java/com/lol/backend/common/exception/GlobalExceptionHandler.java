package com.lol.backend.common.exception;

import com.lol.backend.common.dto.ErrorDetail;
import com.lol.backend.common.dto.ErrorResponse;
import com.lol.backend.common.util.RequestContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 글로벌 예외 핸들러.
 * ERROR_MODEL.md 에러 Envelope 규칙에 따라 응답을 생성한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 비즈니스 예외 처리.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        String requestId = RequestContextHolder.getRequestId();
        ErrorCode errorCode = ex.getErrorCode();

        log.warn("Business exception: code={}, message={}, requestId={}",
                errorCode.getCode(), ex.getMessage(), requestId);

        ErrorDetail errorDetail = ErrorDetail.of(
                errorCode.getCode(),
                ex.getMessage(),
                ex.getDetails()
        );

        ErrorResponse response = ErrorResponse.of(errorDetail, requestId);
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * Bean Validation 검증 실패 처리.
     * ERROR_MODEL.md 2.1 검증 오류 상세 참조.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String requestId = RequestContextHolder.getRequestId();

        List<ErrorDetail.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        log.warn("Validation failed: fieldErrors={}, requestId={}", fieldErrors, requestId);

        ErrorDetail errorDetail = ErrorDetail.withFieldErrors(
                ErrorCode.VALIDATION_FAILED.getCode(),
                ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                fieldErrors
        );

        ErrorResponse response = ErrorResponse.of(errorDetail, requestId);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 예상하지 못한 예외 처리.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        String requestId = RequestContextHolder.getRequestId();

        log.error("Unexpected exception: requestId={}", requestId, ex);

        ErrorDetail errorDetail = ErrorDetail.of(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage()
        );

        ErrorResponse response = ErrorResponse.of(errorDetail, requestId);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private ErrorDetail.FieldError toFieldError(FieldError fieldError) {
        String field = fieldError.getField();
        String reason = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "invalid";

        return new ErrorDetail.FieldError(field, reason);
    }
}
