package org.poten.backend.clova.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.poten.backend.clova.dto.request.ClovaRequest;
import org.poten.backend.clova.dto.request.Content;
import org.poten.backend.clova.dto.request.Message;
import org.poten.backend.clova.dto.response.QuestionDto;
import org.poten.backend.global.infra.clova.ClovaProperty;
import org.poten.backend.global.infra.clova.OkHttpJsonRequest;
import org.poten.backend.question.entity.Question;
import org.poten.backend.question.repository.QuestionRepository;
import org.poten.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import lombok.Getter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.poten.backend.global.infra.clova.OkHttpRequest.createRequest;
import org.poten.backend.global.error.ErrorCode;
import org.poten.backend.global.exception.CustomException;

@Service
@RequiredArgsConstructor
public class ClovaQuestionService {

    private final ClovaProperty clovaProperty;
    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepository;

    @Transactional
    public List<QuestionDto> generateAndSaveQuestion(String plainText, String type, User user) {
        String requestBody = createClovaRequestBody(plainText, type);

        Request request = new Request.Builder()
                .url(clovaProperty.getUrl())
                .header("Authorization", "Bearer " + clovaProperty.getKey())
                .header("X-NCP-CLOVASTUDIO-REQUEST-ID", clovaProperty.getId())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = createRequest(request)) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            if (response.body() == null) {
                throw new IOException("Response body is null");
            }
            String responseBody = response.body().string();

            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(responseBody);
            String content = rootNode.path("result").path("message").path("content").asText();
            String jsonContent = content.substring(content.indexOf("["), content.lastIndexOf("]") + 1);

            List<QuestionDto> questionDtos = objectMapper.readValue(jsonContent, new com.fasterxml.jackson.core.type.TypeReference<List<QuestionDto>>() {});

            saveQuestions(questionDtos, user);

            return questionDtos;
        } catch (IOException e) {
            throw new ClovaQuestionServiceException(ClovaQuestionServiceErrorCode.CLOVA_API_ERROR);
        }
    }

    private void saveQuestions(List<QuestionDto> questionDtos, User user) {
        List<Question> questions = questionDtos.stream()
                .map(questionDto -> Question.builder()
                        .questionText(questionDto.getQuestion())
                        .answer(questionDto.getAnswer())
                        .questionType(questionDto.getType())
                        .explanation(questionDto.getExplanation())
                        .user(user)
                        .build())
                .collect(Collectors.toList());
        questionRepository.saveAll(questions);
    }

    private String createClovaRequestBody(String plainText, String type) {
        Message systemMessage = new Message("system", List.of(new Content("text", getSystemContent())));
        Message userMessage = new Message("user", List.of(new Content("text", "<입력으로 들어온 정리> " + plainText + " 문제 유형: <" + type + ">")));
        ClovaRequest clovaRequest = new ClovaRequest(List.of(systemMessage, userMessage));
        return new OkHttpJsonRequest(clovaRequest).convertRequestToString();
    }

    private String getSystemContent() {
        return """
        [역할]
        너는 사용자가 정리한 내용을 바탕으로만 문제를 생성하는 AI야.
        지식 창작, 일반 상식, 부정확한 내용 추가는 절대 하지 마.

        [규칙]
        - 문제는 반드시 입력 내용 안에서만 만들 것
        - 문제 유형은 반드시 다음 중 하나로만 제한할 것: MULTIPLE_CHOICE, SHORT_ANSWER, TRUE_FALSE
        - 무조건 3개의 문제만 생성할 것
        - 텍스트 지문에 포함되지 않은 내용은 퀴즈로 출제하지 않습니다.
        - 모든 문제는 아래 구조와 동일한 JSON 형식으로 반환할 것
        - 모든 문제 유형에서 다음 키 값을 항상 포함해야 함:
          `question`, `type`, `options`, `answer`, `explanation`

        객관식(MULTIPLE_CHOICE)
        - `options`는 반드시 4개의 보기 항목 포함
        - `answer`는 `options` 중 하나여야 함

        단답형(SHORT_ANSWER) 또는 OX(TRUE_FALSE)
        - `options`는 빈 배열 `[]`
        - `answer`는 간결한 문자열
        - `TRUE_FALSE`는 "TRUE" 또는 "FALSE"만 허용

        모든 문제에는 `explanation` 필드를 포함
        - 사용자가 정답을 이해할 수 있도록, AI가 간단히 핵심 개념을 요약하거나 정답의 이유를 1문장으로 설명
        - 입력 내용 기반이어야 하며, 과도한 추측이나 새로운 지식 창작은 지양

        ---

        [출력 예시]
        ```json
        [
          {
            "question": "HTTP는 상태를 저장하지 않는 프로토콜이다.",
            "type": "TRUE_FALSE",
            "options": [],
            "answer": "TRUE",
            "explanation": "HTTP는 무상태(stateless) 프로토콜로, 각 요청은 독립적으로 처리된다."
          }
        ]
        ```
        """;
    }

    @Getter
    @RequiredArgsConstructor
    public enum ClovaQuestionServiceErrorCode implements ErrorCode {
        CLOVA_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CS001", "클로바 API 연동 중 오류가 발생했습니다.");

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;
    }

    public static class ClovaQuestionServiceException extends CustomException {
        public ClovaQuestionServiceException(ErrorCode errorCode) {
            super(errorCode);
        }
    }
}