package ec.udla.consulta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public AppConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST");
    }

    @Bean
    public RestClient sriClient(@Value("${app.sri.timeout-ms}") int timeoutMs) {
        return RestClient.builder()
                .requestFactory(factory(timeoutMs))
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient antClient(@Value("${app.ant.timeout-ms}") int timeoutMs) {
        return RestClient.builder()
                .requestFactory(factory(timeoutMs))
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .defaultHeader("Accept", "text/html")
                .build();
    }

    private ClientHttpRequestFactory factory(int timeoutMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeoutMs);
        f.setReadTimeout(timeoutMs);
        return f;
    }
}
