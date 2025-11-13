package com.warrencrasta.fantasy.yahoo.config.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;

// --- GEREKLİ YENİ IMPORT'LAR ---
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
// ---

@EnableWebSecurity
@Configuration
public class OAuth2LoginSecurityConfig {

    // --- YENİ ALAN (Resolver için gerekli) ---
    private final ClientRegistrationRepository clientRegistrationRepository;

    // --- CONSTRUCTOR GÜNCELLEMESİ ---
    public OAuth2LoginSecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }
    // ---

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(authorize -> authorize
                // Bu kurallar CSS/JS (MIME type) sorununu çözer (Sizde zaten vardı)
                .antMatchers("/signin", "/examples", "/contact", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/signin")
                .defaultSuccessUrl("/matchup-comparisons", true)
                // --- YENİ AYAR (HTTPS/Redirect Hatasını çözer) ---
                .authorizationEndpoint(endpoint -> 
                    endpoint.authorizationRequestResolver(
                        authorizationRequestResolver(this.clientRegistrationRepository)
                    )
                )
                // ---
            );
            
        return http.build();
    }

    // --- YENİ METOT (HTTPS/Redirect Hatasını çözer) ---
    /**
     * Spring'in varsayılan adres oluşturucusunu alır ve
     * adres 'http://' ile başlıyorsa onu 'https://' olarak düzeltir.
     */
    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
              ClientRegistrationRepository clientRegistrationRepository) {

        DefaultOAuth2AuthorizationRequestResolver resolver = 
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(customizer -> {
            String originalRedirectUri = customizer.getRedirectUri();
            
            // Eğer URI varsa VE 'http://' ile başlıyorsa...
            if (originalRedirectUri != null && originalRedirectUri.startsWith("http://")) {
                // Onu 'https://' ile değiştir
                String secureRedirectUri = originalRedirectUri.replace("http://", "https://");
                customizer.redirectUri(secureRedirectUri);
            }
        });

        return resolver;
    }
    // ---

    /*
    * Changes RequestRejectedException logs level to DEBUG.
    * (Bu sizde zaten vardı)
    */
    @Bean
    RequestRejectedHandler requestRejectedHandler() {
      return new HttpStatusRequestRejectedHandler();
    }
}
