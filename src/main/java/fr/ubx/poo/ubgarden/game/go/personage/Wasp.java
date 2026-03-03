package fr.ubx.poo.ubgarden.game.go.personage;

import fr.ubx.poo.ubgarden.game.Game;
import fr.ubx.poo.ubgarden.game.Position;

public class Wasp extends Insect {

    private static final int WASP_LIVES = 1; // dies after 1 sting or bomb hit
    private static final int WASP_STING_DAMAGE = 20;

    public Wasp(Game game, Position position) {
        super(game, position,
                game.configuration().waspMoveFrequency(),
                WASP_LIVES,
                WASP_STING_DAMAGE);
    }

}