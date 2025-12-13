package com.fantasytoys.fantasy.yahoo.service.core.player;

import java.util.List;

import com.fantasytoys.fantasy.yahoo.dto.external.yahoo.PlayerDTO;

public interface PlayerService {

  // Parametre sayısını artırıyoruz: leagueId ekliyoruz
  PlayerDTO getPlayerStatsForSeason(String leagueId, String playerKey, String season);

  // Parametre sayısını artırıyoruz: leagueId ekliyoruz
  PlayerDTO getPlayerStatsForWeek(String leagueId, String playerKey, String week);

  PlayerDTO getPlayerStatsAverageLastMonth(String leagueId, String playerKey);

  List<PlayerDTO> searchFreeAgents(String leagueId, String query);

}