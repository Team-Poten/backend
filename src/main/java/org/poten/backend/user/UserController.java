package org.poten.backend.user;

/*
컨트롤러 예시입니다.

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.poten.backend.global.error.ErrorCode;
import org.poten.backend.global.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public void getUser(@PathVariable Long id) {
        if (id <= 0) {
            throw new CustomException(UserErrorCode.INVALID_USER_ID);
        }
        // userService.findUserById(id); // 실제로는 서비스 메서드 호출
    }

    @Getter
    @RequiredArgsConstructor
    public enum UserErrorCode implements ErrorCode {
        INVALID_USER_ID(HttpStatus.BAD_REQUEST, "U001", "유효하지 않은 유저 ID 입니다.");

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;
    }
}
 */