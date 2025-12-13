package com.fantasytoys.fantasy.yahoo.dto.external.yahoo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatDTO {

  @JsonAlias({ "stat_id", "statId" })
  private String statId;

  private String value; // Oyuncu istatistiği için

  // === YENİ EKLENEN KRİTİK ALANLAR (Hataları çözer) ===

  @JsonAlias({ "display_name", "displayName" })
  private String displayName;

  @JsonAlias({ "is_only_display_stat", "isOnlyDisplayStat" })
  private String isOnlyDisplayStat;

  @JsonAlias({ "sort_order", "sortOrder" })
  private String sortOrder;
}