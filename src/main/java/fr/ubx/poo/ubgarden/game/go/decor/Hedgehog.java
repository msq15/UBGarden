package fr.ubx.poo.ubgarden.game.go.decor;

import fr.ubx.poo.ubgarden.game.Position;
import fr.ubx.poo.ubgarden.game.go.personage.Gardener;

public class Hedgehog extends Decor {
    private boolean isPickedUp = false;

    public Hedgehog(Position position) {
        super(position);
    }

    public boolean isPickedUp() {
        return isPickedUp;
    }

    public void setPickedUp(boolean pickedUp) {
        isPickedUp = pickedUp;
    }

    @Override
    public void pickUpBy(Gardener gardener) {
        gardener.pickUp(this);
    }
}
