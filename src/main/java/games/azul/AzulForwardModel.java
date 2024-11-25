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

        // Initialize player components
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

        // Set up factories only once
        int nFactories = (state.getNPlayers() * 2) + 1;
        for (int i = 0; i < nFactories; i++) {
            state.getFactories().add(new GridBoard<>(2, 2));
        }

        // Initialize center pool
        state.setCenterPool(new ArrayList<>());

        // Reset and setup tile bags
        state.setTileBag(new Deck<>("Tile Bag", CoreConstants.VisibilityMode.HIDDEN_TO_ALL));
        state.setDiscardBag(new Deck<>("Discard Bag", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));

        // Initialize tile bag
        for (AzulTile.TileType type : AzulTile.TileType.values()) {
            for (int i = 0; i < params.getNTilesPerType(); i++) {
                state.getTileBag().add(new AzulTile(type));
            }
        }
        state.getTileBag().shuffle(state.getRnd());
        fillFactories((AzulGameState)firstState);
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
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        if (currentState.isActionInProgress()) return;

        AzulGameState state = (AzulGameState) currentState;
        if (action instanceof TakeTilesAction) {
            executeTakeTilesAction(state, (TakeTilesAction) action);

            if (isRoundOver(state)) {
                endRound(state);
                if (checkGameEnd(state)) {
                    endGame(state);
                } else {
                    setupNextRound(state);
                }
            }
            endPlayerTurn(state);
        }
    }
    @Override
    protected void endPlayerTurn(AbstractGameState state) {
        state.setTurnOwner((state.getCurrentPlayer() + 1) % state.getNPlayers());
    }
    private void executeTakeTilesAction(AzulGameState state, TakeTilesAction action) {
        action.execute(state);
    }
    private boolean isRoundOver(AzulGameState state) {
        // Check if all factories are empty
        for (GridBoard<AzulTile> factory : state.getFactories()) {
            for (int i = 0; i < factory.getHeight(); i++) {
                for (int j = 0; j < factory.getWidth(); j++) {
                    if (factory.getElement(i, j) != null) return false;
                }
            }
        }
        // Check if center pool is empty
        return state.getCenterPool().isEmpty();
    }
    private void endRound(AzulGameState state) {
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
    private int calculateFloorPenalty(int floorTiles) {
        int[] penalties = {0, -1, -2, -4, -6, -8, -11, -14};
        return penalties[Math.min(floorTiles, penalties.length - 1)];
    }
    private void setupNextRound(AzulGameState state) {
        if (state.getTileBag().getSize() == 0) {
            while (state.getDiscardBag().getSize() > 0) {
                state.getTileBag().add(state.getDiscardBag().draw());
            }
            state.getTileBag().shuffle(state.getRnd());
        }
        fillFactories(state);
    }
    private boolean checkGameEnd(AzulGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++) {
            Wall wall = state.getPlayerWall(p);
            for (int i = 0; i < 5; i++) {
                boolean complete = true;
                for (int j = 0; j < 5; j++) {
                    if (!wall.isTilePlaced(i, j)) {
                        complete = false;
                        break;
                    }
                }
                if (complete) return true;
            }
        }
        return false;
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

        // Center pool actions
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
            for (int line = 0; line < state.getPatternLines(currentPlayer).length; line++) {
                if (state.getPatternLines(currentPlayer)[line].canAdd(new AzulTile(type))) {
                    actions.add(new TakeTilesAction(currentPlayer, factoryId, line, type));
                }
            }
        }
    }
    private void addCenterPoolActions(AzulGameState state, List<AbstractAction> actions, int currentPlayer) {
        List<AzulTile> centerPool = state.getCenterPool();
        Set<AzulTile.TileType> availableTypes = new HashSet<>();

        // Collect available tile types from center
        for (AzulTile tile : centerPool) {
            if (tile != null) {
                availableTypes.add(tile.getTileType());
            }
        }

        // Create actions for each type and valid pattern line
        for (AzulTile.TileType type : availableTypes) {
            for (int line = 0; line < state.getPatternLines(currentPlayer).length; line++) {
                if (state.getPatternLines(currentPlayer)[line].canAdd(new AzulTile(type))) {
                    actions.add(new TakeTilesAction(currentPlayer, -1, line, type));
                }
            }
        }
    }
    @Override
    protected AbstractForwardModel _copy() {
        return new AzulForwardModel();
    }
}