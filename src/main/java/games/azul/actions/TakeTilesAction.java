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
    private final int factoryId;  // -1 for centre pool
    private final int patternLineIdx;  // -1 for floor line
    private final AzulTile.TileType tileType;
    private static final int FLOOR_LINE = -1;
    private static final boolean DEBUG_MODE = true;

    public TakeTilesAction(int playerId, int factoryId, int patternLineIdx, AzulTile.TileType tileType) {
        this.playerId = playerId;
        this.factoryId = factoryId;
        this.patternLineIdx = patternLineIdx;
        this.tileType = tileType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        AzulGameState state = (AzulGameState) gs;

        if (!isActionValid(state)) {
            return false;
        }

        if (factoryId == -1) {
            // Check if first player token is in centre before we start
            boolean hasFirstPlayerToken = state.isFirstPlayerTokenInCenter();
            
            // Take from centre pool
            moveTilesFromCenter(state);
            
            // If the first player token was in the centre, take it
            if (hasFirstPlayerToken) {
                // Remove first player token from centre and add to floor line
                state.removeFirstPlayerTokenFromCenter();
                state.getFloorLine(playerId).increment();
                state.setFirstPlayer(playerId);
                
                if (DEBUG_MODE) {
                    System.out.println("Player " + playerId + " took the first player token");
                    System.out.println("First player for next round set to: " + playerId);
                    System.out.println("Floor line count after adding token: " + state.getFloorLine(playerId).getValue());
                }
            }
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
        int matchingTileCount = 0;
        
        // Count matching tiles first
        for (int i = 0; i < factory.getHeight(); i++) {
            for (int j = 0; j < factory.getWidth(); j++) {
                AzulTile tile = factory.getElement(i, j);
                if (tile != null && tile.getTileType() == tileType) {
                    matchingTileCount++;
                }
            }
        }
        
        // If placing to pattern line, check if it can fit all tiles
        if (patternLineIdx != FLOOR_LINE) {
            PatternLine targetLine = state.getPatternLines(playerId)[patternLineIdx];
            int availableSpace = patternLineIdx + 1 - targetLine.getCount();
            
            // Move matching tiles to pattern line, excess to floor line
            for (int i = 0; i < factory.getHeight(); i++) {
                for (int j = 0; j < factory.getWidth(); j++) {
                    AzulTile tile = factory.getElement(i, j);
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
                        factory.setElement(i, j, null);
                    }
                }
            }
        } else {
            // Move all matching tiles directly to floor line
            for (int i = 0; i < factory.getHeight(); i++) {
                for (int j = 0; j < factory.getWidth(); j++) {
                    AzulTile tile = factory.getElement(i, j);
                    if (tile != null) {
                        if (tile.getTileType() == tileType) {
                            state.getFloorLine(playerId).increment();
                            state.getDiscardBag().add(tile);
                        } else {
                            moveToCenter(state, tile);
                        }
                        factory.setElement(i, j, null);
                    }
                }
            }
        }
    }

    private void moveTilesFromCenter(AzulGameState state) {
        List<AzulTile> centre = state.getCenterPool();
        int matchingTileCount = 0;
        
        // Count matching tiles first
        for (AzulTile tile : centre) {
            if (tile.getTileType() == tileType) {
                matchingTileCount++;
            }
        }
        
        // If placing to pattern line, check if it can fit all tiles
        if (patternLineIdx != FLOOR_LINE) {
            PatternLine targetLine = state.getPatternLines(playerId)[patternLineIdx];
            int availableSpace = patternLineIdx + 1 - targetLine.getCount();
            
            // Remove matching tiles and add to pattern line or floor line
            Iterator<AzulTile> iterator = centre.iterator();
            while (iterator.hasNext()) {
                AzulTile tile = iterator.next();
                // Skip the first player token (it's handled separately)
                if (tile.getTileType() == null) {
                    continue;
                }
                
                if (tile.getTileType() == tileType) {
                    if (availableSpace > 0) {
                        targetLine.add(tile);
                        availableSpace--;
                    } else {
                        // Excess tiles go to floor line
                        state.getFloorLine(playerId).increment();
                        state.getDiscardBag().add(tile);
                    }
                    iterator.remove();
                }
            }
        } else {
            // Move all matching tiles directly to floor line
            Iterator<AzulTile> iterator = centre.iterator();
            while (iterator.hasNext()) {
                AzulTile tile = iterator.next();
                // Skip the first player token (it's handled separately)
                if (tile.getTileType() == null) {
                    continue;
                }
                
                if (tile.getTileType() == tileType) {
                    state.getFloorLine(playerId).increment();
                    state.getDiscardBag().add(tile);
                    iterator.remove();
                }
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

        // Check centre pool
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
            for (int i = 0; i < factory.getHeight(); i++) {
                for (int j = 0; j < factory.getWidth(); j++) {
                    AzulTile tile = factory.getElement(i, j);
                    if (tile != null && tile.getTileType() == tileType) {
                        hasTileType = true;
                        break;
                    }
                }
                if (hasTileType) break;
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