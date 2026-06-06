package com.pickdeal.common.error;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 나타내는 최상위 예외. {@link ErrorCode}를 실어 보내면
 * {@code GlobalExceptionHandler}가 적절한 HTTP 상태/코드로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
