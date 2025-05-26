package net.theflapjack.fa_report.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class FaGraphQLClient {

    private final WebClient baseClient;
    private static final Logger logger = LoggerFactory.getLogger(FaGraphQLClient.class);


    public FaGraphQLClient() {
        this.baseClient = WebClient.builder()
                .baseUrl("https://tryme.fasolutions.com/graphql")
                .defaultHeader("Content-Type", "application/json")
                .build();

    }

    public String sendQuery(String graphqlQuery, String variablesJson, String accessToken) {
        String requestBody = """
                {
                  "query": "%s",
                  "variables": %s
                }
            """.formatted(graphqlQuery.replace("\"", "\\\""), variablesJson);

        logger.debug("Sending GraphQL query to API with token ending in: {}", accessToken.substring(Math.max(0, accessToken.length() - 6)));

        try {

            return baseClient.post()
                    .uri("")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

        } catch (WebClientResponseException e) {
            logger.error("GraphQL API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("GraphQL call failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in GraphQL client", e);
            throw new RuntimeException("Unexpected GraphQL error", e);
        }
    }
}
