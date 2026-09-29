package com.cloudnativespringinaction.catalogservice.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "polar")
public class Properties {
    /**
     * A message to welcome users.
     */
    private String greeting;
    private String saluting;

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public void setSaluting(String saluting) {
        this.saluting = saluting;
    };

    public String getSaluting() {
        return saluting;
    }


}