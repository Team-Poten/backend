package org.poten.backend.global.infra.clova;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.poten.backend.global.exception.OkhttpException;

import java.io.IOException;

@Slf4j
public abstract class OkHttpRequest {

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // 응답 대기 시간
        .build();

    public static Response createRequest(Request request) {
        StringBuilder logStringBuilder = new StringBuilder();
        Response response;
        try {
            response = client.newCall(request)
                .execute();
        } catch (IOException e) {
            logStringBuilder.append("[Okhttp Request Fail]");
            logStringBuilder.append("Request Url : ");
            logStringBuilder.append(request.url());
            logStringBuilder.append(" Request Body : ");
            logStringBuilder.append(requestBodyToString(request));
            log.error(logStringBuilder.toString());
            throw new OkhttpException(e.getMessage());
        }
        return response;
    }

    private static String requestBodyToString(Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final okio.Buffer buffer = new okio.Buffer();
            RequestBody body = copy.body();
            if (body != null) {
                body.writeTo(buffer);
                return buffer.readUtf8();
            }
            return "";
        } catch (final IOException e) {
            return "did not work";
        }
    }

    protected abstract String convertRequestToString();
}
