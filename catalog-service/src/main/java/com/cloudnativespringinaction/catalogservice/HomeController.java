package com.cloudnativespringinaction.catalogservice;

import com.cloudnativespringinaction.catalogservice.config.Properties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final Properties bookshopProperties;

    public HomeController(Properties bookshopProperties) {
        this.bookshopProperties = bookshopProperties;
    }

    @GetMapping("/")
    public String getGreeting() {
        return bookshopProperties.getGreeting();
    }

    ;
}
