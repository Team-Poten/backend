package org.poten.backend.user.controller;

import lombok.RequiredArgsConstructor;
import org.poten.backend.auth.dto.TokenRefreshRequestDto;
import org.poten.backend.auth.dto.TokenRefreshResponseDto;
import org.poten.backend.user.dto.UserLoginRequestDto;
import org.poten.backend.user.dto.UserLoginResponseDto;
import org.poten.backend.user.dto.UserSignupRequestDto;
import org.poten.backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserSignupRequestDto dto) {
        userService.signup(dto);
        return ResponseEntity.ok("회원가입 성공!");
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody UserLoginRequestDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenRefreshResponseDto> reissue(@RequestBody TokenRefreshRequestDto dto) {
        return ResponseEntity.ok(userService.reissueAccessToken(dto));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@RequestHeader("Authorization") String token) {
        String loginId = userService.getLoginIdFromToken(token);
        userService.delete(loginId);
        return ResponseEntity.ok("탈퇴 완료");
    }

}
