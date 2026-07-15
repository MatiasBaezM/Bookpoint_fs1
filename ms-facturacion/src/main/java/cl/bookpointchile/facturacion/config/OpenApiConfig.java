package cl.bookpointchile.facturacion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("BookPoint Chile - API de Facturación")
                .description("Microservicio de emisión y consulta de documentos tributarios "
                        + "(boletas y facturas) asociados a las ventas.")
                .version("1.0.0"));
    }
}
