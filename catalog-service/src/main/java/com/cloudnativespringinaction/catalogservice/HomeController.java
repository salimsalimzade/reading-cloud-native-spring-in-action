package com.cloudnativespringinaction.catalogservice;

import com.cloudnativespringinaction.catalogservice.config.Properties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final Properties polarProperties;

    public HomeController(Properties polarProperties) {
        this.polarProperties = polarProperties;
    }

    @GetMapping("/")
    public String getGreeting() {
        return polarProperties.getGreeting();
    }

    ;
}
