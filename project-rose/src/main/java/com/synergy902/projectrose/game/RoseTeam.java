package com.synergy902.projectrose.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum RoseTeam {
    NONE("none", ChatFormatting.GRAY),
    RED("red", ChatFormatting.RED),
    BLUE("blue", ChatFormatting.BLUE);

    private final String serializedName;
    private final ChatFormatting color;

    RoseTeam(String serializedName, ChatFormatting color) {
        this.serializedName = serializedName;
        this.color = color;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean isPlayable() {
        return this == RED || this == BLUE;
    }

    public Component displayName() {
        return Component.literal(serializedName.toUpperCase(Locale.ROOT)).withStyle(color);
    }

    public RoseTeam opposite() {
        return this == RED ? BLUE : this == BLUE ? RED : NONE;
    }

    public static RoseTeam parse(String value) {
        for (RoseTeam team : values()) {
            if (team.serializedName.equalsIgnoreCase(value)) {
                return team;
            }
        }
        return NONE;
    }
}

