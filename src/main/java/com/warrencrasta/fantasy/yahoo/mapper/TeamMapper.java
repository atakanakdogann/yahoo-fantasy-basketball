package com.warrencrasta.fantasy.yahoo.mapper;

import com.warrencrasta.fantasy.yahoo.domain.team.YahooTeam;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.TeamWrapperDTO;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

  public List<YahooTeam> teamWrapperDTOsToYahooTeams(List<TeamWrapperDTO> teamWrapperDTOs) {
    if (teamWrapperDTOs == null) {
      return Collections.emptyList();
    }
    return teamWrapperDTOs.stream()
        .map(this::teamWrapperDTOtoYahooTeam)
        .collect(Collectors.toList());
  }

  public YahooTeam teamWrapperDTOtoYahooTeam(TeamWrapperDTO teamWrapperDTO) {
    if (teamWrapperDTO == null || teamWrapperDTO.getTeam() == null) {
      return null;
    }
    YahooTeam yahooTeam = new YahooTeam();
    // Map to ID (superclass field) AND teamKey (subclass field) for safety
    String key = teamWrapperDTO.getTeam().getTeamKey();
    yahooTeam.setId(key);
    yahooTeam.setTeamKey(key);

    yahooTeam.setName(teamWrapperDTO.getTeam().getName());
    return yahooTeam;
  }
}
