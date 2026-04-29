package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.web.reactive.function.BodyInserters;

import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Informe;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InformeControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Medico medico;
    private Paciente paciente;
    private Imagen imagen;
    private Informe informe;

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

        imagen = new Imagen();
        imagen.setId(1L);
        imagen.setPaciente(paciente);

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

        // Crea imagen
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(Paths.get("src/test/resources/healthy.png").toFile()));
        builder.part("paciente", paciente);

        testClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk();

        // Obtener la imagen creada para tener su ID correcto
        FluxExchangeResult<Imagen> result = testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .returnResult(Imagen.class);

        this.imagen = result.getResponseBody().blockFirst();

        informe = new Informe();
        informe.setId(1L);
        informe.setContenido("Contenido del informe");
        informe.setImagen(this.imagen);
        informe.setPrediccion("NO CANCER");
    }

    @Test
    @DisplayName("Crear informe y obtenerlo por ID")
    void createInforme_GetById() {
        testClient.post().uri("/informe")
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        testClient.get().uri("/informe/" + informe.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Informe.class)
                .value(i -> {
                    assertTrue(i.getId() == informe.getId());
                    assertTrue(i.getContenido().equals(informe.getContenido()));
                });
    }

    @Test
    @DisplayName("Obtener informes por imagen")
    void getInformesByImagen() {
        testClient.post().uri("/informe")
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        testClient.get().uri("/informe/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Informe.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("Eliminar informe")
    void deleteInforme() {
        testClient.post().uri("/informe")
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated();

        testClient.delete().uri("/informe/" + informe.getId())
                .exchange()
                .expectStatus().isNoContent();

        testClient.get().uri("/informe/" + informe.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Informe.class)
                .value(i -> {
                    assertTrue(i == null);
                });
    }
}
