package com.warrencrasta.fantasy.yahoo.dto.external.yahoo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FantasyContentDTO {

  private List<UserWrapperDTO> users;
  private LeagueDTO league;
  private TeamDTO team;

  // >> YENİ EKLENEN ALAN 1 (Tekil Oyuncu için - /player/...)
  @JsonAlias("player")
  private PlayerWrapperDTO playerWrapper;

  // >> YENİ EKLENEN ALAN 2 (Oyuncu Listesi için - /league/.../players)
  @JsonAlias("players")
  private PlayersDTO players;
}
