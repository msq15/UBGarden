package fr.ubx.poo.ubgarden.game;

import fr.ubx.poo.ubgarden.game.go.decor.Hedgehog;
import fr.ubx.poo.ubgarden.game.go.personage.Gardener;

public class Game {

    private final Configuration configuration;
    private final World world;
    private final Gardener gardener;
    private Hedgehog hedgehog;
    private boolean switchLevelRequested = false;
    private int switchLevelTarget = 0;

    public Game(World world, Configuration configuration, Position gardenerPosition) {
        this.configuration = configuration;
        this.world = world;
        gardener = new Gardener(this, gardenerPosition);
    }

    public Configuration configuration() {
        return configuration;
    }

    public Gardener getGardener() {
        return this.gardener;
    }

    public Hedgehog getHedgehog() {
        return this.hedgehog;
    }

    public void setHedgehog(Hedgehog hedgehog) {
        this.hedgehog = hedgehog;
    }

    public World world() {
        return world;
    }

    public boolean isSwitchLevelRequested() {
        return switchLevelRequested;
    }

    public void requestSwitchLevel(int level) {
        this.switchLevelTarget = level;
        switchLevelRequested = true;
    }

    public int getSwitchLevelTarget() {
        return switchLevelTarget;
    }

    public void clearSwitchLevel() {
        switchLevelRequested = false;
        this.switchLevelTarget = 0;
    }
}