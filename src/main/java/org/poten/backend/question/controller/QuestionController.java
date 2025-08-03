package org.poten.backend.question.controller;

import lombok.RequiredArgsConstructor;
import org.poten.backend.global.error.GlobalErrorCode;
import org.poten.backend.global.exception.CustomException;
import org.poten.backend.question.dto.response.QuestionListResponse;
import org.poten.backend.question.service.QuestionService;
import org.poten.backend.global.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/question")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<List<QuestionListResponse>> findAllQuestion(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            throw new CustomException(GlobalErrorCode.REFRESH_TOKEN_MISMATCH);
        }
        return ResponseEntity.ok(questionService.findAllQuestion(customUserDetails.getUser()));
    }
}
