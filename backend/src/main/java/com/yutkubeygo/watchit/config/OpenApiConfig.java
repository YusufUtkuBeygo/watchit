package com.yutkubeygo.watchit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Swagger sayfasının (http://localhost:8080/swagger-ui.html) başlığı ve token ayarı
//Sayfadaki "Authorize" düğmesine login'den dönen token yapıştırılınca bütün istekler Authorization: Bearer <token> ile gider
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI watchitOpenApi()
    {
        return new OpenAPI()
                .info(new Info()
                        .title("Watchit API")
                        .description("YouTube benzeri video platformu. Token almak için POST /users/login")
                        .version("0.0.1"))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
