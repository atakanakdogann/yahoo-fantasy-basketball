package com.warrencrasta.fantasy.yahoo.config.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// 'WebSecurityConfigurerAdapter' import'unu sildik
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.filter.ForwardedHeaderFilter;

@EnableWebSecurity
@Configuration // @Configuration anotasyonunu eklemek önemlidir
public class OAuth2LoginSecurityConfig { // Artık 'extends WebSecurityConfigurerAdapter' DEĞİL

  /*
   * Bu, "WebSecurityConfigurerAdapter"in yerini alan
   * modern Spring Security (2.6+) yöntemidir.
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeRequests(authorize -> authorize
            // Bu kurallar CSS/JS/Resim (MIME type) sorununu çözer
            .antMatchers("/signin", "/examples", "/contact", "/css/**", "/js/**", "/images/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/signin")
            .defaultSuccessUrl("/matchup-comparisons", true)
        );
        
    return http.build();
  }

  /*
  * Changes RequestRejectedException logs level to DEBUG.
  * (Bu sizde zaten vardı)
  */
  @Bean
  RequestRejectedHandler requestRejectedHandler() {
    return new HttpStatusRequestRejectedHandler();
  }

  /*
  * 'invalid_redirect_uri' HATASINI ÇÖZEN BEAN
  * Bu, Spring'e Railway'in 'X-Forwarded-Proto' (HTTPS)
  * başlığını (header) tanımasını ve kullanmasını söyler.
  */
  @Bean
  public ForwardedHeaderFilter forwardedHeaderFilter() {
    return new ForwardedHeaderFilter();
  }
}
