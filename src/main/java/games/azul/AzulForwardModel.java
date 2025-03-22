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
            score.setMaximum(100);
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
        AzulGameParameters params = (AzulGameParameters) state.getGameParameters();
        int totalTilesAvailable = state.getTileBag().getSize() + state.getDiscardBag().getSize();
        
        if (DEBUG_MODE) {
            System.out.println("Total tiles available: " + totalTilesAvailable);
        }
        
        // Move tiles from discard bag to tile bag if needed
        if (state.getTileBag().getSize() < totalTilesAvailable) {
            if (DEBUG_MODE) {
                System.out.println("Moving tiles from discard bag to tile bag");
            }
            while (state.getDiscardBag().getSize() > 0) {
                state.getTileBag().add(state.getDiscardBag().draw());
            }
            state.getTileBag().shuffle(state.getRnd());
        }
        
        // Fill factories with available tiles
        int tilesPerFactory = params.getNTilesPerFactory();
        int totalFactories = state.getFactories().size();
        int totalTilesNeeded = totalFactories * tilesPerFactory;
        
        if (DEBUG_MODE) {
            System.out.println("Tiles per factory: " + tilesPerFactory);
            System.out.println("Total factories: " + totalFactories);
            System.out.println("Total tiles needed: " + totalTilesNeeded);
        }
        
        // Calculate how many complete factories we can fill
        int completeFactories = Math.min(totalFactories, totalTilesAvailable / tilesPerFactory);
        int remainingTiles = totalTilesAvailable % tilesPerFactory;
        
        if (DEBUG_MODE) {
            System.out.println("Complete factories to fill: " + completeFactories);
            System.out.println("Remaining tiles: " + remainingTiles);
        }
        
        // Fill complete factories first
        for (int i = 0; i < completeFactories; i++) {
            GridBoard<AzulTile> factory = state.getFactories().get(i);
            for (int j = 0; j < tilesPerFactory; j++) {
                for (int row = 0; row < factory.getHeight(); row++) {
                    for (int col = 0; col < factory.getWidth(); col++) {
                        if (factory.getElement(col, row) == null) {
                            factory.setElement(col, row, state.getTileBag().draw());
                            break;
                        }
                    }
                }
            }
        }
        
        // Fill remaining tiles in the next factory if any
        if (remainingTiles > 0 && completeFactories < totalFactories) {
            GridBoard<AzulTile> factory = state.getFactories().get(completeFactories);
            for (int j = 0; j < remainingTiles; j++) {
                for (int row = 0; row < factory.getHeight(); row++) {
                    for (int col = 0; col < factory.getWidth(); col++) {
                        if (factory.getElement(col, row) == null) {
                            factory.setElement(col, row, state.getTileBag().draw());
                            break;
                        }
                    }
                }
            }
        }
        
        if (DEBUG_MODE) {
            System.out.println("\n===== FACTORIES FILLED =====");
            state.printDebugInfo();
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
            // Don't add any actions when transitioning to wall tiling phase
            return actions;
        }
        
        // If in wall tiling phase, return a dummy action to prevent RMHC errors
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
            
            if (DEBUG_MODE) {
                System.out.println("\n===== END OF ROUND - AFTER MOVING TILES TO WALLS =====");
                System.out.println("Current phase: " + state.getPhase());
                state.printDebugInfo();
            }
            
            // Check if game is over (any player has a complete row)
            boolean gameOver = false;
            for (Wall wall : state.getPlayerWalls()) {
                if (wall.getCompletedRows() > 0) {
                    gameOver = true;
                    break;
                }
            }
            
            if (gameOver) {
                // Game is over, calculate final scores
                calculateFinalScores(state);
                
                if (DEBUG_MODE) {
                    System.out.println("\n===== GAME OVER - FINAL SCORES =====");
                    System.out.println("Setting game status to GAME_END");
                    state.printDebugInfo();
                }
                
                // Set game status to game over
                state.setGameStatus(CoreConstants.GameResult.GAME_END);
            } else {
                // Prepare for next round
                prepareNextRound(state);
                
                if (DEBUG_MODE) {
                    System.out.println("\n===== NEW ROUND PREPARED =====");
                    System.out.println("Current phase: " + state.getPhase());
                    System.out.println("Current player: " + state.getCurrentPlayer());
                    state.printDebugInfo();
                }
            }
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
                        System.out.println("All factories and center are empty - performing wall tiling immediately");
                    }
                    
                    // Check if there are any tiles in pattern lines that can be moved to walls
                    boolean hasTilesToMove = false;
                    for (int player = 0; player < state.getNPlayers(); player++) {
                        PatternLine[] lines = state.getPatternLines(player);
                        for (PatternLine line : lines) {
                            if (line.getCount() > 0) {
                                hasTilesToMove = true;
                                break;
                            }
                        }
                        if (hasTilesToMove) break;
                    }
                    
                    if (hasTilesToMove) {
                        // Perform wall tiling immediately
                        movePatternLinesToWalls(state);
                        
                        // Check if game is over
                        boolean gameOver = false;
                        for (Wall wall : state.getPlayerWalls()) {
                            if (wall.getCompletedRows() > 0) {
                                gameOver = true;
                                break;
                            }
                        }
                        
                        if (gameOver) {
                            calculateFinalScores(state);
                            state.setGameStatus(CoreConstants.GameResult.GAME_END);
                        } else {
                            prepareNextRound(state);
                        }
                    } else {
                        // No tiles to move to walls, prepare for next round immediately
                        if (DEBUG_MODE) {
                            System.out.println("No tiles to move to walls - preparing for next round");
                        }
                        prepareNextRound(state);
                    }
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

        // Move tiles from pattern lines to walls for each player
        for (int player = 0; player < state.getNPlayers(); player++) {
            PatternLine[] lines = state.getPatternLines(player);
            Wall wall = state.getPlayerWall(player);
            
            for (int row = 0; row < lines.length; row++) {
                PatternLine line = lines[row];
                if (line.getCount() > 0) {
                    // Get the column for this tile type in this row
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
                        
                        // Get all tiles from the pattern line and discard them except the one placed
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
            int floorPenalty = state.getFloorLine(player).getValue() * 
                             ((AzulGameParameters)state.getGameParameters()).getPenaltyPerFloorTile();
            state.getScore(player).increment(floorPenalty);
            
            if (DEBUG_MODE) {
                System.out.println("Player " + player + " floor line penalty: " + floorPenalty);
            }
            
            // Clear floor line
            state.getFloorLine(player).setValue(0);
        }
        
        // Check if any player has completed a row
        boolean gameOver = false;
        for (Wall wall : state.getPlayerWalls()) {
            if (wall.getCompletedRows() > 0) {
                gameOver = true;
                break;
            }
        }
        
        if (gameOver) {
            if (DEBUG_MODE) {
                System.out.println("Game over - a player has completed a row");
            }
            state.setGameStatus(CoreConstants.GameResult.GAME_END);
        } else {
            if (DEBUG_MODE) {
                System.out.println("No completed rows - preparing for next round");
            }
            prepareNextRound(state);
        }
    }
    
    private int calculateTileScore(Wall wall, int row, int col) {
        int points = 1;
        points += countAdjacentTiles(wall, row, col, true);
        points += countAdjacentTiles(wall, row, col, false);
        return points > 1 ? points : 0;
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
            
            // Add bonus points for completed rows
            int completedRows = wall.getCompletedRows();
            score.increment(completedRows * params.getBonusPoints());
            
            // Add bonus points for completed columns
            int completedColumns = wall.getCompletedColumns();
            score.increment(completedColumns * params.getBonusPoints());
            
            // Add bonus points for completed colour sets
            int completedSets = wall.getCompletedColorSets();
            score.increment(completedSets * params.getBonusPoints());
            
            if (DEBUG_MODE) {
                System.out.println("Player " + player + " final score breakdown:");
                System.out.println("  Completed rows bonus: " + (completedRows * params.getBonusPoints()));
                System.out.println("  Completed columns bonus: " + (completedColumns * params.getBonusPoints()));
                System.out.println("  Completed colour sets bonus: " + (completedSets * params.getBonusPoints()));
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

    private void addFactoryActions(AzulGameState state, List<AbstractAction> actions, int factoryId, int currentPlayer) {
        GridBoard<AzulTile> factory = state.getFactories().get(factoryId);
        Set<AzulTile.TileType> availableTypes = new HashSet<>();

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
            Wall playerWall = state.getPlayerWall(currentPlayer);
            
            for (int line = 0; line < state.getPatternLines(currentPlayer).length; line++) {
                PatternLine patternLine = state.getPatternLines(currentPlayer)[line];
                
                // Check if the pattern line can accept this tile type
                if (patternLine.canAdd(new AzulTile(type))) {
                    // Check if the corresponding wall spot is already filled
                    int col = playerWall.getColumnForColor(line, type);
                    if (col != -1 && !playerWall.isTilePlaced(line, col)) {
                        actions.add(new TakeTilesAction(currentPlayer, factoryId, line, type));
                    }
                }
            }
            
            // Add action for floor line (always an option)
            actions.add(new TakeTilesAction(currentPlayer, factoryId, TakeTilesAction.getFloorLine(), type));
        }
    }
    
    private void addCenterPoolActions(AzulGameState state, List<AbstractAction> actions, int currentPlayer) {
        List<AzulTile> centerPool = state.getCenterPool();
        Set<AzulTile.TileType> availableTypes = new HashSet<>();

        // Collect available tile types from centre
        for (AzulTile tile : centerPool) {
            if (tile != null && tile.getTileType() != null) {
                availableTypes.add(tile.getTileType());
            }
        }

        // Create actions for each type and valid pattern line
        for (AzulTile.TileType type : availableTypes) {
            // Add actions for pattern lines
            Wall playerWall = state.getPlayerWall(currentPlayer);
            
            for (int line = 0; line < state.getPatternLines(currentPlayer).length; line++) {
                PatternLine patternLine = state.getPatternLines(currentPlayer)[line];
                
                // Check if the pattern line can accept this tile type
                if (patternLine.canAdd(new AzulTile(type))) {
                    // Check if the corresponding wall spot is already filled
                    int col = playerWall.getColumnForColor(line, type);
                    if (col != -1 && !playerWall.isTilePlaced(line, col)) {
                        actions.add(new TakeTilesAction(currentPlayer, -1, line, type));
                    }
                }
            }
            
            // Add action for floor line (always an option)
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), type));
        }
    }
}