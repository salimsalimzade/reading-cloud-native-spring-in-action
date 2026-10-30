package com.cloudnativespringinaction.dispatcherservice;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
class FunctionsStreamIntegrationTests {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void whenOrderAcceptedThenDispatched() {
        long orderId = 121;
        Message<OrderAcceptedMessage> inputMessage = MessageBuilder
                .withPayload(new OrderAcceptedMessage(orderId)).build();
        Message<OrderDispatchedMessage> expectedOutputMessage = MessageBuilder
                .withPayload(new OrderDispatchedMessage(orderId)).build();

        this.input.send(inputMessage);

        assertThat(jsonMapper.readValue(output.receive().getPayload(), OrderDispatchedMessage.class))
                .isEqualTo(expectedOutputMessage.getPayload());
    }
}
