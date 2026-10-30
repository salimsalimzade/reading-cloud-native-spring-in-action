package com.cloudnativespringinaction.dispatcherservice;

public record OrderAcceptedMessage(
        Long orderId
) {}
