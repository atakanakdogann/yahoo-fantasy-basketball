package com.fantasytoys.fantasy.yahoo.service.core.scoreboard;

import com.fantasytoys.fantasy.yahoo.dto.internal.MatchupDTO;
import java.util.List;

public interface ScoreboardService {

  List<MatchupDTO> getWeeklyMatchups(String leagueId, String week, String teamId);
}
