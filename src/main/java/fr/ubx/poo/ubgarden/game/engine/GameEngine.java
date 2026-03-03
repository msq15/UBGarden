/*
 * Copyright (c) 2020. Laurent Réveillère
 */

package fr.ubx.poo.ubgarden.game.engine;

import fr.ubx.poo.ubgarden.game.Map;
import fr.ubx.poo.ubgarden.game.*;
import fr.ubx.poo.ubgarden.game.go.GameObject;
import fr.ubx.poo.ubgarden.game.go.bonus.Bonus;
import fr.ubx.poo.ubgarden.game.go.bonus.Insecticide;
import fr.ubx.poo.ubgarden.game.go.decor.*;
import fr.ubx.poo.ubgarden.game.go.decor.ground.Grass;
import fr.ubx.poo.ubgarden.game.go.decor.ground.Land;
import fr.ubx.poo.ubgarden.game.go.personage.Gardener;
import fr.ubx.poo.ubgarden.game.go.personage.Hornet;
import fr.ubx.poo.ubgarden.game.go.personage.Insect;
import fr.ubx.poo.ubgarden.game.go.personage.Wasp;
import fr.ubx.poo.ubgarden.game.view.ImageResource;
import fr.ubx.poo.ubgarden.game.view.Sprite;
import fr.ubx.poo.ubgarden.game.view.SpriteFactory;
import fr.ubx.poo.ubgarden.game.view.SpriteGardener;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.*;

import static fr.ubx.poo.ubgarden.game.go.personage.Insect.random;


public final class GameEngine {

    private static final long WASP_SPAWN_INTERVAL = 5000; // 5 seconds
    private static final long HORNET_SPAWN_INTERVAL = 10000; // 10 seconds
    private static AnimationTimer gameLoop;
    private final Game game;
    private final Gardener gardener;
    private final List<Sprite> sprites = new LinkedList<>();
    private final Set<Sprite> cleanUpSprites = new HashSet<>();
    private final Scene scene;
    private final Pane rootPane = new Pane();
    private final Group root = new Group();
    private final Pane layer = new Pane();
    private final List<Insect> activeInsects = new LinkedList<>();
    private final Timer waspSpawnTimer;
    private final Timer hornetSpawnTimer;
    private StatusBar statusBar;
    private Input input;

    public GameEngine(Game game, Scene scene) {
        this.game = game;
        this.scene = scene;
        this.gardener = game.getGardener();
        this.waspSpawnTimer = new Timer(WASP_SPAWN_INTERVAL);
        this.hornetSpawnTimer = new Timer(HORNET_SPAWN_INTERVAL);
        initialize();
        buildAndSetGameLoop();
    }

    public Pane getRoot() {
        return rootPane;
    }

    private void initialize() {
        Map currentMap = game.world().getGrid();
        int height = currentMap.height();
        int width = currentMap.width();
        int sceneWidth = width * ImageResource.size;
        int sceneHeight = height * ImageResource.size;
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/application.css")).toExternalForm());
        input = new Input(scene);

        root.getChildren().clear();
        root.getChildren().add(layer);
        statusBar = new StatusBar(root, sceneWidth, sceneHeight);

        rootPane.getChildren().clear();
        rootPane.setPrefSize(sceneWidth, sceneHeight + StatusBar.height);
        rootPane.getChildren().add(root);

        for (var decor : currentMap.values()) {
            sprites.add(SpriteFactory.create(layer, decor));
            decor.setModified(true);
            var bonus = decor.getBonus();
            if (bonus != null) {
                sprites.add(SpriteFactory.create(layer, bonus));
                bonus.setModified(true);
            }
        }
        gardener.setModified(true);
        sprites.add(new SpriteGardener(layer, gardener));

        resizeScene(sceneWidth, sceneHeight);
    }

    void buildAndSetGameLoop() {
        gameLoop = new AnimationTimer() {
            public void handle(long now) {
                checkLevel();
                if (gameLoop == null) return;
                processInput();
                update(now);
                checkCollision();
                cleanupSprites();
                render();
                statusBar.update(game);
            }
        };
    }

    private Position findMatchingDoor(Map map, Door.DoorType doorType) {
        for (Decor decor : map.values()) {
            if (decor instanceof Door entryDoor && entryDoor.getType() == doorType) {
                return entryDoor.getPosition();
            }
        }
        return null;
    }

