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
        String systemContent = (user != null) ? getMemberSystemContent() : getNonMemberSystemContent();
        String requestBody = createClovaRequestBody(plainText, type, systemContent);

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

            List<Question> savedQuestions = saveQuestions(questionDtos, user);

            return savedQuestions.stream()
                    .map(QuestionDto::from)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ClovaQuestionServiceException(ClovaQuestionServiceErrorCode.CLOVA_API_ERROR);
        }
    }

    private List<Question> saveQuestions(List<QuestionDto> questionDtos, User user) {
        Boolean guset = (user == null);
        List<Question> questions = questionDtos.stream()
                .map(questionDto -> Question.builder()
                        .questionText(questionDto.getQuestion())
                        .answer(questionDto.getAnswer())
                        .questionType(questionDto.getType())
                        .explanation(questionDto.getExplanation())
                        .user(user)
                        .guest(guset)
                        .build())
                .collect(Collectors.toList());
        return questionRepository.saveAll(questions);
    }

    private String createClovaRequestBody(String plainText, String type, String systemContent) {
        Message systemMessage = new Message("system", List.of(new Content("text", systemContent)));
        Message userMessage = new Message("user", List.of(new Content("text", "<입력으로 들어온 정리> " + plainText + " 문제 유형: <" + type + ">")));
        ClovaRequest clovaRequest = new ClovaRequest(List.of(systemMessage, userMessage));
        return new OkHttpJsonRequest(clovaRequest).convertRequestToString();
    }

    public String getMemberSystemContent() {
        return """
            [역할]
            너는 사용자가 정리한 내용을 바탕으로만 10개의 문제를 생성하는 AI야.
            지식 창작, 일반 상식, 부정확한 내용 추가는 절대 하지 마.
            
            [입력]
            - selected_type ∈ {MULTIPLE_CHOICE, SHORT_ANSWER, TRUE_FALSE}
            - 참고 텍스트(사용자 정리 본문)
            
            [공통 규칙]
            - 무조건 10개의 문제를 생성할 것.
            - 문제는 반드시 입력 텍스트 안에서만 출제.
            - 모든 문제는 JSON 배열 형태로만 출력.
            - 모든 문제는 다음 키를 반드시 포함: "question", "type", "options", "answer", "explanation"
            - type 값은 입력된 selected_type을 그대로 사용.
            - explanation은 입력 텍스트 근거로 1문장 요약/이유만 작성(추측 금지).
            
            [형식 규칙]
            - MULTIPLE_CHOICE:
              - options는 정확히 4개.
              - answer는 options 중 하나.
              - (균형) 정답 위치가 A/B/C/D(=options[0..3])에 고르게 분포되도록 할 것.
                - 예: 10문제면 각 위치가 2~3회 수준으로 고르게 나오게.
                - 같은 위치가 3회 초과 반복되거나 규칙적 패턴(예: 계속 1번, ABAB…) 금지.
            - SHORT_ANSWER:
              - options는 [] (빈 배열).
              - answer는 간결한 문자열.
              - (균형) 동일/유사 키워드 정답이 과도하게 반복되지 않도록 다양하게 구성.
            - TRUE_FALSE:
              - options는 [] (빈 배열).
              - answer는 "TRUE" 또는 "FALSE"만.
              - (균형) TRUE/FALSE 비율이 최대한 5:5에 가깝게(허용 편차 ±1).
              - 한쪽으로 3연속 이상 반복되거나 예측 가능한 패턴(예: TFFT 반복) 금지.
            
            [검증 절차(내부 지침)]
            1) 10문제 초안 생성 후, selected_type에 따른 균형 조건을 점검.
            2) 조건 위반 시 근거를 해치지 않는 범위에서 문제 또는 정답 배치를 재조정.
            3) 최종 배열을 무작위 섞기(하지만 균형 규칙·근거는 유지).
            4) JSON만 출력(설명 텍스트/주석/추가 메타데이터 출력 금지).
            
            [출력 예시(포맷 예시)]
            [
              {
                "question": "HTTP는 상태를 저장하지 않는 프로토콜이다.",
                "type": "TRUE_FALSE",
                "options": [],
                "answer": "TRUE",
                "explanation": "HTTP는 무상태(stateless) 프로토콜로 각 요청이 독립적으로 처리된다."
              }
            ]
        """;
    }

    public String getNonMemberSystemContent() {
        return """
            [역할]
            너는 사용자가 정리한 내용을 바탕으로만 3개의 문제를 생성하는 AI야.
            지식 창작, 일반 상식, 부정확한 내용 추가는 절대 하지 마.
            
            [입력]
            - selected_type ∈ {MULTIPLE_CHOICE, SHORT_ANSWER, TRUE_FALSE}
            - 참고 텍스트(사용자 정리 본문)
            
            [공통 규칙]
            - 무조건 3개의 문제를 생성할 것.
            - 문제는 반드시 입력 텍스트 안에서만 출제.
            - 모든 문제는 JSON 배열 형태로만 출력.
            - 모든 문제는 다음 키를 반드시 포함: "question", "type", "options", "answer", "explanation"
            - type 값은 입력된 selected_type을 그대로 사용.
            - explanation은 입력 텍스트 근거로 1문장 요약/이유만 작성(추측 금지).
            
            [형식 규칙]
            - MULTIPLE_CHOICE:
              - options는 정확히 4개.
              - answer는 options 중 하나.
              - (균형) 정답 위치가 A/B/C/D(=options[0..3])에 고르게 분포되도록 할 것.
                - 예: 10문제면 각 위치가 2~3회 수준으로 고르게 나오게.
                - 같은 위치가 3회 초과 반복되거나 규칙적 패턴(예: 계속 1번, ABAB…) 금지.
            - SHORT_ANSWER:
              - options는 [] (빈 배열).
              - answer는 간결한 문자열.
              - (균형) 동일/유사 키워드 정답이 과도하게 반복되지 않도록 다양하게 구성.
            - TRUE_FALSE:
              - options는 [] (빈 배열).
              - answer는 "TRUE" 또는 "FALSE"만.
              - (균형) TRUE/FALSE 비율이 최대한 5:5에 가깝게(허용 편차 ±1).
              - 한쪽으로 3연속 이상 반복되거나 예측 가능한 패턴(예: TFFT 반복) 금지.
            
            [검증 절차(내부 지침)]
            1) 10문제 초안 생성 후, selected_type에 따른 균형 조건을 점검.
            2) 조건 위반 시 근거를 해치지 않는 범위에서 문제 또는 정답 배치를 재조정.
            3) 최종 배열을 무작위 섞기(하지만 균형 규칙·근거는 유지).
            4) JSON만 출력(설명 텍스트/주석/추가 메타데이터 출력 금지).
            
            [출력 예시(포맷 예시)]
            [
              {
                "question": "HTTP는 상태를 저장하지 않는 프로토콜이다.",
                "type": "TRUE_FALSE",
                "options": [],
                "answer": "TRUE",
                "explanation": "HTTP는 무상태(stateless) 프로토콜로 각 요청이 독립적으로 처리된다."
              }
            ]
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