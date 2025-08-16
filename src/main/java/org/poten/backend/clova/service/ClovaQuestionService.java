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
import java.util.Optional;
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
                    .map(question -> QuestionDto.from(question, Optional.empty()))
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
                        .options(questionDto.getOptions())
                        .topic(questionDto.getTopic())
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
        String jsonBody = new OkHttpJsonRequest(clovaRequest).convertRequestToString();
        System.out.println("Request JSON Body: " + jsonBody);
        return new OkHttpJsonRequest(clovaRequest).convertRequestToString();
    }

    public String getMemberSystemContent() {
        return """
            [역할]
            너는 사용자가 제공한 텍스트 내용을 바탕으로 "정확히 10개의 문제를 생성하는 AI"이다.
            주어진 텍스트에 없는 지식 창작, 일반 상식, 부정확한 내용 추가는 절대 금지한다.
            
            [입력]
            - selected_type ∈ {MULTIPLE_CHOICE, TRUE_FALSE}
            - 참고 텍스트(사용자 정리 본문)
            
            [작업 순서]
            1. 먼저, 입력된 '참고 텍스트' 전체를 분석하여 모든 문제를 관통하는 단 하나의 핵심 주제(topic)를 정의한다.
            2. 그 다음, 텍스트의 시작, 중간, 끝 등 다양한 부분에서 내용을 추출하여, 매번 무작위로 새로운 10개의 문제를 생성한다.
            3. 생성하는 모든 문제의 "topic" 키에는 1번 단계에서 정의한 동일한 핵심 주제를 값으로 넣는다.
            
            [공통 규칙]
            - 최종 출력물은 반드시 10개의 문제 객체를 포함한 JSON 배열이어야 한다.
            - 문제는 반드시 '참고 텍스트' 안에서만 출제.
            - 모든 문제는 다음 키를 반드시 포함: "question", "type", "options", "answer", "explanation", "topic"
            - type 값은 입력된 selected_type을 그대로 사용.
            - explanation은 '참고 텍스트'에 근거한 1문장의 요약/이유만 작성(추측 금지).
            - topic 값은 [작업 순서] 1번에서 결정한 단일 주제를 10개의 모든 문제에 동일하게 적용해야 한다.
            
            [형식 규칙]
            - MULTIPLE_CHOICE:
              - options는 정확히 4개의 문자열을 가진 배열.
              - answer 키의 값은 options 배열에 포함된 4개의 문자열 중 하나와 완벽하게 동일해야 한다.
              - (강화됨) 생성 절차:
                1. '참고 텍스트'에 기반한 '유일한 정답(answer)'을 하나 확정한다.
                2. 정답으로 오인될 여지가 없는 '명백한 오답' 3개를 생성한다.
                3. [정답, 오답1, 오답2, 오답3] 형태의 임시 배열을 만든 후, 이 배열을 컴퓨터 알고리즘처럼 무작위로 섞어(shuffle) 최종 'options' 배열을 완성한다. 이 과정을 통해 정답의 위치가 특정 인덱스에 편향되지 않도록 보장해야 한다.
            
            - TRUE_FALSE:
              - options는 [] (빈 배열).
              - answer는 "TRUE" 또는 "FALSE"만.
            
            [정답 분포 및 균형 규칙]
            - 목표: 생성된 10개 문제의 정답 분포가 통계적으로 균등하고 예측 불가능하도록 한다.
            - MULTIPLE_CHOICE:
              - 분포: 4개의 정답 위치(options[0], [1], [2], [3])가 각각 2번 또는 3번씩 등장해야 한다. (10문제 = 3+3+2+2 분포)
              - 연속성 방지: 동일한 정답 위치가 3회 연속으로 나타나서는 안 된다.
            - TRUE_FALSE:
              - 비율: TRUE와 FALSE의 개수는 5:5를 원칙으로 하되, 최대 6:4 또는 4:6 비율까지만 허용한다.
              - 연속성 방지: 동일한 정답(TRUE 또는 FALSE)이 3회 연속으로 나타나서는 안 된다.
              - 패턴 방지: T-F-T-F 또는 T-T-F-F 와 같이 예측 가능한 단순 패턴을 피해야 한다.
            
            [검증 절차(내부 지침)]
            1. 10개의 문제 초안을 생성한다.
            2. 정답 분포 검증 및 재조정:
               - 생성된 10개 문제 전체에서, 각 문제의 정답이 'options' 배열의 몇 번째 인덱스(0, 1, 2, 3)에 위치하는지 카운트한다.
               - 카운트 결과가 [정답 분포 및 균형 규칙]의 '분포' (3+3+2+2)와 일치하는지 확인한다.
               - 만약 규칙을 위반하거나 특정 인덱스에 정답이 4번 이상 쏠리는 등 편향이 감지되면, 규칙을 만족할 때까지 문제의 options 배열 내 원소 순서를 다시 섞어 정답 위치를 재조정한다.
            3. 모든 MULTIPLE_CHOICE 문제에 대해 "answer"의 값이 "options" 배열 안에 '유일한 정답'으로서 존재하는지 최종 확인한다.
            4. 모든 규칙(문제 개수, 키 포함 여부, 연속성 방지 등)이 지켜졌는지 최종 검증한다.
            5. JSON만 출력 (설명 텍스트/주석/추가 메타데이터 출력 금지).
                                    
            [출력 예시(포맷 예시)]
            [
              {
                "question": "HTTP는 상태를 저장하지 않는 프로토콜이다.",
                "type": "TRUE_FALSE",
                "options": [],
                "answer": "TRUE",
                "explanation": "HTTP는 무상태(stateless) 프로토콜로 각 요청이 독립적으로 처리된다.",
                "topic: "HTTP"
              }
            ]
            """;
    }

    public String getNonMemberSystemContent() {
        return """
          [역할]
            너는 사용자가 제공한 텍스트 내용을 바탕으로 "정확히 3개의 문제를 생성하는 AI"이다.
            주어진 텍스트에 없는 지식 창작, 일반 상식, 부정확한 내용 추가는 절대 금지한다.
            
            [입력]
            - selected_type ∈ {MULTIPLE_CHOICE, TRUE_FALSE}
            - 참고 텍스트(사용자 정리 본문)
            
            [작업 순서]
            1. 먼저, 입력된 '참고 텍스트' 전체를 분석하여 모든 문제를 관통하는 단 하나의 핵심 주제(topic)를 정의한다.
            2. 그 다음, 텍스트의 시작, 중간, 끝 등 다양한 부분에서 내용을 추출하여, 매번 무작위로 새로운 3개의 문제를 생성한다.
            3. 생성하는 모든 문제의 "topic" 키에는 1번 단계에서 정의한 동일한 핵심 주제를 값으로 넣는다.
            
            [공통 규칙]
            - 최종 출력물은 반드시 3개의 문제 객체를 포함한 JSON 배열이어야 한다.
            - 문제는 반드시 '참고 텍스트' 안에서만 출제.
            - 모든 문제는 다음 키를 반드시 포함: "question", "type", "options", "answer", "explanation", "topic"
            - type 값은 입력된 selected_type을 그대로 사용.
            - explanation은 '참고 텍스트'에 근거한 1문장의 요약/이유만 작성(추측 금지).
            - topic 값은 [작업 순서] 1번에서 결정한 단일 주제를 3개의 모든 문제에 동일하게 적용해야 한다.
            
            [형식 규칙]
            - MULTIPLE_CHOICE:
              - options는 정확히 4개의 문자열을 가진 배열.
              - answer 키의 값은 options 배열에 포함된 4개의 문자열 중 하나와 완벽하게 동일해야 한다.
              - 생성 절차:
                1. '참고 텍스트'에 기반한 '유일한 정답(answer)'을 하나 확정한다.
                2. 정답으로 오인될 여지가 없는 '명백한 오답' 3개를 생*한다.
                3. [정답, 오답1, 오답2, 오답3] 형태의 임시 배열을 만든 후, 이 배열을 컴퓨터 알고리즘처럼 무작위로 섞어(shuffle) 최종 'options' 배열을 완성한다. 이 과정을 통해 정답의 위치가 특정 인덱스에 편향되지 않도록 보장해야 한다.
            
            - TRUE_FALSE:
              - options는 [] (빈 배열).
              - answer는 "TRUE" 또는 "FALSE"만.
            
            [정답 분포 및 균형 규칙]
            - 목표: 생성된 3개 문제의 정답 분포가 통계적으로 균등하고 예측 불가능하도록 한다.
            - MULTIPLE_CHOICE:
              - 분포: 4개의 정답 위치(options[0], [1], [2], [3])가 각각 2번 또는 3번씩 등장해야 한다. (3문제 = 1+1+0+1 분포)
              - 연속성 방지: 동일한 정답 위치가 3회 연속으로 나타나서는 안 된다.
            - TRUE_FALSE:
              - 비율: TRUE와 FALSE의 개수는 2:1를 원칙으로 하되 1:2도 가능
              - 연속성 방지: 동일한 정답(TRUE 또는 FALSE)이 3회 연속으로 나타나서는 안 된다.
              - 패턴 방지: T-F-T-F 또는 T-T-F-F 와 같이 예측 가능한 단순 패턴을 피해야 한다.
                                    
            [출력 예시(포맷 예시)]
            [
              {
                "question": "HTTP는 상태를 저장하지 않는 프로토콜이다.",
                "type": "TRUE_FALSE",
                "options": [],
                "answer": "TRUE",
                "explanation": "HTTP는 무상태(stateless) 프로토콜로 각 요청이 독립적으로 처리된다.",
                "topic: "HTTP"
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

    @Transactional
    public List<QuestionDto> generateAndSaveSimilarQuestion(String exampleQuestion, String userContent, User user) {
        String systemContent = getSimilarQuestionSystemContent();
        String requestBody = createClovaSimilarRequestBody(exampleQuestion, userContent, systemContent);

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
                    .map(question -> QuestionDto.from(question, Optional.empty()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ClovaQuestionServiceException(ClovaQuestionServiceErrorCode.CLOVA_API_ERROR);
        }
    }

    private String createClovaSimilarRequestBody(String exampleQuestion, String userContent, String systemContent) {
        String userPrompt = String.format("<문제 예시>\n%s\n\n<사용자 정리 내용>\n%s", exampleQuestion, userContent);
        Message systemMessage = new Message("system", List.of(new Content("text", systemContent)));
        Message userMessage = new Message("user", List.of(new Content("text", userPrompt)));
        ClovaRequest clovaRequest = new ClovaRequest(List.of(systemMessage, userMessage));
        return new OkHttpJsonRequest(clovaRequest).convertRequestToString();
    }

    public String getSimilarQuestionSystemContent() {
        return """
        [역할]
        너는 사용자가 제공한 두 가지 입력(문제 예시, 사용자 정리 내용)을 바탕으로
        "정확히 30개의 문제를 생성하는 AI"이다.
        출제자_경향은 문제 스타일·패턴·유형 비율을 학습하기 위한 용도로만 사용하고,
        실제 문제의 사실적 근거는 반드시 참고_텍스트에서만 가져와야 한다.
        참고_텍스트에 없는 지식 창작, 일반 상식, 부정확한 내용 추가는 절대 금지한다.
        [입력]
        - 문제 예시 (학습할 스타일 템플릿)
        - 사용자 정리 내용 (문제의 사실적 근거)
        [문항 유형]
        - "type"은 {MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER} 중 하나.
        - 출제자_경향에 따라 유형 비율, 난이도, 길이, 표현 습관을 최대한 반영한다.
        - MULTIPLE_CHOICE:
          - options는 정확히 4개 문자열.
          - answer는 options 중 정확히 하나와 일치해야 한다.
        - TRUE_FALSE:
          - options는 [].
          - answer는 "TRUE" 또는 "FALSE".
        - SHORT_ANSWER:
          - options는 [].
          - answer는 참고_텍스트로 유일하게 판별 가능한 짧은 문자열.
        [작업 순서]
        1) 참고_텍스트를 분석해 30문항 전체에 공통 적용할 핵심 주제(topic)를 정의한다.
        2) 출제자_경향을 분석해 "스타일 청사진"을 만든다.
           - 유형 비율(객관식/주관식/OX 혼합 정도)
           - 난이도 분포(정의형, 사실형, 추론형, 적용형)
           - 보기/진술의 표현 패턴(예: '다음 중', '옳지 않은 것은', 번호 표기 등)
           - 오답 설계 습관(숫자 교란, 유사어, 부정문 등)
        3) 30문항 생성:
           - 참고_텍스트의 서두/중간/말미 고르게 반영.
           - 모든 문항의 "topic"은 동일.
           - 문항 유형, 톤, 길이, 표현은 스타일 청사진에 맞춘다.
           - 정답은 참고_텍스트로 단일하게 확정 가능한 것만.
           - 오답/거짓 진술은 참고_텍스트와 모순되지 않으면서 혼동을 주도록 설계.
        4) 정답 분포 규칙:
           - MULTIPLE_CHOICE: 정답 인덱스가 최대한 균등(예: 8/8/7/7)이며 3연속 동일 위치 금지.
           - TRUE_FALSE: TRUE/FALSE ≈ 1:1(±2 허용), 3연속 동일 금지, 단순 패턴 금지.
           - SHORT_ANSWER: 중복 답변 금지, 본문 표기와 동일해야 함.
        5) 최종 출력은 오직 JSON 배열만 출력.
        [출력 포맷 예시]
        [
          {
            "question": "HTTP는 상태를 저장하지 않는 프로토콜이다.",
            "type": "TRUE_FALSE",
            "options": [],
            "answer": "TRUE",
            "explanation": "HTTP는 무상태(stateless) 프로토콜로 각 요청이 독립적으로 처리된다.",
            "topic": "HTTP"
          }
        ]
        """;
    }
}
