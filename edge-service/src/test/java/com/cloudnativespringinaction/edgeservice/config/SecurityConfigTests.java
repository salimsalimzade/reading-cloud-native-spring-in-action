package com.cloudnativespringinaction.edgeservice.config;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.when;

@WebFluxTest
@Import(SecurityConfig.class)
class SecurityConfigTests {

    @Autowired
    WebTestClient webClient;

    @MockitoBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Test
    void whenLogoutAuthenticatedAndWithCsrfTokenThen302() {
        when(clientRegistrationRepository.findByRegistrationId("test"))
                .thenReturn(Mono.just(testClientRegistration()));

        webClient
                .mutateWith(SecurityMockServerConfigurers.mockOidcLogin())
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .post()
                .uri("/logout")
                .exchange()
                .expectStatus().isFound();
    }

    @Test
    void whenLogoutWithCsrfTokenFromCookieLikeAngularThen302() {
        when(clientRegistrationRepository.findByRegistrationId("test"))
                .thenReturn(Mono.just(testClientRegistration()));

        var token = "plain-csrf-token";

        webClient
                .mutateWith(SecurityMockServerConfigurers.mockOidcLogin())
                .post()
                .uri("/logout")
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)
                .exchange()
                .expectStatus().isFound();
    }

    @Test
    void whenLogoutAuthenticatedAndWithoutCsrfTokenThen403() {
        webClient
                .mutateWith(SecurityMockServerConfigurers.mockOidcLogin())
                .post()
                .uri("/logout")
                .exchange()
                .expectStatus().isForbidden();
    }

    private ClientRegistration testClientRegistration() {
        return ClientRegistration.withRegistrationId("test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("test")
                .authorizationUri("https://sso.bookshop.com/auth")
                .tokenUri("https://sso.bookshop.com/token")
                .redirectUri("https://bookshop.com")
                .build();
    }
}
