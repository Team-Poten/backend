package org.poten.backend.clova.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.poten.backend.clova.dto.response.QuestionDto;
import org.poten.backend.clova.service.ClovaQuestionService;
import org.poten.backend.global.error.ErrorCode;
import org.poten.backend.global.exception.CustomException;
import org.poten.backend.global.jwt.JwtProvider;
import org.poten.backend.clova.dto.response.OcrResponseDto;
import org.poten.backend.clova.service.ClovaOcrService;
import org.poten.backend.user.entity.User;
import org.poten.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clova")
@RequiredArgsConstructor
public class ClovaQuestionController {

    private final ClovaQuestionService clovaQuestionService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final ClovaOcrService clovaOcrService;

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

    @PostMapping("/question/ocr")
    public List<QuestionDto> generateByOcrAndSaveQuestion(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestParam("image") MultipartFile image,
        @RequestParam("type") String type
        ) {
        OcrResponseDto ocrResponse = clovaOcrService.extractTextFromImage(image);
        if(ocrResponse == null) {
            throw new GenerateByOcrAndSaveQuestionException(GenerateByOcrAndSaveQuestionErrorCode.OCR_RESPONSE_FAILED);
        }
        String plainText = ocrResponse.getFullText();
        if (plainText == null || plainText.isBlank()) {
            throw new GenerateByOcrAndSaveQuestionException(GenerateByOcrAndSaveQuestionErrorCode.EXTRACTED_TEXT_EMPTY);
        }
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

    @Getter
    @RequiredArgsConstructor
    public enum GenerateByOcrAndSaveQuestionErrorCode implements ErrorCode {
        OCR_RESPONSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CQC001", "OCR 응답을 받는데 실패했습니다."),
        EXTRACTED_TEXT_EMPTY(HttpStatus.BAD_REQUEST, "CQC002", "OCR로 추출된 텍스트가 없습니다.");

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;
    }

    public static class GenerateByOcrAndSaveQuestionException extends CustomException {
        public GenerateByOcrAndSaveQuestionException(ErrorCode errorCode) {
            super(errorCode);
        }
    }

    @PostMapping("/question/similar")
    public List<QuestionDto> generateAndSaveSimilarQuestion(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "example_question_file", required = false) MultipartFile exampleQuestionFile,
            @RequestParam(value = "user_content_file", required = false) MultipartFile userContentFile,
            @RequestParam(value = "example_question_text", required = false) String exampleQuestionText,
            @RequestParam(value = "user_content_text", required = false) String userContentText) {

        String exampleQuestion = getTextFromInput(exampleQuestionFile, exampleQuestionText);
        String userContent = getTextFromInput(userContentFile, userContentText);

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

        return clovaQuestionService.generateAndSaveSimilarQuestion(exampleQuestion, userContent, user);
    }

    private String getTextFromInput(MultipartFile file, String text) {
        if (file != null && !file.isEmpty()) {
            OcrResponseDto ocrResponse = clovaOcrService.extractTextFromImage(file);
            if (ocrResponse == null || ocrResponse.getFullText() == null || ocrResponse.getFullText().isBlank()) {
                throw new GenerateByOcrAndSaveQuestionException(GenerateByOcrAndSaveQuestionErrorCode.EXTRACTED_TEXT_EMPTY);
            }
            return ocrResponse.getFullText();
        } else if (text != null && !text.isBlank()) {
            return text;
        } else {
            throw new IllegalArgumentException("Either a file or a text must be provided.");
        }
    }

}
