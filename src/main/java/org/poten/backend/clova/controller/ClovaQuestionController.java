package org.poten.backend.clova.controller;

import lombok.RequiredArgsConstructor;
import org.poten.backend.clova.dto.response.QuestionDto;
import org.poten.backend.clova.service.ClovaQuestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.poten.backend.global.security.CustomUserDetails;
import org.poten.backend.global.error.GlobalErrorCode;
import org.poten.backend.global.exception.CustomException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clova")
@RequiredArgsConstructor
public class ClovaQuestionController {

    private final ClovaQuestionService clovaQuestionService;

    @PostMapping("/question")
    public List<QuestionDto> generateAndSaveQuestion(@RequestBody Map<String, String> request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            throw new CustomException(GlobalErrorCode.REFRESH_TOKEN_MISMATCH);
        }
        String plainText = request.get("plainText");
        String type = request.get("type");
        return clovaQuestionService.generateAndSaveQuestion(plainText, type, customUserDetails.getUser());
    }
}
