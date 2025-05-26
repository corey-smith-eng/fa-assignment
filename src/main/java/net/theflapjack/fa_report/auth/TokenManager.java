package net.theflapjack.fa_report.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TokenManager {

    private final OAuthClient oauthClient;
    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiry;

    private final ObjectMapper mapper = new ObjectMapper();

    public TokenManager(OAuthClient oauthClient) {
        this.oauthClient = oauthClient;
        this.tokenExpiry = Instant.EPOCH;
    }

    public synchronized String getValidAccessToken(String username, String password) {
        Instant now = Instant.now();

        if (accessToken == null || tokenExpiry.isBefore(now)) {
            if (refreshToken != null) {
                try {
                    System.out.println("Refreshing access token...");
                    String response = oauthClient.refreshAccessToken(refreshToken);
                    updateTokensFromJson(response);
                    return accessToken;
                } catch (Exception e) {
                    System.out.println("Refresh failed, falling back to full login");
                }
            }
            String response = oauthClient.getAccessToken(username, password);
            updateTokensFromJson(response);
        }

        return accessToken;
    }

    private void updateTokensFromJson(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            this.accessToken = root.get("access_token").asText();
            this.refreshToken = root.get("refresh_token").asText();
            this.tokenExpiry = Instant.now().plusSeconds(root.get("expires_in").asLong() - 30);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token JSON", e);
        }
    }
}
