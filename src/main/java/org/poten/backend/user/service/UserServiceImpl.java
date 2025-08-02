package org.poten.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.poten.backend.auth.dto.TokenRefreshRequestDto;
import org.poten.backend.auth.dto.TokenRefreshResponseDto;
import org.poten.backend.auth.entity.RefreshToken;
import org.poten.backend.auth.repository.RefreshTokenRepository;
import org.poten.backend.global.error.GlobalErrorCode;
import org.poten.backend.global.exception.CustomException;
import org.poten.backend.global.jwt.JwtProvider;
import org.poten.backend.user.dto.UserLoginRequestDto;
import org.poten.backend.user.dto.UserLoginResponseDto;
import org.poten.backend.user.dto.UserSignupRequestDto;
import org.poten.backend.user.entity.User;
import org.poten.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    @Override
    public void signup(UserSignupRequestDto requestDto) {
        if(userRepository.findByLoginId(requestDto.getLoginId()).isPresent()){
            throw new CustomException(GlobalErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (requestDto.getNickname() == null || requestDto.getNickname().isBlank()) {
            throw new CustomException(GlobalErrorCode.NICKNAME_REQUIRED);
        }

        User user = User.builder()
                .loginId(requestDto.getLoginId())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .nickname(requestDto.getNickname())
                .build();

        userRepository.save(user);
    }

    @Override
    public UserLoginResponseDto login(UserLoginRequestDto requestDto) {
        User user = userRepository.findByLoginId(requestDto.getLoginId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        if(!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())){
            throw new CustomException(GlobalErrorCode.PASSWORD_MISMATCH);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getLoginId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getLoginId());

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .loginId(user.getLoginId())
                        .token(refreshToken)
                        .build()
        );

        return new UserLoginResponseDto(accessToken,refreshToken);
    }

    @Override
    public void delete(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(()->new IllegalArgumentException("유저를 찾을 수 없습니다"));
        userRepository.delete(user);

    }

    public String getLoginIdFromToken(String token) {
        return jwtProvider.getLoginIdFromToken(token.replace("Bearer ", ""));
    }

    @Override
    public TokenRefreshResponseDto reissueAccessToken(TokenRefreshRequestDto requestDto) {
        String refreshToken = requestDto.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        String loginId = jwtProvider.getLoginIdFromToken(refreshToken);

        RefreshToken saved = refreshTokenRepository.findById(loginId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!saved.getToken().equals(refreshToken)) {
            throw new CustomException(GlobalErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtProvider.generateAccessToken(loginId);
        return new TokenRefreshResponseDto(newAccessToken);
    }


}