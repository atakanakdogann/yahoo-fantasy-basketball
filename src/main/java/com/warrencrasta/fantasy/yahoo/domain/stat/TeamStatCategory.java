package com.fantasytoys.fantasy.yahoo.domain.stat;

import com.fantasytoys.fantasy.yahoo.domain.team.Team;
import com.fantasytoys.fantasy.yahoo.domain.team.YahooTeam;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TeamStatCategory extends YahooTeam {

  private List<StatCategory> statCategories;

  public TeamStatCategory(TeamStatCategory original) {
    // Miras alınan alanları manuel set et
    this.setId(original.getId());
    this.setName(original.getName());
    this.setTeamKey(original.getTeamKey());
    this.setLogoUrl(original.getLogoUrl());

    // Listeyi kopyala
    if (original.getStatCategories() != null) {
      this.statCategories = original.getStatCategories().stream()
          .map(StatCategory::new)
          .collect(Collectors.toList());
    }
  }
}