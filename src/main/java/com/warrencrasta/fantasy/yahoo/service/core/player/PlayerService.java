package com.warrencrasta.fantasy.yahoo.service.core.player;

import java.util.List;

import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.PlayerDTO;

public interface PlayerService {

  // Parametre sayısını artırıyoruz: leagueId ekliyoruz
  PlayerDTO getPlayerStatsForSeason(String leagueId, String playerKey, String season);
  
  // Parametre sayısını artırıyoruz: leagueId ekliyoruz
  PlayerDTO getPlayerStatsForWeek(String leagueId, String playerKey, String week);

  List<PlayerDTO> searchFreeAgents(String leagueId, String query);

}