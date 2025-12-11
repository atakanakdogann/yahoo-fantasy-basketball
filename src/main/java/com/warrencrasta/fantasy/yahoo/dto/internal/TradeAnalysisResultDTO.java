package com.warrencrasta.fantasy.yahoo.dto.internal;

import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.PlayerDTO;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class TradeAnalysisResultDTO {
  // Örn: <"3PTM", +5>, <"REB", -3>, <"BLK", +8>
  private Map<String, Double> categoryChanges;

  // Örn: <"3PTM_before", 45>, <"3PTM_after", 50>
  private Map<String, Double> categoryTotalsBefore;
  private Map<String, Double> categoryTotalsAfter;

  // Örn: .518
  private double winRateBefore;
  // Örn: .537
  private double winRateAfter;

  // Players involved
  private List<PlayerDTO> teamAPlayers;
  private List<PlayerDTO> teamBPlayers;
}