package fr.ubx.poo.ubgarden.game.go;

import fr.ubx.poo.ubgarden.game.go.bonus.Carrots;
import fr.ubx.poo.ubgarden.game.go.bonus.EnergyBoost;
import fr.ubx.poo.ubgarden.game.go.bonus.Insecticide;
import fr.ubx.poo.ubgarden.game.go.bonus.PoisonedApple;
import fr.ubx.poo.ubgarden.game.go.decor.Hedgehog;

public interface PickupVisitor {
    /**
     * Called when visiting and picking up an {@link EnergyBoost}.
     *
     * @param energyBoost the energy boost to be picked up
     */
    default void pickUp(EnergyBoost energyBoost) {
    }

    /**
     * Called when visiting and picking up an {@link Insecticide}.
     *
     * @param insecticide the insecticide to be picked up
     */
    default void pickUp(Insecticide insecticide) {
    }

    /**
     * Called when visiting and picking up a {@link PoisonedApple}.
     *
     * @param poisonedApple the poisoned apple to be picked up
     */
    default void pickUp(PoisonedApple poisonedApple) {
    }

    /**
     * Called when visiting and picking up a {@link Carrots}.
     *
     * @param carrots the carrot to be picked up
     */
    default void pickUp(Carrots carrots) {
    }

    /**
     * Called when visiting and picking up a {@link Hedgehog}.
     *
     * @param hedgehog the hedgehog to be picked up
     */
    default void pickUp(Hedgehog hedgehog) {
    }
}
