package games.azul.players;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.azul.AzulGameState;
import games.azul.actions.TakeTilesAction;
import games.azul.components.AzulTile;
import games.azul.components.PatternLine;
import games.azul.components.Wall;

import java.util.*;

/**
 * A simple rule-based player for Azul that follows a set of heuristic strategies:
 * 1. Prioritize moves that complete pattern lines and can be placed on the wall immediately
 * 2. Avoid moves that would result in excessive floor penalties
 * 3. Prioritize higher-value pattern lines (rows that score more points)
 * 4. Try to build patterns on the wall that maximize adjacency bonuses
 */
public class AzulRuleBasedPlayer extends AbstractPlayer {
    private static final double FLOOR_PENALTY = 15.0;
    private static final double INVALID_WALL_PENALTY = 10.0;
    private static final double COMPLETE_LINE_BONUS = 10.0;
    private static final double OVERFLOW_PENALTY_PER_TILE = 2.0;
    private static final double LINE_FILL_MULTIPLIER = 5.0;
    private static final double ROW_BONUS_MULTIPLIER = 0.5;
    private static final double ADJACENT_TILE_BONUS = 2.0;
    private static final double COLOR_SET_BONUS = 0.3;
    
    private final Random rnd;

    public AzulRuleBasedPlayer() {
        this(new Random());
    }

    public AzulRuleBasedPlayer(Random random) {
        super(null, "RuleBased");
        this.rnd = random;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        if (possibleActions.isEmpty()) return null;
        
        List<TakeTilesAction> tileActions = possibleActions.stream()
            .filter(TakeTilesAction.class::isInstance)
            .map(TakeTilesAction.class::cast)
            .toList();
            
        if (tileActions.isEmpty()) {
            return possibleActions.get(rnd.nextInt(possibleActions.size()));
        }
        
        return findBestAction((AzulGameState) gameState, tileActions);
    }

    private TakeTilesAction findBestAction(AzulGameState state, List<TakeTilesAction> actions) {
        return actions.stream()
            .map(action -> Map.entry(action, evaluateAction(action, state)))
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(actions.get(rnd.nextInt(actions.size())));
    }

    /**
     * Evaluates a tile taking action based on various heuristics
     */
    private double evaluateAction(TakeTilesAction action, AzulGameState state) {
        if (action.getPatternLineIdx() == TakeTilesAction.getFloorLine()) {
            return -FLOOR_PENALTY;
        }

        Wall playerWall = state.getPlayerWall(action.getPlayerID());
        if (!playerWall.canPlaceTile(action.getPatternLineIdx(), action.getTileType())) {
            return -INVALID_WALL_PENALTY;
        }

        double score = 0.0;
        int tilesCount = countTilesOfType(action, state);
        PatternLine targetLine = state.getPatternLines(action.getPlayerID())[action.getPatternLineIdx()];
        int spaceAvailable = (action.getPatternLineIdx() + 1) - targetLine.getCount();

        if (targetLine.getCount() > 0 && targetLine.getCurrentType() != action.getTileType()) {
            return -INVALID_WALL_PENALTY * 2;
        }

        score += evaluatePatternLinePlacement(tilesCount, spaceAvailable, targetLine);
        score += action.getPatternLineIdx() * ROW_BONUS_MULTIPLIER;
        score += evaluateWallPlacement(playerWall, action.getPatternLineIdx(), action.getTileType());
        score += rnd.nextDouble() * 0.1;

        return score;
    }

    private double evaluatePatternLinePlacement(int tilesCount, int spaceAvailable, PatternLine targetLine) {
        if (tilesCount == spaceAvailable) {
            return COMPLETE_LINE_BONUS;
        }
        if (tilesCount > spaceAvailable) {
            return -(tilesCount - spaceAvailable) * OVERFLOW_PENALTY_PER_TILE;
        }
        return ((double)(targetLine.getCount() + tilesCount) / (targetLine.getCount() + spaceAvailable)) * LINE_FILL_MULTIPLIER;
    }

    /**
     * Evaluates the potential value of placing a tile on the wall
     */
    private double evaluateWallPlacement(Wall wall, int row, AzulTile.TileType tileType) {
        int col = wall.getColumnForColor(row, tileType);
        if (col == -1) return -INVALID_WALL_PENALTY / 2;

        double score = evaluateAdjacentTiles(wall, row, col);
        score += evaluatePatternCompletion(wall, row, col);
        score += evaluateColorSet(wall, tileType);

        return score;
    }

    private double evaluateAdjacentTiles(Wall wall, int row, int col) {
        double score = 0.0;
        for (int r = Math.max(0, row - 1); r <= Math.min(4, row + 1); r++) {
            if (r != row && wall.isTilePlaced(r, col)) score += ADJACENT_TILE_BONUS;
        }
        for (int c = Math.max(0, col - 1); c <= Math.min(4, col + 1); c++) {
            if (c != col && wall.isTilePlaced(row, c)) score += ADJACENT_TILE_BONUS;
        }
        return score;
    }

    private double evaluatePatternCompletion(Wall wall, int row, int col) {
        return countTilesInLine(wall, row, true) * ROW_BONUS_MULTIPLIER +
               countTilesInLine(wall, col, false) * ROW_BONUS_MULTIPLIER;
    }

    private int countTilesInLine(Wall wall, int index, boolean isRow) {
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (wall.isTilePlaced(isRow ? index : i, isRow ? i : index)) count++;
        }
        return count;
    }

    private double evaluateColorSet(Wall wall, AzulTile.TileType tileType) {
        int colorSetCount = 0;
        for (int r = 0; r < 5; r++) {
            int c = wall.getColumnForColor(r, tileType);
            if (wall.isTilePlaced(r, c)) colorSetCount++;
        }
        return colorSetCount * COLOR_SET_BONUS;
    }

    /**
     * Estimates the number of tiles of the specified type that would be acquired
     */
    private int countTilesOfType(TakeTilesAction action, AzulGameState state) {
        if (action.getFactoryId() == -1) {
            return (int) state.getCenterPool().stream()
                .filter(tile -> tile.getTileType() == action.getTileType())
                .count();
        }

        GridBoard<AzulTile> factory = state.getFactories().get(action.getFactoryId());
        int count = 0;
        for (int row = 0; row < factory.getHeight(); row++) {
            for (int col = 0; col < factory.getWidth(); col++) {
                AzulTile tile = factory.getElement(col, row);
                if (tile != null && tile.getTileType() == action.getTileType()) count++;
            }
        }
        return count;
    }

    @Override
    public AzulRuleBasedPlayer copy() {
        return new AzulRuleBasedPlayer(new Random(rnd.nextInt()));
    }
} 