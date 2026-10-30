package com.cloudnativespringinaction.orderservice.order.event;

public record OrderAcceptedMessage(
        Long orderId
) {}
