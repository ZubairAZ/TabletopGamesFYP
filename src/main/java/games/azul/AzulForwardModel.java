package games.azul;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.CoreConstants.GameResult;
import core.components.Counter;
import core.components.Deck;
import core.components.GridBoard;
import games.azul.actions.TakeTilesAction;
import games.azul.components.AzulTile;
import games.azul.components.PatternLine;
import games.azul.components.Wall;
import java.util.*;

public class AzulForwardModel extends AbstractForwardModel {
    // Debug flag - set to true to enable detailed debug output
    private static final boolean DEBUG_MODE = false;
    
    @Override
    protected void _setup(AbstractGameState firstState) {
        AzulGameState state = (AzulGameState) firstState;
        AzulGameParameters params = (AzulGameParameters) state.getGameParameters();

        // Clear existing components first
        state.getFactories().clear();
        state.getPatternLines().clear();
        state.getPlayerWalls().clear();
        state.getFloorLines().clear();
        state.getScores().clear();

        // Initialise player components
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.getPlayerWalls().add(new Wall());

            PatternLine[] lines = new PatternLine[5];
            for (int j = 0; j < 5; j++) {
                lines[j] = new PatternLine(j + 1);
            }
            state.getPatternLines().add(lines);

            Counter floorLine = new Counter(0, "Floor Line " + i);
            floorLine.setMinimum(0);
            floorLine.setMaximum(7);
            state.getFloorLines().add(floorLine);

            Counter score = new Counter("Score " + i);
            score.setMinimum(0);
            score.setMaximum(240);
            score.setValue(0);
            state.getScores().add(score);
        }

        // Set up factories
        int nFactories = (state.getNPlayers() * 2) + 1;
        for (int i = 0; i < nFactories; i++) {
            state.getFactories().add(new GridBoard<>(2, 2));
        }

        // Initialise centre pool
        state.setCenterPool(new ArrayList<>());

        // Reset and setup tile bags
        state.setTileBag(new Deck<>("Tile Bag", CoreConstants.VisibilityMode.HIDDEN_TO_ALL));
        state.setDiscardBag(new Deck<>("Discard Bag", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));

        // Initialise tile bag
        for (AzulTile.TileType type : AzulTile.TileType.values()) {
            for (int i = 0; i < params.getNTilesPerType(); i++) {
                state.getTileBag().add(new AzulTile(type));
            }
        }
        state.getTileBag().shuffle(state.getRnd());
        fillFactories((AzulGameState)firstState);
        
