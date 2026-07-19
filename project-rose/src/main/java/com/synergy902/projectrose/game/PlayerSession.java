package com.synergy902.projectrose.game;

import java.util.UUID;

public final class PlayerSession {
    private final UUID playerId;
    private final long joinOrder;
    private RoseTeam team = RoseTeam.NONE;
    private int loadoutIndex = -1;
    private int kills;
    private int deaths;
    private int assists;
    private int lastRecordedDeathTick = Integer.MIN_VALUE;
    private boolean deployedThisMatch;

    public PlayerSession(UUID playerId, long joinOrder) {
        this.playerId = playerId;
        this.joinOrder = joinOrder;
    }

    public UUID playerId() {
        return playerId;
    }

    public long joinOrder() {
        return joinOrder;
    }

    public RoseTeam team() {
        return team;
    }

    public void setTeam(RoseTeam team) {
        this.team = team;
    }

    public int loadoutIndex() {
        return loadoutIndex;
    }

    public void setLoadoutIndex(int loadoutIndex) {
        this.loadoutIndex = loadoutIndex;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public int assists() {
        return assists;
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public void addAssist() {
        assists++;
    }

    public int lastRecordedDeathTick() {
        return lastRecordedDeathTick;
    }

    public void setLastRecordedDeathTick(int lastRecordedDeathTick) {
        this.lastRecordedDeathTick = lastRecordedDeathTick;
    }

    public boolean deployedThisMatch() {
        return deployedThisMatch;
    }

    public void setDeployedThisMatch(boolean deployedThisMatch) {
        this.deployedThisMatch = deployedThisMatch;
    }

    public void resetStatistics() {
        kills = 0;
        deaths = 0;
        assists = 0;
        lastRecordedDeathTick = Integer.MIN_VALUE;
        deployedThisMatch = false;
    }
}
