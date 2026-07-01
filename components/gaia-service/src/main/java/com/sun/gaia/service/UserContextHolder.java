package com.sun.gaia.service;

import java.util.UUID;

public class UserContextHolder {

  private static final ThreadLocal<UUID> CONTEXT = new ThreadLocal<>();

  public static void setUserId(UUID id) {
    CONTEXT.set(id);
  }

  public static UUID getUserId() {
    return CONTEXT.get();
  }

  public static void clear() {
    CONTEXT.remove();
  }
}
