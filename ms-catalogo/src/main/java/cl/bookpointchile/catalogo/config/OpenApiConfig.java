package cl.bookpointchile.catalogo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("BookPoint Chile - API de Catálogo")
                .description("Microservicio que administra el catálogo de productos (libros): "
                        + "búsqueda con filtros y paginación, registro de productos y reseñas de clientes.")
                .version("1.0.0"));
    }
}
