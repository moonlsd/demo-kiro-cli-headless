package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HelloController.class)
class HelloControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void indexReturnsWelcomeMessage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Welcome to the demo Spring Boot application"));
    }

    @Test
    void helloReturnsGreetingWithDefaultName() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, World!"));
    }

    @Test
    void helloReturnsGreetingWithProvidedName() throws Exception {
        mockMvc.perform(get("/hello").param("name", "Kiro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Kiro!"));
    }

}
