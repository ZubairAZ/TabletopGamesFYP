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
    private static final boolean DEBUG_MODE = true;
    
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
        for (int i = 0; i < state.getFactories().size(); i++) {
            GridBoard<AzulTile> factory = state.getFactories().get(i);
            for (int j = 0; j < params.getNTilesPerFactory(); j++) {
                if (state.getTileBag().getSize() > 0) {
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
        }
        
        // Add first player token to centre pool at the beginning of each round
        state.addFirstPlayerTokenToCenter();
        
        if (DEBUG_MODE) {
            System.out.println("\n===== FACTORIES FILLED =====");
            System.out.println("First player token added to centre pool");
            state.printDebugInfo();
        }
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        if (currentState.isActionInProgress()) return;

        AzulGameState state = (AzulGameState) currentState;
        if (action instanceof TakeTilesAction) {
            executeTakeTilesAction(state, (TakeTilesAction) action);
            
            if (DEBUG_MODE) {
                System.out.println("\n===== AFTER TAKE TILES ACTION =====");
                System.out.println("Action: " + action.toString());
                state.printDebugInfo();
            }
            
            // Check if all factories and centre are empty
            boolean allEmpty = true;
            for (GridBoard<AzulTile> factory : state.getFactories()) {
                if (!isFactoryEmpty(factory)) {
                    allEmpty = false;
                    break;
                }
            }
            
            if (allEmpty && state.getCenterPool().isEmpty()) {
                // End of round, move tiles from pattern lines to walls
                movePatternLinesToWalls(state);
                
                if (DEBUG_MODE) {
                    System.out.println("\n===== END OF ROUND - AFTER MOVING TILES TO WALLS =====");
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
                        state.printDebugInfo();
                    }
                    
                    // Set game status to game over
                    state.setGameStatus(CoreConstants.GameResult.GAME_END);
                } else {
                    // Prepare for next round
                    prepareNextRound(state);
                    
                    if (DEBUG_MODE) {
                        System.out.println("\n===== NEW ROUND PREPARED =====");
                        state.printDebugInfo();
                    }
                }
            } else {
                // End the current player's turn if the round is not over
                endPlayerTurn(state);
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
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        AzulGameState state = (AzulGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        int currentPlayer = gameState.getCurrentPlayer();

        // Factory actions
        for (int f = 0; f < state.getFactories().size(); f++) {
            addFactoryActions(state, actions, f, currentPlayer);
        }

        // Centre pool actions
        addCenterPoolActions(state, actions, currentPlayer);

        return actions;
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
        for (int player = 0; player < state.getNPlayers(); player++) {
            PatternLine[] lines = state.getPatternLines(player);
            Wall wall = state.getPlayerWall(player);

            // Score completed pattern lines
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].isFull()) {
                    AzulTile.TileType type = lines[i].getCurrentType();
                    if (wall.canPlaceTile(i, type)) {
                        wall.placeTile(i, type);
                        scoreWallPlacement(state, player, i, wall.getColumnForColor(i, type));
                        lines[i].clear();
                    }
                }
            }

            // Apply floor penalties
            applyFloorPenalties(state, player);
        }
    }
    
    private void scoreWallPlacement(AzulGameState state, int player, int row, int col) {
        int points = 1;
        points += countAdjacentTiles(state.getPlayerWall(player), row, col, true);
        points += countAdjacentTiles(state.getPlayerWall(player), row, col, false);
        if (points > 1) {  // Only add points if there are adjacent tiles
            state.getScore(player).increment(points);
        }
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
    
    private void applyFloorPenalties(AzulGameState state, int player) {
        int floorTiles = state.getFloorLine(player).getValue();
        int penalty = calculateFloorPenalty(floorTiles);
        state.getScore(player).increment(penalty);
        state.getFloorLine(player).setValue(0);
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
        
        // Move first player token to the player who took the first player marker
        int firstPlayer = state.getFirstPlayer();
        state.setTurnOwner(firstPlayer);
        
        if (DEBUG_MODE) {
            System.out.println("Setting first player for new round to: " + firstPlayer);
        }
        
        // Refill factories from tile bag, using discard bag if needed
        if (state.getTileBag().getSize() < state.getFactories().size() * 4) {
            // Move tiles from discard bag to tile bag if needed
            while (state.getDiscardBag().getSize() > 0) {
                state.getTileBag().add(state.getDiscardBag().draw());
            }
            state.getTileBag().shuffle(state.getRnd());
        }
        
        // Fill factories with tiles
        fillFactories(state);
    }
}