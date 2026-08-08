package cn.moze.catfight.bukkit;

import java.util.UUID;
import org.bukkit.Location;

final class FightPair {
    final UUID first;
    final UUID second;
    FightPhase phase;
    long phaseStarted;
    Location firstAnchor;
    Location secondAnchor;

    FightPair(UUID first, UUID second, FightPhase phase, long phaseStarted) {
        this.first = first;
        this.second = second;
        this.phase = phase;
        this.phaseStarted = phaseStarted;
    }
}