    private void performLevelSwitch(int targetLevel, Map nextMap, Position spawnPosition) {
        int currentLevelIndex = game.world().currentLevel();
        Map currentMapObject = game.world().getGrid(currentLevelIndex);
        if (currentMapObject instanceof Level currentLevelCasted) {
            currentLevelCasted.setInsects(new LinkedList<>(this.activeInsects));
        }

        sprites.clear();
        cleanUpSprites.clear();
        activeInsects.clear();

        if (waspSpawnTimer != null) waspSpawnTimer.start();
        if (hornetSpawnTimer != null) hornetSpawnTimer.start();

        game.world().setCurrentLevel(targetLevel);
        gardener.setPosition(spawnPosition);

        int sceneWidth = nextMap.width() * ImageResource.size;
        int sceneHeight = nextMap.height() * ImageResource.size;

        rootPane.getChildren().clear();
        root.getChildren().clear();
        layer.getChildren().clear();
        layer.setPrefSize(sceneWidth, sceneHeight);

        try {
            for (Decor decor : nextMap.values()) {
                sprites.add(SpriteFactory.create(layer, decor));
                decor.setModified(true);
                Bonus bonus = decor.getBonus();
                if (bonus != null) {
                    sprites.add(SpriteFactory.create(layer, bonus));
                    bonus.setModified(true);
                }
            }

            if (nextMap instanceof Level nextLevelCasted) {
                this.activeInsects.addAll(nextLevelCasted.getInsects());
                for (Insect insect : this.activeInsects) {
                    sprites.add(SpriteFactory.create(layer, insect));
                    insect.setModified(true);
                }
            }

            gardener.setModified(true);
            sprites.add(new SpriteGardener(layer, gardener));

        } catch (Exception e) {
            System.err.println("FATAL ERROR during sprite creation for new level:");
            e.printStackTrace();
            handleLevelError("Error loading level visuals: " + e.getMessage(), Color.RED);
            return;
        }

        root.getChildren().add(layer);

        if (statusBar != null) {
            Node sbNode = statusBar.getNode();
            if (sbNode != null) {
                if (sbNode instanceof Region) {
                    ((Region) sbNode).setPrefWidth(sceneWidth);
                }
                sbNode.relocate(0, sceneHeight);
                root.getChildren().add(sbNode);
            }
        }

        rootPane.getChildren().add(root);
        rootPane.setPrefSize(sceneWidth, sceneHeight + StatusBar.height);
        game.clearSwitchLevel();

        Platform.runLater(() -> {
            rootPane.requestFocus();
            if (scene != null && scene.getWindow() != null) {
                scene.getWindow().sizeToScene();
            }
        });

        System.out.println("Level switch complete to level " + targetLevel);
    }


    private void handleLevelError(String errorMessage, Color messageColor) {
        System.err.println(errorMessage);
        game.clearSwitchLevel();
        showMessage(errorMessage, messageColor);
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
    }

    private void checkLevel() {
        if (!game.isSwitchLevelRequested()) {
            return;
        }
        int targetLevel = game.getSwitchLevelTarget();
        int currentLevel = game.world().currentLevel(); // get current level BEFORE switch

        try {
            Map nextMap = game.world().getGrid(targetLevel);
            if (nextMap == null) {
                handleLevelError("Error: Level " + targetLevel + " doesn't exist", Color.ORANGE);
                return;
            }

            boolean isMovingForward = targetLevel > currentLevel;
            Door.DoorType requiredDoorTypeInTargetLevel = isMovingForward ? Door.DoorType.PREVIOUS : Door.DoorType.NEXT;
            Position foundSpawnPosition = findMatchingDoor(nextMap, requiredDoorTypeInTargetLevel);

            if (foundSpawnPosition == null) {
                handleLevelError("CRITICAL ERROR: No corresponding door of type " + requiredDoorTypeInTargetLevel +
                        " found in Level " + targetLevel, Color.RED);
                return;
            }
            if (!nextMap.inside(foundSpawnPosition)) {
                handleLevelError("ERROR: Found spawn position " + foundSpawnPosition +
                        " is outside the bounds of Level " + targetLevel, Color.RED);
                return;
            }

            performLevelSwitch(targetLevel, nextMap, foundSpawnPosition);

        } catch (Exception e) {
            System.err.println("Unexpected error during level transition:");
            e.printStackTrace();
            handleLevelError("Critical error during level transition: " + e.getMessage(), Color.RED);
        }
    }

