package org.poten.backend.clova.controller;

import lombok.RequiredArgsConstructor;
import org.poten.backend.clova.dto.response.QuestionDto;
import org.poten.backend.clova.service.ClovaQuestionService;
import org.poten.backend.global.jwt.JwtProvider;
import org.poten.backend.user.entity.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.poten.backend.user.repository.UserRepository;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clova")
@RequiredArgsConstructor
public class ClovaQuestionController {

    private final ClovaQuestionService clovaQuestionService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @PostMapping("/question")
    public List<QuestionDto> generateAndSaveQuestion(
        @RequestBody Map<String, String> request,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String plainText = request.get("plainText");
        String type = request.get("type");
        User user = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtProvider.validateToken(token)) {
                String loginId = jwtProvider.getLoginIdFromToken(token);
                user = userRepository.findByLoginId(loginId)
                    .orElseThrow(() -> new IllegalArgumentException("user not found"));
            } else {
                throw new IllegalArgumentException("Invalid token");
            }
        }

        return clovaQuestionService.generateAndSaveQuestion(plainText, type, user);
    }
}
