package com.fantasytoys.fantasy.yahoo.dto.internal;

import java.util.List;
import lombok.Data;

@Data
public class TradeAnalysisRequestDTO {
  // Takım A'nın (kullanıcının) verdiği oyuncuların playerKey'leri
  private List<String> teamAPlayerKeys;

  // Takım A'nın aldığı oyuncuların (rakip + filler) playerKey'leri
  private List<String> teamBPlayerKeys;
}