    private void checkCollision() {
        Position gardenerPos = gardener.getPosition();
        Iterator<Insect> insectIterator = activeInsects.iterator();

        while (insectIterator.hasNext()) {
            Insect insect = insectIterator.next();

            if (insect.isDeleted()) {
                insectIterator.remove();
                continue;
            }

            Position insectPos = insect.getPosition();

            if (insectPos.equals(gardenerPos)) {
                if (gardener.getInsecticideCount() > 0) {
                    gardener.useBomb();
                    insect.takeBombHit();
                } else {
                    int damage = insect.getStingDamage();
                    gardener.hurt(damage);
                    insect.sting();
                }
                if (insect.isDeleted()) {
                    insectIterator.remove();
                }
            }

        }
    }

    private void processInput() {
        if (input == null) return;
        if (input.isExit()) {
            stopAndClean();
            Platform.exit();
            System.exit(0);
        } else if (input.isMoveDown()) {
            gardener.requestMove(Direction.DOWN);
        } else if (input.isMoveLeft()) {
            gardener.requestMove(Direction.LEFT);
        } else if (input.isMoveRight()) {
            gardener.requestMove(Direction.RIGHT);
        } else if (input.isMoveUp()) {
            gardener.requestMove(Direction.UP);
        }
        input.clear();
    }

    private void showMessage(String msg, Color color) {
        Platform.runLater(() -> {
            if (gameLoop != null) {
                gameLoop.stop();
                gameLoop = null;
            }

            Text message = new Text(msg);
            message.setTextAlignment(TextAlignment.CENTER);
            message.setFont(new Font(60));
            message.setFill(color);
            StackPane pane = new StackPane(message);
            pane.setPrefSize(rootPane.getWidth(), rootPane.getHeight());

            rootPane.getChildren().clear();
            rootPane.getChildren().add(pane);

            AnimationTimer messageLoop = new AnimationTimer() {
                final Input messageInput = (input != null) ? input : new Input(scene);

                public void handle(long now) {
                    if (messageInput.isExit()) {
                        this.stop();
                        Platform.exit();
                        System.exit(0);
                    }
                    messageInput.clear();
                }
            };
            messageLoop.start();
        });
    }


    private void update(long now) {
        waspSpawnTimer.update(now);
        if (!waspSpawnTimer.isRunning()) {
            spawnInsect(NestWasp.class);
            waspSpawnTimer.start();
        }

        hornetSpawnTimer.update(now);
        if (!hornetSpawnTimer.isRunning()) {
            spawnInsect(NestHornet.class);
            hornetSpawnTimer.start();
        }

        game.world().getGrid().values().forEach(decor -> decor.update(now));

        gardener.update(now);

        Iterator<Insect> insectIterator = activeInsects.iterator();
        while (insectIterator.hasNext()) {
            Insect insect = insectIterator.next();
            if (insect.isDeleted()) {
                insectIterator.remove();
                continue;
            }
            insect.update(now);
            if (insect.isDeleted()) {
                insectIterator.remove();
            }
        }

        if (gardener.getEnergy() <= 0) {
            showMessage("Perdu!", Color.RED);
        }
        Hedgehog hedgehog = game.getHedgehog();
        if (hedgehog != null && hedgehog.getPosition().level() == game.world().currentLevel() && hedgehog.isPickedUp()) {
            showMessage("Gagné !", Color.GREEN);
        }
    }


    public void cleanupSprites() {
        Iterator<Sprite> spriteIterator = sprites.iterator();
        while (spriteIterator.hasNext()) {
            Sprite sprite = spriteIterator.next();
            GameObject go = sprite.getGameObject();
            if (go != null && go.isDeleted()) {
                sprite.remove();
                spriteIterator.remove();

                if (go instanceof Insect) {
                    activeInsects.remove(go);
                }
            }
        }
        cleanUpSprites.clear();
        activeInsects.removeIf(GameObject::isDeleted);
    }

    private void render() {
        sprites.forEach(Sprite::render);
    }

    public void start() {
        if (gameLoop != null) {
            this.waspSpawnTimer.start();
            this.hornetSpawnTimer.start();
            gameLoop.start();
        } else System.err.println("GameLoop was null on start() call!");
    }

    public void stopAndClean() {
        System.out.println("Stopping and cleaning game engine...");
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
        if (waspSpawnTimer != null) {
            waspSpawnTimer.stop();
        }
        if (hornetSpawnTimer != null) {
            hornetSpawnTimer.stop();
        }
        sprites.clear();
        cleanUpSprites.clear();
        activeInsects.clear();
        layer.getChildren().clear();
        root.getChildren().clear();
        rootPane.getChildren().clear();

        System.out.println("Game engine stopped.");
    }

