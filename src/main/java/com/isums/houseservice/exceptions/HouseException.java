package com.isums.houseservice.exceptions;

import lombok.Getter;

@Getter
public class HouseException extends RuntimeException {

    private final HouseErrorCode code;
    private final int httpStatus;

    public HouseException(HouseErrorCode code) {
        super(code.name());
        this.code = code;
        this.httpStatus = resolveStatus(code);
    }

    private static int resolveStatus(HouseErrorCode code) {
        return switch (code) {
            case HOUSE_NOT_FOUND,
                 TENANT_GROUP_NOT_FOUND,
                 MEMBER_NOT_FOUND -> 404;
            case NOT_HOUSE_OWNER -> 403;
            case MEMBER_ALREADY_EXISTS,
                 CANNOT_REMOVE_SELF,
                 CANNOT_REMOVE_OWNER -> 409;
        };
    }
}
