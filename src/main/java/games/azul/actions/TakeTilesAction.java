package games.azul.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.azul.AzulGameState;
import games.azul.components.AzulTile;
import games.azul.components.PatternLine;
import games.azul.components.Wall;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TakeTilesAction extends AbstractAction {
    private final int playerId;
    private final int factoryId;  // -1 for center pool
    private final int patternLineIdx;
    private final AzulTile.TileType tileType;

    public TakeTilesAction(int playerId, int factoryId, int patternLineIdx, AzulTile.TileType tileType) {
        this.playerId = playerId;
        this.factoryId = factoryId;
        this.patternLineIdx = patternLineIdx;
        this.tileType = tileType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        AzulGameState state = (AzulGameState) gs;

        // Check if action is still valid
        if (!isActionValid(state)) {
            return false;
        }

        // Move tiles from factory/center to pattern line
        if (factoryId == -1) {
            // Take from center pool
            moveTilesFromCenter(state);
        } else {
            // Take from factory
            moveTilesFromFactory(state);
        }

        // Check if round should end (no more valid moves possible)
        if (areFactoriesAndCenterEmpty(state)) {
            state.setPhase(AzulGameState.GamePhase.WALL_TILING);
        }

        return true;
    }

    private void moveTilesFromFactory(AzulGameState state) {
        GridBoard<AzulTile> factory = state.getFactories().get(factoryId);
        PatternLine targetLine = state.getPatternLines(playerId)[patternLineIdx];

        // Move matching tiles to pattern line, others to center
        for (int i = 0; i < factory.getHeight(); i++) {
            for (int j = 0; j < factory.getWidth(); j++) {
                AzulTile tile = factory.getElement(i, j);
                if (tile != null) {
                    if (tile.getTileType() == tileType) {
                        targetLine.add(tile);
                    } else {
                        moveToCenter(state, tile);
                    }
                    factory.setElement(i, j, null);
                }
            }
        }
    }

    private void moveTilesFromCenter(AzulGameState state) {
        List<AzulTile> center = state.getCenterPool();
        PatternLine targetLine = state.getPatternLines(playerId)[patternLineIdx];

        // Remove matching tiles and add to pattern line
        Iterator<AzulTile> iterator = center.iterator();
        while (iterator.hasNext()) {
            AzulTile tile = iterator.next();
            if (tile.getTileType() == tileType) {
                targetLine.add(tile);
                iterator.remove();
            }
        }
    }
    private void moveToCenter(AzulGameState state, AzulTile tile) {
        state.getCenterPool().add(tile);
    }
    private boolean areFactoriesAndCenterEmpty(AzulGameState state) {
        // Check factories
        for (GridBoard<AzulTile> factory : state.getFactories()) {
            if (hasAnyTiles(factory)) return false;
        }

        // Check center pool
        return state.getCenterPool().isEmpty();
    }

    private boolean hasAnyTiles(GridBoard<AzulTile> board) {
        for (int i = 0; i < board.getHeight(); i++) {
            for (int j = 0; j < board.getWidth(); j++) {
                if (board.getElement(i, j) != null) return true;
            }
        }
        return false;
    }

    private boolean isActionValid(AzulGameState state) {
        // Add validation logic here
        return factoryId < state.getFactories().size() &&
                patternLineIdx < state.getPatternLines(playerId).length &&
                state.getPlayerWall(playerId).canPlaceTile(patternLineIdx, tileType);
    }

    @Override
    public AbstractAction copy() {
        return new TakeTilesAction(playerId, factoryId, patternLineIdx, tileType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TakeTilesAction)) return false;
        TakeTilesAction other = (TakeTilesAction) o;
        return playerId == other.playerId &&
                factoryId == other.factoryId &&
                patternLineIdx == other.patternLineIdx &&
                tileType == other.tileType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, factoryId, patternLineIdx, tileType);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String source = factoryId == -1 ? "center pool" : "factory " + factoryId;
        return String.format("Player %d takes %s tiles from %s to pattern line %d",
                playerId, tileType, source, patternLineIdx);
    }

    // Getters
    public int getPlayerID() { return playerId; }
    public int getFactoryId() { return factoryId; }
    public int getPatternLineIdx() { return patternLineIdx; }
    public AzulTile.TileType getTileType() { return tileType; }
}