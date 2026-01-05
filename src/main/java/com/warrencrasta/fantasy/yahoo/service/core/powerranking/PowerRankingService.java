package com.fantasytoys.fantasy.yahoo.service.core.powerranking;

import com.fantasytoys.fantasy.yahoo.domain.stat.StatCategory;
import com.fantasytoys.fantasy.yahoo.domain.stat.TeamStatCategory;
import com.fantasytoys.fantasy.yahoo.domain.team.YahooTeam;
import com.fantasytoys.fantasy.yahoo.dto.internal.LeagueInfoDTO;
import com.fantasytoys.fantasy.yahoo.service.core.league.LeagueService;
import com.fantasytoys.fantasy.yahoo.service.core.stat.StatService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PowerRankingService {

  private final LeagueService leagueService;
  private final StatService statService;

  public PowerRankingService(LeagueService leagueService, StatService statService) {
    this.leagueService = leagueService;
    this.statService = statService;
  }

  @Cacheable(value = "power-rankings", key = "#leagueId + '-last6weeks'")
  public List<YahooTeam> calculatePowerRankings(String leagueId) {
    return calculatePowerRankings(leagueId, false);
  }

  @Cacheable(value = "power-rankings", key = "#leagueId + '-' + (#fullSeason ? 'fullseason' : 'last6weeks')")
  public List<YahooTeam> calculatePowerRankings(String leagueId, boolean fullSeason) {
    List<StatCategory> relevantCategories = leagueService.getRelevantCategories(leagueId);
    LeagueInfoDTO leagueInfo = leagueService.getLeagueInfo(leagueId);

    int currentWeek = Integer.parseInt(leagueInfo.getWeeks().get(0).replace("Week ", ""));
    int actualStartWeek = Integer.parseInt(leagueInfo.getWeeks()
        .get(leagueInfo.getWeeks().size() - 1).replace("Week ", ""));

    // Use full season or limit to last 6 weeks based on parameter
    int startWeek = fullSeason ? actualStartWeek : Math.max(actualStartWeek, currentWeek - 5);

    Map<String, YahooTeam> rankingsMap = leagueInfo.getTeams().stream()
        .collect(Collectors.toMap(YahooTeam::getId, team -> team));

    for (int week = startWeek; week <= currentWeek; week++) {
      List<TeamStatCategory> allTeamsStats = statService.getAllTeamsStats(leagueId, String.valueOf(week),
          relevantCategories);

      if (allTeamsStats.isEmpty()) {
        continue;
      }

      // Skip weeks where all stats are 0 (e.g., Monday before games have been played)
      if (isWeekEmpty(allTeamsStats)) {
        continue;
      }

      for (TeamStatCategory teamAstats : allTeamsStats) {
        YahooTeam prTeamA = rankingsMap.get(teamAstats.getId());
        if (prTeamA == null) {
          continue;
        }

        double weeklyWins = 0;
        double weeklyTies = 0;

        for (TeamStatCategory teamBstats : allTeamsStats) {
          if (teamAstats.getId().equals(teamBstats.getId())) {
            continue;
          }
          double[] results = compareTwoTeams(teamAstats, teamBstats, relevantCategories);
          weeklyWins += results[0];
          weeklyTies += results[1];
        }

        prTeamA.addWeeklyWins(weeklyWins);
        prTeamA.addWeeklyTies(weeklyTies);
        prTeamA.addWeeklyCategoriesPlayed(relevantCategories.size() * (allTeamsStats.size() - 1));
      }
    }

    List<YahooTeam> finalRankings = new ArrayList<>(rankingsMap.values());

    for (YahooTeam team : finalRankings) {
      team.calculateFinalWinRate();

    }

    finalRankings.sort(Comparator.comparingDouble(YahooTeam::getWinRate).reversed());

    return finalRankings;
  }

  private double[] compareTwoTeams(TeamStatCategory teamA,
      TeamStatCategory teamB, List<StatCategory> categories) {
    double wins = 0;
    double ties = 0;
    for (var i = 0; i < categories.size(); i++) {
      StatCategory catInfo = categories.get(i);
      var teamAstat = teamA.getStatCategories().get(i);
      var teamBstat = teamB.getStatCategories().get(i);
      var valueA = Double.parseDouble(teamAstat.getValue());
      var valueB = Double.parseDouble(teamBstat.getValue());
      boolean isBad = catInfo.isBad();
      int comparison = Double.compare(valueA, valueB);

      if (comparison == 0) {
        ties++;
      } else if ((comparison > 0 && !isBad) || (comparison < 0 && isBad)) {
        wins++;
      }
    }
    return new double[] { wins, ties };
  }

  /**
   * Checks if a week has no meaningful stats (all zeros).
   * This happens on Mondays before any games have been played.
   */
  private boolean isWeekEmpty(List<TeamStatCategory> allTeamsStats) {
    for (TeamStatCategory teamStats : allTeamsStats) {
      for (StatCategory stat : teamStats.getStatCategories()) {
        double value = Double.parseDouble(stat.getValue());
        if (value != 0.0) {
          return false;
        }
      }
    }
    return true;
  }
}