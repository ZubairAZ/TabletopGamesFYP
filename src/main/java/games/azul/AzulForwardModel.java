package games.azul;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.CoreConstants.GameResult;
import core.components.*;
import games.azul.actions.TakeTilesAction;
import games.azul.components.*;
import java.util.*;

public class AzulForwardModel extends StandardForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        AzulGameState state = (AzulGameState) firstState;
        AzulGameParameters params = (AzulGameParameters) state.getGameParameters();

        state.getFactories().clear();
        state.getPatternLines().clear();
        state.getPlayerWalls().clear();
        state.getFloorLines().clear();
        state.getScores().clear();

        for (int i = 0; i < state.getNPlayers(); i++) {
            state.getPlayerWalls().add(new Wall());
            PatternLine[] lines = new PatternLine[5];
            for (int j = 0; j < 5; j++) lines[j] = new PatternLine(j + 1);
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

        int nFactories = (state.getNPlayers() * 2) + 1;
        for (int i = 0; i < nFactories; i++) state.getFactories().add(new GridBoard<>(2, 2));

        state.setCenterPool(new ArrayList<>());
        state.setTileBag(new Deck<>("Tile Bag", CoreConstants.VisibilityMode.HIDDEN_TO_ALL));
        state.setDiscardBag(new Deck<>("Discard Bag", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));

        for (AzulTile.TileType type : AzulTile.TileType.values()) {
            for (int i = 0; i < params.getNTilesPerType(); i++) {
                state.getTileBag().add(new AzulTile(type));
            }
        }
        state.getTileBag().shuffle(state.getRnd());
        fillFactories(state);
        
        // Initialize game phase
        state.setPhase(AzulGameState.GamePhase.TILE_PICKING);
    }

    private void fillFactories(AzulGameState state) {
        if (!state.isFirstPlayerTokenInCenter()) state.addFirstPlayerTokenToCenter();
        
        for (GridBoard<AzulTile> factory : state.getFactories()) {
            for (int row = 0; row < factory.getHeight(); row++)
                for (int col = 0; col < factory.getWidth(); col++)
                    factory.setElement(col, row, null);
            
            for (int i = 0; i < 4; i++) {
                if (state.getTileBag().getSize() > 0) {
                    factory.setElement(i % 2, i / 2, state.getTileBag().draw());
                } else if (state.getDiscardBag().getSize() > 0) {
                    while (state.getDiscardBag().getSize() > 0)
                        state.getTileBag().add(state.getDiscardBag().draw());
                    state.getTileBag().shuffle(state.getRnd());
                    if (state.getTileBag().getSize() > 0)
                        factory.setElement(i % 2, i / 2, state.getTileBag().draw());
                }
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        AzulGameState state = (AzulGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        int currentPlayer = gameState.getCurrentPlayer();

        if (gameState.getGameStatus() != CoreConstants.GameResult.GAME_ONGOING ||
            state.getPhase() == AzulGameState.GamePhase.WALL_TILING ||
            areFactoriesAndCenterEmpty(state)) {
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), AzulTile.TileType.BLUE));
            if (areFactoriesAndCenterEmpty(state)) state.setPhase(AzulGameState.GamePhase.WALL_TILING);
            return actions;
        }

        for (int f = 0; f < state.getFactories().size(); f++) addFactoryActions(state, actions, f, currentPlayer);
        addCenterPoolActions(state, actions, currentPlayer);

        if (actions.isEmpty())
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), AzulTile.TileType.BLUE));

        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        AzulGameState state = (AzulGameState) currentState;
        if (actionTaken instanceof TakeTilesAction) {
            // Check if factories and center are empty
            if (areFactoriesAndCenterEmpty(state)) {
                state.setPhase(AzulGameState.GamePhase.WALL_TILING);
                movePatternLinesToWalls(state);
            } else {
                endPlayerTurn(state, (state.getCurrentPlayer() + 1) % state.getNPlayers());
            }
        }
    }
    
    private int calculateFloorPenalty(int floorTiles) {
        int[] penalties = {-1, -1, -2, -2, -2, -3, -3};
        int totalPenalty = 0;
        for (int i = 0; i < Math.min(floorTiles, penalties.length); i++)
            totalPenalty += penalties[i];
        return totalPenalty;
    }

    private boolean isFactoryEmpty(GridBoard<AzulTile> factory) {
        for (int row = 0; row < factory.getHeight(); row++)
            for (int col = 0; col < factory.getWidth(); col++)
                if (factory.getElement(col, row) != null) return false;
        return true;
    }
    
    private void movePatternLinesToWalls(AzulGameState state) {
        for (int player = 0; player < state.getNPlayers(); player++) {
            Wall wall = state.getPlayerWall(player);
            PatternLine[] lines = state.getPatternLines(player);
            
            for (int row = 0; row < lines.length; row++) {
                PatternLine line = lines[row];
                if (line.getCount() == row + 1) {
                    AzulTile.TileType tileType = line.getCurrentType();
                    if (wall.canPlaceTile(row, tileType)) {
                        wall.placeTile(row, tileType);
                        int col = wall.getColumnForColor(row, tileType);
                        state.getScore(player).increment(calculateTileScore(wall, row, col));
                        for (AzulTile tile : line.getTiles())
                            if (tile != null) state.getDiscardBag().add(tile);
                        line.clear();
                    }
                }
            }
            
            state.getScore(player).increment(calculateFloorPenalty(state.getFloorLine(player).getValue()));
            state.getFloorLine(player).setValue(0);
        }
        
        if (areFactoriesAndCenterEmpty(state)) {
            boolean gameOver = state.getPlayerWalls().stream().anyMatch(w -> w.getCompletedRows() > 0);
            if (gameOver) {
                calculateFinalScores(state);
                state.setGameStatus(CoreConstants.GameResult.GAME_END);
            } else {
                prepareNextRound(state);
            }
        }
    }
    
    private int calculateTileScore(Wall wall, int row, int col) {
        return 1 + countAdjacentTiles(wall, row, col, true) + countAdjacentTiles(wall, row, col, false);
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
    
    private void calculateFinalScores(AzulGameState state) {
        AzulGameParameters params = (AzulGameParameters) state.getGameParameters();
        
        for (int player = 0; player < state.getNPlayers(); player++) {
            Wall wall = state.getPlayerWall(player);
            Counter score = state.getScore(player);
            score.increment(wall.getCompletedRows() * params.getRowBonusPoints() +
                          wall.getCompletedColumns() * params.getColumnBonusPoints() +
                          wall.getCompletedColorSets() * params.getColorSetBonusPoints());
        }
        
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
        
        if (winners.size() == 1) {
            state.setPlayerResult(GameResult.WIN_GAME, winners.get(0));
            for (int player = 0; player < state.getNPlayers(); player++)
                if (player != winners.get(0))
                    state.setPlayerResult(GameResult.LOSE_GAME, player);
        } else {
            for (int player : winners)
                state.setPlayerResult(GameResult.DRAW_GAME, player);
            for (int player = 0; player < state.getNPlayers(); player++)
                if (!winners.contains(player))
                    state.setPlayerResult(GameResult.LOSE_GAME, player);
        }
    }
    
    private void prepareNextRound(AzulGameState state) {
        state.setPhase(AzulGameState.GamePhase.TILE_PICKING);
        if (state.getTileBag().getSize() > 0) {
            fillFactories(state);
        } else {
            state.setGameStatus(CoreConstants.GameResult.GAME_END);
        }
    }

    private boolean areFactoriesAndCenterEmpty(AzulGameState state) {
        return state.getFactories().stream().allMatch(this::isFactoryEmpty) 
            && state.getCenterPool().isEmpty();
    }

    private void addFactoryActions(AzulGameState state, List<AbstractAction> actions, int factoryId, int currentPlayer) {
        GridBoard<AzulTile> factory = state.getFactories().get(factoryId);
        Set<AzulTile.TileType> availableTypes = new HashSet<>();
        Wall playerWall = state.getPlayerWall(currentPlayer);

        for (int i = 0; i < factory.getHeight(); i++)
            for (int j = 0; j < factory.getWidth(); j++) {
                AzulTile tile = factory.getElement(i, j);
                if (tile != null) availableTypes.add(tile.getTileType());
            }

        for (AzulTile.TileType type : availableTypes) {
            PatternLine[] playerLines = state.getPatternLines(currentPlayer);
            for (int line = 0; line < playerLines.length; line++) {
                PatternLine patternLine = playerLines[line];
                int col = playerWall.getColumnForColor(line, type);
                if (col != -1 && !playerWall.isTilePlaced(line, col) &&
                    (patternLine.getCount() == 0 || 
                     (patternLine.getCurrentType() == type && patternLine.getCount() < line + 1))) {
                    actions.add(new TakeTilesAction(currentPlayer, factoryId, line, type));
                }
            }
            actions.add(new TakeTilesAction(currentPlayer, factoryId, TakeTilesAction.getFloorLine(), type));
        }
    }
    
    private void addCenterPoolActions(AzulGameState state, List<AbstractAction> actions, int currentPlayer) {
        Set<AzulTile.TileType> availableTypes = new HashSet<>();
        Wall playerWall = state.getPlayerWall(currentPlayer);

        for (AzulTile tile : state.getCenterPool())
            if (tile != null && tile.getTileType() != null)
                availableTypes.add(tile.getTileType());

        for (AzulTile.TileType type : availableTypes) {
            PatternLine[] playerLines = state.getPatternLines(currentPlayer);
            for (int line = 0; line < playerLines.length; line++) {
                PatternLine patternLine = playerLines[line];
                int col = playerWall.getColumnForColor(line, type);
                if (col != -1 && !playerWall.isTilePlaced(line, col) &&
                    (patternLine.getCount() == 0 || 
                     (patternLine.getCurrentType() == type && patternLine.getCount() < line + 1))) {
                    actions.add(new TakeTilesAction(currentPlayer, -1, line, type));
                }
            }
            actions.add(new TakeTilesAction(currentPlayer, -1, TakeTilesAction.getFloorLine(), type));
        }
    }
}