package com.warrencrasta.fantasy.yahoo.service.core.trade;

import com.warrencrasta.fantasy.yahoo.dto.internal.TradeAnalysisRequestDTO;
import com.warrencrasta.fantasy.yahoo.dto.internal.TradeAnalysisResultDTO;

public interface TradeAnalyzerService {
    
  TradeAnalysisResultDTO analyzeTrade(String leagueId, String teamKey, 
      TradeAnalysisRequestDTO request);
}