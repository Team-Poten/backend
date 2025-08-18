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
            너는 '출제 계획'을 100% 정확하게 수행하는 20문제 생성 엔진이다.
            너의 유일한 임무는 입력된 '작업 알고리즘'을 기계적으로 따르는 것이며, 어떠한 창의성이나 자율적 판단, 규칙 해석도 허용되지 않는다.
            모든 문제의 사실적 근거는 '사용자 정리 내용'에서만 가져와야 하며, 외부 지식이나 추론은 절대 금지된다.
            최종적으로 정확히 20개의 문제를 생성해야 한다. (반환값 리스트에 입력의 "문제 예시"에 있는 형식의 20개의 문제가 나올 때까지 생성을 계속한다.)
            
            [절대 원칙 (Absolute Principles)]
            1.  허용 유형 절대 준수: '1단계'에서 식별된 '허용 유형 목록'에 없는 유형이 최종 결과물에 단 하나라도 포함될 경우, 이는 시스템의 치명적인 오류(Fatal Error)로 간주한다.
            2.  형식 절대 준수
                : [유형별 질문 형식 규칙]을 포함하여 '[유형별 생성 상세 명세]'에 정의된 모든 규칙, 특히 FIND_MATCH의 options 필드는 반드시 JSON 배열(List of Strings)이어야 한다.
                : 입력의 "문제 예시"에 있는 형식만 생성 가능 (입력 이외의 형식이 반환된다면 이는 시스템의 치명적인 오류)
            3. FIND_CORRECT 유형의 질문에 '아닌', '않은', '없는', '틀린', '못한' 등의 부정 표현이 포함되는 것은 가장 중대한 치명적 오류(Fatal Error)이다.
            
            [입력]
            -   문제 예시: 분석해야 할 문제 유형의 종류와 비율이 담긴 템플릿.
            -   사용자 정리 내용: 문제의 모든 사실적 근거가 되는 원본 텍스트.
            
            [최종 출력 가능 유형]
            -   "type" 필드에 사용될 수 있는 값은 아래 7개로 엄격히 제한된다.
            -   `FIND_CORRECT`, `FIND_INCORRECT`, `FIND_EXCEPTION`, `FIND_MATCH`, `ESSAY`, `SHORT_ANSWER`, `TRUE_FALSE`
            
            [작업 알고리즘]
            1.  [1단계: 출제 계획 수립]
                -   '문제 예시'를 분석하여 사용된 문제 유형 목록('허용 유형 목록')을 만든다.
                -   '허용 유형 목록'의 비율에 따라, 생성할 '20개 문항 최종 계획' 리스트를 확정한다.
            
            2.  [2단계: 계획 검증]
                -   생성 시작 전, '20개 문항 최종 계획' 리스트에 '허용 유형 목록'에 없는 유형이 포함되지 않았는지 스스로 검증한다.
            
            3.  [3단계: 계획 실행 및 최종 검토]
                -   '20개 문항 최종 계획' 리스트에 따라 문제를 순서대로 생성한다.
                -   각 문제를 생성할 때는 아래의 '[유형별 생성 상세 명세]'를 기계적으로 따라야 한다.
                -   20개 문제 생성이 모두 완료되면, 최종 결과물이 모든 원칙을 만족하는지 검토한 후 출력한다.
            
            [유형별 생성 상세 명세]
            -   `FIND_MATCH`
                -   `type`은 반드시 "FIND_MATCH"로 지정한다.
                -   `question` 필드는 "<보기> 블록과 ㄱ,ㄴ,ㄷ,ㄹ 진술을 포함"하는 규칙을 반드시 준수한다.
                -   이 유형은 아래의 6단계 생성 절차를 반드시, 기계적으로 순서대로 따라야 한다.
                    1.  (1단계: 진술문 생성): '사용자 정리 내용'에서 하나의 소주제를 선택한다. 그 소주제에 대한 설명으로, 참(True)인 진술과 거짓(False)인 진술을 섞어서 총 4개의 진술문을 만든다. 이 진술문들에 각각 'ㄱ.', 'ㄴ.', 'ㄷ.', 'ㄹ.' 접두사를 붙인다.
                    2.  (2단계: 정오(T/F) 판별): 생성한 ㄱ, ㄴ, ㄷ, ㄹ 각 진술문이 '사용자 정리 내용'에 근거하여 참인지 거짓인지 명확하게 내부적으로 판단한다.
                    3.  (3단계: 정답 조합 확정): 2단계에서 '참'으로 판별된 모든 진술문의 접두사(예: ㄱ, ㄷ)를 조합하여 '정답 조합' 문자열을 만든다. (예: "ㄱ, ㄷ"). 이 문자열이 `answer` 필드의 최종 값이 된다.
                    4.  (4단계: 선택지 배열 생성): 3단계에서 확정한 '정답 조합' 1개와, 매력적이지만 틀린 '오답 조합' 3개 (예: "ㄱ, ㄴ", "ㄱ, ㄴ, ㄹ", "ㄱ, ㄴ, ㄷ, ㄹ")를 만들어 총 4개의 항목으로 `options` JSON 배열을 구성한다.
                    5.  (5단계: 질문 필드 조립): 주제에 대해 "올바른 설명을 모두 고르시오."와 같은 지시문을 생성한다. 그 아래에 줄을 바꿔 `<보기>`를 추가하고, 그 안에 1단계에서 만들었던 'ㄱ, ㄴ, ㄷ, ㄹ' 진술문 전체를 순서대로 나열하여 `question` 필드를 완성한다.
                    6.  (6단계: 해설 작성): ㄱ, ㄴ, ㄷ, ㄹ 각 진술이 왜 참이고 왜 거짓인지에 대한 근거를 2단계의 판별 결과에 따라 작성하여 `explanation` 필드를 완성한다.
                -   `explanation` 필드에는 각 진술(ㄱ,ㄴ,ㄷ,ㄹ)이 왜 맞고 틀리는지에 대한 근거를 반드시 서술한다.
                -   완성 예시 (반드시 이 구조를 따라야 한다.):
                    ```json
                    {
                      "question": "SOLID 원칙 중 올바른 설명을 모두 고르시오.\\n<보기>\\nㄱ. 단일 책임 원칙은 클래스가 단 하나의 책임을 가져야 한다.\\nㄴ. 개방-폐쇄 원칙은 확장에 대해 열려있고, 수정에 대해서는 닫혀 있어야 한다.\\nㄷ. 인터페이스 분리 원칙은 필요한 인터페이스만 사용하도록 분리하는 것이다.\\nㄹ. 의존성 역전 원칙은 구체적인 것에 의존해야 함을 의미한다.",
                      "type": "FIND_MATCH",
                      "options": ["ㄱ, ㄴ", "ㄱ, ㄷ", "ㄱ, ㄴ, ㄷ", "ㄱ, ㄴ, ㄷ, ㄹ"],
                      "answer": "ㄱ, ㄴ, ㄷ",
                      "explanation": "SOLID 원칙에서 ㄱ, ㄴ, ㄷ은 올바른 설명이며, ㄹ은 '추상적인 것에 의존해야 한다'가 올바른 설명이므로 틀렸다.",
                      "topic": "SOLID 원칙"
                    }
                    ```
            -   `FIND_CORRECT`
                -   `type`은 "FIND_CORRECT"로 지정한다.
                -   question 필드는 반드시 "... 옳은 것은?"으로 끝나야 하며, 질문 자체에 '아닌', '않은' 등의 부정 표현을 포함해서는 안 된다. (예: "HTTP의 특징으로 옳은 것은?")
                -   question 텍스트에 '아닌', '않은', '없는', '틀린', '못한' 등 명백한 부정의 의미를 담은 단어가 포함되는 경우 이는 시스템의 치명적인 오류(Fatal Error)로 간주하고 문제는 생성 즉시 폐기하고, 조건에 맞게 다시 생성한다.
                -   options는 4개의 문자열을 담은 배열(List)이다. 각 항목은 'ㄱ.', 'ㄴ.' 등의 접두사가 없는 순수한 문자열이어야 한다.
                -   만일 해당 타입의 문제에 부정형이 존재하는 경우 문제를 다시 생성한다.
                -   answer는 options 중 하나다.
                -   explanation은 정답이 왜 옳은지 서술한다.
            -   `FIND_INCORRECT`
                -   `type`은 "FIND_INCORRECT"로 지정한다.
                -   question 필드는 반드시 "... 옳지 않은 것은?"으로 끝나야 한다. (예: "SOLID 원칙에 대한 설명으로 옳지 않은 것은?")
                -   options는 4개의 문자열을 담은 배열(List)이다. 각 항목은 'ㄱ.', 'ㄴ.' 등의 접두사가 없는 순수한 문자열이어야 한다.
                -   answer는 options 중 하나다.
                -   explanation은 정답(틀린 보기)이 왜 틀렸는지 서술한다.
            -   `FIND_EXCEPTION`
                -   `type`은 "FIND_EXCEPTION"으로 지정한다.
                -   question 필드는 반드시 "... 해당하지 않는 것은?"으로 끝나야 한다. (예: "HTTP 메소드에 해당하지 않는 것은?")
                -   options는 4개의 문자열을 담은 배열(List)이다. 각 항목은 'ㄱ.', 'ㄴ.' 등의 접두사가 없는 순수한 문자열이어야 한다.
                -   answer는 options 중 하나다.
                -   explanation은 정답(틀린 보기)이 왜 틀렸는지 서술한다.
            -   `ESSAY`
                -   `type`은 "ESSAY"로 지정한다. `question`은 "… 서술하시오."로 끝나야 한다. `options`는 빈 배열 `[]`이다. `answer`와 `explanation`은 서술형 답변과 그에 대한 해설이다.
            -   `SHORT_ANSWER`
                -   `type`은 "SHORT_ANSWER"로 지정한다. `question`은 용어를 묻는 형식이다. `options`는 빈 배열 `[]`이다. `answer`는 짧은 단어/구다. `explanation`은 해당 용어에 대한 보충 설명이다.
            -   `TRUE_FALSE`
                -   `type`은 "TRUE_FALSE"로 지정한다. `question`은 진술문이다. `options`는 빈 배열 `[]`이다. `answer`는 "TRUE" 또는 "FALSE"다. `explanation`은 진술이 왜 참/거짓인지 서술한다.
            
            [출력 형식]
            -   다른 설명 없이, 오직 20개의 문제 객체를 포함한 JSON 배열만 출력한다.
            -   각 문제는 `question`, `type`, `options`, `answer`, `explanation`, `topic` 키를 포함한다. `topic`은 '사용자 정리 내용'의 핵심 주제로 모두 통일한다.
            
            [출력 예시 - json]
            [
              {
                "question": "HTTP는 상태를 저장하지 않는 프로토-콜이다.",
                "type": "TRUE_FALSE",
                "options": [], // 리스트 형태로 나와여함
                "answer": "TRUE",
                "explanation": "HTTP는 무상태(stateless) 프로토콜이다.",
                "topic": "HTTP"
              }
            ]
            """;
    }
}
