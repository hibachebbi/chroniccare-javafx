package com.chroniccare.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GoogleOAuthService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private volatile boolean configLoaded = false;
    private volatile Properties fileConfig = null;

    public static final class GoogleUserInfo {
        private final String email;
        private final String givenName;
        private final String familyName;
        private final String name;
        private final String pictureUrl;
        private final String sub;

        public GoogleUserInfo(String email, String givenName, String familyName, String name, String pictureUrl, String sub) {
            this.email = email;
            this.givenName = givenName;
            this.familyName = familyName;
            this.name = name;
            this.pictureUrl = pictureUrl;
            this.sub = sub;
        }

        public String getEmail() {
            return email;
        }

        public String getGivenName() {
            return givenName;
        }

        public String getFamilyName() {
            return familyName;
        }

        public String getName() {
            return name;
        }

        public String getPictureUrl() {
            return pictureUrl;
        }

        public String getSub() {
            return sub;
        }
    }

    public boolean isConfigured() {
        String clientId = getSetting("GOOGLE_CLIENT_ID");
        String clientSecret = getSetting("GOOGLE_CLIENT_SECRET");
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public String getMissingConfigKeys() {
        String clientId = getSetting("GOOGLE_CLIENT_ID");
        String clientSecret = getSetting("GOOGLE_CLIENT_SECRET");

        StringBuilder sb = new StringBuilder();
        if (clientId == null || clientId.isBlank()) {
            sb.append("GOOGLE_CLIENT_ID");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("GOOGLE_CLIENT_SECRET");
        }
        return sb.toString();
    }

    public GoogleUserInfo authenticateInteractive() throws Exception {
        String clientId = getSetting("GOOGLE_CLIENT_ID");
        String clientSecret = getSetting("GOOGLE_CLIENT_SECRET");

        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID non configuré.");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_SECRET non configuré.");
        }

        String state = randomUrlSafeToken(16);

        HttpServer server = null;
        try {
            CompletableFuture<Map<String, String>> callbackParams = new CompletableFuture<>();

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            int port = server.getAddress().getPort();
            String redirectUri = "http://127.0.0.1:" + port + "/oauth2callback";

            server.createContext("/oauth2callback", exchange -> handleCallback(exchange, state, callbackParams));
            server.start();

            URI authUri = buildAuthUri(clientId, redirectUri, state);
            openBrowser(authUri);

            Map<String, String> params = callbackParams.get(180, TimeUnit.SECONDS);
            String code = params.get("code");
            if (code == null || code.isBlank()) {
                throw new IllegalStateException("Code OAuth manquant.");
            }

            String accessToken = exchangeCodeForToken(code, redirectUri, clientId, clientSecret);
            return fetchUserInfo(accessToken);
        } finally {
            if (server != null) {
                try {
                    server.stop(0);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void handleCallback(HttpExchange exchange, String expectedState,
                                       CompletableFuture<Map<String, String>> callbackParams) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());

        String state = query.get("state");
        String error = query.get("error");

        String html;
        int status = 200;

        if (error != null && !error.isBlank()) {
            html = "<html><body><h3>Connexion annulée (" + escapeHtml(error) + ").</h3>Vous pouvez fermer cette fenêtre.</body></html>";
            status = 400;
            callbackParams.completeExceptionally(new IllegalStateException("OAuth error: " + error));
        } else if (expectedState != null && !expectedState.equals(state)) {
            html = "<html><body><h3>Erreur de sécurité (state invalide).</h3>Vous pouvez fermer cette fenêtre.</body></html>";
            status = 400;
            callbackParams.completeExceptionally(new IllegalStateException("State invalide."));
        } else {
            html = "<html><body><h3>Connexion réussie.</h3>Vous pouvez fermer cette fenêtre et revenir à l'application.</body></html>";
            callbackParams.complete(query);
        }

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static URI buildAuthUri(String clientId, String redirectUri, String state) {
        String scope = urlEncode("openid email profile");

        String query = "client_id=" + urlEncode(clientId) +
                "&redirect_uri=" + urlEncode(redirectUri) +
                "&response_type=code" +
                "&scope=" + scope +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + urlEncode(state);

        return URI.create(AUTH_URL + "?" + query);
    }

    private String exchangeCodeForToken(String code, String redirectUri, String clientId, String clientSecret) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String body = "code=" + urlEncode(code) +
                "&client_id=" + urlEncode(clientId) +
                (clientSecret != null && !clientSecret.isBlank() ? "&client_secret=" + urlEncode(clientSecret) : "") +
                "&redirect_uri=" + urlEncode(redirectUri) +
                "&grant_type=authorization_code";

        HttpRequest req = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("Échec token OAuth (" + resp.statusCode() + ") : " + resp.body());
        }

        Map<String, Object> json = MAPPER.readValue(resp.body(), new TypeReference<Map<String, Object>>() {
        });
        Object token = json.get("access_token");
        if (token == null) {
            throw new IllegalStateException("access_token manquant.");
        }
        return String.valueOf(token);
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest req = HttpRequest.newBuilder(URI.create(USERINFO_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("Échec userinfo OAuth (" + resp.statusCode() + ") : " + resp.body());
        }

        Map<String, Object> json = MAPPER.readValue(resp.body(), new TypeReference<Map<String, Object>>() {
        });

        String email = toStr(json.get("email"));
        String givenName = toStr(json.get("given_name"));
        String familyName = toStr(json.get("family_name"));
        String name = toStr(json.get("name"));
        String picture = toStr(json.get("picture"));
        String sub = toStr(json.get("sub"));

        return new GoogleUserInfo(email, givenName, familyName, name, picture, sub);
    }

    private static String toStr(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return map;

        for (String part : rawQuery.split("&")) {
            if (part.isBlank()) continue;
            int idx = part.indexOf('=');
            String k = idx >= 0 ? part.substring(0, idx) : part;
            String v = idx >= 0 ? part.substring(idx + 1) : "";

            k = urlDecode(k);
            v = urlDecode(v);
            map.put(k, v);
        }
        return map;
    }

    private static void openBrowser(URI uri) throws IOException {
        if (uri == null) throw new IllegalArgumentException("URL manquante.");

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
                return;
            }
        }

        throw new IllegalStateException("Impossible d'ouvrir le navigateur automatiquement. Ouvrez ce lien : " + uri);
    }

    private static String randomUrlSafeToken(int bytes) {
        byte[] b = new byte[Math.max(8, bytes)];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String getSetting(String key) {
        String envOrProp = readEnvOrProperty(key);
        if (envOrProp != null && !envOrProp.isBlank()) {
            return envOrProp.trim();
        }

        Properties props = loadFileConfig();
        if (props == null) return null;

        String fileValue = props.getProperty(key);
        if (fileValue == null || fileValue.isBlank()) return null;
        return fileValue.trim();
    }

    private Properties loadFileConfig() {
        if (configLoaded) return fileConfig;
        synchronized (this) {
            if (configLoaded) return fileConfig;
            configLoaded = true;

            Path path = resolveConfigPath();
            if (path == null || !Files.exists(path)) {
                fileConfig = null;
                return null;
            }

            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
                fileConfig = props;
                return props;
            } catch (IOException e) {
                fileConfig = null;
                return null;
            }
        }
    }

    private Path resolveConfigPath() {
        String explicit = readEnvOrProperty("GOOGLE_OAUTH_CONFIG_PATH");
        if (explicit != null && !explicit.isBlank()) {
            try {
                return Paths.get(explicit.trim());
            } catch (Exception ignored) {
                return null;
            }
        }

        return Paths.get(System.getProperty("user.dir"), "google-oauth.properties");
    }

    private static String readEnvOrProperty(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) return value;
        return System.getProperty(key);
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
