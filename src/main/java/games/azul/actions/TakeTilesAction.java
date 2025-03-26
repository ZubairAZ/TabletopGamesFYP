package games.azul.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.azul.AzulGameState;
import games.azul.components.AzulTile;
import games.azul.components.PatternLine;
import games.azul.components.Wall;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TakeTilesAction extends AbstractAction {
    private final int playerId;
    private final int factoryId;  // -1 for centre pool
    private final int patternLineIdx;  // -1 for floor line
    private final AzulTile.TileType tileType;
    private static final int FLOOR_LINE = -1;
    private static final boolean DEBUG_MODE = false;

    public TakeTilesAction(int playerId, int factoryId, int patternLineIdx, AzulTile.TileType tileType) {
        this.playerId = playerId;
        this.factoryId = factoryId;
        this.patternLineIdx = patternLineIdx;
        this.tileType = tileType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        AzulGameState state = (AzulGameState) gs;
        if (isDummyAction(state) || !isActionValid(state)) return isDummyAction(state);
        
        if (factoryId == -1) moveTilesFromCenter(state);
        else moveTilesFromFactory(state);

        if (areFactoriesAndCenterEmpty(state)) 
            state.setPhase(AzulGameState.GamePhase.WALL_TILING);
        
        return true;
    }

    private boolean isDummyAction(AzulGameState state) {
        return factoryId == -1 && patternLineIdx == FLOOR_LINE && 
               (state.getPhase() == AzulGameState.GamePhase.WALL_TILING || 
                state.getGameStatus() != CoreConstants.GameResult.GAME_ONGOING);
    }

    private void moveTilesFromFactory(AzulGameState state) {
        GridBoard<AzulTile> factory = state.getFactories().get(factoryId);
        int matchingTileCount = countMatchingTiles(factory);
        if (matchingTileCount == 0) return;

        if (patternLineIdx != FLOOR_LINE) {
            PatternLine targetLine = state.getPatternLines(playerId)[patternLineIdx];
            int availableSpace = patternLineIdx + 1 - targetLine.getCount();
            processTilesFromFactory(state, factory, targetLine, availableSpace);
        } else {
            processTilesFromFactoryToFloor(state, factory);
        }
    }

    private int countMatchingTiles(GridBoard<AzulTile> factory) {
        int count = 0;
        for (int row = 0; row < factory.getHeight(); row++)
            for (int col = 0; col < factory.getWidth(); col++)
                if (factory.getElement(col, row) != null && factory.getElement(col, row).getTileType() == tileType)
                    count++;
        return count;
    }

    private void processTilesFromFactory(AzulGameState state, GridBoard<AzulTile> factory, PatternLine targetLine, int availableSpace) {
        for (int row = 0; row < factory.getHeight(); row++) {
            for (int col = 0; col < factory.getWidth(); col++) {
                AzulTile tile = factory.getElement(col, row);
                if (tile != null) {
                    if (tile.getTileType() == tileType) {
                        if (availableSpace > 0) {
                            targetLine.add(tile);
                            availableSpace--;
                        } else {
                            addTileToFloor(state, tile);
                        }
                    } else {
                        state.getCenterPool().add(tile);
                    }
                    factory.setElement(col, row, null);
                }
            }
        }
    }

    private void processTilesFromFactoryToFloor(AzulGameState state, GridBoard<AzulTile> factory) {
        for (int row = 0; row < factory.getHeight(); row++) {
            for (int col = 0; col < factory.getWidth(); col++) {
                AzulTile tile = factory.getElement(col, row);
                if (tile != null) {
                    if (tile.getTileType() == tileType) addTileToFloor(state, tile);
                    else state.getCenterPool().add(tile);
                    factory.setElement(col, row, null);
                }
            }
        }
    }

    private void moveTilesFromCenter(AzulGameState state) {
        List<AzulTile> centerPool = state.getCenterPool();
        List<AzulTile> tilesToMove = new ArrayList<>();
        
        if (state.isFirstPlayerTokenInCenter()) {
            state.removeFirstPlayerTokenFromCenter();
            state.getFloorLine(playerId).increment();
            state.setFirstPlayer(playerId);
            
            if (DEBUG_MODE) {
                System.out.println("Player " + playerId + " takes first player token");
            }
        }
        
        Iterator<AzulTile> iterator = centerPool.iterator();
        while (iterator.hasNext()) {
            AzulTile tile = iterator.next();
            if (tile.getTileType() == tileType) {
                tilesToMove.add(tile);
                iterator.remove();
            }
        }
        
        if (patternLineIdx >= 0) {
            PatternLine patternLine = state.getPatternLines(playerId)[patternLineIdx];
            int availableSpace = patternLineIdx + 1 - patternLine.getCount();
            
            for (AzulTile tile : tilesToMove) {
                if (availableSpace > 0) {
                    patternLine.add(tile);
                    availableSpace--;
                } else addTileToFloor(state, tile);
            }
        } else {
            tilesToMove.forEach(tile -> addTileToFloor(state, tile));
        }
    }

    private void addTileToFloor(AzulGameState state, AzulTile tile) {
        state.getFloorLine(playerId).increment();
        state.getDiscardBag().add(tile);
    }
    
    private boolean areFactoriesAndCenterEmpty(AzulGameState state) {
        if (state.getFactories().stream().anyMatch(this::hasAnyTiles)) return false;
        List<AzulTile> centerPool = state.getCenterPool();
        return centerPool.isEmpty() || 
               (centerPool.size() == 1 && centerPool.get(0).getTileType() == null);
    }

    private boolean hasAnyTiles(GridBoard<AzulTile> board) {
        for (int i = 0; i < board.getHeight(); i++)
            for (int j = 0; j < board.getWidth(); j++)
                if (board.getElement(i, j) != null) return true;
        return false;
    }

    public boolean isActionValid(AzulGameState state) {
        if (isDummyAction(state)) return true;
        if (playerId < 0 || playerId >= state.getNPlayers()) return false;
        
        if (factoryId == -1) {
            return state.getCenterPool().stream()
                       .anyMatch(tile -> tile.getTileType() == tileType);
        }
        
        if (factoryId < 0 || factoryId >= state.getFactories().size()) return false;
        
        GridBoard<AzulTile> factory = state.getFactories().get(factoryId);
        boolean hasTileType = false;
        for (int row = 0; row < factory.getHeight() && !hasTileType; row++)
            for (int col = 0; col < factory.getWidth() && !hasTileType; col++)
                if (factory.getElement(col, row) != null && 
                    factory.getElement(col, row).getTileType() == tileType)
                    hasTileType = true;
        
        return hasTileType;
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
        return String.format("Player %d takes %s tiles from %s to %s", 
            playerId, tileType,
            factoryId == -1 ? "center" : "factory " + factoryId,
            patternLineIdx == FLOOR_LINE ? "floor line" : "pattern line " + patternLineIdx);
    }

    public int getPlayerID() { return playerId; }
    public int getFactoryId() { return factoryId; }
    public int getPatternLineIdx() { return patternLineIdx; }
    public AzulTile.TileType getTileType() { return tileType; }
    public static int getFloorLine() { return FLOOR_LINE; }
}