    private void resizeScene(int width, int height) {
        Platform.runLater(() -> {
            rootPane.setPrefSize(width, height + StatusBar.height);
            layer.setPrefSize(width, height);
            if (statusBar != null) {
                Node sbNode = statusBar.getNode();
                if (sbNode != null) {
                    if (sbNode instanceof Region) ((Region) sbNode).setPrefWidth(width);
                    sbNode.relocate(0, height);
                }
            }
            if (scene != null && scene.getWindow() != null) scene.getWindow().sizeToScene();
        });
    }

    private void spawnInsect(Class<? extends Nest> nestClass) {
        Map currentMap = game.world().getGrid();
        List<Position> potentialSpawnPoints = new ArrayList<>();
        List<Position> nestPositions = new ArrayList<>();

        for (Decor decor : currentMap.values()) {
            if (nestClass.isInstance(decor)) {
                nestPositions.add(decor.getPosition());
            }
        }

        if (nestPositions.isEmpty()) return;

        Position chosenNestPos = nestPositions.get(random.nextInt(nestPositions.size()));

        for (Direction dir : Direction.values()) {
            Position spawnPos = dir.nextPosition(chosenNestPos);
            if (currentMap.inside(spawnPos)) {
                Decor spawnDecor = currentMap.get(spawnPos);
                // cannot spawn on Tree or Door
                if (spawnDecor instanceof Tree || spawnDecor instanceof Door) {
                    continue;
                }
                // cannot spawn on Gardener
                if (spawnPos.equals(gardener.getPosition())) {
                    continue;
                }
                // cannot spawn on existing insect
                boolean occupiedByInsect = false;
                for (Insect insect : activeInsects) {
                    if (!insect.isDeleted() && insect.getPosition().equals(spawnPos)) {
                        occupiedByInsect = true;
                        break;
                    }
                }
                if (occupiedByInsect) {
                    continue;
                }
                if (spawnDecor != null && !spawnDecor.walkableByInsect(true)) {
                    continue;
                }

                potentialSpawnPoints.add(spawnPos);
            }
        }

        if (!potentialSpawnPoints.isEmpty()) {
            Position finalSpawnPos = potentialSpawnPoints.get(random.nextInt(potentialSpawnPoints.size()));
            Insect newInsect = null;
            int bombsToSpawn = 0;

            if (nestClass == NestWasp.class) {
                newInsect = new Wasp(game, finalSpawnPos);
                bombsToSpawn = 1;
            } else if (nestClass == NestHornet.class) {
                newInsect = new Hornet(game, finalSpawnPos);
                bombsToSpawn = 2;
            }

            if (newInsect != null) {
                activeInsects.add(newInsect);
                Sprite insectSprite = SpriteFactory.create(layer, newInsect);
                sprites.add(insectSprite);
                newInsect.setModified(true);

                // spawn associated insecticides
                spawnInsecticides(bombsToSpawn);
            }
        }
    }


    private void spawnInsecticides(int count) {
        if (count <= 0) return;

        Map currentMap = game.world().getGrid();
        List<Position> validBonusLocations = new ArrayList<>();

        for (int y = 0; y < currentMap.height(); y++) {
            for (int x = 0; x < currentMap.width(); x++) {
                Position pos = new Position(game.world().currentLevel(), x, y);
                Decor decor = currentMap.get(pos);

                if ((decor instanceof Grass || decor instanceof Land) && decor.getBonus() == null) {
                    boolean occupied = pos.equals(gardener.getPosition()) ||
                            activeInsects.stream().anyMatch(i -> !i.isDeleted() && i.getPosition().equals(pos));

                    if (!occupied) {
                        validBonusLocations.add(pos);
                    }
                }
            }
        }

        Collections.shuffle(validBonusLocations);

        int placedCount = 0;
        for (Position bonusPos : validBonusLocations) {
            if (placedCount >= count) break;

            Decor targetDecor = currentMap.get(bonusPos);
            if ((targetDecor instanceof Grass || targetDecor instanceof Land) && targetDecor.getBonus() == null) {
                Insecticide newBonus = new Insecticide(bonusPos, targetDecor);
                targetDecor.setBonus(newBonus);

                Sprite bonusSprite = SpriteFactory.create(layer, newBonus);
                sprites.add(bonusSprite);
                newBonus.setModified(true);
                targetDecor.setModified(true);
                placedCount++;
            }
        }
    }
}