package com.fantasytoys.fantasy.yahoo.service.core.trade;

import com.fantasytoys.fantasy.yahoo.dto.internal.TradeAnalysisRequestDTO;
import com.fantasytoys.fantasy.yahoo.dto.internal.TradeAnalysisResultDTO;

public interface TradeAnalyzerService {

  TradeAnalysisResultDTO analyzeTrade(String leagueId, String teamKey,
      TradeAnalysisRequestDTO request);
}