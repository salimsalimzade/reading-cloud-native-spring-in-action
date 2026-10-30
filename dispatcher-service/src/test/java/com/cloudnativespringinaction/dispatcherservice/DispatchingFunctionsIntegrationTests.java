package com.cloudnativespringinaction.dispatcherservice;

import java.util.function.Function;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.messaging.Message;

@Disabled("Replaced by FunctionsStreamIntegrationTests, which covers the messaging integration")
@FunctionalSpringBootTest
class DispatchingFunctionsIntegrationTests {

    @Autowired
    private FunctionCatalog catalog;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void packAndLabelOrder() {
        Function<OrderAcceptedMessage, Flux<Message<byte[]>>> packAndLabel =
                catalog.lookup(Function.class, "pack|label");
        long orderId = 121;

        StepVerifier.create(packAndLabel.apply(new OrderAcceptedMessage(orderId)))
                .expectNextMatches(message -> jsonMapper
                        .readValue(message.getPayload(), OrderDispatchedMessage.class)
                        .equals(new OrderDispatchedMessage(orderId)))
                .verifyComplete();
    }
}
