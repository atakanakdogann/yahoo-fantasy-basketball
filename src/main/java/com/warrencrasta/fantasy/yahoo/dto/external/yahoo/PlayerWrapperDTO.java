package com.fantasytoys.fantasy.yahoo.dto.external.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PlayerWrapperDTO.PlayerWrapperDeserializer.class)
public class PlayerWrapperDTO {

  private PlayerDTO player;

  public static class PlayerWrapperDeserializer extends JsonDeserializer<PlayerWrapperDTO> {
    @Override
    public PlayerWrapperDTO deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {
      PlayerWrapperDTO wrapper = new PlayerWrapperDTO();
      ObjectCodec codec = p.getCodec();
      JsonNode node = codec.readTree(p);

      // 1. DURUM: Veri direkt oyuncunun kendisi ise (Şu anki loglarınızdaki durum)
      // Eğer "player_key" veya "player_id" varsa, bu bir oyuncudur.
      if (node.has("player_key") || node.has("player_id")) {
        try {
          PlayerDTO playerDTO = codec.treeToValue(node, PlayerDTO.class);
          wrapper.setPlayer(playerDTO);
          return wrapper; // Hemen dön, iş bitti.
        } catch (Exception e) {
          // Hata olursa aşağıdakileri dene
        }
      }

      // 2. DURUM: İç içe "player" alanı varsa (Eski/Alternatif format)
      JsonNode playerNode = null;
      if (node.has("player")) {
        playerNode = node.get("player");
      } else if (node.isArray()) {
        playerNode = node; // Direkt dizi ise
      }

      if (playerNode != null) {
        // Eğer nesne ise
        if (playerNode.isObject()) {
          try {
            wrapper.setPlayer(codec.treeToValue(playerNode, PlayerDTO.class));
            return wrapper;
          } catch (Exception e) {
            // Boş
          }
        }
        // Eğer dizi ise (Array)
        if (playerNode.isArray()) {
          for (JsonNode part : playerNode) {
            if (part.has("player_key")) {
              try {
                PlayerDTO playerDTO = codec.treeToValue(part, PlayerDTO.class);
                wrapper.setPlayer(playerDTO);
                return wrapper;
              } catch (Exception e) {
                // boş
              }
            }
          }
        }
      }
      return wrapper;
    }
  }
}