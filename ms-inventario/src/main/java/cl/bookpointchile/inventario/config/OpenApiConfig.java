package cl.bookpointchile.inventario.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("BookPoint Chile - API de Inventario")
                .description("Microservicio de control de stock por sucursal: consultas, ajustes físicos, "
                        + "traslados, alertas de reposición y verificación/descuento de stock para ventas.")
                .version("1.0.0"));
    }
}
