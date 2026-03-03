/*
 * Copyright (c) 2020. Laurent Réveillère
 */

package fr.ubx.poo.ubgarden.game.go.personage;

import fr.ubx.poo.ubgarden.game.Direction;
import fr.ubx.poo.ubgarden.game.Game;
import fr.ubx.poo.ubgarden.game.Level;
import fr.ubx.poo.ubgarden.game.Position;
import fr.ubx.poo.ubgarden.game.engine.Timer;
import fr.ubx.poo.ubgarden.game.go.GameObject;
import fr.ubx.poo.ubgarden.game.go.Movable;
import fr.ubx.poo.ubgarden.game.go.PickupVisitor;
import fr.ubx.poo.ubgarden.game.go.WalkVisitor;
import fr.ubx.poo.ubgarden.game.go.bonus.Carrots;
import fr.ubx.poo.ubgarden.game.go.bonus.EnergyBoost;
import fr.ubx.poo.ubgarden.game.go.bonus.Insecticide;
import fr.ubx.poo.ubgarden.game.go.bonus.PoisonedApple;
import fr.ubx.poo.ubgarden.game.go.decor.Decor;
import fr.ubx.poo.ubgarden.game.go.decor.Hedgehog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Gardener extends GameObject implements Movable, PickupVisitor, WalkVisitor {

    private static final long INVULNERABILITY_DURATION_MS = 1000; // 1 second
    private final List<Timer> poisoningTimers = new ArrayList<>();
    private final Timer energyRecoveryTimer;
    private final Timer invulnerabilityTimer;
    protected int diseaseLevel = 1;
    protected int insecticideCount = 0;
    private int energy;
    private Direction direction;
    private boolean moveRequested = false;
    private boolean isInvulnerable = false;

    public Gardener(Game game, Position position) {
        super(game, position);
        this.direction = Direction.DOWN;
        this.energy = game.configuration().gardenerEnergy();
        long energyRecoverDuration = game.configuration().energyRecoverDuration();
        this.energyRecoveryTimer = new Timer(energyRecoverDuration);
        this.energyRecoveryTimer.start();
        this.invulnerabilityTimer = new Timer(INVULNERABILITY_DURATION_MS);
    }


    @Override
    public void pickUp(EnergyBoost energyBoost) {
        // increase energy without exceeding maximum energy
        int maxEnergy = game.configuration().gardenerEnergy();
        int boostAmount = game.configuration().energyBoost();
        this.energy = Math.min(this.energy + boostAmount, maxEnergy);

        // cure all diseases
        this.diseaseLevel = 1;

        // clear all poisoning timers
        poisoningTimers.clear();
        // remove the bonus
        energyBoost.remove();

        System.out.println("Apple picked up");
    }


    public void pickUp(Hedgehog hedgehog) {
        hedgehog.setPickedUp(true);
        System.out.println("Hedgehog is picked up so game is won");

    }


    public void pickUp(PoisonedApple poisonedApple) {
        // increase disease level
        this.diseaseLevel++;

        // create new timer for each poisoned apple
        Timer poisoningTimer = new Timer(game.configuration().diseaseDuration());
        poisoningTimer.start();
        poisoningTimers.add(poisoningTimer);

        poisonedApple.remove();
        System.out.println("Poisoned apple picked up");
    }


    public void pickUp(Insecticide insecticide) {
        this.insecticideCount++;
        insecticide.remove();
        System.out.println("Insecticide picked up");

    }

    public void pickUp(Carrots carrots) {
        Level currentLevel = (Level) game.world().getGrid();
        currentLevel.decrementCarrotCount();
        carrots.remove();
        System.out.println("Carrots picked up");

    }


    public int getEnergy() {
        return this.energy;
    }

    public int getDiseaseLevel() {
        return this.diseaseLevel;
    }

    public int getInsecticideCount() {
        return this.insecticideCount;
    }


    public void requestMove(Direction direction) {
        if (direction != this.direction) {
            this.direction = direction;
            setModified(true);
        }
        moveRequested = true;
    }

    @Override
    public final boolean canMove(Direction direction) {
        Position nextPos = direction.nextPosition(getPosition());
        // verify if we are inside the map limits
        if (!game.world().getGrid().inside(nextPos)) {
            return false;
        }

        // verify if there is a decor that's walkable by
        Decor decor = game.world().getGrid().get(nextPos);
        if (decor != null && !decor.walkableBy(this)) {
            return false;
        }

        return true; // no obstacle
    }

    @Override
    public Position move(Direction direction) {
        Position nextPos = direction.nextPosition(getPosition());
        Decor next = game.world().getGrid().get(nextPos);
        setPosition(nextPos);
        if (next != null)
            next.pickUpBy(this);

        return nextPos;
    }

    public void update(long now) {
        if (isInvulnerable) {
            invulnerabilityTimer.update(now);
            if (!invulnerabilityTimer.isRunning()) {
                isInvulnerable = false;
                setModified(true);
            }
        }

        boolean hasMoved = false;
        if (moveRequested) {
            if (canMove(direction)) {
                hasMoved = true;
                Decor targetDecor = game.world().getGrid().get(direction.nextPosition(getPosition()));
                int moveCost = (targetDecor != null ? targetDecor.energyConsumptionWalk() : 1) * this.diseaseLevel;
                this.energy -= moveCost;

                move(direction);
            }
            moveRequested = false;
        }
        // if gardener moved, reset the timer
        if (hasMoved) {
            energyRecoveryTimer.restart();
        }
        // update energy recovery timer
        energyRecoveryTimer.update(now);
        if (!energyRecoveryTimer.isRunning()) {
            // gardener has been immobile for energyRecoverDuration time
            recoverEnergy();
            energyRecoveryTimer.start();
        }

        // update all poisoned apple timers
        updatePoisoningTimers(now);
    }

    public void hurt(int damage) {
        if (!isInvulnerable && damage > 0) {
            this.energy -= damage;

            // become invulnerable
            isInvulnerable = true;
            invulnerabilityTimer.start(); // start the invulnerability timer

            setModified(true);

        }
    }

    public void hurt() {
        hurt(1);
    }

    public Direction getDirection() {
        return direction;
    }

    private void recoverEnergy() {
        int maxEnergy = game.configuration().gardenerEnergy();
        if (energy < maxEnergy)
            energy++;
    }

    private void updatePoisoningTimers(long now) {
        // reset disease level to 1
        this.diseaseLevel = 1;

        // update each timer and count how many are still active
        Iterator<Timer> iterator = poisoningTimers.iterator();
        while (iterator.hasNext()) {
            Timer timer = iterator.next();
            timer.update(now);

            if (timer.isRunning()) {
                // this timer is still active so the poisoned apple still has an effect
                this.diseaseLevel++;
            } else {
                // this timer has expired so remove from the list
                iterator.remove();
            }
        }
        // the final diseaseLevel is now 1 + the number of active poisoned apples
    }

    // consume one insecticide bomb from inventory
    public void useBomb() {
        if (this.insecticideCount > 0) {
            this.insecticideCount--;
            System.out.println("Gardener used an insecticide bomb");
        }
    }
}