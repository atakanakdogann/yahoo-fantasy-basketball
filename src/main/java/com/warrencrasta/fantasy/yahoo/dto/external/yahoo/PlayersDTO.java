package com.fantasytoys.fantasy.yahoo.dto.external.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PlayersDTO.PlayersDeserializer.class)
public class PlayersDTO {

  private List<PlayerWrapperDTO> players = new ArrayList<>();

  public List<PlayerWrapperDTO> getPlayers() {
    return players != null ? players : new ArrayList<>();
  }

  public void setPlayers(List<PlayerWrapperDTO> players) {
    this.players = players;
  }

  public static class PlayersDeserializer extends JsonDeserializer<PlayersDTO> {
    @Override
    public PlayersDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      PlayersDTO dto = new PlayersDTO();
      ObjectCodec codec = p.getCodec();
      JsonNode node = codec.readTree(p);

      // ==============================================================
      // YENİ MANTIK: DİZİ İSE DİREKT İŞLE (Logunuzdaki durum)
      // ==============================================================
      if (node.isArray()) {
        for (JsonNode element : node) {
          // Yahoo'nun garip yapısı: Bazen dizi içinde { "player": ... } olur
          // Bazen direkt player verisi olur.
          // PlayerWrapperDTO zaten bu yapıyı çözmek için tasarlandı.
          try {
            // Her elemanı PlayerWrapperDTO'ya çevirip listeye ekle
            PlayerWrapperDTO wrapper = codec.treeToValue(element, PlayerWrapperDTO.class);
            if (wrapper != null) {
              dto.getPlayers().add(wrapper);
            }
          } catch (Exception e) {
            // Hatalı elemanı atla
          }
        }
        return dto;
      }

      // ==============================================================
      // ESKİ MANTIK: NESNE İSE (Map) İÇİNE GİR
      // ==============================================================
      if (node.isObject()) {
        // Bazen "roster": { "players": ... } şeklinde iç içe olabilir
        if (node.has("players")) {
          node = node.get("players");
          // Eğer içindeki "players" bir diziyse, yukarıdaki mantığı tekrarla
          if (node.isArray()) {
            for (JsonNode element : node) {
              try {
                PlayerWrapperDTO wrapper = codec.treeToValue(element, PlayerWrapperDTO.class);
                dto.getPlayers().add(wrapper);
              } catch (Exception e) {
                // Eğer içindeki "players" bir diziyse, yukarıdaki mantığı tekrarla
              }
            }
            return dto;
          }
        }

        // Eğer Nesne {"0": ..., "1": ...} şeklindeyse (Eski Yahoo formatı)
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
          String fieldName = fieldNames.next();
          if (fieldName.matches("-?\\d+(\\.\\d+)?")) {
            JsonNode childNode = node.get(fieldName);
            if (childNode.has("player")) {
              try {
                PlayerWrapperDTO wrapper = codec.treeToValue(childNode, PlayerWrapperDTO.class);
                dto.getPlayers().add(wrapper);
              } catch (Exception e) {
                // Eğer içindeki "players" bir diziyse, yukarıdaki mantığı tekrarla
              }
            }
          }
        }
      }
      return dto;
    }
  }
}