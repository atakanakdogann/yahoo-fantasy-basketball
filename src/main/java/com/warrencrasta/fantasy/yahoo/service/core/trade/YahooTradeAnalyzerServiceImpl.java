package com.warrencrasta.fantasy.yahoo.service.core.trade;

import com.warrencrasta.fantasy.yahoo.domain.stat.StatCategory;
import com.warrencrasta.fantasy.yahoo.domain.stat.TeamStatCategory;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.PlayerDTO;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.StatDTO;
import com.warrencrasta.fantasy.yahoo.dto.external.yahoo.StatWrapperDTO;
import com.warrencrasta.fantasy.yahoo.dto.internal.LeagueInfoDTO;
import com.warrencrasta.fantasy.yahoo.dto.internal.TradeAnalysisRequestDTO;
import com.warrencrasta.fantasy.yahoo.dto.internal.TradeAnalysisResultDTO;
import com.warrencrasta.fantasy.yahoo.service.core.league.LeagueService;
import com.warrencrasta.fantasy.yahoo.service.core.player.PlayerService;
import com.warrencrasta.fantasy.yahoo.service.core.roster.RosterService;
import com.warrencrasta.fantasy.yahoo.service.core.stat.StatService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class YahooTradeAnalyzerServiceImpl implements TradeAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(YahooTradeAnalyzerServiceImpl.class);

    private final LeagueService leagueService;
    private final StatService statService;
    private final RosterService rosterService;
    private final PlayerService playerService;

    private final String CURRENT_SEASON = "2025";

    // >>> GÜNCELLEME 1: İsteğinize göre değiştirildi <<<
    private final double TEAM_WEEKLY_FGA = 500.0;
    private final double TEAM_WEEKLY_FTA = 130.0;

    public YahooTradeAnalyzerServiceImpl(LeagueService leagueService, StatService statService,
            RosterService rosterService, PlayerService playerService) {
        this.leagueService = leagueService;
        this.statService = statService;
        this.rosterService = rosterService;
        this.playerService = playerService;
    }

    @Override
    public TradeAnalysisResultDTO analyzeTrade(String leagueId, String teamKey, TradeAnalysisRequestDTO request) {
        long startTime = System.currentTimeMillis();
        logger.info("[TRADE ANALYSIS START] League: {}, Team: {}", leagueId, teamKey);

        List<StatCategory> relevantCategories = leagueService.getRelevantCategories(leagueId);
        LeagueInfoDTO leagueInfo = leagueService.getLeagueInfo(leagueId);

        int currentWeek = 1;
        int startWeek = 1;
        try {
            currentWeek = Integer.parseInt(leagueInfo.getWeeks().get(0).replace("Week ", ""));
            startWeek = Integer
                    .parseInt(leagueInfo.getWeeks().get(leagueInfo.getWeeks().size() - 1).replace("Week ", ""));
        } catch (Exception e) {
            logger.warn("Week parsing error, defaulting to 1.");
        }

        Map<String, PlayerDTO> playerSeasonAvgs = cachePlayerSeasonStats(leagueId, request);

        Map<String, Double> categoryTotalsBefore = new HashMap<>();
        Map<String, Double> categoryTotalsAfter = new HashMap<>();
        double totalWinsBefore = 0.0;
        double totalWinsAfter = 0.0;

        // Analysis runs from Start Week up to (but excluding) Current Week
        // (Backtesting) - Synced with Power Rankings (Last 6 Weeks)
        int backtestStartWeek = Math.max(startWeek, currentWeek - 6);
        for (int week = backtestStartWeek; week < currentWeek; week++) {
            List<TeamStatCategory> allTeamsStatsWeekX = statService.getAllTeamsStats(leagueId, String.valueOf(week),
                    relevantCategories);
            if (allTeamsStatsWeekX.isEmpty())
                continue;

            TeamStatCategory teamA_RealStats = allTeamsStatsWeekX.stream()
                    .filter(t -> t.getId().equals(teamKey)).findFirst().orElse(null);
            List<TeamStatCategory> opponentsWeekX = allTeamsStatsWeekX.stream()
                    .filter(t -> !t.getId().equals(teamKey)).collect(Collectors.toList());

            if (teamA_RealStats == null)
                continue;

            int teamGamesPlayedWeekX = getTeamGamesPlayedForWeek(teamKey, String.valueOf(week));

            TeamStatCategory teamA_BaselineStats = createTeamBaseline(teamA_RealStats, request.getTeamAPlayerKeys(),
                    playerSeasonAvgs, teamGamesPlayedWeekX);
            TeamStatCategory teamA_Scenario1_Stats = createScenarioStats(teamA_BaselineStats, playerSeasonAvgs,
                    request.getTeamAPlayerKeys(), teamGamesPlayedWeekX);
            TeamStatCategory teamA_Scenario2_Stats = createScenarioStats(teamA_BaselineStats, playerSeasonAvgs,
                    request.getTeamBPlayerKeys(), teamGamesPlayedWeekX);

            for (TeamStatCategory opponent : opponentsWeekX) {
                Map<String, Double> results1 = compareTeamsForCategories(teamA_Scenario1_Stats, opponent,
                        relevantCategories);
                aggregateCategoryResults(categoryTotalsBefore, results1);
                totalWinsBefore += results1.values().stream().mapToDouble(Double::doubleValue).sum();

                Map<String, Double> results2 = compareTeamsForCategories(teamA_Scenario2_Stats, opponent,
                        relevantCategories);
                aggregateCategoryResults(categoryTotalsAfter, results2);
                totalWinsAfter += results2.values().stream().mapToDouble(Double::doubleValue).sum();
            }
        }

        TradeAnalysisResultDTO result = new TradeAnalysisResultDTO();
        int totalWeeksAnalyzed = (currentWeek - startWeek);
        int totalOpponents = leagueInfo.getTeams().size() - 1;
        int totalCategories = relevantCategories.size();
        double totalMatchupsPlayed = totalWeeksAnalyzed * totalOpponents * totalCategories;

        double totalCategoryOpponents = totalWeeksAnalyzed * totalOpponents;

        Map<String, Double> unsortedChanges = new HashMap<>();
        for (StatCategory cat : relevantCategories) {
            String catName = cat.getName();
            double winsBefore = categoryTotalsBefore.getOrDefault(catName, 0.0);
            double winsAfter = categoryTotalsAfter.getOrDefault(catName, 0.0);

            if (totalCategoryOpponents > 0) {
                double pctBefore = (winsBefore / totalCategoryOpponents) * 100.0;
                double pctAfter = (winsAfter / totalCategoryOpponents) * 100.0;

                categoryTotalsBefore.put(catName, pctBefore);
                categoryTotalsAfter.put(catName, pctAfter);

                unsortedChanges.put(catName, pctAfter - pctBefore);
            } else {
                unsortedChanges.put(catName, 0.0);
            }
        }

        result.setCategoryChanges(sortByCategoryOrder(unsortedChanges));
        result.setCategoryTotalsBefore(sortByCategoryOrder(categoryTotalsBefore));
        result.setCategoryTotalsAfter(sortByCategoryOrder(categoryTotalsAfter));

        if (totalMatchupsPlayed > 0) {
            result.setWinRateBefore(totalWinsBefore / totalMatchupsPlayed);
            result.setWinRateAfter(totalWinsAfter / totalMatchupsPlayed);
        } else {
            result.setWinRateBefore(0.0);
            result.setWinRateAfter(0.0);
        }

        List<PlayerDTO> teamAPlayersList = new ArrayList<>();
        for (String key : request.getTeamAPlayerKeys()) {
            if (playerSeasonAvgs.containsKey(key)) {
                teamAPlayersList.add(playerSeasonAvgs.get(key));
            }
        }
        result.setTeamAPlayers(teamAPlayersList);

        List<PlayerDTO> teamBPlayersList = new ArrayList<>();
        for (String key : request.getTeamBPlayerKeys()) {
            if (playerSeasonAvgs.containsKey(key)) {
                teamBPlayersList.add(playerSeasonAvgs.get(key));
            }
        }
        result.setTeamBPlayers(teamBPlayersList);

        logger.info("[TRADE ANALYSIS END] Analysis complete.");
        return result;
    }

    private Map<String, Double> sortByCategoryOrder(Map<String, Double> unsortedMap) {
        List<String> desiredOrder = Arrays.asList(
                "FG%", "FT%", "3PTM", "PTS", "REB", "AST", "ST", "BLK", "TO");

        Map<String, Double> sortedMap = new LinkedHashMap<>();
        for (String key : desiredOrder) {
            if (unsortedMap.containsKey(key)) {
                sortedMap.put(key, unsortedMap.get(key));
            }
        }
        for (Map.Entry<String, Double> entry : unsortedMap.entrySet()) {
            if (!sortedMap.containsKey(entry.getKey())) {
                sortedMap.put(entry.getKey(), entry.getValue());
            }
        }
        return sortedMap;
    }

    private TeamStatCategory createTeamBaseline(TeamStatCategory teamA_RealStats,
            List<String> playersOutKeys,
            Map<String, PlayerDTO> playerSeasonAvgs,
            int teamGamesPlayed) {

        TeamStatCategory baselineStats = new TeamStatCategory(teamA_RealStats);

        for (String playerKey : playersOutKeys) {
            PlayerDTO playerAvgDTO = playerSeasonAvgs.get(playerKey);
            if (playerAvgDTO == null || playerAvgDTO.getPlayerStats() == null)
                continue;

            for (StatCategory teamStat : baselineStats.getStatCategories()) {
                if (teamStat.getId().equals("5")) { // FG%
                    updatePercentageStat(teamStat, playerAvgDTO, "9004003", TEAM_WEEKLY_FGA, teamGamesPlayed, false);
                    continue;
                }
                if (teamStat.getId().equals("8")) { // FT%
                    updatePercentageStat(teamStat, playerAvgDTO, "9007006", TEAM_WEEKLY_FTA, teamGamesPlayed, false);
                    continue;
                }

                double playerAvgStatValue = getAverageStatValueFromDTO(playerAvgDTO.getPlayerStats().getStats(),
                        teamStat.getId());
                if (playerAvgStatValue != 0) {
                    double estimatedContribution = playerAvgStatValue * teamGamesPlayed;
                    double currentTeamValue = Double.parseDouble(teamStat.getValue());
                    double newTeamStatValue = currentTeamValue - estimatedContribution;

                    // >>> GÜNCELLEME 2: Sıfır Kontrolü Kaldırıldı <<<
                    // Eksi değerlere izin veriyoruz ki "Çıkar + Ekle" işlemi sonucu değiştirmesin.
                    teamStat.setValue(String.valueOf(newTeamStatValue));
                }
            }
        }
        return baselineStats;
    }

    private TeamStatCategory createScenarioStats(TeamStatCategory baselineStats,
            Map<String, PlayerDTO> playerSeasonAvgs,
            List<String> playerKeys, int teamGamesPlayed) {
        TeamStatCategory scenarioStats = new TeamStatCategory(baselineStats);

        for (String playerKey : playerKeys) {
            PlayerDTO playerAvgDTO = playerSeasonAvgs.get(playerKey);
            if (playerAvgDTO == null || playerAvgDTO.getPlayerStats() == null)
                continue;

            for (StatCategory teamStat : scenarioStats.getStatCategories()) {
                if (teamStat.getId().equals("5")) {
                    updatePercentageStat(teamStat, playerAvgDTO, "9004003", TEAM_WEEKLY_FGA, teamGamesPlayed, true);
                    continue;
                }
                if (teamStat.getId().equals("8")) {
                    updatePercentageStat(teamStat, playerAvgDTO, "9007006", TEAM_WEEKLY_FTA, teamGamesPlayed, true);
                    continue;
                }

                double playerAvgStatValue = getAverageStatValueFromDTO(playerAvgDTO.getPlayerStats().getStats(),
                        teamStat.getId());
                if (playerAvgStatValue != 0) {
                    double normalizedStatValue = playerAvgStatValue * teamGamesPlayed;
                    double newTeamStatValue = Double.parseDouble(teamStat.getValue()) + normalizedStatValue;
                    teamStat.setValue(String.valueOf(newTeamStatValue));
                }
            }
        }
        return scenarioStats;
    }

    private void updatePercentageStat(StatCategory teamStat, PlayerDTO player, String rawStatId,
            double estimatedTeamAttempts, int gamesPlayed, boolean isAdding) {
        try {
            String rawVal = getStatValueStringFromDTO(player.getPlayerStats().getStats(), rawStatId);
            ShootingStat pStat = parseShootingStat(rawVal);

            if (pStat.attempted == 0)
                return;

            double currentTeamPct = Double.parseDouble(teamStat.getValue());
            double currentTeamMade = currentTeamPct * estimatedTeamAttempts;
            double currentTeamAttempted = estimatedTeamAttempts;

            double playerMadeTotal = pStat.made * gamesPlayed;
            double playerAttemptedTotal = pStat.attempted * gamesPlayed;

            double newTeamMade, newTeamAttempted;
            if (isAdding) {
                newTeamMade = currentTeamMade + playerMadeTotal;
                newTeamAttempted = currentTeamAttempted + playerAttemptedTotal;
            } else {
                newTeamMade = currentTeamMade - playerMadeTotal;
                newTeamAttempted = currentTeamAttempted - playerAttemptedTotal;
            }

            if (newTeamAttempted > 0) {
                double newPct = newTeamMade / newTeamAttempted;
                teamStat.setValue(String.format(Locale.US, "%.4f", newPct));
            }
        } catch (Exception e) {
            logger.warn("Error calculating percentage stats: {}", e.getMessage());
        }
    }

    private Map<String, PlayerDTO> cachePlayerSeasonStats(String leagueId, TradeAnalysisRequestDTO request) {
        Map<String, PlayerDTO> playerCache = new HashMap<>();
        List<String> allPlayerKeys = new ArrayList<>(request.getTeamAPlayerKeys());
        allPlayerKeys.addAll(request.getTeamBPlayerKeys());

        for (String playerKey : allPlayerKeys) {
            PlayerDTO stats = playerService.getPlayerStatsForSeason(leagueId, playerKey, this.CURRENT_SEASON);
            if (stats != null)
                playerCache.put(playerKey, stats);
        }
        return playerCache;
    }

    private int getTeamGamesPlayedForWeek(String teamKey, String week) {
        try {
            List<PlayerDTO> roster = rosterService.getTeamRosterForWeek(teamKey, week);
            if (roster == null || roster.isEmpty())
                return 3;
            Optional<PlayerDTO> playerWithSchedule = roster.stream()
                    .filter(p -> p.getGameSchedule() != null && !p.getGameSchedule().isEmpty()).findFirst();
            return playerWithSchedule.map(playerDTO -> playerDTO.getGameSchedule().size()).orElse(3);
        } catch (Exception e) {
            return 3;
        }
    }

    private Map<String, Double> compareTeamsForCategories(TeamStatCategory teamA, TeamStatCategory teamB,
            List<StatCategory> categories) {
        Map<String, Double> categoryResults = new HashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            StatCategory catInfo = categories.get(i);
            if (i >= teamA.getStatCategories().size() || i >= teamB.getStatCategories().size())
                continue;

            var teamA_Stat = teamA.getStatCategories().get(i);
            var teamB_Stat = teamB.getStatCategories().get(i);

            try {
                var valueA = Double.parseDouble(teamA_Stat.getValue());
                var valueB = Double.parseDouble(teamB_Stat.getValue());
                boolean isBad = catInfo.isBad();
                int comparison = Double.compare(valueA, valueB);

                if (comparison == 0)
                    categoryResults.put(catInfo.getName(), 0.5);
                else if ((comparison > 0 && !isBad) || (comparison < 0 && isBad))
                    categoryResults.put(catInfo.getName(), 1.0);
                else
                    categoryResults.put(catInfo.getName(), 0.0);
            } catch (NumberFormatException e) {
                categoryResults.put(catInfo.getName(), 0.0);
            }
        }
        return categoryResults;
    }

    private void aggregateCategoryResults(Map<String, Double> categoryTotals, Map<String, Double> weeklyResults) {
        for (Map.Entry<String, Double> entry : weeklyResults.entrySet()) {
            categoryTotals.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
    }

    private double getAverageStatValueFromDTO(List<StatWrapperDTO> stats, String statId) {
        return getStatValueFromDTO(stats, statId);
    }

    private double getStatValueFromDTO(List<StatWrapperDTO> stats, String statId) {
        String val = getStatValueStringFromDTO(stats, statId);
        if (isNumeric(val))
            return Double.parseDouble(val);
        return 0.0;
    }

    private String getStatValueStringFromDTO(List<StatWrapperDTO> stats, String statId) {
        if (stats == null)
            return "0";
        Optional<StatDTO> stat = stats.stream()
                .map(StatWrapperDTO::getStat)
                .filter(s -> s.getStatId().equals(statId))
                .findFirst();
        return stat.map(StatDTO::getValue).orElse("0");
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty() || str.equals("-"))
            return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static class ShootingStat {
        double made;
        double attempted;

        public ShootingStat(double made, double attempted) {
            this.made = made;
            this.attempted = attempted;
        }
    }

    private ShootingStat parseShootingStat(String value) {
        if (value == null || !value.contains("/"))
            return new ShootingStat(0.0, 0.0);
        try {
            String[] parts = value.split("/");
            double made = Double.parseDouble(parts[0]);
            double attempted = Double.parseDouble(parts[1]);
            return new ShootingStat(made, attempted);
        } catch (NumberFormatException e) {
            return new ShootingStat(0.0, 0.0);
        }
    }
}