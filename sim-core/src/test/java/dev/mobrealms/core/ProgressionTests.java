package dev.mobrealms.core;

import dev.mobrealms.core.Development.*;
import java.util.UUID;

public final class ProgressionTests {
    private static final UUID CAMP = new UUID(0, 901);

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    private static RealmSimulation settlement() {
        var state = new RealmSimulation(2, 20, 20);
        state.found(new RealmSimulation.Camp(CAMP, "mobrealms:human", new ChunkKey("minecraft:overworld", 0, 0), 8, 64, 8));
        for (int i = 0; i < 3; i++) state.addCitizen(new UUID(0, 910 + i), CAMP, .5);
        return state;
    }

    private static void infrastructureBeforeHousing() {
        var state = settlement();
        var town = state.development().town(CAMP);
        check(town.objective(3) == Building.FARM, "starter farm skipped");
        town.buildings.put(Building.FARM, 1);
        check(town.objective(3) == Building.STORE, "store hidden behind full starter housing");
        town.buildings.put(Building.STORE, 1);
        check(town.objective(3) == Building.WORKSHOP, "workshop hidden behind full starter housing");
        town.strategy = Strategy.EXPANSION;
        check(town.objective(3) == Building.HOUSE, "expansion strategy has no housing effect");
        town.strategy = Strategy.PROSPERITY;
        town.buildings.put(Building.WORKSHOP, 1);
        check(town.objective(3) == Building.HOUSE, "housing ignored after infrastructure");
        town.strategy = Strategy.SECURITY;
        town.losses = 1;
        check(town.objective(3) == Building.WATCHTOWER, "security strategy ignored losses");
    }

    private static void farmResearchUsesHarvestedSeeds() {
        var state = settlement();
        var town = state.development().town(CAMP);
        town.buildings.put(Building.FARM, 1);
        state.credit(CAMP, "minecraft:bread", 30);
        for (int i = 0; i < 10; i++) {
            state.advanceDay();
            state.development().daily(state, CAMP, .2, 1, 2);
        }
        check(town.technologies.contains(Technology.AGRICULTURE), "farm research cannot start without a workshop or stone");
        check(state.stock(CAMP).getOrDefault("minecraft:cobblestone", 0L) == 0, "farm research invented stone");
        check(state.stock(CAMP).getOrDefault("minecraft:wheat_seeds", 0L) == 32, "farm research did not consume harvested seeds");
        check(town.research == 0, "research points were not spent");
    }

    private static void idleStarvationDoesNotResearch() {
        var state = settlement();
        for (int i = 0; i < 10; i++) {
            state.advanceDay();
            state.development().daily(state, CAMP, .2, 1, 2);
        }
        check(state.development().town(CAMP).research == 0, "unfed, idle town invented knowledge");
    }

    private static void workerSkillIsBounded() {
        var person = new Person();
        check(person.workIntervalBonus() == 0, "untrained worker has skill bonus");
        person.reward(250);
        check(person.workIntervalBonus() == 2, "worker experience has no work benefit");
        person.reward(10000);
        check(person.workIntervalBonus() == 4, "work bonus exceeded rank cap");
    }

    public static void main(String[] args) {
        infrastructureBeforeHousing();
        farmResearchUsesHarvestedSeeds();
        idleStarvationDoesNotResearch();
        workerSkillIsBounded();
        System.out.println("Passed 4 progression scenarios.");
    }
}
