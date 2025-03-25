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

        // Special case for dummy actions used in RMHC player
        if (factoryId == -1 && patternLineIdx == FLOOR_LINE && 
            (state.getPhase() == AzulGameState.GamePhase.WALL_TILING || 
             state.getGameStatus() != CoreConstants.GameResult.GAME_ONGOING)) {
            if (DEBUG_MODE) {
                System.out.println("Executing dummy action for RMHC player");
            }
            return true;
        }

        if (!isActionValid(state)) {
            if (DEBUG_MODE) {
                System.out.println("Invalid action: " + this.getString(state));
            }
            return false;
        }

        if (factoryId == -1) {
            // Take from centre pool
            moveTilesFromCenter(state);
        } else {
            // Take from factory
            moveTilesFromFactory(state);
        }

        // Check if round should end (no more valid moves possible)
        if (areFactoriesAndCenterEmpty(state)) {
            state.setPhase(AzulGameState.GamePhase.WALL_TILING);
            
            if (DEBUG_MODE) {
                System.out.println("All factories and center are empty - transitioning to wall tiling phase");
            }
        }

        return true;
    }

    private void moveTilesFromFactory(AzulGameState state) {
        GridBoard<AzulTile> factory = state.getFactories().get(factoryId);
        int matchingTileCount = 0;
        
        // Count matching tiles first
        for (int row = 0; row < factory.getHeight(); row++) {
            for (int col = 0; col < factory.getWidth(); col++) {
                AzulTile tile = factory.getElement(col, row);
                if (tile != null && tile.getTileType() == tileType) {
                    matchingTileCount++;
                }
            }
        }
        
        // If no matching tiles found, return false
        if (matchingTileCount == 0) {
            if (DEBUG_MODE) {
                System.out.println("No matching tiles found in factory " + factoryId);
            }
            return;
        }
        
        // If placing to pattern line, check if it can fit all tiles
        if (patternLineIdx != FLOOR_LINE) {
            PatternLine targetLine = state.getPatternLines(playerId)[patternLineIdx];
            int availableSpace = patternLineIdx + 1 - targetLine.getCount();
            
            // Move matching tiles to pattern line, excess to floor line
            for (int row = 0; row < factory.getHeight(); row++) {
                for (int col = 0; col < factory.getWidth(); col++) {
                    AzulTile tile = factory.getElement(col, row);
                    if (tile != null) {
                        if (tile.getTileType() == tileType) {
                            if (availableSpace > 0) {
                                targetLine.add(tile);
                                availableSpace--;
                            } else {
                                // Excess tiles go to floor line
                                state.getFloorLine(playerId).increment();
                                state.getDiscardBag().add(tile);
                            }
                        } else {
                            moveToCenter(state, tile);
                        }
                        factory.setElement(col, row, null);
                    }
                }
            }
        } else {
            // Move all matching tiles directly to floor line
            for (int row = 0; row < factory.getHeight(); row++) {
                for (int col = 0; col < factory.getWidth(); col++) {
                    AzulTile tile = factory.getElement(col, row);
                    if (tile != null) {
                        if (tile.getTileType() == tileType) {
                            state.getFloorLine(playerId).increment();
                            state.getDiscardBag().add(tile);
                        } else {
                            moveToCenter(state, tile);
                        }
                        factory.setElement(col, row, null);
                    }
                }
            }
        }
    }

    private void moveTilesFromCenter(AzulGameState state) {
        // Move matching tiles from center pool to pattern line or floor line
        List<AzulTile> centerPool = state.getCenterPool();
        List<AzulTile> tilesToMove = new ArrayList<>();
        
        // Check if this is the first take from center in this round
        if (state.isFirstPlayerTokenInCenter()) {
            state.removeFirstPlayerTokenFromCenter();
            state.getFloorLine(playerId).increment();
            state.setFirstPlayer(playerId);
            
            if (DEBUG_MODE) {
                System.out.println("Player " + playerId + " takes first player token");
            }
        }
        
        // Collect matching tiles
        Iterator<AzulTile> iterator = centerPool.iterator();
        while (iterator.hasNext()) {
            AzulTile tile = iterator.next();
            if (tile.getTileType() == tileType) {
                tilesToMove.add(tile);
                iterator.remove();
            }
        }
        
        // Move tiles to pattern line or floor line
        if (patternLineIdx >= 0) {
            // Move to pattern line
            PatternLine patternLine = state.getPatternLines(playerId)[patternLineIdx];
            int availableSpace = patternLineIdx + 1 - patternLine.getCount();
            
            for (AzulTile tile : tilesToMove) {
                if (availableSpace > 0) {
                    patternLine.add(tile);
                    availableSpace--;
                } else {
                    // Move excess tiles to floor line
                    state.getFloorLine(playerId).increment();
                    state.getDiscardBag().add(tile);
                }
            }
        } else {
            // Move all tiles to floor line
            for (AzulTile tile : tilesToMove) {
                state.getFloorLine(playerId).increment();
                state.getDiscardBag().add(tile);
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

        // Check centre pool - consider it empty if it only contains the first player token
        List<AzulTile> centerPool = state.getCenterPool();
        if (centerPool.isEmpty()) {
            return true;
        }
        
        // If there's only one tile and it's the first player token, consider it empty
        if (centerPool.size() == 1 && centerPool.get(0).getTileType() == null) {
            return true;
        }
        
        return false;
    }

    private boolean hasAnyTiles(GridBoard<AzulTile> board) {
        for (int i = 0; i < board.getHeight(); i++) {
            for (int j = 0; j < board.getWidth(); j++) {
                if (board.getElement(i, j) != null) return true;
            }
        }
        return false;
    }

    // Make isActionValid public so it can be accessed from outside
    public boolean isActionValid(AzulGameState state) {
        // Special case for dummy actions used in RMHC player
        if (factoryId == -1 && patternLineIdx == FLOOR_LINE && 
            (state.getPhase() == AzulGameState.GamePhase.WALL_TILING || 
             state.getGameStatus() != CoreConstants.GameResult.GAME_ONGOING)) {
            return true;
        }

        // Check if it's a valid player
        if (playerId < 0 || playerId >= state.getNPlayers()) {
            if (DEBUG_MODE) System.out.println("Invalid player ID: " + playerId);
            return false;
        }
        
        // Check if factory/centre has the requested tile type
        if (factoryId == -1) {
            // Check centre pool
            boolean hasTileType = false;
            for (AzulTile tile : state.getCenterPool()) {
                if (tile.getTileType() == tileType) {
                    hasTileType = true;
                    break;
                }
            }
            if (!hasTileType) {
                if (DEBUG_MODE) System.out.println("Centre pool doesn't have tile type: " + tileType);
                return false;
            }
        } else {
            // Check factory
            if (factoryId < 0 || factoryId >= state.getFactories().size()) {
                if (DEBUG_MODE) System.out.println("Invalid factory ID: " + factoryId);
                return false;
            }
            
            GridBoard<AzulTile> factory = state.getFactories().get(factoryId);
            boolean hasTileType = false;
            boolean hasAnyTiles = false;
            
            for (int row = 0; row < factory.getHeight(); row++) {
                for (int col = 0; col < factory.getWidth(); col++) {
                    AzulTile tile = factory.getElement(col, row);
                    if (tile != null) {
                        hasAnyTiles = true;
                        if (tile.getTileType() == tileType) {
                            hasTileType = true;
                            break;
                        }
                    }
                }
                if (hasTileType) break;
            }
            
            if (!hasAnyTiles) {
                if (DEBUG_MODE) System.out.println("Factory " + factoryId + " is empty");
                return false;
            }
            
            if (!hasTileType) {
                if (DEBUG_MODE) System.out.println("Factory " + factoryId + " doesn't have tile type: " + tileType);
                return false;
            }
        }
        
        // If placing to pattern line, check if it's valid
        if (patternLineIdx != FLOOR_LINE) {
            if (patternLineIdx < 0 || patternLineIdx >= state.getPatternLines(playerId).length) {
                if (DEBUG_MODE) System.out.println("Invalid pattern line index: " + patternLineIdx);
                return false;
            }
            
            PatternLine line = state.getPatternLines(playerId)[patternLineIdx];
            if (!line.canAdd(new AzulTile(tileType))) {
                if (DEBUG_MODE) System.out.println("Cannot add tile type " + tileType + " to pattern line " + patternLineIdx);
                return false;
            }
            
            // Check if the colour is already on the wall in that row
            Wall wall = state.getPlayerWall(playerId);
            int col = wall.getColumnForColor(patternLineIdx, tileType);
            
            if (col == -1) {
                if (DEBUG_MODE) System.out.println("Tile type " + tileType + " doesn't match any column in row " + patternLineIdx);
                return false;
            }
            
            if (wall.isTilePlaced(patternLineIdx, col)) {
                if (DEBUG_MODE) System.out.println("Tile of type " + tileType + " already placed on wall at row " + patternLineIdx);
                return false;
            }
        }
        
        return true;
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
        String source = factoryId == -1 ? "centre pool" : "factory " + factoryId;
        String destination = patternLineIdx == FLOOR_LINE ? "floor line" : "pattern line " + patternLineIdx;
        return String.format("Player %d takes %s tiles from %s to %s",
                playerId, tileType, source, destination);
    }

    // Getters
    public int getPlayerID() { return playerId; }
    public int getFactoryId() { return factoryId; }
    public int getPatternLineIdx() { return patternLineIdx; }
    public AzulTile.TileType getTileType() { return tileType; }
    
    // Static getter for floor line constant
    public static int getFloorLine() { return FLOOR_LINE; }
}