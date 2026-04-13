package com.isums.houseservice.exceptions;

import com.isums.houseservice.domains.dtos.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler (house-service)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleDb returns 500 with DB_ERROR code and root cause message")
    void db() {
        DataAccessException ex = new DataAccessException("outer", new RuntimeException("root cause")) {};
        ResponseEntity<ApiResponse<Void>> res = handler.handleDb(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(500);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("DB_ERROR");
        assertThat(res.getBody().getErrors().get(0).getMessage()).isEqualTo("root cause");
    }

    @Test
    @DisplayName("handleBadRequest returns 400 with BAD_REQUEST code")
    void badRequest() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleBadRequest(new IllegalArgumentException("no arg"));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("handleGeneric returns 500 with INTERNAL_ERROR code")
    void generic() {
        ResponseEntity<ApiResponse<Void>> res = handler.handleGeneric(new Exception("boom"));
        assertThat(res.getStatusCode().value()).isEqualTo(500);
        assertThat(res.getBody().getMessage()).isEqualTo("Unexpected error");
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with specific message when serial_number in cause")
    void dataIntegritySerial() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_serial_number\"");
        ResponseEntity<ApiResponse<?>> res = handler.handleDataIntegrityViolation(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().getMessage()).isEqualTo("Serial number already exists");
    }

    @Test
    @DisplayName("handleDataIntegrityViolation returns 409 with generic message otherwise")
    void dataIntegrityGeneric() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("fk violation");
        ResponseEntity<ApiResponse<?>> res = handler.handleDataIntegrityViolation(ex);

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().getMessage()).isEqualTo("Data integrity violation");
    }

    @Test
    @DisplayName("handleConflictException returns 409 with the exception message")
    void conflict() {
        ResponseEntity<ApiResponse<?>> res = handler.handleConflictException(new ConflictException("dup"));
        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().getMessage()).isEqualTo("dup");
    }

    @Test
    @DisplayName("handleNotFound returns 404 with NOT_FOUND code")
    void notFound() {
        ResponseEntity<ApiResponse<Void>> res = handler.handleNotFound(new NotFoundException("missing"));
        assertThat(res.getStatusCode().value()).isEqualTo(404);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("handleHouseException HOUSE_NOT_FOUND → 404")
    void houseNotFound() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleHouseException(new HouseException(HouseErrorCode.HOUSE_NOT_FOUND));
        assertThat(res.getStatusCode().value()).isEqualTo(404);
        assertThat(res.getBody().getErrors().get(0).getCode()).isEqualTo("HOUSE_NOT_FOUND");
    }

    @Test
    @DisplayName("handleHouseException NOT_HOUSE_OWNER → 403")
    void notOwner() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleHouseException(new HouseException(HouseErrorCode.NOT_HOUSE_OWNER));
        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("handleHouseException MEMBER_ALREADY_EXISTS → 409")
    void memberExists() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleHouseException(new HouseException(HouseErrorCode.MEMBER_ALREADY_EXISTS));
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @DisplayName("handleHouseException CANNOT_REMOVE_SELF → 409")
    void removeSelf() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleHouseException(new HouseException(HouseErrorCode.CANNOT_REMOVE_SELF));
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @DisplayName("handleHouseException CANNOT_REMOVE_OWNER → 409")
    void removeOwner() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleHouseException(new HouseException(HouseErrorCode.CANNOT_REMOVE_OWNER));
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @DisplayName("handleHouseException TENANT_GROUP_NOT_FOUND → 404")
    void tenantGroupNotFound() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleHouseException(new HouseException(HouseErrorCode.TENANT_GROUP_NOT_FOUND));
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("handleHouseException MEMBER_NOT_FOUND → 404")
    void memberNotFound() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleHouseException(new HouseException(HouseErrorCode.MEMBER_NOT_FOUND));
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }
}
