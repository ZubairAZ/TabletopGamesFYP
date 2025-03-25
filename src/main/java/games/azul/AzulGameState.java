package games.azul;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IGamePhase;
import games.GameType;
import games.azul.components.AzulTile;
import games.azul.components.PatternLine;
import games.azul.components.Wall;
import java.util.*;

public class AzulGameState extends AbstractGameState {
    public enum GamePhase {
        TILE_PICKING,
        WALL_TILING
    }

    private GamePhase gamePhase;
    // Game state components with proper visibility modes
    private final List<Wall> playerWalls = new ArrayList<>();  // Scoring walls for each player
    private final List<PatternLine[]> patternLines = new ArrayList<>();  // Pattern lines for each player
    private final List<Counter> floorLines = new ArrayList<>();  // Penalty tiles for each player
    private final List<Counter> scores = new ArrayList<>();  // Player scores

    private final List<GridBoard<AzulTile>> factories = new ArrayList<>();  // Factory displays
    private List<AzulTile> centerPool;  // Center of table
    private Deck<AzulTile> tileBag;  // Main bag of tiles
    private Deck<AzulTile> discardBag;  // Discard pile
    private int firstPlayer;  // Player who has the first player token

    public AzulGameState(AbstractParameters params, int nPlayers) {
        super(params, nPlayers);
        gamePhase = GamePhase.TILE_PICKING;  // Initialize game phase
        firstPlayer = 0;  // First player starts with the token
    }

    // Constructor specifically for AzulGameParameters
    public AzulGameState(AzulGameParameters params, int nPlayers) {
        super(params, nPlayers);
        gamePhase = GamePhase.TILE_PICKING;  // Initialize game phase
        firstPlayer = 0;  // First player starts with the token
    }

