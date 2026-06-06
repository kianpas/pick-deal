package com.pickdeal.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 에러 코드. HTTP 상태와 머신 분기용 코드 문자열을 함께 정의한다.
 * 데이터가 아니라 API 계약이므로 DB가 아닌 코드로 관리한다. 신규 에러는 여기에 추가한다.
 */
@Getter
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");

    private final HttpStatus status;
    private final String code;

    ErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
