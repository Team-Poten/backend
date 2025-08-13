package org.poten.backend.clova.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.poten.backend.global.error.ErrorCode;
import org.poten.backend.global.exception.CustomException;
import org.poten.backend.global.infra.ocr.OcrProperty;
import org.poten.backend.clova.dto.request.OcrRequestDto;
import org.poten.backend.clova.dto.response.OcrResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClovaOcrService {

    private final OcrProperty ocrProperty;
    private final ObjectMapper objectMapper;
    private final OkHttpClient client = new OkHttpClient();

    public OcrResponseDto extractTextFromImage(MultipartFile imageFile) {
        try {
            String fileExtension = getFileExtension(imageFile.getOriginalFilename());

            OcrRequestDto ocrRequest = new OcrRequestDto(
                    Collections.singletonList(new OcrRequestDto.Image(fileExtension, "demo")),
                    UUID.randomUUID().toString(),
                    "V2",
                    System.currentTimeMillis()
            );

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("message", objectMapper.writeValueAsString(ocrRequest))
                    .addFormDataPart("file", imageFile.getOriginalFilename(),
                            RequestBody.create(imageFile.getBytes(), MediaType.parse(imageFile.getContentType())))
                    .build();

            Request request = new Request.Builder()
                    .url(ocrProperty.getUrl())
                    .header("X-OCR-SECRET", ocrProperty.getSecret())
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                if (response.body() == null) {
                    throw new IOException("Response body is null");
                }
                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, OcrResponseDto.class);
            }
        } catch (IOException e) {
            throw new OcrServiceException(OcrServiceErrorCode.CLOVA_OCR_API_ERROR);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    @Getter
    @RequiredArgsConstructor
    public enum OcrServiceErrorCode implements ErrorCode {
        CLOVA_OCR_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OCR001", "클로바 OCR API 연동 중 오류가 발생했습니다.");

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;
    }

    public static class OcrServiceException extends CustomException {
        public OcrServiceException(ErrorCode errorCode) {
            super(errorCode);
        }
    }
}
