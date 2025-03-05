package contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp.logger.MyCustomLogger;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// https://x-clients-be.onrender.com/docs
public class XClientsTest {

    public static final String URL = "https://x-clients-be.onrender.com/company";
    public static final String URL_LOGIN = "https://x-clients-be.onrender.com/auth/login";
    private OkHttpClient client;
    private final MediaType JSON = MediaType.get("application/json");
    ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(new MyCustomLogger());
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        mapper = new ObjectMapper();
        client = new OkHttpClient.Builder().addNetworkInterceptor(interceptor).build();
    }

    @Test
    public void shouldReturnArrayOnGetCompanyList() throws IOException {
        Request request = new Request.Builder().url(URL).build();

        Response response = client.newCall(request).execute();

        String body = response.body().string();
        assertEquals(200, response.code());
        assertTrue(body.startsWith("["));
        assertTrue(body.endsWith("]"));
    }

    @Test
    public void shouldReturn401WithoutToken() throws IOException {

        String json = """
                {
                  "name": "ping",
                  "description": "pong"
                }
                """;

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(URL).post(body).build();

        Response response = client.newCall(request).execute();

        assertEquals(401, response.code());
        assertEquals("{\"statusCode\":401,\"message\":\"Unauthorized\"}", response.body().string());
    }

    @Test
    public void shouldReturn401WithoutValidToken() throws IOException {

        String json = """
                {
                  "name": "ping",
                  "description": "pong"
                }
                """;

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(URL)
                .header("x-client-token", "NON_VALID_TOKEN")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        assertEquals(401, response.code());
        assertEquals("{\"statusCode\":401,\"message\":\"Unauthorized\"}", response.body().string());
    }

    @Test
    public void shouldReturn201OnCompanyCreated() throws IOException {

        String json = """
                {
                  "name": "ping",
                  "description": "pong"
                }
                """;


        RequestBody reqBody = RequestBody.create(json, JSON);
        Request createRequest = new Request.Builder()
                .url(URL)
                .header("x-client-token", getToken())
                .post(reqBody)
                .build();

        Response response = client.newCall(createRequest).execute();

        JsonNode jsonNode = mapper.readTree(response.body().string());

        int newID = jsonNode.get("id").asInt();

        assertEquals(201, response.code());
        assertTrue(newID > 0);
    }

    private String getToken() throws IOException {

        String json = """
                {
                  "username": "leonardo",
                  "password": "leads"
                }
                """;

        RequestBody authBody = RequestBody.create(json, JSON);
        Request request = new Request.Builder().post(authBody).url(URL_LOGIN).build();
        Response response = client.newCall(request).execute();

        String body = response.body().string();


        JsonNode jsonNode = mapper.readTree(body);
        return jsonNode.get("userToken").asText();
    }
}
