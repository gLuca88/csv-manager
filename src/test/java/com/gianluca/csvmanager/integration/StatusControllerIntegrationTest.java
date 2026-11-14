package com.gianluca.csvmanager.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StatusControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void restituisceLoStatoApplicativoInFormatoJson() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.application").value("csv-manager"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void rifiutaMetodiNonPrevistiPerLoStato() throws Exception {
        mockMvc.perform(post("/api/status"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void restituisceNotFoundPerUnPercorsoNonEsistente() throws Exception {
        mockMvc.perform(get("/api/non-esiste"))
                .andExpect(status().isNotFound());
    }
}
