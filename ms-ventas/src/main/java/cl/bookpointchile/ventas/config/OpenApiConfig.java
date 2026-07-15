package cl.bookpointchile.ventas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("BookPoint Chile - API de Ventas")
                .description("Microservicio encargado del registro y consulta de ventas. "
                        + "Orquesta la validación de usuario, promociones y stock vía Feign, "
                        + "y publica eventos para la emisión de documentos tributarios.")
                .version("1.0.0"));
    }
}
