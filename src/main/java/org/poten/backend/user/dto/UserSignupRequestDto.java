package org.poten.backend.user.dto;


import lombok.Getter;

@Getter
public class UserSignupRequestDto {
    private String loginId;
    private String password;
    private String nickname;
}