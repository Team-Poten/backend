package org.poten.backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G001", "알 수 없는 내부 오류입니다."),

    // User/Auth
    DUPLICATE_LOGIN_ID     (HttpStatus.CONFLICT,      "U001", "이미 존재하는 아이디입니다."),
    USER_NOT_FOUND         (HttpStatus.NOT_FOUND,     "U002", "가입되지 않은 아이디입니다."),
    PASSWORD_MISMATCH      (HttpStatus.UNAUTHORIZED,  "U003", "비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN  (HttpStatus.UNAUTHORIZED,  "A001", "유효하지 않은 refreshToken입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED,  "A002", "저장된 refreshToken이 없습니다."),
    REFRESH_TOKEN_MISMATCH (HttpStatus.UNAUTHORIZED,  "A003", "토큰 정보가 일치하지 않습니다."),
    NICKNAME_REQUIRED      (HttpStatus.BAD_REQUEST,   "U004", "닉네임은 필수입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
