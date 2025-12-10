package com.warrencrasta.fantasy.yahoo.dto.internal;

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
}