package com.sun.medusa.repository;

public interface GameRecordProjection {
  long getWins();

  long getLosses();

  long getDraws();
}
