package com.bdsystems.builder_api.auth.security;

import java.util.UUID;

public final class TenantContext {

  private static final ThreadLocal<UUID> TENANT =
      new ThreadLocal<>();

  private TenantContext() {
  }

  public static void set(UUID tenantId) {
    TENANT.set(tenantId);
  }

  public static UUID get() {
    UUID tenantId = TENANT.get();

    if (tenantId == null) {
      throw new IllegalStateException(
          "Tenant context not initialized"
      );
    }

    return tenantId;
  }

  public static void clear() {
    TENANT.remove();
  }
}
