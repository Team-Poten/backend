package org.poten.backend.global.security;

import lombok.RequiredArgsConstructor;
import org.poten.backend.user.entity.User;
import org.poten.backend.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(()->new UsernameNotFoundException("해당 이메일을 찾을 수 없습니다."));
        return new CustomUserDetails(user);
    }
}