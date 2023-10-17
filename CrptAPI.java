package selsup;

import com.google.gson.Gson;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CrptAPI {
    private final int requestLimit;
    private final long requestIntervalMillis;
    private long lastRequestTime;
    private int requestCount;
    private final Object lock = new Object();

    private final HttpClient httpClient;
    private final Gson gson;

    public CrptAPI(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.requestIntervalMillis = timeUnit.toMillis(1);
        this.lastRequestTime = System.currentTimeMillis();
        this.requestCount = 0;
        this.httpClient = HttpClients.createDefault();
        this.gson = new Gson();
    }

    public void createDocument(String document, String signature) throws IOException {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime >= requestIntervalMillis) {
                requestCount = 0;
                lastRequestTime = currentTime;
            }
            if (requestCount >= requestLimit) {
                throw new IllegalStateException("Request limit exceeded.");
            }
            requestCount++;
        }

        String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/send";
        CreateDocumentRequest request = new CreateDocumentRequest(document, signature);
        String requestBody = gson.toJson(request);
        HttpPost httpPost = new HttpPost(apiUrl);
        httpPost.setEntity(new StringEntity(requestBody));
        httpPost.setHeader("Content-Type", "application/json");
        String responseJson = EntityUtils.toString(httpClient.execute(httpPost).getEntity());
        CreateDocumentResponse response = gson.fromJson(responseJson, CreateDocumentResponse.class);
        if (response.isSuccess()) {
            System.out.println("Document created: " + response.getDocumentId());
        } else {
            System.err.println("Failed to create document: " + response.getErrorDescription());
        }
    }

    private static class CreateDocumentRequest {
        private String document;
        private String signature;

        public CreateDocumentRequest(String document, String signature) {
            this.document = document;
            this.signature = signature;
        }
    }

    private static class CreateDocumentResponse {
        private boolean success;
        private String documentId;
        private String errorDescription;

        public boolean isSuccess() {
            return success;
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getErrorDescription() {
            return errorDescription;
        }
    }
}
