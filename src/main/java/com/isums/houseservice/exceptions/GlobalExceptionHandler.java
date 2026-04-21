package com.isums.houseservice.exceptions;

import com.isums.houseservice.domains.dtos.ApiError;
import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDb(DataAccessException ex) {
        ex.getMostSpecificCause();
        String detail = ex.getMostSpecificCause().getMessage();

        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database error",
                List.of(ApiError.builder()
                        .code("DB_ERROR")
                        .message(detail)
                        .build())
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                List.of(ApiError.builder()
                        .code("BAD_REQUEST")
                        .message(ex.getMessage())
                        .build())
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return badRequestValidation(ex.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        return badRequestValidation(ex.getBindingResult());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                List.of(ApiError.builder()
                        .code("BAD_REQUEST")
                        .message(ex.getMessage())
                        .build())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                List.of(ApiError.builder()
                        .code("INTERNAL_ERROR")
                        .message(ex.getMessage())
                        .build())
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Data integrity violation";
        if (ex.getMessage() != null && ex.getMessage().contains("serial_number")) {
            message = "Serial number already exists";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponses.fail(HttpStatus.CONFLICT, message));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleConflictException(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponses.fail(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                List.of(ApiError.builder()
                        .code("NOT_FOUND")
                        .message(ex.getMessage())
                        .build())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiResponse<Void> res = ApiResponses.fail(
                status,
                ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                List.of(ApiError.builder()
                        .code(status.name())
                        .message(ex.getReason() != null ? ex.getReason() : status.getReasonPhrase())
                        .build())
        );
        return ResponseEntity.status(status).body(res);
    }

    private ResponseEntity<ApiResponse<Void>> badRequestValidation(BindingResult bindingResult) {
        List<ApiError> errors = bindingResult.getFieldErrors().stream()
                .map(fieldError -> ApiError.builder()
                        .code("BAD_REQUEST")
                        .message(fieldError.getField() + ": " + fieldError.getDefaultMessage())
                        .build())
                .toList();

        ApiResponse<Void> res = ApiResponses.fail(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                errors.isEmpty()
                        ? List.of(ApiError.builder()
                        .code("BAD_REQUEST")
                        .message("Validation failed")
                        .build())
                        : errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(HouseException.class)
    public ResponseEntity<ApiResponse<Void>> handleHouseException(HouseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getHttpStatus());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiResponse<Void> res = ApiResponses.fail(
                status,
                ex.getCode().name(),
                List.of(ApiError.builder()
                        .code(ex.getCode().name())
                        .message(ex.getCode().name())
                        .build())
        );
        return ResponseEntity.status(status).body(res);
    }
}
