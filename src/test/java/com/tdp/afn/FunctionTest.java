package com.tdp.afn;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.tdp.afn.model.dto.TokenResponse;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@TestMethodOrder(OrderAnnotation.class)
public class FunctionTest {

    Function mockFunction;

    private static MockWebServer mockWebServer;

    private static MockWebServer mockTableServer;

    public FunctionTest() {
        mockFunction = new Function() {
            @Override
            protected void getSetting() {
                mockFunction.salt = "value";
                mockFunction.baseUrl = "http://localhost:9797";
                mockFunction.password = "value";
                mockFunction.tableName = "token";
                mockFunction.storageConnection = "UseDevelopmentStorage=true";
            }
        };
    }

    @BeforeAll
    public static void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(9797);
        mockTableServer = new MockWebServer();
        mockTableServer.start(10002);
    }

    @AfterAll
    static void tearDown() throws Exception {
        mockWebServer.shutdown();
        mockTableServer.shutdown();
    }

    @Test
    @Order(1)
    public void testTimeTriggerOk() throws Exception {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/devstoreaccount1/token":
                        return new MockResponse().setResponseCode(200).setBody(
                            "{\"value\":[{\"PartitionKey\":\"04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff\",\"RowKey\":\"04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff\",\"Timestamp\":\"2021-09-10T05:41:38.1320000Z\",\"RefreshToken\":\"2T5YfvelvTqLn0PFlm1d1Tird8ZcidRw2vMkc+3l8eWUngw2GGZvLuroMPd/FDIZqs5DlNNKhRmymGs5Twmybj8mEdUU11IzIUniDI6989oAXx21knjpkJD0smCV0Q9aXoDz8GccOW2gCJlDnNaOUpWVkytYaHSNFDRJcBqLSvKiuauL2MlpLFcJtqT95ZbNxZitrxiQ5JRqZY+B+Uzj\",\"AccessToken\":\"vmi5qnlDgl32PJNv/Tj+skQdji76wUqaw7E8e8wMuKGrnvPO05ZqtiQotkJsgamFmViABXFQ0oTFEKWizJKLry+q96T4IZPxBW0++ZzHGE95Iei0K0pjeMh5jCDaUbNj/3L4MV0j1fDOn34IlgZ5u+7W/JH8unW6RgrNzUwZ+9eTxlVea5Q7VdK2y9HuFZpIpjLQNTJb5szcj8ymQ6ZQxGQlVF9VkO7hy+zlTG6pBv5xc/SD6NExQp2Qhw==\"}]}")
                                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;odata=nometadata");
                    case "/devstoreaccount1/token(PartitionKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff',RowKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff')":
                        return new MockResponse().setResponseCode(204).addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mockTableServer.setDispatcher(dispatcher);

        mockWebServer.enqueue(
            new MockResponse()
                .setBody(convertObjectToString(createTokenResponse()))
                .setResponseCode(HttpStatus.OK.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            );

        // Invoke
        final String ret = this.mockFunction.run(null, context);

        // Verify
        assertEquals("false", ret);

    }

    @Test
    @Order(2)
    public void testTimeTriggerBackendUnauthorized() throws Exception {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/devstoreaccount1/token":
                        return new MockResponse().setResponseCode(200).setBody(
                            "{\"value\":[{\"PartitionKey\":\"04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff\",\"RowKey\":\"04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff\",\"Timestamp\":\"2021-09-10T05:41:38.1320000Z\",\"RefreshToken\":\"2T5YfvelvTqLn0PFlm1d1Tird8ZcidRw2vMkc+3l8eWUngw2GGZvLuroMPd/FDIZqs5DlNNKhRmymGs5Twmybj8mEdUU11IzIUniDI6989oAXx21knjpkJD0smCV0Q9aXoDz8GccOW2gCJlDnNaOUpWVkytYaHSNFDRJcBqLSvKiuauL2MlpLFcJtqT95ZbNxZitrxiQ5JRqZY+B+Uzj\",\"AccessToken\":\"vmi5qnlDgl32PJNv/Tj+skQdji76wUqaw7E8e8wMuKGrnvPO05ZqtiQotkJsgamFmViABXFQ0oTFEKWizJKLry+q96T4IZPxBW0++ZzHGE95Iei0K0pjeMh5jCDaUbNj/3L4MV0j1fDOn34IlgZ5u+7W/JH8unW6RgrNzUwZ+9eTxlVea5Q7VdK2y9HuFZpIpjLQNTJb5szcj8ymQ6ZQxGQlVF9VkO7hy+zlTG6pBv5xc/SD6NExQp2Qhw==\"}]}")
                                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;odata=nometadata");
                    case "/devstoreaccount1/token(PartitionKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff',RowKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff')":
                        return new MockResponse().setResponseCode(204).addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mockTableServer.setDispatcher(dispatcher);

        mockWebServer.enqueue(
            new MockResponse()
                .setBody(createUnauthorizedResponse())
                .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            );

        final String ret = this.mockFunction.run(null, context);

        assertEquals("true", ret);
    }

    @Test
    @Order(3)
    public void testTimeTriggerBackendStatusCreated() throws Exception {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/devstoreaccount1/token":
                        return new MockResponse().setResponseCode(200).setBody(
                            "{\"value\":[{\"PartitionKey\":\"04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff\",\"RowKey\":\"04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff\",\"Timestamp\":\"2021-09-10T05:41:38.1320000Z\",\"RefreshToken\":\"2T5YfvelvTqLn0PFlm1d1Tird8ZcidRw2vMkc+3l8eWUngw2GGZvLuroMPd/FDIZqs5DlNNKhRmymGs5Twmybj8mEdUU11IzIUniDI6989oAXx21knjpkJD0smCV0Q9aXoDz8GccOW2gCJlDnNaOUpWVkytYaHSNFDRJcBqLSvKiuauL2MlpLFcJtqT95ZbNxZitrxiQ5JRqZY+B+Uzj\",\"AccessToken\":\"vmi5qnlDgl32PJNv/Tj+skQdji76wUqaw7E8e8wMuKGrnvPO05ZqtiQotkJsgamFmViABXFQ0oTFEKWizJKLry+q96T4IZPxBW0++ZzHGE95Iei0K0pjeMh5jCDaUbNj/3L4MV0j1fDOn34IlgZ5u+7W/JH8unW6RgrNzUwZ+9eTxlVea5Q7VdK2y9HuFZpIpjLQNTJb5szcj8ymQ6ZQxGQlVF9VkO7hy+zlTG6pBv5xc/SD6NExQp2Qhw==\"}]}")
                                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;odata=nometadata");
                    case "/devstoreaccount1/token(PartitionKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff',RowKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff')":
                        return new MockResponse().setResponseCode(204).addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mockTableServer.setDispatcher(dispatcher);

        mockWebServer.enqueue(
            new MockResponse()
            .setBody(convertObjectToString(createTokenResponse()))
            .setResponseCode(HttpStatus.CREATED.value())
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            );

        final String ret = this.mockFunction.run(null, context);

        assertEquals("true", ret);
    }

    @Test
    @Order(4)
    public void testTimeTriggerBackendNoAccessToken() throws Exception {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/devstoreaccount1/token":
                        return new MockResponse().setResponseCode(200).setBody(
                            "{\"value\":[{\"PartitionKey\":\"11111111-aaaa-2222-bbbb-33333333dddd\",\"RowKey\":\"11111111-aaaa-2222-bbbb-33333333dddd\",\"Timestamp\":\"2021-09-10T05:41:38.1320000Z\",\"RefreshToken\":\"2T5YfvelvTqLn0PFlm1d1Tird8ZcidRw2vMkc+3l8eWUngw2GGZvLuroMPd/FDIZqs5DlNNKhRmymGs5Twmybj8mEdUU11IzIUniDI6989oAXx21knjpkJD0smCV0Q9aXoDz8GccOW2gCJlDnNaOUpWVkytYaHSNFDRJcBqLSvKiuauL2MlpLFcJtqT95ZbNxZitrxiQ5JRqZY+B+Uzj\",\"AccessToken\":\"\"}]}")
                                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;odata=nometadata");
                    case "/devstoreaccount1/token(PartitionKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff',RowKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff')":
                        return new MockResponse().setResponseCode(204).addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mockTableServer.setDispatcher(dispatcher);

        mockWebServer.enqueue(
            new MockResponse()
                .setBody(convertObjectToString(createEmptyTokenResponse()))
                .setResponseCode(HttpStatus.OK.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            );

        // Invoke
        final String ret = this.mockFunction.run(null, context);

        // Verify
        assertEquals("true", ret);

    }

    @Test
    @Order(5)
    public void testTimeTriggerNoSettings() throws Exception {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        final String ret = new Function().run(null, context);

        assertEquals("true", ret);
    }


    @Test
    @Order(6)
    public void testTimeTriggerBackendUnavailable() throws Exception {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/devstoreaccount1/token":
                        return new MockResponse().setResponseCode(200).setBody(
                            "{\"value\":[{\"PartitionKey\":\"04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff\",\"RowKey\":\"04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff\",\"Timestamp\":\"2021-09-10T05:41:38.1320000Z\",\"RefreshToken\":\"2T5YfvelvTqLn0PFlm1d1Tird8ZcidRw2vMkc+3l8eWUngw2GGZvLuroMPd/FDIZqs5DlNNKhRmymGs5Twmybj8mEdUU11IzIUniDI6989oAXx21knjpkJD0smCV0Q9aXoDz8GccOW2gCJlDnNaOUpWVkytYaHSNFDRJcBqLSvKiuauL2MlpLFcJtqT95ZbNxZitrxiQ5JRqZY+B+Uzj\",\"AccessToken\":\"vmi5qnlDgl32PJNv/Tj+skQdji76wUqaw7E8e8wMuKGrnvPO05ZqtiQotkJsgamFmViABXFQ0oTFEKWizJKLry+q96T4IZPxBW0++ZzHGE95Iei0K0pjeMh5jCDaUbNj/3L4MV0j1fDOn34IlgZ5u+7W/JH8unW6RgrNzUwZ+9eTxlVea5Q7VdK2y9HuFZpIpjLQNTJb5szcj8ymQ6ZQxGQlVF9VkO7hy+zlTG6pBv5xc/SD6NExQp2Qhw==\"}]}")
                                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json;odata=nometadata");
                    case "/devstoreaccount1/token(PartitionKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff',RowKey='04cf3e3e-2e2a-401d-a5f1-d5e3dadee3ff')":
                        return new MockResponse().setResponseCode(204).addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mockTableServer.setDispatcher(dispatcher);

        final String ret = this.mockFunction.run(null, context);

        assertEquals("true", ret);
    }


    private static TokenResponse createTokenResponse() {
        return new TokenResponse("token_type", "access_token", 0L, 0L, "scope", "refresh_token", 0L);
    }

    private static TokenResponse createEmptyTokenResponse() {
        return new TokenResponse("token_type", "", 0L, 0L, "scope", "", 0L);
    }

    private static String createUnauthorizedResponse() {
        return "{ \"error\" : \"invalid_grant\"}";
    }

    private static String convertObjectToString(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = StringUtils.EMPTY;
        try {
            jsonString = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

}
