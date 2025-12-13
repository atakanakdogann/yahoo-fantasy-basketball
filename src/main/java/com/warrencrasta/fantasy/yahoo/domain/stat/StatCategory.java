package com.fantasytoys.fantasy.yahoo.domain.stat;

import lombok.Data;
import lombok.NoArgsConstructor; // >> 1. BU IMPORT'U EKLEYİN

@Data
@NoArgsConstructor // >> 2. BU ANOTASYONU EKLEYİN
public class StatCategory {

  private String id;
  private String name;
  private String value;
  private boolean isBad;

  public StatCategory(StatCategory original) {
    this.id = original.id;
    this.name = original.name;
    this.value = original.value;
    this.isBad = original.isBad;
  }
}