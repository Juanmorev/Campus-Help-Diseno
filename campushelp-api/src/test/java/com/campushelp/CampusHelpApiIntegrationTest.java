package com.campushelp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta contra un MongoDB embebido: el flujo completo de CampusHelp por rol,
 * las negativas de permisos (403), la foto de evidencia y el mapa del campus.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"de.flapdoodle.mongodb.embedded.version=7.0.14", "app.datos-demo=false"})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CampusHelpApiIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private TestRestTemplate rest;

    @Test
    @Order(1)
    void flujoCompletoPorRol() throws Exception {
        String admin = usuario("Patricia", "patricia@campus.edu", "ADM-1", "ADMINISTRADOR", null);
        String vale = usuario("Valentina", "vale@campus.edu", "2023101", "ESTUDIANTE", null);
        String andres = usuario("Andrés", "andres@campus.edu", "2022145", "ESTUDIANTE", null);
        String julian = usuario("Julián", "julian@campus.edu", "MNT-1", "MANTENIMIENTO", "ELECTRICO");
        String marta = usuario("Marta", "marta@campus.edu", "MNT-2", "MANTENIMIENTO", "MOBILIARIO");

        // lugares: solo el administrador los registra
        enviar(post("/api/lugares"), "{\"usuarioId\":\"%s\",\"codigo\":\"a-101\",\"nombre\":\"Aula 101\",\"edificio\":\"Bloque A\",\"piso\":1,\"x\":20,\"y\":30}".formatted(vale))
                .andExpect(status().isForbidden());
        String aula = id(enviar(post("/api/lugares"), "{\"usuarioId\":\"%s\",\"codigo\":\"a-101\",\"nombre\":\"Aula 101\",\"edificio\":\"Bloque A\",\"piso\":1,\"x\":20,\"y\":30}".formatted(admin))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.codigo").value("A-101")));
        String cancha = id(enviar(post("/api/lugares"), "{\"usuarioId\":\"%s\",\"codigo\":\"CANCHA\",\"nombre\":\"Cancha\",\"edificio\":\"Deportes\",\"piso\":0,\"x\":80,\"y\":70}".formatted(admin)));

        // ---- estudiante registra (mantenimiento no puede registrar)
        enviar(post("/api/incidencias"), reporte(julian, aula, "MEDIA")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("estudiantes")));
        enviar(post("/api/incidencias"), "{\"estudianteId\":\"%s\"}".formatted(vale)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasSize(4)));
        JsonNode inc = cuerpo(enviar(post("/api/incidencias"), reporte(vale, aula, "MEDIA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("CH-000001"))
                .andExpect(jsonPath("$.estado").value("REPORTADA"))
                .andExpect(jsonPath("$.colorMapa").value("AMARILLO"))
                .andExpect(jsonPath("$.lugar.nombre").value("Aula 101")));
        String id = inc.get("id").asText();

        // ---- foto: tipo invalido (400), imagen real (201) y descarga
        mvc.perform(multipart("/api/incidencias/" + id + "/evidencias").file(new MockMultipartFile("archivo", "nota.txt", "text/plain", "hola".getBytes()))
                .param("usuarioId", vale)).andExpect(status().isBadRequest());
        byte[] png = pngDePrueba();
        JsonNode conFoto = cuerpo(mvc.perform(multipart("/api/incidencias/" + id + "/evidencias")
                        .file(new MockMultipartFile("archivo", "lampara.png", "image/png", png)).param("usuarioId", vale))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.evidencias", hasSize(1))));
        String url = conFoto.get("evidencias").get(0).get("url").asText();
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png));

        // ---- visibilidad: el otro estudiante no la ve; cada rol lista lo suyo
        mvc.perform(get("/api/incidencias/" + id).param("usuarioId", andres)).andExpect(status().isForbidden());
        mvc.perform(get("/api/incidencias/CH-000001").param("usuarioId", vale)).andExpect(status().isOk());
        mvc.perform(get("/api/incidencias").param("usuarioId", andres)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/incidencias").param("usuarioId", admin)).andExpect(jsonPath("$", hasSize(1)));
        enviar(post("/api/incidencias/" + id + "/informacion"), "{\"usuarioId\":\"%s\",\"texto\":\"Es la lámpara del fondo\"}".formatted(vale))
                .andExpect(status().isCreated());

        // ---- administrador: revisa, valida y asigna (el estudiante no puede validar)
        enviar(patch("/api/incidencias/" + id + "/validacion"), "{\"usuarioId\":\"%s\",\"valida\":true,\"responsableId\":\"%s\"}".formatted(vale, julian))
                .andExpect(status().isForbidden());
        enviar(patch("/api/incidencias/" + id + "/revision"), "{\"usuarioId\":\"%s\"}".formatted(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REVISION"));
        enviar(patch("/api/incidencias/" + id + "/validacion"), "{\"usuarioId\":\"%s\",\"valida\":true}".formatted(admin))
                .andExpect(status().isConflict());
        enviar(patch("/api/incidencias/" + id + "/validacion"), "{\"usuarioId\":\"%s\",\"valida\":true,\"responsableId\":\"%s\",\"prioridad\":\"URGENTE\"}".formatted(admin, julian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ASIGNADA"))
                .andExpect(jsonPath("$.colorMapa").value("ROJO"));

        // ---- mantenimiento: solo el responsable; el admin no puede marcar solucionado
        mvc.perform(get("/api/incidencias").param("usuarioId", julian)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/incidencias").param("usuarioId", marta)).andExpect(jsonPath("$", hasSize(0)));
        enviar(patch("/api/incidencias/" + id + "/reparacion"), "{\"usuarioId\":\"%s\"}".formatted(marta)).andExpect(status().isConflict());
        enviar(patch("/api/incidencias/" + id + "/reparacion"), "{\"usuarioId\":\"%s\",\"nota\":\"Voy en camino\"}".formatted(julian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REPARACION"));
        enviar(post("/api/incidencias/" + id + "/progreso"), "{\"usuarioId\":\"%s\",\"nota\":\"Se cambió el balasto\"}".formatted(julian))
                .andExpect(status().isCreated());
        enviar(patch("/api/incidencias/" + id + "/solucion"), "{\"usuarioId\":\"%s\",\"descripcion\":\"x\"}".formatted(admin))
                .andExpect(status().isForbidden());
        enviar(patch("/api/incidencias/" + id + "/solucion"), "{\"usuarioId\":\"%s\",\"descripcion\":\"Se cambió el tubo LED\"}".formatted(julian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("SOLUCIONADA"))
                .andExpect(jsonPath("$.colorMapa").value("VERDE"));

        // ---- el estudiante recibe las actualizaciones
        mvc.perform(get("/api/usuarios/" + vale + "/notificaciones")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].mensaje", hasItem(containsString("solucionado"))))
                .andExpect(jsonPath("$[*].mensaje", hasItem(containsString("asignado a Julián"))));

        // ---- reporte no valido: se cierra con motivo
        String id2 = id(enviar(post("/api/incidencias"), reporte(andres, cancha, "URGENTE")));
        enviar(patch("/api/incidencias/" + id2 + "/validacion"), "{\"usuarioId\":\"%s\",\"valida\":false,\"motivo\":\"No se encontró el problema\"}".formatted(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADA"));
        enviar(post("/api/incidencias/" + id2 + "/informacion"), "{\"usuarioId\":\"%s\",\"texto\":\"sí existe\"}".formatted(andres))
                .andExpect(status().isConflict());

        // ---- una urgente activa en la cancha para el mapa
        String id3 = id(enviar(post("/api/incidencias"), reporte(andres, cancha, "URGENTE")));
        mvc.perform(get("/api/mapa")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lugaresEnRojo").value(1))
                .andExpect(jsonPath("$.lugaresEnVerde").value(1))
                .andExpect(jsonPath("$.puntos[?(@.codigo=='CANCHA')].color").value(hasItem("ROJO")))
                .andExpect(jsonPath("$.puntos[?(@.codigo=='A-101')].color").value(hasItem("VERDE")));

        // ---- estadisticas solo para el administrador
        mvc.perform(get("/api/estadisticas").param("usuarioId", vale)).andExpect(status().isForbidden());
        mvc.perform(get("/api/estadisticas").param("usuarioId", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.solucionadas").value(1))
                .andExpect(jsonPath("$.cerradas").value(1))
                .andExpect(jsonPath("$.lugaresConMasProblemas[0].codigo").value("CANCHA"));
        assertThat(id3).isNotBlank();
        mvc.perform(get("/api-docs")).andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void fotoDeMasDe10MbEsRechazadaConMensajeEnEspanol() throws Exception {
        String est = usuario("Camila", "camila@campus.edu", "2024033", "ESTUDIANTE", null);
        String adm = usuario("Admin", "admin2@campus.edu", "ADM-2", "ADMINISTRADOR", null);
        String lugar = id(enviar(post("/api/lugares"), "{\"usuarioId\":\"%s\",\"codigo\":\"LAB\",\"nombre\":\"Laboratorio\",\"edificio\":\"B\",\"piso\":2,\"x\":50,\"y\":30}".formatted(adm)));
        String id = id(enviar(post("/api/incidencias"), reporte(est, lugar, "ALTA")));

        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        HttpHeaders cab = new HttpHeaders();
        cab.setContentType(MediaType.IMAGE_JPEG);
        partes.add("archivo", new HttpEntity<>(new ByteArrayResource(new byte[11 * 1024 * 1024]) {
            @Override
            public String getFilename() {
                return "grande.jpg";
            }
        }, cab));
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> r = rest.postForEntity("/api/incidencias/" + id + "/evidencias?usuarioId=" + est,
                new HttpEntity<>(partes, h), String.class);
        assertThat(r.getStatusCode().value()).isEqualTo(413);
        assertThat(r.getBody()).contains("10 MB");
    }

    // ------------------------------------------------------------------ auxiliares

    private String usuario(String nombre, String email, String codigo, String rol, String especialidad) throws Exception {
        String esp = especialidad == null ? "" : ",\"especialidad\":\"" + especialidad + "\"";
        return id(enviar(post("/api/usuarios"), "{\"nombre\":\"%s\",\"email\":\"%s\",\"codigo\":\"%s\",\"rol\":\"%s\"%s}"
                .formatted(nombre, email, codigo, rol, esp)).andExpect(status().isCreated()));
    }

    private static String reporte(String estudiante, String lugar, String prioridad) {
        return "{\"estudianteId\":\"%s\",\"lugarId\":\"%s\",\"tipo\":\"ELECTRICO\",\"descripcion\":\"La lámpara no enciende\",\"prioridad\":\"%s\"}"
                .formatted(estudiante, lugar, prioridad);
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String cuerpo) throws Exception {
        return mvc.perform(b.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String id(ResultActions r) throws Exception {
        return cuerpo(r).get("id").asText();
    }

    private JsonNode cuerpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private static byte[] pngDePrueba() throws Exception {
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
