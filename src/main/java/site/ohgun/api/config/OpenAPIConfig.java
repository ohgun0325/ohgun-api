package site.ohgun.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OHGUN API Gateway")
                        .version("1.0.0")
                        .description("""
                                # OHGUN Monolithic API Documentation
                                
                                ## Features
                                
                                - **OAuth Integration**: Social login and authentication
                                - **JWT Token Management**: Access Token and Refresh Token support
                                - **User Management**: User registration and profile management
                                - **API Gateway**: Centralized API endpoint management
                                
                                ## Service Endpoints
                                
                                - **OAuth Service**: Social authentication (`/oauth/naver/**`)
                                - **User Service**: User management (CRUD)
                                - **Common Service**: Common utilities (Utilities)
                                - **Environment Service**: Environment configuration (Config)
                                - **Social Service**: Social features (Social)
                                - **Governance Service**: System governance (Admin)
                                
                                ## AI/ML Services (Microservices)
                                
                                - **Crawler Service**: Web crawling (`http://localhost:9001`)
                                - **Chatbot Service**: Chat bot service (`http://localhost:9002`)
                                - **MLS Service**: Machine learning service (`http://localhost:9004`)
                                - **Transformer Service**: KoELECTRA model service (`http://localhost:9005`)
                                
                                ## Quick Start
                                
                                ### Get OAuth Login URL
                                ```bash
                                GET http://localhost:8080/oauth/naver/login-url
                                ```
                                
                                ### OAuth Callback
                                ```bash
                                GET http://localhost:8080/oauth/naver/callback?code={code}&state={state}
                                ```
                                """)
                        .contact(new Contact()
                                .name("OHGUN Team")
                                .email("support@ohgun.site"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