        // Print initial game state
        if (DEBUG_MODE) {
            System.out.println("\n===== GAME SETUP COMPLETE =====");
            state.printDebugInfo();
        }
    }

    private void fillFactories(AzulGameState state) {
        // Only add first player token if it's not already in the center
        if (!state.isFirstPlayerTokenInCenter()) {
            state.addFirstPlayerTokenToCenter();
        }
        
        // Fill each factory with tiles
        for (GridBoard<AzulTile> factory : state.getFactories()) {
            // Clear any remaining tiles first
            for (int row = 0; row < factory.getHeight(); row++) {
                for (int col = 0; col < factory.getWidth(); col++) {
                    factory.setElement(col, row, null);
                }
            }
            
            // Fill with new tiles if available
            for (int i = 0; i < 4; i++) {
                if (state.getTileBag().getSize() > 0) {
                    AzulTile tile = state.getTileBag().draw();
                    factory.setElement(i % 2, i / 2, tile);
                } else if (state.getDiscardBag().getSize() > 0) {
                    // If main bag is empty, shuffle discard bag into it
                    while (state.getDiscardBag().getSize() > 0) {
                        state.getTileBag().add(state.getDiscardBag().draw());
                    }
                    state.getTileBag().shuffle(state.getRnd());
                    
                    // Try to draw tile again
                    if (state.getTileBag().getSize() > 0) {
                        AzulTile tile = state.getTileBag().draw();
                        factory.setElement(i % 2, i / 2, tile);
                    }
                }
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        AzulGameState state = (AzulGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        int currentPlayer = gameState.getCurrentPlayer();

        // If the game is over, return a dummy action to prevent RMHC errors
        if (gameState.getGameStatus() != CoreConstants.GameResult.GAME_ONGOING) {
            if (DEBUG_MODE) {
                System.out.println("Game is over - returning dummy action");
            }
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), AzulTile.TileType.BLUE));
            return actions;
        }

        // Check if all factories and center are empty
        boolean allFactoriesEmpty = areFactoriesAndCenterEmpty(state);
        
        if (allFactoriesEmpty) {
            if (DEBUG_MODE) {
                System.out.println("All factories and center are empty - transitioning to wall tiling phase");
            }
            state.setPhase(AzulGameState.GamePhase.WALL_TILING);
            // Add a dummy action for the wall tiling phase
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), AzulTile.TileType.BLUE));
            return actions;
        }
        
        // If in wall tiling phase, return a dummy action
        if (state.getPhase() == AzulGameState.GamePhase.WALL_TILING) {
            if (DEBUG_MODE) {
                System.out.println("In wall tiling phase - returning dummy action");
            }
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), AzulTile.TileType.BLUE));
            return actions;
        }

        // Factory actions
        for (int f = 0; f < state.getFactories().size(); f++) {
            addFactoryActions(state, actions, f, currentPlayer);
        }

        // Centre pool actions
        addCenterPoolActions(state, actions, currentPlayer);

        // If no valid actions were found, add a dummy action to prevent errors
        if (actions.isEmpty()) {
            if (DEBUG_MODE) {
                System.out.println("No valid actions found - adding dummy action");
            }
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), AzulTile.TileType.BLUE));
        }

        return actions;
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        if (currentState.isActionInProgress()) return;

        AzulGameState state = (AzulGameState) currentState;
        if (DEBUG_MODE) {
            System.out.println("\n===== BEFORE ACTION EXECUTION =====");
            System.out.println("Current phase: " + state.getPhase());
            System.out.println("Current player: " + state.getCurrentPlayer());
            System.out.println("Action: " + action.toString());
        }
        
        // Handle wall tiling phase first
        if (state.getPhase() == AzulGameState.GamePhase.WALL_TILING) {
            // Move tiles from pattern lines to walls
            movePatternLinesToWalls(state);
            return;
        }
        
        // Handle tile picking phase
        if (action instanceof TakeTilesAction) {
            // Execute the action
            boolean success = action.execute(state);
            
            if (DEBUG_MODE) {
                System.out.println("\n===== AFTER TAKE TILES ACTION =====");
                System.out.println("Action: " + action.toString());
                System.out.println("Current phase: " + state.getPhase());
                state.printDebugInfo();
            }
            
            if (success) {
                // Check if all factories and centre are empty after action
                boolean allEmpty = areFactoriesAndCenterEmpty(state);
                
                if (allEmpty) {
                    if (DEBUG_MODE) {
                        System.out.println("All factories and center are empty - performing wall tiling");
                    }
                    
                    // Always transition to wall tiling phase when factories are empty
                    state.setPhase(AzulGameState.GamePhase.WALL_TILING);
                } else {
                    // End the current player's turn if the round is not over
                    endPlayerTurn(state);
                    
                    if (DEBUG_MODE) {
                        System.out.println("\n===== PLAYER TURN ENDED =====");
                        System.out.println("Next player: " + state.getCurrentPlayer());
                    }
                }
            }
        }
    }
    
    @Override
    protected void endPlayerTurn(AbstractGameState state) {
        state.setTurnOwner((state.getCurrentPlayer() + 1) % state.getNPlayers());
    }
    
    private void executeTakeTilesAction(AzulGameState state, TakeTilesAction action) {
        action.execute(state);
    }
    
    private int calculateFloorPenalty(int floorTiles) {
        // Official Azul penalties: -1, -1, -2, -2, -2, -3, -3
        // These are the individual penalties for each position
        int[] individualPenalties = {-1, -1, -2, -2, -2, -3, -3};
        
        // Calculate cumulative penalty based on number of tiles
        int totalPenalty = 0;
        for (int i = 0; i < Math.min(floorTiles, individualPenalties.length); i++) {
            totalPenalty += individualPenalties[i];
        }
        
        if (DEBUG_MODE) {
            System.out.println("Floor tiles: " + floorTiles + ", Total Penalty: " + totalPenalty);
        }
        
        return totalPenalty;
    }
    
    @Override
    protected AbstractForwardModel _copy() {
        return new AzulForwardModel();
    }

    // Helper method to check if a factory is empty
    private boolean isFactoryEmpty(GridBoard<AzulTile> factory) {
        for (int row = 0; row < factory.getHeight(); row++) {
            for (int col = 0; col < factory.getWidth(); col++) {
                if (factory.getElement(col, row) != null) {
                    return false;
                }
            }
        }
        return true;
    }
    
    // Move tiles from pattern lines to walls at the end of a round
    private void movePatternLinesToWalls(AzulGameState state) {
        if (DEBUG_MODE) {
            System.out.println("\n===== MOVING TILES TO WALLS =====");
            System.out.println("Current phase: " + state.getPhase());
            System.out.println("Current player: " + state.getCurrentPlayer());
        }

        // For each player
        for (int player = 0; player < state.getNPlayers(); player++) {
            Wall wall = state.getPlayerWall(player);
            PatternLine[] lines = state.getPatternLines(player);
            
            // Check each pattern line
            for (int row = 0; row < lines.length; row++) {
                PatternLine line = lines[row];
                
                // Only proceed if the pattern line is completely filled
                if (line.getCount() == row + 1) {
                    AzulTile.TileType tileType = line.getCurrentType();
                    
                    if (wall.canPlaceTile(row, tileType)) {
                        // Place tile on wall
                        wall.placeTile(row, tileType);
                        
                        // Calculate score for this tile
                        int col = wall.getColumnForColor(row, tileType);
                        int score = calculateTileScore(wall, row, col);
                        state.getScore(player).increment(score);
                        
                        if (DEBUG_MODE) {
                            System.out.println("Player " + player + " placed tile of type " + tileType + 
                                            " at row " + row + ", col " + col + " for " + score + " points");
                        }
                        
                        // Get all tiles from the pattern line and discard them
                        AzulTile[] tiles = line.getTiles();
                        for (AzulTile tile : tiles) {
                            if (tile != null) {
                                state.getDiscardBag().add(tile);
                            }
                        }
                        
                        // Clear the pattern line
                        line.clear();
                    }
                }
            }
            
            // Apply floor line penalties
            int floorPenalty = calculateFloorPenalty(state.getFloorLine(player).getValue());
            state.getScore(player).increment(floorPenalty);
            
            if (DEBUG_MODE) {
                System.out.println("Player " + player + " floor line penalty: " + floorPenalty);
            }
            
            // Clear floor line
            state.getFloorLine(player).setValue(0);
        }
        
        // Check if any player has completed a row AND we're at the end of a round
        boolean gameOver = false;
        if (areFactoriesAndCenterEmpty(state)) {
            for (Wall wall : state.getPlayerWalls()) {
                if (wall.getCompletedRows() > 0) {
                    gameOver = true;
                    break;
                }
            }
        }
        
        if (gameOver) {
            if (DEBUG_MODE) {
                System.out.println("Game over - a player has completed a row and round is finished");
            }
            calculateFinalScores(state);
            state.setGameStatus(CoreConstants.GameResult.GAME_END);
        } else {
            if (DEBUG_MODE) {
                System.out.println("No completed rows or round not finished - preparing for next round");
            }
            prepareNextRound(state);
        }
    }
    
    private int calculateTileScore(Wall wall, int row, int col) {
        int points = 1;
        points += countAdjacentTiles(wall, row, col, true);
        points += countAdjacentTiles(wall, row, col, false);
        // Always return at least 1 point for a placed tile
        return Math.max(1, points);
    }
    
    private int countAdjacentTiles(Wall wall, int row, int col, boolean horizontal) {
        int count = 0;
        if (horizontal) {
            for (int c = col - 1; c >= 0 && wall.isTilePlaced(row, c); c--) count++;
            for (int c = col + 1; c < 5 && wall.isTilePlaced(row, c); c++) count++;
        } else {
            for (int r = row - 1; r >= 0 && wall.isTilePlaced(r, col); r--) count++;
            for (int r = row + 1; r < 5 && wall.isTilePlaced(r, col); r++) count++;
        }
        return count;
    }
    
    // Calculate final scores at the end of the game
    private void calculateFinalScores(AzulGameState state) {
        AzulGameParameters params = (AzulGameParameters) state.getGameParameters();
        
        for (int player = 0; player < state.getNPlayers(); player++) {
            Wall wall = state.getPlayerWall(player);
            Counter score = state.getScore(player);
            
            // Add bonus points for completed rows (2 points each)
            int completedRows = wall.getCompletedRows();
            score.increment(completedRows * params.getRowBonusPoints());
            
            // Add bonus points for completed columns (7 points each)
            int completedColumns = wall.getCompletedColumns();
            score.increment(completedColumns * params.getColumnBonusPoints());
            
            // Add bonus points for completed colour sets (10 points each)
            int completedSets = wall.getCompletedColorSets();
            score.increment(completedSets * params.getColorSetBonusPoints());
            
            if (DEBUG_MODE) {
                System.out.println("Player " + player + " final score breakdown:");
                System.out.println("  Completed rows bonus (" + params.getRowBonusPoints() + " each): " + (completedRows * params.getRowBonusPoints()));
                System.out.println("  Completed columns bonus (" + params.getColumnBonusPoints() + " each): " + (completedColumns * params.getColumnBonusPoints()));
                System.out.println("  Completed colour sets bonus (" + params.getColorSetBonusPoints() + " each): " + (completedSets * params.getColorSetBonusPoints()));
                System.out.println("  Total score: " + score.getValue());
            }
        }
        
        // Determine winner(s)
        int maxScore = -1;
        List<Integer> winners = new ArrayList<>();
        
        for (int player = 0; player < state.getNPlayers(); player++) {
            int playerScore = state.getScore(player).getValue();
            if (playerScore > maxScore) {
                maxScore = playerScore;
                winners.clear();
                winners.add(player);
            } else if (playerScore == maxScore) {
                winners.add(player);
            }
        }
        
        // Set game results
        if (winners.size() == 1) {
            state.setPlayerResult(GameResult.WIN_GAME, winners.get(0));
            for (int player = 0; player < state.getNPlayers(); player++) {
                if (player != winners.get(0)) {
                    state.setPlayerResult(GameResult.LOSE_GAME, player);
                }
            }
        } else {
            // Handle ties
            for (int player : winners) {
                state.setPlayerResult(GameResult.DRAW_GAME, player);
            }
            for (int player = 0; player < state.getNPlayers(); player++) {
                if (!winners.contains(player)) {
                    state.setPlayerResult(GameResult.LOSE_GAME, player);
                }
            }
        }
    }
    
    // Prepare for the next round
    private void prepareNextRound(AzulGameState state) {
        // Reset to tile picking phase
        state.setPhase(AzulGameState.GamePhase.TILE_PICKING);
        
        // Set the current player to the first player
        state.setTurnOwner(state.getFirstPlayer());
        
        if (DEBUG_MODE) {
            System.out.println("Setting first player for new round to: " + state.getFirstPlayer());
        }
        
        // Check if we have any tiles available
        int totalTilesAvailable = state.getTileBag().getSize() + state.getDiscardBag().getSize();
        
        if (totalTilesAvailable == 0) {
            if (DEBUG_MODE) {
                System.out.println("No tiles available to continue the game");
                System.out.println("Tile bag: " + state.getTileBag().getSize());
                System.out.println("Discard bag: " + state.getDiscardBag().getSize());
            }
            // End the game only if we have no tiles at all
            state.setGameStatus(CoreConstants.GameResult.GAME_END);
            return;
        }
        
        // Fill factories with available tiles (can be incomplete)
        fillFactories(state);
        
        if (DEBUG_MODE) {
            System.out.println("New round prepared");
            System.out.println("Tile bag: " + state.getTileBag().getSize());
            System.out.println("Discard bag: " + state.getDiscardBag().getSize());
        }
    }

    // Helper method to check if all factories and center pool are empty
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
        return centerPool.size() == 1 && centerPool.get(0).getTileType() == null;
    }

    private boolean hasAnyTiles(GridBoard<AzulTile> board) {
        for (int i = 0; i < board.getHeight(); i++) {
            for (int j = 0; j < board.getWidth(); j++) {
                if (board.getElement(i, j) != null) return true;
            }
        }
        return false;
    }

    private void addFactoryActions(AzulGameState state, List<AbstractAction> actions, int factoryId, int currentPlayer) {
        GridBoard<AzulTile> factory = state.getFactories().get(factoryId);
        Set<AzulTile.TileType> availableTypes = new HashSet<>();
        Wall playerWall = state.getPlayerWall(currentPlayer);

        // Collect available tile types
        for (int i = 0; i < factory.getHeight(); i++) {
            for (int j = 0; j < factory.getWidth(); j++) {
                AzulTile tile = factory.getElement(i, j);
                if (tile != null) availableTypes.add(tile.getTileType());
            }
        }

        // Create actions for each type and valid pattern line
        for (AzulTile.TileType type : availableTypes) {
            // Add actions for pattern lines
            PatternLine[] playerLines = state.getPatternLines(currentPlayer);
            
            for (int line = 0; line < playerLines.length; line++) {
                PatternLine patternLine = playerLines[line];
                
                // Check if this tile can be placed on the wall in this row
                int col = playerWall.getColumnForColor(line, type);
                if (col != -1 && !playerWall.isTilePlaced(line, col)) {
                    // A pattern line can accept tiles if:
                    // 1. It's empty, or
                    // 2. It has the same type of tiles and isn't full
                    if (patternLine.getCount() == 0 || 
                        (patternLine.getCurrentType() == type && patternLine.getCount() < line + 1)) {
                        actions.add(new TakeTilesAction(currentPlayer, factoryId, line, type));
                    }
                }
            }
            
            // Always add floor line action as an option
            actions.add(new TakeTilesAction(currentPlayer, factoryId, TakeTilesAction.getFloorLine(), type));
        }
    }
    
    private void addCenterPoolActions(AzulGameState state, List<AbstractAction> actions, int currentPlayer) {
        List<AzulTile> centerPool = state.getCenterPool();
        Set<AzulTile.TileType> availableTypes = new HashSet<>();
        Wall playerWall = state.getPlayerWall(currentPlayer);

        // Collect available tile types from centre
        for (AzulTile tile : centerPool) {
            if (tile != null && tile.getTileType() != null) {
                availableTypes.add(tile.getTileType());
            }
        }

        // Create actions for each type and valid pattern line
        for (AzulTile.TileType type : availableTypes) {
            // Add actions for pattern lines
            PatternLine[] playerLines = state.getPatternLines(currentPlayer);
            
            for (int line = 0; line < playerLines.length; line++) {
                PatternLine patternLine = playerLines[line];
                
                // Check if this tile can be placed on the wall in this row
                int col = playerWall.getColumnForColor(line, type);
                if (col != -1 && !playerWall.isTilePlaced(line, col)) {
                    // A pattern line can accept tiles if:
                    // 1. It's empty, or
                    // 2. It has the same type of tiles and isn't full
                    if (patternLine.getCount() == 0 || 
                        (patternLine.getCurrentType() == type && patternLine.getCount() < line + 1)) {
                        actions.add(new TakeTilesAction(currentPlayer, -1, line, type));
                    }
                }
            }
            
            // Always add floor line action as an option
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), type));
        }
    }
}