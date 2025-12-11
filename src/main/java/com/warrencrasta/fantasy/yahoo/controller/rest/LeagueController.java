package com.warrencrasta.fantasy.yahoo.controller.rest;

import com.warrencrasta.fantasy.yahoo.domain.team.YahooTeam;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.PlayerDTO;
import com.warrencrasta.fantasy.yahoo.dto.internal.LeagueInfoDTO;
import com.warrencrasta.fantasy.yahoo.dto.internal.MatchupDTO;
import com.warrencrasta.fantasy.yahoo.dto.internal.TradeAnalysisRequestDTO;
import com.warrencrasta.fantasy.yahoo.dto.internal.TradeAnalysisResultDTO;
import com.warrencrasta.fantasy.yahoo.service.core.league.LeagueService;
import com.warrencrasta.fantasy.yahoo.service.core.livestandings.LiveStandingsService;
import com.warrencrasta.fantasy.yahoo.service.core.player.PlayerService;
import com.warrencrasta.fantasy.yahoo.service.core.powerranking.PowerRankingService;
import com.warrencrasta.fantasy.yahoo.service.core.roster.RosterService;
import com.warrencrasta.fantasy.yahoo.service.core.scoreboard.ScoreboardService;
import com.warrencrasta.fantasy.yahoo.service.core.trade.TradeAnalyzerService;

import java.util.List;

import javax.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leagues")
@Validated
public class LeagueController {

  private final LiveStandingsService liveStandingsService;
  private final LeagueService leagueService;
  private final ScoreboardService scoreboardService;
  private final PowerRankingService powerRankingService;
  private final PlayerService playerService;
  private final RosterService rosterService;
  private final TradeAnalyzerService tradeAnalyzerService;

  public LeagueController(LeagueService leagueService, ScoreboardService scoreboardService,
      PowerRankingService powerRankingService, LiveStandingsService liveStandingsService,
      PlayerService playerService, RosterService rosterService,
      TradeAnalyzerService tradeAnalyzerService) {
    this.leagueService = leagueService;
    this.liveStandingsService = liveStandingsService;
    this.scoreboardService = scoreboardService;
    this.powerRankingService = powerRankingService;
    this.playerService = playerService;
    this.rosterService = rosterService;
    this.tradeAnalyzerService = tradeAnalyzerService;
  }

  @GetMapping("/{leagueId}/info")
  public LeagueInfoDTO getLeagueInfo(@PathVariable @NotBlank String leagueId) {
    return leagueService.getLeagueInfo(leagueId);
  }

  @GetMapping("/{leagueId}/sos-info")
  public LeagueInfoDTO getLeagueInfoWithSos(@PathVariable @NotBlank String leagueId) {
    return leagueService.getLeagueInfoWithSos(leagueId);
  }

  @GetMapping("/{leagueId}/power-rankings")
  public List<YahooTeam> getPowerRankings(@PathVariable String leagueId) {
    return powerRankingService.calculatePowerRankings(leagueId);
  }

  @GetMapping("/{leagueId}/matchup-comparisons")
  public List<MatchupDTO> getWeeklyMatchups(
      @PathVariable String leagueId, @RequestParam String week, @RequestParam String teamId) {
    return scoreboardService.getWeeklyMatchups(leagueId, week, teamId);
  }

  @GetMapping("/{leagueId}/live-standings")
  public List<YahooTeam> getLiveStandings(@PathVariable String leagueId) {
    // Şimdilik direkt servisi çağıralım (servisi constructor'a eklemeyi unutmayın!)
    return liveStandingsService.getLiveStandings(leagueId);
  }

  // Bir takımın kadrosunu getirir (Dropdown doldurmak için)
  @GetMapping("/{leagueId}/team/{teamKey}/roster")
  public List<PlayerDTO> getTeamRoster(@PathVariable String leagueId,
      @PathVariable String teamKey) {
    return rosterService.getTeamRoster(teamKey);
  }

  @PostMapping("/{leagueId}/team/{teamKey}/analyze-trade")
  public TradeAnalysisResultDTO analyzeTrade(
      @PathVariable String leagueId,
      @PathVariable String teamKey,
      @RequestBody TradeAnalysisRequestDTO request) {

    return tradeAnalyzerService.analyzeTrade(leagueId, teamKey, request);
  }

  // Boştaki oyuncuları getirir ("Filler Player" araması için)
  @GetMapping("/{leagueId}/free-agents")
  public List<PlayerDTO> getFreeAgents(@PathVariable String leagueId,
      @RequestParam(required = false, defaultValue = "") String query) {
    return playerService.searchFreeAgents(leagueId, query);
  }
}