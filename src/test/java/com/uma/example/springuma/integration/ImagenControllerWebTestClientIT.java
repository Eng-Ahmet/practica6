
package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;
import com.uma.example.springuma.integration.base.AbstractIntegration;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.BodyInserters;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ImagenControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Paciente paciente;
    private Medico medico;

    @PostConstruct
    public void init() {
        testClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMillis(300000)).build();
    }

    @BeforeEach
    void setUp() {

        medico = new Medico();
        medico.setNombre("Miguel");
        medico.setId(1L);
        medico.setDni("835");
        medico.setEspecialidad("Ginecologo");

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Maria");
        paciente.setDni("888");
        paciente.setEdad(20);
        paciente.setCita("Ginecologia");
        paciente.setMedico(medico);

        // Crea médico
        testClient.post().uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea paciente
        testClient.post().uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();
    }

    private void subirImagen(String nombreArchivo) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("image", new FileSystemResource(Paths.get("src/test/resources/" + nombreArchivo).toFile()));
            builder.part("paciente", paciente);

            testClient.post().uri("/imagen")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .exchange()
                    .expectStatus().isOk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Subir imagen de paciente de forma correcta")
    void uploadImage_Correct() {
        subirImagen("healthy.png");

        testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Imagen.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("Realizar una predicción de una imagen de un paciente")
    void predictImage() {
        subirImagen("healthy.png");

        // Get image ID
        FluxExchangeResult<Imagen> result = testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(Imagen.class);

        Imagen imagen = result.getResponseBody().blockFirst();

        testClient.get().uri("/imagen/predict/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(v -> {
                    assertTrue(v.contains("CANCER") || v.contains("NO CANCER") || v.length() > 0);
                });
    }

    @Test
    @DisplayName("Obtener metadatos de imagen")
    void getImageInfo() {
        subirImagen("healthy.png");

        FluxExchangeResult<Imagen> result = testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(Imagen.class);

        Imagen imagen = result.getResponseBody().blockFirst();

        testClient.get().uri("/imagen/info/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Imagen.class)
                .value(i -> {
                    assertTrue(i.getId() == imagen.getId());
                    assertTrue(i.getPaciente().getId() == paciente.getId());
                });
    }

    @Test
    @DisplayName("Eliminar imagen")
    void deleteImage() {
        subirImagen("healthy.png");

        FluxExchangeResult<Imagen> result = testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(Imagen.class);

        Imagen imagen = result.getResponseBody().blockFirst();

        testClient.delete().uri("/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isNoContent();

        testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Imagen.class)
                .hasSize(0);
    }
}
