package com.uma.example.springuma.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;

public class MedicoControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Medico medico;

    @BeforeEach
    void setUp() {
        medico = new Medico();
        medico.setId(1L);
        medico.setDni("835");
        medico.setNombre("Miguel");
        medico.setEspecialidad("Ginecologia");
    }

    private void crearMedico(Medico medico) throws Exception {
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Crear medico y obtenerlo por ID")
    void saveMedico_GetByID() throws Exception {
        crearMedico(medico);

        this.mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(medico.getId()))
                .andExpect(jsonPath("$.nombre").value(medico.getNombre()))
                .andExpect(jsonPath("$.dni").value(medico.getDni()))
                .andExpect(jsonPath("$.especialidad").value(medico.getEspecialidad()));
    }

    @Test
    @DisplayName("Actualizar medico")
    void updateMedico() throws Exception {
        crearMedico(medico);

        medico.setNombre("Miguel Actualizado");
        this.mockMvc.perform(put("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Miguel Actualizado"));
    }

    @Test
    @DisplayName("Eliminar medico")
    void deleteMedico() throws Exception {
        crearMedico(medico);

        this.mockMvc.perform(delete("/medico/" + medico.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Obtener medico por DNI")
    void getByDni() throws Exception {
        crearMedico(medico);

        this.mockMvc.perform(get("/medico/dni/" + medico.getDni()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value(medico.getDni()));
    }

}
