package com.warrencrasta.fantasy.yahoo.service.core.roster;

import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.PlayerDTO;
import java.util.List;

public interface RosterService {

  List<PlayerDTO> getTeamRoster(String teamKey);
  
  List<PlayerDTO> getFreeAgents(String leagueKey);
  
  List<PlayerDTO> getTeamRosterForWeek(String teamKey, String week);

}