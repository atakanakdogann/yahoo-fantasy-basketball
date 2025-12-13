package com.fantasytoys.fantasy.yahoo.dto.external.yahoo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PlayerDTO.PlayerDeserializer.class)
public class PlayerDTO {

  @JsonAlias("player_key")
  private String playerKey;

  private NameDTO name;

  @JsonAlias("editorial_team_abbr")
  private String editorialTeamAbbr;

  @JsonAlias("display_position")
  private String displayPosition;

  @JsonAlias("player_stats")
  private PlayerStatsDTO playerStats;

  @JsonAlias("game_schedule")
  private Map<String, String> gameSchedule;

  public String getFullName() {
    return (name != null) ? name.getFull() : "Unknown Player";
  }

  // === ÖZEL ÇEVİRİCİ (Deserializer) ===
  public static class PlayerDeserializer extends JsonDeserializer<PlayerDTO> {
    @Override
    public PlayerDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      PlayerDTO dto = new PlayerDTO();
      ObjectCodec codec = p.getCodec();
      JsonNode node = codec.readTree(p);

      // Eğer Yahoo veriyi DİZİ [] olarak gönderirse (Week/Season Stats çağrıları
      // için)
      if (node.isArray()) {
        for (JsonNode element : node) {
          // 1. Temel Bilgiler (player_key, name, team_abbr)
          // Verinin bu parçası genellikle her zaman vardır
          if (element.has("player_key")) {
            mapBasicFields(dto, element, codec);
          }
          // 2. İstatistikler (player_stats)
          if (element.has("player_stats")) {
            mapStats(dto, element, codec);
          }
        }
      } else if (node.isObject()) {
        mapBasicFields(dto, node, codec);
        mapStats(dto, node, codec);
      }

      return dto;
    }

    // Yardımcı Metot: Temel alanları doldurur
    private void mapBasicFields(PlayerDTO dto, JsonNode node, ObjectCodec codec) {
      try {
        if (node.has("player_key")) {
          dto.setPlayerKey(node.get("player_key").asText());
        }
        if (node.has("editorial_team_abbr")) {
          dto.setEditorialTeamAbbr(node.get("editorial_team_abbr").asText());
        }
        if (node.has("display_position")) {
          dto.setDisplayPosition(node.get("display_position").asText());
        }
        if (node.has("name")) {
          dto.setName(codec.treeToValue(node.get("name"), NameDTO.class));
        }
        if (node.has("game_schedule")) {
          // Game schedule'ı harita olarak oku
          Map<String, String> schedule = codec.treeToValue(node.get("game_schedule"), Map.class);
          dto.setGameSchedule(schedule);
        }
      } catch (Exception e) {
        System.err.println("PlayerDTO Basic Field Mapping Error: " + e.getMessage());
      }
    }

    // Yardımcı Metot: İstatistikleri doldurur (player_stats)
    private void mapStats(PlayerDTO dto, JsonNode node, ObjectCodec codec) {
      if (node.has("player_stats")) {
        try {
          // PlayerStatsDTO'ya çevir
          PlayerStatsDTO stats = codec.treeToValue(node.get("player_stats"), PlayerStatsDTO.class);
          dto.setPlayerStats(stats);
        } catch (Exception e) {
          // BU SATIRI EKLEYİN:
          System.err.println("PLAYER STATS PARSE HATASI: " + e.getMessage());
          e.printStackTrace();
        }
      }
    }
  }
}