    /**
     * Prints detailed debug information about the current game state to the terminal.
     * This includes player stats, factory displays, center pool, and tile bags.
     */
    public void printDebugInfo() {
        System.out.println("\n========== AZUL GAME STATE DEBUG INFO ==========");
        System.out.println("Game Phase: " + gamePhase);
        System.out.println("Current Player: " + getCurrentPlayer());
        System.out.println("First Player: " + firstPlayer);
        System.out.println("Round: " + getRoundCounter() + ", Turn: " + getTurnCounter());
        
        // Print player stats
        System.out.println("\n----- PLAYER STATS -----");
        for (int i = 0; i < getNPlayers(); i++) {
            System.out.println("\nPlayer " + i + (i == getCurrentPlayer() ? " (Current)" : ""));
            System.out.println("Score: " + scores.get(i).getValue());
            System.out.println("Floor Line: " + floorLines.get(i).getValue() + " tiles");
            
            // Print pattern lines
            System.out.println("Pattern Lines:");
            PatternLine[] lines = patternLines.get(i);
            for (int j = 0; j < lines.length; j++) {
                PatternLine line = lines[j];
                System.out.println("  Line " + j + " (max " + (j+1) + "): " + 
                                  line.getCount() + " tiles" + 
                                  (line.getCurrentType() != null ? " of type " + line.getCurrentType() : " (empty)"));
            }
            
            // Print wall
            Wall wall = playerWalls.get(i);
            System.out.println("Wall:");
            System.out.println("  Completed Rows: " + wall.getCompletedRows());
            System.out.println("  Completed Columns: " + wall.getCompletedColumns());
            System.out.println("  Completed Color Sets: " + wall.getCompletedColorSets());
            
            // Count total tiles on wall
            int totalTiles = 0;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    if (wall.isTilePlaced(row, col)) {
                        totalTiles++;
                    }
                }
            }
            System.out.println("  Total Tiles: " + totalTiles);
        }
        
        // Print factory displays
        System.out.println("\n----- FACTORY DISPLAYS -----");
        for (int i = 0; i < factories.size(); i++) {
            GridBoard<AzulTile> factory = factories.get(i);
            System.out.print("Factory " + i + ": ");
            
            Map<AzulTile.TileType, Integer> tileCounts = new HashMap<>();
            for (int row = 0; row < factory.getHeight(); row++) {
                for (int col = 0; col < factory.getWidth(); col++) {
                    AzulTile tile = factory.getElement(col, row);
                    if (tile != null) {
                        AzulTile.TileType tileType = tile.getTileType();
                        tileCounts.put(tileType, tileCounts.getOrDefault(tileType, 0) + 1);
                    }
                }
            }
            
            if (tileCounts.isEmpty()) {
                System.out.println("Empty");
            } else {
                for (Map.Entry<AzulTile.TileType, Integer> entry : tileCounts.entrySet()) {
                    System.out.print(entry.getValue() + " " + entry.getKey() + ", ");
                }
                System.out.println();
            }
        }
        
        // Print center pool
        System.out.println("\n----- CENTER POOL -----");
        if (centerPool.isEmpty()) {
            System.out.println("Empty");
        } else {
            Map<AzulTile.TileType, Integer> centerCounts = new HashMap<>();
            for (AzulTile tile : centerPool) {
                AzulTile.TileType tileType = tile.getTileType();
                centerCounts.put(tileType, centerCounts.getOrDefault(tileType, 0) + 1);
            }
            
            for (Map.Entry<AzulTile.TileType, Integer> entry : centerCounts.entrySet()) {
                System.out.print(entry.getValue() + " " + entry.getKey() + ", ");
            }
            System.out.println();
        }
        
        // Print tile bags
        System.out.println("\n----- TILE BAGS -----");
        System.out.println("Main Bag: " + tileBag.getSize() + " tiles");
        System.out.println("Discard Bag: " + discardBag.getSize() + " tiles");
        
        System.out.println("\n=================================================\n");
    }

    // Getters
    public GamePhase getPhase() { return gamePhase; }
    public void setPhase(GamePhase phase) { this.gamePhase = phase; }
    public List<Wall> getPlayerWalls() { return playerWalls; }
    public Wall getPlayerWall(int player) { return playerWalls.get(player); }
    public List<PatternLine[]> getPatternLines() { return patternLines; }
    public PatternLine[] getPatternLines(int player) { return patternLines.get(player); }
    public List<Counter> getFloorLines() { return floorLines; }
    public Counter getFloorLine(int player) { return floorLines.get(player); }
    public List<Counter> getScores() { return scores; }
    public Counter getScore(int player) { return scores.get(player); }
    public List<GridBoard<AzulTile>> getFactories() { return factories; }
    public void setCenterPool(List<AzulTile> centerPool) { this.centerPool = centerPool; }
    public List<AzulTile> getCenterPool() { return centerPool; }
    public Deck<AzulTile> getTileBag() { return tileBag; }
    public void setTileBag(Deck<AzulTile> tileBag) { this.tileBag = tileBag; }
    public Deck<AzulTile> getDiscardBag() { return discardBag; }
    public void setDiscardBag(Deck<AzulTile> discardBag) { this.discardBag = discardBag; }
    public int getFirstPlayer() { return firstPlayer; }
    public void setFirstPlayer(int player) { firstPlayer = player; }

    @Override
    protected GameType _getGameType() {
        return GameType.Azul;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerWalls);
        for (PatternLine[] lines : patternLines) {
            components.addAll(Arrays.asList(lines));
        }
        components.addAll(floorLines);
        components.addAll(scores);
        components.addAll(factories);
        components.addAll(centerPool);
        components.add(tileBag);
        components.add(discardBag);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        AzulGameState copy = new AzulGameState((AzulGameParameters) gameParameters, getNPlayers());
        copy.gamePhase = this.gamePhase;

        // Initialize lists in copy with same size as original
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerWalls.add(null);
            copy.patternLines.add(new PatternLine[5]);
            copy.floorLines.add(null);
            copy.scores.add(null);
        }

        for (int i = 0; i < factories.size(); i++) {
            copy.factories.add(null);
        }

        // Copy all components with proper visibility
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerWalls.set(i, playerWalls.get(i).copy());
            for (int j = 0; j < patternLines.get(i).length; j++) {
                copy.patternLines.get(i)[j] = (PatternLine) patternLines.get(i)[j].copy();
            }
            copy.floorLines.set(i, floorLines.get(i).copy());
            copy.scores.set(i, scores.get(i).copy());
        }

        for (int i = 0; i < factories.size(); i++) {
            copy.factories.set(i, factories.get(i).copy());
        }

        copy.centerPool = new ArrayList<>();
        for (AzulTile tile : centerPool) {
            copy.centerPool.add(tile.copy());
        }

        // Handle hidden information properly for tile bag
        if (playerId != -1) {
            copy.tileBag = (Deck<AzulTile>) tileBag.copy(playerId);
        } else {
            copy.tileBag = tileBag.copy();
        }
        copy.discardBag = discardBag.copy();
        copy.firstPlayer = firstPlayer;

        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        double score = scores.get(playerId).getValue();

        Wall wall = playerWalls.get(playerId);
        AzulGameParameters params = (AzulGameParameters) gameParameters;
        
        // Add bonus points for completed rows, columns, and color sets
        score += wall.getCompletedRows() * params.getRowBonusPoints();
        score += wall.getCompletedColumns() * params.getColumnBonusPoints();
        score += wall.getCompletedColorSets() * params.getColorSetBonusPoints();

        // Apply floor line penalties
        score += floorLines.get(playerId).getValue() * params.getPenaltyPerFloorTile();

        return score / 100.0;  // Normalize to [-1, 1] range
    }

    @Override
    public double getGameScore(int playerId) {
        return scores.get(playerId).getValue();
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof AzulGameState)) return false;
        AzulGameState other = (AzulGameState) o;
        return playerWalls.equals(other.playerWalls) &&
                patternLines.equals(other.patternLines) &&
                floorLines.equals(other.floorLines) &&
                scores.equals(other.scores) &&
                factories.equals(other.factories) &&
                centerPool.equals(other.centerPool) &&
                tileBag.equals(other.tileBag) &&
                discardBag.equals(other.discardBag) &&
                firstPlayer == other.firstPlayer;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hash(playerWalls, patternLines, floorLines, scores,
                factories, centerPool, tileBag, discardBag, firstPlayer);
        return result;
    }

    /**
     * Checks if the first player token is in the center pool.
     * @return true if the first player token is in the center pool, false otherwise
     */
    public boolean isFirstPlayerTokenInCenter() {
        // Since we don't have a FIRST_PLAYER enum, we need to check if the center pool
        // contains a tile that doesn't match any of the standard colors
        for (AzulTile tile : centerPool) {
            if (tile.getTileType() == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the first player token from the center pool.
     */
    public void removeFirstPlayerTokenFromCenter() {
        Iterator<AzulTile> iterator = centerPool.iterator();
        while (iterator.hasNext()) {
            AzulTile tile = iterator.next();
            if (tile.getTileType() == null) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Adds the first player token to the center pool.
     */
    public void addFirstPlayerTokenToCenter() {
        // Create a special tile to represent the first player token
        // We'll use a null type to distinguish it from regular tiles
        centerPool.add(new AzulTile(null) {
            @Override
            public AzulTile.TileType getTileType() {
                return null;
            }
            
            @Override
            public String toString() {
                return "F";  // First player token
            }
        });
    }
}