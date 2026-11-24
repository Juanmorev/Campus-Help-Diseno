package com.campushelp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Documentacion OpenAPI y CORS para clientes web. */
@Configuration
public class ConfiguracionApi implements WebMvcConfigurer {

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("CampusHelp API: gestión de incidencias del campus")
                .version("1.0.0")
                .description("Los estudiantes reportan problemas del campus (lugar, tipo, descripción, foto y prioridad), "
                        + "el administrador los valida y asigna, mantenimiento los repara y el mapa muestra dónde se concentran."));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST", "PATCH", "DELETE");
    }
}
