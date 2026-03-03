package fr.ubx.poo.ubgarden.game;

import fr.ubx.poo.ubgarden.game.go.GameObject;
import fr.ubx.poo.ubgarden.game.go.bonus.*;
import fr.ubx.poo.ubgarden.game.go.decor.*;
import fr.ubx.poo.ubgarden.game.go.decor.ground.Grass;
import fr.ubx.poo.ubgarden.game.go.decor.ground.Land;
import fr.ubx.poo.ubgarden.game.go.personage.Insect;
import fr.ubx.poo.ubgarden.game.launcher.MapEntity;
import fr.ubx.poo.ubgarden.game.launcher.MapLevel;

import java.util.*;

public class Level implements Map {

    private final int level;
    private final int width;
    private final int height;

    private final java.util.Map<Position, Decor> decors = new HashMap<>();
    private final List<Door> doorsInLevel = new ArrayList<>();
    private final List<Insect> insects = new LinkedList<>();
    private int totalCarrots = 0;
    private int remainingCarrots = 0;

    public Level(Game game, int level, MapLevel entities) {
        this.level = level;
        this.width = entities.width();
        this.height = entities.height();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Position position = new Position(level, i, j);
                MapEntity mapEntity = entities.get(i, j);
                Decor currentDecor = null;
                switch (mapEntity) {
                    // decor
                    case Grass:
                        currentDecor = new Grass(position);
                        break;
                    case Tree:
                        currentDecor = new Tree(position);
                        break;
                    case Land:
                        currentDecor = new Land(position);
                        break;
                    case Flowers:
                        currentDecor = new Flowers(position);
                        break;
                    case NestWasp:
                        currentDecor = new NestWasp(position);
                        break;
                    case NestHornet:
                        currentDecor = new NestHornet(position);
                        break;
                    case DoorNextClosed:
                        Door doorNext = new Door(position, level + 1, Door.DoorType.NEXT);
                        doorsInLevel.add(doorNext); // Keep track of the door
                        currentDecor = doorNext;
                        break;
                    case DoorNextOpened:
                        Door doorNextOpen = new Door(position, level + 1, Door.DoorType.NEXT);
                        doorNextOpen.openDoor(false); // Open without check
                        doorsInLevel.add(doorNextOpen); // Keep track of the door
                        currentDecor = doorNextOpen;
                        break;
                    case DoorPrevOpened:
                        Door doorPrev = new Door(position, level - 1, Door.DoorType.PREVIOUS);
                        doorPrev.openDoor(false); // Open without check
                        doorsInLevel.add(doorPrev); // Keep track of the door
                        currentDecor = doorPrev;
                        break;
                    case Hedgehog:
                        Hedgehog hedgehog = new Hedgehog(position);
                        game.setHedgehog(hedgehog); // Set hedgehog reference in Game
                        currentDecor = hedgehog;
                        break;

                    // bonus
                    case Apple:
                        Decor grassApple = new Grass(position);
                        grassApple.setBonus(new EnergyBoost(position, grassApple));
                        currentDecor = grassApple;
                        break;
                    case PoisonedApple:
                        Decor grassPoison = new Grass(position);
                        grassPoison.setBonus(new PoisonedApple(position, grassPoison));
                        currentDecor = grassPoison;
                        break;
                    case Carrots:
                        Decor landCarrot = new Land(position);
                        Carrots carrotBonus = new Carrots(position, landCarrot);
                        landCarrot.setBonus(carrotBonus);
                        currentDecor = landCarrot;
                        break;
                    case Insecticide:
                        Decor grassInsecticide = new Grass(position);
                        grassInsecticide.setBonus(new Insecticide(position, grassInsecticide));
                        currentDecor = grassInsecticide;
                        break;
                    default:
                        if (mapEntity == MapEntity.Gardener) {
                            currentDecor = new Grass(position);
                        } else {
                            throw new RuntimeException("EntityCode " + mapEntity.name() + " not processed");
                        }
                }
                decors.put(position, currentDecor);
            }
        }

        // count total carrots in a levek
        for (Decor decor : decors.values()) {
            Bonus bonus = decor.getBonus();
            if (bonus instanceof Carrots) {
                this.totalCarrots++;
            }
        }
        this.remainingCarrots = this.totalCarrots;
        System.out.println("Level " + level + " initialized with " + this.totalCarrots + " carrots.");

        // open doors immediately if there are no carrots
        if (this.remainingCarrots == 0) {
            System.out.println("Level " + level + " has no carrots, opening doors immediately.");
            openAllDoors();
        }
    }

    public void decrementCarrotCount() {
        if (remainingCarrots > 0) {
            remainingCarrots--;
            System.out.println("Carrot collected in level " + level + ". Remaining: " + remainingCarrots);
            if (remainingCarrots == 0) {
                System.out.println("All carrots collected in level " + level + "! Opening doors.");
                openAllDoors();
            }
        }
    }

    private void openAllDoors() {
        for (Door door : doorsInLevel) {
            if (door.getType() == Door.DoorType.NEXT && !door.isOpen()) {
                door.openDoor(true);
            }
        }
    }

    public int getRemainingCarrots() {
        return remainingCarrots;
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    public Decor get(Position position) {
        return decors.get(position);
    }

    public Collection<Decor> values() {
        return decors.values();
    }


    @Override
    public boolean inside(Position position) {
        // check if position is inside the map limits
        return position.x() >= 0 && position.x() < width
                && position.y() >= 0 && position.y() < height;
    }

    public List<Insect> getInsects() {
        insects.removeIf(GameObject::isDeleted);
        return insects;
    }

    public void setInsects(List<Insect> insects) {
        this.insects.clear();
        this.insects.addAll(insects);
    }
}
