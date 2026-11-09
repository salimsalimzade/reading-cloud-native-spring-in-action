package com.cloudnativespringinaction.quoteservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class QuoteServiceApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
    }

    @Test
    void getAllQuotesReturnsAllQuotes() {
        webTestClient.get()
                .uri("/quotes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(6);
    }

    @Test
    void getRandomQuoteReturnsAQuote() {
        webTestClient.get()
                .uri("/quotes/random")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isNotEmpty()
                .jsonPath("$.author").isNotEmpty()
                .jsonPath("$.genre").isNotEmpty();
    }

    @Test
    void getRandomQuoteByGenreReturnsQuoteFromRequestedGenre() {
        webTestClient.get()
                .uri("/quotes/random/FANTASY")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.genre").isEqualTo("FANTASY");
    }
}
