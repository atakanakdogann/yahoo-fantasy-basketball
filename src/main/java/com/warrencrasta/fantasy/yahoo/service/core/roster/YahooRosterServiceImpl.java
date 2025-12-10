package com.warrencrasta.fantasy.yahoo.service.core.roster;

import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.FantasyContentDTO;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.LeagueDTO;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.PlayerDTO;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.PlayerWrapperDTO;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.PlayersDTO;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.TeamDTO;
import com.warrencrasta.fantasy.yahoo.service.client.YahooClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class YahooRosterServiceImpl implements RosterService {
    
  private static final Logger logger = LoggerFactory.getLogger(YahooRosterServiceImpl.class);
  private final YahooClient yahooClient;

  public YahooRosterServiceImpl(YahooClient yahooClient) {
    this.yahooClient = yahooClient;
  }

  @Override
  public List<PlayerDTO> getTeamRoster(String teamKey) {
    Map<String, String> uriVariables = Map.of("team_key", teamKey);
    FantasyContentDTO response = yahooClient.getFantasyContent(uriVariables, 
        "/team/{team_key}/roster");

    // Zincirleme Null Kontrolü (NPE Koruması)
    if (response != null) {
      TeamDTO team = response.getTeam();
      if (team != null) {
        PlayersDTO roster = team.getRoster();
        if (roster != null && roster.getPlayers() != null) {
          return roster.getPlayers().stream()
              .map(PlayerWrapperDTO::getPlayer)
              .collect(Collectors.toList());
        }
      }
    }
    return Collections.emptyList();
  }

  @Override
  public List<PlayerDTO> getFreeAgents(String leagueKey) {
    Map<String, String> uriVariables = Map.of("league_key", leagueKey);
    FantasyContentDTO response = yahooClient.getFantasyContent(uriVariables, 
        "/league/{league_key}/players;status=FA");

    // Zincirleme Null Kontrolü (NPE Koruması)
    if (response != null) {
      LeagueDTO league = response.getLeague();
      if (league != null) {
        PlayersDTO playersWrapper = league.getPlayers();
        if (playersWrapper != null && playersWrapper.getPlayers() != null) {
          return playersWrapper.getPlayers().stream()
              .map(PlayerWrapperDTO::getPlayer)
              .collect(Collectors.toList());
        }
      }
    }
    return Collections.emptyList();
  }
  
  @Override
  public List<PlayerDTO> getTeamRosterForWeek(String teamKey, String week) {
    Map<String, String> uriVariables = Map.of("team_key", teamKey, "week", week);
    FantasyContentDTO response = yahooClient.getFantasyContent(uriVariables, 
        "/team/{team_key}/roster;week={week}");

    // Zincirleme Null Kontrolü
    if (response != null && response.getTeam() != null 
        && response.getTeam().getRoster() != null 
        && response.getTeam().getRoster().getPlayers() != null) {
        
      return response.getTeam().getRoster().getPlayers().stream()
          .map(PlayerWrapperDTO::getPlayer)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}