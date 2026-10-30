package com.cloudnativespringinaction.dispatcherservice;

public record OrderDispatchedMessage(
        Long orderId
) {}
