package com.creditflow.common;

/**
 * Who is performing an action. Resolved from the {@code X-Actor} / {@code X-Role}
 * request headers (demo-grade — a stand-in for real authentication).
 */
public record Actor(String name, ActorRole role) {

    public static Actor of(String name, String role) {
        String resolvedName = (name == null || name.isBlank()) ? "anonymous" : name;
        ActorRole resolvedRole;
        try {
            resolvedRole = role == null || role.isBlank() ? ActorRole.ANALYST
                    : ActorRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            resolvedRole = ActorRole.ANALYST;
        }
        return new Actor(resolvedName, resolvedRole);
    }

    public boolean canDecide() {
        return role == ActorRole.REVIEWER || role == ActorRole.ADMIN;
    }
}
