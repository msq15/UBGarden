package fr.ubx.poo.ubgarden.game.go.decor;

import fr.ubx.poo.ubgarden.game.Position;
import fr.ubx.poo.ubgarden.game.go.personage.Gardener;

public class Door extends Decor {

    private final int targetLevel; // level this door leads to
    private final DoorType type; // type of door (next/prev)
    private boolean isOpen = false;

    public Door(Position position, int targetLevel, DoorType type) {
        super(position);
        this.targetLevel = targetLevel;
        this.type = type;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public DoorType getType() {
        return type;
    }

    public void openDoor(boolean setModified) {
        if (!this.isOpen) {
            this.isOpen = true;
            if (setModified) {
                this.setModified(true);
            }
            System.out.println("Door opened leading to level " + targetLevel);
        }
    }

    @Override
    public boolean walkableBy(Gardener gardener) {
        // gardener can only walk on open doors
        return isOpen;
    }

    @Override
    public boolean walkableByInsect(boolean allowFlowers) {
        return false;
    }

    @Override
    public void pickUpBy(Gardener gardener) {
        // if the gardener steps on an open door, switch level
        if (isOpen) {
            System.out.println("Gardener stepping on open door leading to level " + targetLevel);

            // request the level switch
            if (gardener.game != null) {
                gardener.game.requestSwitchLevel(targetLevel);
            } else {
                System.err.println("Gardener game reference is null, cannot switch level");
            }
        }
    }

    public enum DoorType {NEXT, PREVIOUS}
}
