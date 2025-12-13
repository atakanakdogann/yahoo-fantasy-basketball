package com.fantasytoys.fantasy.yahoo.service.core.player;

import java.util.ArrayList;
import java.util.List;

import com.fantasytoys.fantasy.yahoo.dto.external.yahoo.PlayerDTO;
import com.fantasytoys.fantasy.yahoo.service.client.YahooClient;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class YahooPlayerServiceImpl implements PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(YahooPlayerServiceImpl.class);
    private final YahooClient yahooClient;

    public YahooPlayerServiceImpl(YahooClient yahooClient) {
        this.yahooClient = yahooClient;
    }

    // Sezon istatistikleri global olduğu için eski yöntem çalışır,
    // ama tutarlılık adına bunu da lig üzerinden çekmek daha güvenlidir.
    // Şimdilik çalışan koda dokunmayalım veya lig bağlamı ekleyelim.
    @Override
    public PlayerDTO getPlayerStatsForSeason(String leagueId, String playerKey, String season) {
        logger.info("[LOG] Fetching SEASON AVERAGES for playerKey: {}", playerKey);

        Map<String, String> uriVariables = Map.of(
                "league_key", leagueId,
                "player_key", playerKey,
                "season", season);

        // >> DEĞİŞİKLİK BURADA: 'type=season' yerine 'type=average_season' yaptık.
        // Bu sayede Yahoo bize direkt 25.5 sayı, 10.2 ribaund gibi ortalamaları
        // verecek.
        var resourceUriFragment = "/league/{league_key}/players;player_keys={player_key}/stats;type=average_season;season={season}";

        try {
            return yahooClient.getFantasyContent(uriVariables, resourceUriFragment)
                    .getLeague().getPlayers().getPlayers().get(0).getPlayer();
        } catch (Exception e) {
            logger.warn("Sezon ortalaması alınamadı: {}", playerKey);
            return null;
        }
    }

    @Override
    public PlayerDTO getPlayerStatsForWeek(String leagueId, String playerKey, String week) {
        logger.info("[LOG] Fetching WEEK stats for playerKey: {} for week: {}", playerKey, week);

        // KRİTİK DÜZELTME: URL artık /league/... ile başlıyor
        Map<String, String> uriVariables = Map.of(
                "league_key", leagueId,
                "player_key", playerKey,
                "week", week);
        var resourceUriFragment = "/league/{league_key}/players;player_keys={player_key}/stats;type=week;week={week}";

        try {
            // JSON Yapısı Değiştiği için Okuma Yöntemi de Değişti:
            // Eski: response.getPlayerWrapper().getPlayer()
            // Yeni: response.getLeague().getPlayers().getPlayers().get(0).getPlayer()

            return yahooClient.getFantasyContent(uriVariables, resourceUriFragment)
                    .getLeague() // Önce Lig kutusuna gir
                    .getPlayers() // Sonra Oyuncular listesini al
                    .getPlayers() // Listenin içindeki
                    .get(0) // İlk elemanı (zaten tek kişi istedik) al
                    .getPlayer(); // Ve onun verisini döndür

        } catch (Exception e) {
            logger.error("Haftalık istatistik hatası (Player: {}, Week: {}): {}", playerKey, week, e.getMessage());
            return null;
        }
    }

    @Override
    public PlayerDTO getPlayerStatsAverageLastMonth(String leagueId, String playerKey) {
        logger.info("[LOG] Fetching LAST MONTH AVERAGES for playerKey: {}", playerKey);

        Map<String, String> uriVariables = Map.of(
                "league_key", leagueId,
                "player_key", playerKey);
        // Yahoo API: stats;type=average_lastmonth gives averages over the last month
        // (usually
        // 30 days)
        var resourceUriFragment = "/league/{league_key}/players;player_keys={player_key}/stats;type=average_lastmonth";

        try {
            return yahooClient.getFantasyContent(uriVariables, resourceUriFragment)
                    .getLeague().getPlayers().getPlayers().get(0).getPlayer();
        } catch (Exception e) {
            logger.warn("Last month average could not be fetched: {}", playerKey);
            return null;
        }
    }

    @Override
    public List<PlayerDTO> searchFreeAgents(String leagueId, String query) {
        String resourceUriFragment;
        Map<String, String> uriVariables;

        // EĞER ARAMA KUTUSU BOŞSA -> En iyi boşta oyuncuları getir (Sıralama: Rank)
        if (query == null || query.trim().isEmpty()) {
            uriVariables = Map.of("league_key", leagueId);
            // sort=OR (Overall Rank) ekledik ki en iyiler gelsin
            resourceUriFragment = "/league/{league_key}/players;status=FA;sort=OR/stats;type=season;season=2025";
        }
        // EĞER ARAMA VARSA (Örn: "Hayes") -> İsim araması yap
        else {
            uriVariables = Map.of(
                    "league_key", leagueId,
                    "search_query", query // "query" yerine açık isim verdik karışmasın
            );
            // Yahoo'da arama formatı: players;status=FA;search={isim}
            resourceUriFragment = "/league/{league_key}/players;status=FA;search={search_query}/stats;type=season;season=2025";
        }

        try {
            logger.info("Free Agent Search URL Pattern: {}", resourceUriFragment);

            return yahooClient.getFantasyContent(uriVariables, resourceUriFragment)
                    .getLeague().getPlayers().getPlayers()
                    .stream()
                    .map(wrapper -> wrapper.getPlayer())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Free Agent search failed for query: {}", query, e);
            return new ArrayList<>();
        }
    }
}