package cl.bookpointchile.usuarios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("BookPoint Chile - API de Usuarios")
                .description("Microservicio de registro y consulta de usuarios (por ID o RUT) "
                        + "y administración de roles.")
                .version("1.0.0"));
    }
}
