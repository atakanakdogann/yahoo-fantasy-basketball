package com.fantasytoys.fantasy.yahoo.service.core.league;

import com.fantasytoys.fantasy.yahoo.domain.stat.StatCategory;
import com.fantasytoys.fantasy.yahoo.dto.internal.LeagueInfoDTO;
import java.util.List;
import java.util.Map;

public interface LeagueService {

  LeagueInfoDTO getLeagueInfo(String leagueId);

  LeagueInfoDTO getLeagueInfoWithSos(String leagueId);

  List<StatCategory> getRelevantCategories(String leagueId);

  Map<String, Double> getLeagueWinRates(String leagueId);
}
