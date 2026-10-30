package com.cloudnativespringinaction.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import({TestcontainersConfiguration.class, TestChannelBinderConfiguration.class})
@SpringBootTest
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
