package com.tally_backup.tally_backup.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RequestDataApiClient {
    private final OkHttpClient client = new OkHttpClient();

    @Value("${request_data_baseurl}")
    public String requestDataBaseUrl;

    @Autowired
    private ObjectMapper mapper;

    public void notifyClient(String processId, String Status) {
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Map<String, String> bodyMap = new HashMap<String, String>();
            bodyMap.put("Status", Status);
            bodyMap.put("ArtifactUrl", "N/A");
            bodyMap.put("RequestId", processId);
            String url = requestDataBaseUrl + "api/tally-report/request/" + processId;
            RequestBody body = RequestBody.create(mediaType, mapper.writeValueAsString(bodyMap));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    log.error("Update status request failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    log.error("Updated status to {}", Status);
                }
            });
        } catch (Exception ex) {
            log.error("ERROR while updating status {} ", ex.getMessage());
        }
    }
}