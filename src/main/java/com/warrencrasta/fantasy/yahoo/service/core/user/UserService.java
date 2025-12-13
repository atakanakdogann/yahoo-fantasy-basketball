package com.fantasytoys.fantasy.yahoo.service.core.user;

import com.fantasytoys.fantasy.yahoo.domain.league.League;
import com.fantasytoys.fantasy.yahoo.domain.season.YahooSeason;
import java.util.List;

public interface UserService {

  List<? extends YahooSeason> getSeasonsForUser();

  List<? extends League> getLeaguesForUser(String seasonId);
}
