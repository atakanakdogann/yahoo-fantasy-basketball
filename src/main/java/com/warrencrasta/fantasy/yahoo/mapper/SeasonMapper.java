package com.fantasytoys.fantasy.yahoo.mapper;

import com.fantasytoys.fantasy.yahoo.domain.season.YahooSeason;
import com.fantasytoys.fantasy.yahoo.dto.external.yahoo.GameWrapperDTO;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SeasonMapper {

  public List<YahooSeason> gameWrapperDTOsToYahooSeasons(List<GameWrapperDTO> gameWrapperDTOs) {
    if (gameWrapperDTOs == null) {
      return Collections.emptyList();
    }
    return gameWrapperDTOs.stream()
        .map(this::gameWrapperDTOtoYahooSeason)
        .collect(Collectors.toList());
  }

  public YahooSeason gameWrapperDTOtoYahooSeason(GameWrapperDTO gameWrapperDTO) {
    if (gameWrapperDTO == null || gameWrapperDTO.getGame() == null) {
      return null;
    }
    // Constructor requires: (id, year)
    String id = gameWrapperDTO.getGame().getGameId();
    String year = gameWrapperDTO.getGame().getSeason();
    return new YahooSeason(id, year);
  }
}