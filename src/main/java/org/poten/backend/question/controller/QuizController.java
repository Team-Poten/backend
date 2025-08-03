package org.poten.backend.question.controller;

import lombok.RequiredArgsConstructor;
import org.poten.backend.global.jwt.JwtProvider;
import org.poten.backend.question.dto.AnswerRequest;
import org.poten.backend.question.dto.AnswerResponse;
import org.poten.backend.question.service.QuizService;
import org.poten.backend.user.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/questions")
public class QuizController {

    private final QuizService quizService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @PostMapping("/{id}/answer")
    public AnswerResponse answer(@PathVariable("id") Long questionId,
                                 @RequestBody AnswerRequest req,
                                 @RequestHeader("Authorization") String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : null;

        if (token == null || !jwtProvider.validateToken(token)) {
            throw new IllegalArgumentException("invalid token");
        }

        String loginId = jwtProvider.getLoginIdFromToken(token);
        Long userId = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"))
                .getId();

        return quizService.submitAnswer(userId, questionId, req);
    }
}

