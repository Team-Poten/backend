package org.poten.backend.user;

/*
서비스 예시입니다.

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.poten.backend.global.error.ErrorCode;
import org.poten.backend.global.exception.CustomException;
import org.poten.backend.global.service.DomainService;
import org.poten.backend.user.UserService.UserErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements DomainService<UserErrorCode> {

    public void findUserById(Long id) {
        // DB에서 유저를 찾는 로직 ~
        throw new CustomException(UserErrorCode.USER_NOT_FOUND); // 찾지 못했다면 아래와 같이 예외 던지고 GlobalExceptionHandler에서 처리
    }

    @Getter
    @RequiredArgsConstructor
    public enum UserErrorCode implements ErrorCode {
        USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "해당 유저를 찾을 수 없습니다.");

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;
    }
}
 */