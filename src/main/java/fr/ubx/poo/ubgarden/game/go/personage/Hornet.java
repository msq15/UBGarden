package fr.ubx.poo.ubgarden.game.go.personage;

import fr.ubx.poo.ubgarden.game.Game;
import fr.ubx.poo.ubgarden.game.Position;

public class Hornet extends Insect {

    private static final int HORNET_LIVES = 2; // dies after 2 stings or/and bomb hits
    private static final int HORNET_STING_DAMAGE = 30;

    public Hornet(Game game, Position position) {
        super(game, position,
                game.configuration().hornetMoveFrequency(),
                HORNET_LIVES,
                HORNET_STING_DAMAGE);
    }
}