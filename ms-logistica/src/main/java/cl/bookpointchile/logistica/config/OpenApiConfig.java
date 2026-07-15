package cl.bookpointchile.logistica.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("BookPoint Chile - API de Logística")
                .description("Microservicio de gestión de envíos: creación, seguimiento "
                        + "y actualización de estado de despachos asociados a ventas.")
                .version("1.0.0"));
    }
}
