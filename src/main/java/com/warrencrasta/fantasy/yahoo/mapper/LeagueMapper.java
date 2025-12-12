package com.warrencrasta.fantasy.yahoo.mapper;

import com.warrencrasta.fantasy.yahoo.domain.league.YahooLeague;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.LeagueWrapperDTO;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LeagueMapper {

  public List<YahooLeague> leagueWrapperDTOsToYahooLeagues(List<LeagueWrapperDTO> leagueWrapperDTOs) {
    if (leagueWrapperDTOs == null) {
      return Collections.emptyList();
    }
    return leagueWrapperDTOs.stream()
        .map(this::leagueWrapperDTOtoYahooLeague)
        .collect(Collectors.toList());
  }

  public YahooLeague leagueWrapperDTOtoYahooLeague(LeagueWrapperDTO leagueWrapperDTO) {
    if (leagueWrapperDTO == null || leagueWrapperDTO.getLeague() == null) {
      return null;
    }
    // Constructor requires: (id, name)
    String id = leagueWrapperDTO.getLeague().getLeagueKey();
    String name = leagueWrapperDTO.getLeague().getName();
    return new YahooLeague(id, name);
  }
}
