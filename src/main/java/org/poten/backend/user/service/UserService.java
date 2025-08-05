package org.poten.backend.user.service;


import org.poten.backend.auth.dto.TokenRefreshRequestDto;
import org.poten.backend.auth.dto.TokenRefreshResponseDto;
import org.poten.backend.user.dto.UserLoginRequestDto;
import org.poten.backend.user.dto.UserLoginResponseDto;
import org.poten.backend.user.dto.UserSignupRequestDto;
import org.poten.backend.user.entity.User;

public interface UserService {

    void signup(UserSignupRequestDto requestDto);
    UserLoginResponseDto login(UserLoginRequestDto requestDto);
    void delete(String loginId);

    String getLoginIdFromToken(String token);

    TokenRefreshResponseDto reissueAccessToken(TokenRefreshRequestDto requestDto);

    boolean checkLoginIdDuplicate(String nickname);
}