package br.com.fiap.agendafit.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        description = "Token obtido em POST /auth/login")
public class OpenApiConfig {

    @Bean
    public OpenAPI agendaFitOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Agenda Fit API")
                .version("1.0.0")
                .description("API da agenda de aulas de uma academia: grade paginada, buscas por Example e "
                        + "Specification, HATEOAS com links condicionais, inscrições e segurança JWT "
                        + "com perfis USER e ADMIN."));
    }
}
