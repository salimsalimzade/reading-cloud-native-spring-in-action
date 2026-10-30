package com.cloudnativespringinaction.orderservice.order.event;

public record OrderDispatchedMessage(
        Long orderId
) {}
