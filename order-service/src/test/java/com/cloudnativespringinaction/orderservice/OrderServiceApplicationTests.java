package com.cloudnativespringinaction.orderservice;

import com.cloudnativespringinaction.orderservice.book.Book;
import com.cloudnativespringinaction.orderservice.book.BookClient;
import com.cloudnativespringinaction.orderservice.order.domain.Order;
import com.cloudnativespringinaction.orderservice.order.domain.OrderStatus;
import com.cloudnativespringinaction.orderservice.order.event.OrderAcceptedMessage;
import com.cloudnativespringinaction.orderservice.order.web.OrderRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({TestcontainersConfiguration.class, TestChannelBinderConfiguration.class})
@Testcontainers
class OrderServiceApplicationTests {

    // Customer and employee
    private static KeycloakToken ryanDahlTokens;
    // Customer
    private static KeycloakToken jackDorseyTokens;

    @Container
    private static final KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:26.8")
            .withRealmImportFile("test-realm-config.json");

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private OutputDestination output;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BookClient bookClient;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakContainer.getAuthServerUrl() + "/realms/Bookshop");
    }

    @BeforeAll
    static void generateAccessTokens() {
        WebClient webClient = WebClient.builder()
                .baseUrl(keycloakContainer.getAuthServerUrl() + "/realms/Bookshop/protocol/openid-connect/token")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();

        ryanDahlTokens = authenticateWith("ryandahl", "password", webClient);
        jackDorseyTokens = authenticateWith("jackdorsey", "password", webClient);
    }

    @Test
    void whenGetOwnOrdersThenReturn() {
        var bookIsbn = "1234567893";
        var book = new Book(bookIsbn, "Title", "Author", 9.90);
        given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
        var orderRequest = new OrderRequest(bookIsbn, 1);

        Order expectedOrder = webTestClient
                .post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(jackDorseyTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(jsonMapper.readValue(output.receive(1000, "order-accepted").getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient
                .get()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(jackDorseyTokens.accessToken()))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class).value(orders ->
                        assertThat(orders).extracting(Order::id).contains(expectedOrder.id()));
    }

    @Test
    void whenGetOrdersForAnotherUserThenNotReturned() {
        var bookIsbn = "1234567898";
        var book = new Book(bookIsbn, "Title", "Author", 9.90);
        given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
        var orderRequest = new OrderRequest(bookIsbn, 1);

        Order orderByJackDorsey = webTestClient
                .post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(jackDorseyTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();
        assertThat(orderByJackDorsey).isNotNull();
        assertThat(jsonMapper.readValue(output.receive(1000, "order-accepted").getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(orderByJackDorsey.id()));

        Order orderByRyanDahl = webTestClient
                .post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(ryanDahlTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();
        assertThat(orderByRyanDahl).isNotNull();
        assertThat(jsonMapper.readValue(output.receive(1000, "order-accepted").getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(orderByRyanDahl.id()));

        webTestClient
                .get()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(jackDorseyTokens.accessToken()))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class).value(orders -> assertThat(orders)
                        .extracting(Order::id)
                        .contains(orderByJackDorsey.id())
                        .doesNotContain(orderByRyanDahl.id()));
    }

    @Test
    void whenPostRequestAndBookExistsThenOrderAccepted() {
        var bookIsbn = "1234567899";
        var book = new Book(bookIsbn, "Title", "Author", 9.90);
        given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
        var orderRequest = new OrderRequest(bookIsbn, 3);

        Order createdOrder = webTestClient
                .post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(jackDorseyTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.bookName()).isEqualTo(book.title() + " - " + book.author());
        assertThat(createdOrder.bookPrice()).isEqualTo(book.price());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);

        assertThat(jsonMapper.readValue(output.receive(1000, "order-accepted").getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(createdOrder.id()));
    }

    @Test
    void whenPostRequestAndBookNotExistsThenOrderRejected() {
        var bookIsbn = "1234567894";
        given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.empty());
        var orderRequest = new OrderRequest(bookIsbn, 3);

        Order createdOrder = webTestClient
                .post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(jackDorseyTokens.accessToken()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void whenPostRequestUnauthenticatedThen401() {
        var orderRequest = new OrderRequest("1234567895", 1);

        webTestClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private static KeycloakToken authenticateWith(String username, String password, WebClient webClient) {
        return webClient
                .post()
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "bookshop-test")
                        .with("username", username)
                        .with("password", password))
                .retrieve()
                .bodyToMono(KeycloakToken.class)
                .block();
    }

    private record KeycloakToken(@JsonProperty("access_token") String accessToken) {
    }

}
