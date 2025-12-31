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
                                # OHGUN Î™®Î?Î¶¨Ïãù API ?úÎ≤Ñ
                                
                                ## Ï£ºÏöî Í∏∞Îä•
                                
                                - **OAuth ?∏Ï¶ù**: ?§Ïù¥Î≤??åÏÖú Î°úÍ∑∏??ÏßÄ??
                                - **JWT ?†ÌÅ∞ Í¥ÄÎ¶?*: Access Token Î∞?Refresh Token Î∞úÍ∏â
                                - **?¨Ïö©??Í¥ÄÎ¶?*: ?¨Ïö©???ïÎ≥¥ Ï°∞Ìöå Î∞?Í¥ÄÎ¶?
                                - **API ?µÌï©**: Î™®Îì† ?úÎπÑ?§Î? ?®Ïùº ?îÎìú?¨Ïù∏?∏Î°ú ?úÍ≥µ
                                
                                ## ?¨Ïö© Í∞Ä?•Ìïú ?úÎπÑ??
                                
                                - **OAuth Service**: ?§Ïù¥Î≤?Î°úÍ∑∏??(`/oauth/naver/**`)
                                - **User Service**: ?¨Ïö©??Í¥ÄÎ¶?(Ï§ÄÎπ?Ï§?
                                - **Common Service**: Í≥µÌÜµ Í∏∞Îä• (Ï§ÄÎπ?Ï§?
                                - **Environment Service**: ?òÍ≤Ω Í¥ÄÎ¶?(Ï§ÄÎπ?Ï§?
                                - **Social Service**: ?åÏÖú Í∏∞Îä• (Ï§ÄÎπ?Ï§?
                                - **Governance Service**: Í±∞Î≤Ñ?åÏä§ Í∏∞Îä• (Ï§ÄÎπ?Ï§?
                                
                                ## AI/ML ?úÎπÑ??(Î≥ÑÎèÑ ÎßàÏù¥?¨Î°ú?úÎπÑ??
                                
                                - **Crawler Service**: ???¨Î°§Îß?(`http://localhost:9001`)
                                - **Chatbot Service**: Ï±óÎ¥á ?úÎπÑ??(`http://localhost:9002`)
                                - **MLS Service**: Î®∏Ïã†?¨Îãù ?úÎπÑ??(`http://localhost:9004`)
                                - **Transformer Service**: KoELECTRA Í∞êÏÑ±Î∂ÑÏÑù (`http://localhost:9005`)
                                
                                ## ?¨Ïö© ?àÏãú
                                
                                ### ?§Ïù¥Î≤?Î°úÍ∑∏??URL ?îÏ≤≠
                                ```bash
                                GET http://localhost:8080/oauth/naver/login-url
                                ```
                                
                                ### ?§Ïù¥Î≤?Î°úÍ∑∏??ÏΩúÎ∞±
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

