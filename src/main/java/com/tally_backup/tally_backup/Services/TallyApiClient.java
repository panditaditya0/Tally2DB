package com.tally_backup.tally_backup.Services;

import com.squareup.okhttp.*;
import com.tally_backup.tally_backup.Config.TallyConnectionConfig;
import com.tally_backup.tally_backup.Dto.TallyConnection;
import com.tally_backup.tally_backup.Enum.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TallyApiClient {
    public TallyApiConnectionPool tallyApiConnectionPool;
    public TallyConnectionConfig tallyConnectionConfig;

    public TallyApiClient(TallyApiConnectionPool tallyApiConnectionPool
    , TallyConnectionConfig tallyConnectionConfig) {
        this.tallyApiConnectionPool = tallyApiConnectionPool;
        this.tallyConnectionConfig = tallyConnectionConfig;
    }

    private String executeApiCall(String requestXml, int timeout, TimeUnit timeUnit, HttpMethod httpMethod, String url) {
        try{
            OkHttpClient client = new OkHttpClient();
            client.setReadTimeout(timeout,timeUnit);
            client.setConnectTimeout(timeout,timeUnit);
            client.setWriteTimeout(timeout,timeUnit);
            MediaType mediaType = MediaType.parse("application/xml");
            RequestBody body = RequestBody.create(mediaType, requestXml);
            Request request;
            if (httpMethod == HttpMethod.POST) {
                request = new Request.Builder()
                        .url(url)
                        .method(httpMethod.toString(), body)
                        .addHeader("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=")
                        .build();
            }
            else{
                request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=")
                        .build();
            }

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new Exception("Error while executing Tally API Status code is " + response.code());
            }
            String responseBody = response.body().string();
            return responseBody;
        } catch (Exception ex){
            throw new RuntimeException("Error while executing Tally API Status code is " + ex.getMessage() + " " + ex.getStackTrace());
        }
    }

    private String prepareUrl(TallyConnection tallyConnection) {
        return tallyConnectionConfig.getBase_url() + ":" + tallyConnection.getPort() +"/";
    }

    private TallyConnection checkHealthAndAcquireConnection() throws InterruptedException {
        int retryConnection = 0;
        do {
            TallyConnection connection = tallyApiConnectionPool.getTallyConnection();

            String url = this.prepareUrl(connection);
            String response = this.executeApiCall("", 10, TimeUnit.MINUTES, HttpMethod.GET, url);
            if (response.toLowerCase().contains("is running")) {
                log.info("Accuired port -> " + connection.getPort());
                return connection;
            }
//            tallyApiConnectionPool.closeTallyConnection(connection);
        } while (retryConnection++ < 4);
        throw new RuntimeException("Tally API Connection Failed");
    }

    public String execute(String xmlRequest) throws Exception {
        TallyConnection connection = checkHealthAndAcquireConnection();
        try {
            String responseBody = executeApiCall(xmlRequest, 60, TimeUnit.MINUTES, HttpMethod.POST, prepareUrl(connection));
            if (responseBody.contains("cannot be processed")) {
                throw new Exception("Req Body might be not correct, Tally Response ->  " + responseBody);
            }
            return responseBody;
        } catch (Exception ex) {
            throw new RuntimeException("Error while calling tally api " + ex.getMessage());
        } finally {
            log.info("Releasing port -> " + connection.getPort());
            tallyApiConnectionPool.closeTallyConnection(connection);
        }
    }
}