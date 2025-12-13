package com.fantasytoys.fantasy.yahoo.config.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;
// --- Bu import 'build()' metodu için gerekli ---

@EnableWebSecurity
@Configuration
public class OAuth2LoginSecurityConfig {

  private final ClientRegistrationRepository clientRegistrationRepository;

  public OAuth2LoginSecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeRequests(authorize -> authorize
            // CSS/JS/Resim (MIME type) sorununu çözer
            .antMatchers("/signin", "/examples", "/contact", "/css/**", "/js/**", "/images/**")
            .permitAll()
            .anyRequest().authenticated())
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/signin")
            .defaultSuccessUrl("/matchup-comparisons", true)
            // HTTPS/Redirect Hatasını çözer
            .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(
                authorizationRequestResolver(this.clientRegistrationRepository))));

    return http.build();
  }

  /**
   * Spring'in varsayılan adres oluşturucusunu alır ve
   * adres 'http://' ile başlıyorsa onu 'https://' olarak düzeltir.
   */
  private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository) {

    DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository, "/oauth2/authorization");

    resolver.setAuthorizationRequestCustomizer(customizer -> {

      // ==========================================================
      // >> HATA DÜZELTMESİ BURADA <<
      // ==========================================================

      // 1. Önce 'customizer' (Builder) üzerinden 'request' nesnesini al
      OAuth2AuthorizationRequest request = customizer.build();

      // 2. Şimdi 'request' nesnesinden URI'yi oku (getRedirectUri() DEĞİL)
      String originalRedirectUri = request.getRedirectUri();

      // 3. Kontrol et ve 'https' ile değiştir
      if (originalRedirectUri != null && originalRedirectUri.startsWith("http://")) {
        String secureRedirectUri = originalRedirectUri.replace("http://", "https://");

        // 4. 'customizer' (Builder) üzerinde yeni URI'yi ayarla
        customizer.redirectUri(secureRedirectUri);
      }
      // ==========================================================
    });

    return resolver;
  }

  @Bean
  RequestRejectedHandler requestRejectedHandler() {
    return new HttpStatusRequestRejectedHandler();
  }
}
