package com.fantasytoys.fantasy.yahoo.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fantasytoys.fantasy.yahoo.dto.external.yahoo.FantasyContentDTO;
import com.fantasytoys.fantasy.yahoo.dto.external.yahoo.FantasyResponseDTO;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
// >> GEREKLİ YENİ IMPORTLAR
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Service
public class YahooClient {

  private static final Logger logger = LoggerFactory.getLogger(YahooClient.class);
  private final WebClient webClient;
  private final String yahooBaseUri;
  private final ObjectMapper objectMapper;

  // Constructor değişti: Artık hazır WebClient yerine Builder ve AuthManager
  // alıyoruz
  public YahooClient(WebClient.Builder webClientBuilder,
      OAuth2AuthorizedClientManager authorizedClientManager,
      @Value("${yahoo.base.uri}") String yahooBaseUri,
      ObjectMapper objectMapper) {

    this.yahooBaseUri = yahooBaseUri;
    this.objectMapper = objectMapper;

    // 1. OAuth2 Ayarları (Giriş yapabilmek için)
    var oauth2Client = new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauth2Client.setDefaultClientRegistrationId("yahoo");

    // 2. Hafıza Limiti (Büyük veriler için)
    final int bufferSize = 16 * 1024 * 1024; // 16MB
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
        .build();

    // 3. URL KODLAMASINI KAPATMA (En Kritik Kısım)
    // Bu sayede ';' karakteri '%3B'ye dönüşmez ve Yahoo parametreleri anlar.
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

    // 4. Özel WebClient'ı İnşa Etme
    this.webClient = webClientBuilder
        .uriBuilderFactory(factory)
        .apply(oauth2Client.oauth2Configuration())
        .exchangeStrategies(strategies)
        .build();
  }

  public FantasyContentDTO getFantasyContent(Map<String, String> uriVariables,
      String pathTemplate) {

    // 1. Değişkenleri (örn: {team_key}) manuel olarak yerleştir
    String finalPath = pathTemplate;
    for (Map.Entry<String, String> entry : uriVariables.entrySet()) {
      finalPath = finalPath.replace("{" + entry.getKey() + "}", entry.getValue());
    }

    // 2. URL Birleştirme (Çift Slash Koruması)
    String base = yahooBaseUri;
    if (base.endsWith("/") && finalPath.startsWith("/")) {
      base = base.substring(0, base.length() - 1);
    } else if (!base.endsWith("/") && !finalPath.startsWith("/")) {
      base = base + "/";
    }

    // Tam URL'i oluştur
    String urlString = base + finalPath + "?format=json_f";

    // 3. URI oluşturma (EncodingMode.NONE olduğu için olduğu gibi gidecek)
    URI fullFantasyUri = URI.create(urlString);

    try {
      // Loglama (Sadece stats veya roster çağrıları için)
      if (pathTemplate.contains("stats") || pathTemplate.contains("roster")) {
        logger.info("[YAHOO REQUEST] {}", fullFantasyUri);
      }

      String rawJson = webClient
          .get()
          .uri(fullFantasyUri)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Debug için raw JSON'u logla (Gerekirse açabilirsiniz)
      if (pathTemplate.contains("stats")) {
        logger.info("[YAHOO RAW JSON] {}", rawJson);
      }

      FantasyResponseDTO responseDTO = objectMapper.readValue(rawJson, FantasyResponseDTO.class);
      return Objects.requireNonNull(responseDTO).getFantasyContent();

    } catch (Exception e) {
      logger.error("Yahoo API Hatası (URL: {}): {}", fullFantasyUri, e.getMessage());
      return null;
    }
  }

  public FantasyContentDTO getFantasyContent(String path) {
    return getFantasyContent(new HashMap<>(), path);
  }
}