package games.azul.players;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
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

    private final Random rnd;

    public AzulRuleBasedPlayer() {
        this(new Random());
    }

    public AzulRuleBasedPlayer(Random random) {
        // Use a standard player type naming convention that the metrics system recognizes
        super(null, "RuleBased");
        this.rnd = random;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        AzulGameState state = (AzulGameState) gameState;
        
        if (possibleActions.isEmpty()) {
            return null;
        }
        
        // Filter actions by type (we only care about TakeTilesAction in Azul)
        List<TakeTilesAction> tileActions = new ArrayList<>();
        for (AbstractAction action : possibleActions) {
            if (action instanceof TakeTilesAction) {
                tileActions.add((TakeTilesAction) action);
            }
        }
        
        // If no tile actions, return a random action
        if (tileActions.isEmpty()) {
            return possibleActions.get(rnd.nextInt(possibleActions.size()));
        }
        
        // Score all possible tile actions
        Map<TakeTilesAction, Double> actionScores = new HashMap<>();
        for (TakeTilesAction action : tileActions) {
            actionScores.put(action, evaluateAction(action, state));
        }
        
        // Find the action with the highest score
        TakeTilesAction bestAction = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (Map.Entry<TakeTilesAction, Double> entry : actionScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestAction = entry.getKey();
            }
        }
        
        return bestAction != null ? bestAction : tileActions.get(rnd.nextInt(tileActions.size()));
    }

    /**
     * Evaluates a tile taking action based on various heuristics
     */
    private double evaluateAction(TakeTilesAction action, AzulGameState state) {
        int playerId = action.getPlayerID();
        int patternLineIdx = action.getPatternLineIdx();
        AzulTile.TileType tileType = action.getTileType();
        double score = 0.0;
        
        Wall playerWall = state.getPlayerWall(playerId);
        PatternLine[] playerPatternLines = state.getPatternLines(playerId);
        
        // If placing directly to floor line, heavily penalize
        if (patternLineIdx == TakeTilesAction.getFloorLine()) {
            score -= 15.0;
            return score; // Very low priority unless absolutely necessary
        }
        
        PatternLine targetLine = playerPatternLines[patternLineIdx];
        
        // Check if this tile type can be placed in this pattern line's corresponding wall position
        int tileRow = patternLineIdx;
        if (!playerWall.canPlaceTile(tileRow, tileType)) {
            score -= 10.0; // Penalize taking tiles that can't go on the wall later
            return score;
        }
        
        // Calculate how many tiles we would get with this action
        int tilesCount = countTilesOfType(action, state);
        
        // Check the space left in the pattern line
        int spaceAvailable = (patternLineIdx + 1) - targetLine.getCount();
        
        // If the line already has tiles, make sure they're the same type
        if (targetLine.getCount() > 0 && targetLine.getCurrentType() != tileType) {
            score -= 20.0; // Can't add different tiles to a line that already has tiles
            return score;
        }
        
        // Reward completing a pattern line exactly (can be moved to wall next round)
        if (tilesCount == spaceAvailable) {
            score += 10.0;
        } 
        // Penalize overflow (tiles going to floor)
        else if (tilesCount > spaceAvailable) {
            score -= (tilesCount - spaceAvailable) * 2.0;
        }
        // Reward efficiently filling lines close to completion
        else {
            double fillPercentage = (double)(targetLine.getCount() + tilesCount) / (patternLineIdx + 1);
            score += fillPercentage * 5.0;
        }
        
        // Reward actions that target higher rows (they need more tiles so are harder to complete)
        score += patternLineIdx * 0.5;
        
        // Look at adjacency potential on the wall
        score += evaluateWallPlacement(playerWall, tileRow, tileType);
        
        // Add a small random factor to break ties
        score += rnd.nextDouble() * 0.1;
        
        return score;
    }
    
    /**
     * Estimates the number of tiles of the specified type that would be acquired
     */
    private int countTilesOfType(TakeTilesAction action, AzulGameState state) {
        int count = 0;
        AzulTile.TileType targetType = action.getTileType();
        
        if (action.getFactoryId() == -1) {
            // Count from center pool
            for (AzulTile tile : state.getCenterPool()) {
                if (tile.getTileType() == targetType) {
                    count++;
                }
            }
        } else {
            // Count from factory
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    AzulTile tile = state.getFactories().get(action.getFactoryId()).getElement(col, row);
                    if (tile != null && tile.getTileType() == targetType) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * Evaluates the potential value of placing a tile on the wall
     */
    private double evaluateWallPlacement(Wall wall, int row, AzulTile.TileType tileType) {
        double score = 0.0;
        int col = wall.getColumnForColor(row, tileType);
        
        if (col == -1) {
            return -5.0; // Can't place this tile in this row
        }
        
        // Check for adjacent tiles already on the wall (horizontally and vertically)
        // This rewards moves that continue existing patterns
        for (int r = Math.max(0, row - 1); r <= Math.min(4, row + 1); r++) {
            if (r != row && wall.isTilePlaced(r, col)) {
                score += 2.0; // Adjacent tile in column
            }
        }
        
        for (int c = Math.max(0, col - 1); c <= Math.min(4, col + 1); c++) {
            if (c != col && wall.isTilePlaced(row, c)) {
                score += 2.0; // Adjacent tile in row
            }
        }
        
        // Reward moves that contribute to completing rows or columns
        int tilesInRow = 0;
        int tilesInCol = 0;
        
        for (int c = 0; c < 5; c++) {
            if (wall.isTilePlaced(row, c)) {
                tilesInRow++;
            }
        }
        
        for (int r = 0; r < 5; r++) {
            if (wall.isTilePlaced(r, col)) {
                tilesInCol++;
            }
        }
        
        // More points for nearly complete rows/columns
        score += tilesInRow * 0.5;
        score += tilesInCol * 0.5;
        
        // Bonus for completing color sets
        int colorSetCount = 0;
        for (int r = 0; r < 5; r++) {
            int c = wall.getColumnForColor(r, tileType);
            if (wall.isTilePlaced(r, c)) {
                colorSetCount++;
            }
        }
        
        score += colorSetCount * 0.3;
        
        return score;
    }

    @Override
    public AzulRuleBasedPlayer copy() {
        return new AzulRuleBasedPlayer(new Random(rnd.nextInt()));
    }
} 