package com.dolpin.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseStatus {
    // 공통
    SUCCESS(HttpStatus.OK, "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "리소스가 성공적으로 생성되었습니다."),

    // 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "잘못된 파라미터입니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // OAuth
    OAUTH_PROVIDER_NOT_EXIST(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다." ),

    // 장소
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),

    // 호출
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "너무 많은 요청이 발생했습니다"),

    // 기록
    MOMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private String message;

    ResponseStatus(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public ResponseStatus withMessage(String message) {
        this.message = message;
        return this;
    }
}
