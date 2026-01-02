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
                                # OHGUN ???? API ??
                                
                                ## ?? ??
                                
                                - **OAuth ??**: ??? ?? ??? ??
                                - **JWT ?? ??**: Access Token ? Refresh Token ??
                                - **??? ??**: ??? ?? ?? ? ??
                                - **API ??**: ?? ???? ??? ?????? ??
                                
                                ## ?? ??? ???
                                
                                - **OAuth Service**: ??? ???(`/oauth/naver/**`)
                                - **User Service**: ??? ??(???)
                                - **Common Service**: ?? ?? (???)
                                - **Environment Service**: ?? ??(???)
                                - **Social Service**: ?? ?? (???)
                                - **Governance Service**: ???? ?? (???)
                                
                                ## AI/ML ???(?? ???????)
                                
                                - **Crawler Service**: ? ???(`http://localhost:9001`)
                                - **Chatbot Service**: ?? ???(`http://localhost:9002`)
                                - **MLS Service**: ???? ???(`http://localhost:9004`)
                                - **Transformer Service**: KoELECTRA ???? (`http://localhost:9005`)
                                
                                ## ?? ??
                                
                                ### ??? ??? URL ??
                                ```bash
                                GET http://localhost:8080/oauth/naver/login-url
                                ```
                                
                                ### ??? ??? ??
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
