package fr.ubx.poo.ubgarden.game.go.personage;

import fr.ubx.poo.ubgarden.game.Direction;
import fr.ubx.poo.ubgarden.game.Game;
import fr.ubx.poo.ubgarden.game.Map;
import fr.ubx.poo.ubgarden.game.Position;
import fr.ubx.poo.ubgarden.game.engine.Timer;
import fr.ubx.poo.ubgarden.game.go.GameObject;
import fr.ubx.poo.ubgarden.game.go.Movable;
import fr.ubx.poo.ubgarden.game.go.bonus.Insecticide;
import fr.ubx.poo.ubgarden.game.go.decor.Decor;
import fr.ubx.poo.ubgarden.game.go.decor.Door;
import fr.ubx.poo.ubgarden.game.go.decor.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class Insect extends GameObject implements Movable {

    public static final Random random = new Random();
    private static final long STING_COOLDOWN_MS = 1000;
    protected final Timer moveTimer;
    protected final int stingDamage;
    private final Timer stingCooldownTimer;
    protected Direction direction;
    protected int lives;
    private boolean canSting = true;

    public Insect(Game game, Position position, int moveFrequency, int initialLives, int stingDamage) {
        super(game, position);
        long moveDelay = 1000;
        if (moveFrequency > 0) moveDelay = 1000 / moveFrequency;
        this.moveTimer = new Timer(moveDelay);
        this.lives = initialLives;
        this.stingDamage = stingDamage;
        this.direction = Direction.random();
        this.moveTimer.start();

        this.stingCooldownTimer = new Timer(STING_COOLDOWN_MS);
    }

    public Direction getDirection() {
        return direction;
    }

    public int getStingDamage() {
        return stingDamage;
    }

    public int getCurrentLives() {
        return lives;
    }

    public boolean canCurrentlySting() {
        return canSting;
    }

    @Override
    public void update(long now) {
        if (isDeleted()) return;
        if (!canSting) {
            stingCooldownTimer.update(now);
            if (!stingCooldownTimer.isRunning()) {
                canSting = true; // cooldown finished, can sting again
            }
        }

        // handle movement based on timer
        moveTimer.update(now);
        if (!moveTimer.isRunning()) {
            tryToMoveRandomly();
            moveTimer.start();
        }
    }

    protected void tryToMoveRandomly() {
        List<Direction> possibleDirections = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(possibleDirections);
        for (Direction potentialDir : possibleDirections) {
            if (canMove(potentialDir)) {
                move(potentialDir);
                return;
            }
        }
    }

    @Override
    public boolean canMove(Direction direction) {
        Position nextPos = direction.nextPosition(getPosition());
        Map map = game.world().getGrid();
        if (!map.inside(nextPos)) return false;
        Decor decor = map.get(nextPos);
        return !(decor instanceof Tree) && !(decor instanceof Door);
    }

    @Override
    public Position move(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            setModified(true);
        }
        Position nextPos = direction.nextPosition(getPosition());
        Map map = game.world().getGrid();
        Decor targetDecor = map.get(nextPos);
        if (targetDecor != null && targetDecor.getBonus() instanceof Insecticide bonus) {
            this.takeBombHit();
            bonus.remove();
            if (this.isDeleted()) {
                setPosition(nextPos);
                return nextPos;
            }
        }
        setPosition(nextPos);
        return nextPos;
    }

    // called after a collision between Gardener and Insect
    public void sting() {
        if (!canSting) {
            return; // do nothing if on cooldown
        }

        this.lives--;
        this.canSting = false; // go on cooldown
        this.stingCooldownTimer.start(); // start the cooldown timer


        if (this.lives <= 0) {
            this.remove();
        }
    }

    // called when the insect is hit by a bomb
    public void takeBombHit() {
        this.lives--;
        if (this.lives <= 0) {
            this.remove();
        } else {
            setModified(true);
        }
    }
}