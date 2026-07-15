package cl.bookpointchile.sucursales.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("BookPoint Chile - API de Sucursales")
                .description("Microservicio de administración de sucursales: creación, "
                        + "actualización y consulta de puntos de venta de la red.")
                .version("1.0.0"));
    }
}
