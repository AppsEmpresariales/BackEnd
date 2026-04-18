package com.eam.proyecto.config;

// IMPORTACIONES NECESARIAS
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration  // Le dice a Spring que esta es una clase de configuración
@OpenAPIDefinition(  // Define la información general de la API
        info = @Info(
                title = "Documentos XFIS API",           // Nombre que aparece en Swagger
                version = "1.0.0",                       // Versión de la API
                description = "API REST para la gestión del ciclo de vida de documentos digitales. " +
                        "Incluye creación, edición, flujos de aprobación y control multi-tenant por organización.",
                contact = @Contact(                      // Información de contacto
                        name = "Construccion de Apps Empresariales",
                        email = "dev@eam.edu.co",
                        url = "eam.edu.co"
                ),
                license = @License(                      // Licencia del software
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        // CORRECCIÓN: Las URLs deben incluir el context-path "/docucloud"
        // definido en application.properties (server.servlet.context-path=/docucloud)
        // Sin esto, el "Try it out" de Swagger apunta a rutas incorrectas.
        servers = {
                @Server(
                        url = "http://localhost:8080/docucloud",
                        description = "Servidor de Desarrollo"
                ),
                @Server(
                        url = "http://localhost:8090/docucloud",
                        description = "Servidor de Pruebas"
                ),
                @Server(
                        url = "http://localhost:8099/docucloud",
                        description = "Servidor de Producción"
                )
        }
)
public class OpenApiConfig {

    @Bean  // Crea el objeto que Spring usa para configurar OpenAPI
    public OpenAPI customOpenAPI() {
        return new OpenAPI();
    }
}
