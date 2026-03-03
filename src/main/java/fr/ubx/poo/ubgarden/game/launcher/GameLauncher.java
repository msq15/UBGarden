package fr.ubx.poo.ubgarden.game.launcher;

import fr.ubx.poo.ubgarden.game.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GameLauncher {

    private GameLauncher() {
    }

    public static GameLauncher getInstance() {
        return LoadSingleton.INSTANCE;
    }

    private int integerProperty(Properties properties, String name, int defaultValue) {
        return Integer.parseInt(properties.getProperty(name, Integer.toString(defaultValue)));
    }

    private boolean booleanProperty(Properties properties, String name, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(name, Boolean.toString(defaultValue)));
    }

    private Configuration getConfiguration(Properties properties) {

        // Load parameters
        int waspMoveFrequency = integerProperty(properties, "waspMoveFrequency", 2);
        int hornetMoveFrequency = integerProperty(properties, "hornetMoveFrequency", 1);

        int gardenerEnergy = integerProperty(properties, "gardenerEnergy", 100);
        int energyBoost = integerProperty(properties, "energyBoost", 50);
        long energyRecoverDuration = integerProperty(properties, "energyRecoverDuration", 1_000);
        long diseaseDuration = integerProperty(properties, "diseaseDuration", 5_000);

        return new Configuration(gardenerEnergy, energyBoost, energyRecoverDuration, diseaseDuration, waspMoveFrequency, hornetMoveFrequency);
    }

    // loads a MapLevel from its string representation
    private MapLevel loadLevelFromString(String levelStr, boolean compressed) {
        String[] rows = levelStr.split("x");
        int height = rows.length;

        if (height == 0) {
            throw new RuntimeException("Empty map string");
        }

        if (compressed) {
            rows = decompressRows(rows);
        }

        int width = rows[0].length();

        MapLevel mapLevel = new MapLevel(width, height);

        // fill the grid
        for (int y = 0; y < height; y++) {
            String row = rows[y];
            if (row.length() != width) {
                throw new RuntimeException("Inconsistent row width at row " + y);
            }

            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                try {
                    mapLevel.set(x, y, MapEntity.fromCode(c));
                } catch (MapException e) {
                    mapLevel.set(x, y, MapEntity.Grass);
                }
            }
        }

        return mapLevel;
    }

    private String[] decompressRows(String[] compressedRows) {
        String[] decompressedRows = new String[compressedRows.length];

        for (int i = 0; i < compressedRows.length; i++) {
            StringBuilder decompressed = new StringBuilder();
            String row = compressedRows[i];
            int j = 0;
            while (j < row.length()) {
                char c = row.charAt(j++);
                StringBuilder countStr = new StringBuilder();
                while (j < row.length() && Character.isDigit(row.charAt(j))) {
                    countStr.append(row.charAt(j++));
                }
                if (!countStr.isEmpty()) {
                    int count = Integer.parseInt(countStr.toString());
                    decompressed.append(String.valueOf(c).repeat(Math.max(0, count - 1)));
                }
                decompressed.append(c);
            }
            decompressedRows[i] = decompressed.toString();
        }
        return decompressedRows;
    }

    public Game load(File file) {
        try (InputStream input = new FileInputStream(file)) {
            Properties config = new Properties();
            config.load(input);
            boolean compression = booleanProperty(config, "compression", false);
            int numLevels = integerProperty(config, "levels", 1);

            // create world with the specified number of levels
            World world = new World(numLevels);

            // load each level map
            List<MapLevel> mapLevels = new ArrayList<>();
            for (int i = 1; i <= numLevels; i++) {
                String levelKey = "level" + i;
                String levelData = config.getProperty(levelKey);
                if (levelData == null) {
                    throw new RuntimeException("Level " + i + " not found in configuration file");
                }
                MapLevel mapLevel = loadLevelFromString(levelData, compression);
                mapLevels.add(mapLevel);
            }

            Position gardenerPosition = mapLevels.get(0).getGardenerPosition();
            if (gardenerPosition == null) {
                throw new RuntimeException("Gardener not found in first level");
            }
            // create game with configuration
            Configuration configuration = getConfiguration(config);
            Game game = new Game(world, configuration, gardenerPosition);

            // add all levels to the world
            for (int i = 0; i < numLevels; i++) {
                int levelNum = i + 1;
                Map level = new Level(game, levelNum, mapLevels.get(i));
                world.put(levelNum, level);
            }

            return game;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Game load() {
        Properties emptyConfig = new Properties();
        MapLevel mapLevel = new MapLevelDefaultStart();
        Position gardenerPosition = mapLevel.getGardenerPosition();
        if (gardenerPosition == null)
            throw new RuntimeException("Gardener not found");
        Configuration configuration = getConfiguration(emptyConfig);
        World world = new World(1);
        Game game = new Game(world, configuration, gardenerPosition);
        Map level = new Level(game, 1, mapLevel);
        world.put(1, level);
        return game;
    }

    private static class LoadSingleton {
        static final GameLauncher INSTANCE = new GameLauncher();
    }

}
