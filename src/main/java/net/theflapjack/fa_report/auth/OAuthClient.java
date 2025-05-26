package net.theflapjack.fa_report.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

@Component
public class OAuthClient {

    private final WebClient webClient;

    public OAuthClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://tryme.fasolutions.com/auth/realms/fa/protocol/openid-connect")
                .defaultHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
    }

    public String getAccessToken(String username, String password) {

        return webClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(
                        "client_id=external-api" +
                                "&username=" + username +
                                "&password=" + password +
                                "&grant_type=password"
                )
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String refreshAccessToken(String refreshToken) {
        return webClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("client_id=external-api&grant_type=refresh_token&refresh_token=" + refreshToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
