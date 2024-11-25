package games.azul;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IGamePhase;
import games.GameType;
import games.azul.components.AzulTile;
import games.azul.components.PatternLine;
import games.azul.components.Wall;
import java.util.*;

public class AzulGameState extends AbstractGameState {
    public enum GamePhase {
        TILE_PICKING,
        WALL_TILING
    }

    private GamePhase gamePhase;
    // Game state components with proper visibility modes
    private final List<Wall> playerWalls = new ArrayList<>();  // Scoring walls for each player
    private final List<PatternLine[]> patternLines = new ArrayList<>();  // Pattern lines for each player
    private final List<Counter> floorLines = new ArrayList<>();  // Penalty tiles for each player
    private final List<Counter> scores = new ArrayList<>();  // Player scores

    private final List<GridBoard<AzulTile>> factories = new ArrayList<>();  // Factory displays
    private List<AzulTile> centerPool;  // Center of table
    private Deck<AzulTile> tileBag;  // Main bag of tiles
    private Deck<AzulTile> discardBag;  // Discard pile
    private int firstPlayerToken;  // Player with first player marker

    public AzulGameState(AbstractParameters params, int nPlayers) {
        super(params, nPlayers);
    }

    // Getters
    public GamePhase getPhase() { return gamePhase; }
    public void setPhase(GamePhase phase) { this.gamePhase = phase; }
    public List<Wall> getPlayerWalls() { return playerWalls; }
    public Wall getPlayerWall(int player) { return playerWalls.get(player); }
    public List<PatternLine[]> getPatternLines() { return patternLines; }
    public PatternLine[] getPatternLines(int player) { return patternLines.get(player); }
    public List<Counter> getFloorLines() { return floorLines; }
    public Counter getFloorLine(int player) { return floorLines.get(player); }
    public List<Counter> getScores() { return scores; }
    public Counter getScore(int player) { return scores.get(player); }
    public List<GridBoard<AzulTile>> getFactories() { return factories; }
    public void setCenterPool(List<AzulTile> centerPool) { this.centerPool = centerPool; }
    public List<AzulTile> getCenterPool() { return centerPool; }
    public Deck<AzulTile> getTileBag() { return tileBag; }
    public void setTileBag(Deck<AzulTile> tileBag) { this.tileBag = tileBag; }
    public Deck<AzulTile> getDiscardBag() { return discardBag; }
    public void setDiscardBag(Deck<AzulTile> discardBag) { this.discardBag = discardBag; }
    public int getFirstPlayer() { return firstPlayerToken; }
    public void setFirstPlayer(int player) { firstPlayerToken = player; }

    @Override
    protected GameType _getGameType() {
        return GameType.Azul;
    }


    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerWalls);
        for (PatternLine[] lines : patternLines) {
            components.addAll(Arrays.asList(lines));
        }
        components.addAll(floorLines);
        components.addAll(scores);
        components.addAll(factories);
        components.addAll(centerPool);
        components.add(tileBag);
        components.add(discardBag);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        AzulGameState copy = new AzulGameState((AzulGameParameters) gameParameters, getNPlayers());
        copy.gamePhase = this.gamePhase;

        // Initialize lists in copy with same size as original
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerWalls.add(null);
            copy.patternLines.add(new PatternLine[5]);
            copy.floorLines.add(null);
            copy.scores.add(null);
        }

        for (int i = 0; i < factories.size(); i++) {
            copy.factories.add(null);
        }

        // Copy all components with proper visibility
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerWalls.set(i, playerWalls.get(i).copy());
            for (int j = 0; j < patternLines.get(i).length; j++) {
                copy.patternLines.get(i)[j] = (PatternLine) patternLines.get(i)[j].copy();
            }
            copy.floorLines.set(i, floorLines.get(i).copy());
            copy.scores.set(i, scores.get(i).copy());
        }

        for (int i = 0; i < factories.size(); i++) {
            copy.factories.set(i, factories.get(i).copy());
        }

        copy.centerPool = new ArrayList<>();
        for (AzulTile tile : centerPool) {
            copy.centerPool.add(tile.copy());
        }

        // Handle hidden information properly for tile bag
        if (playerId != -1) {
            copy.tileBag = (Deck<AzulTile>) tileBag.copy(playerId);
        } else {
            copy.tileBag = tileBag.copy();
        }
        copy.discardBag = discardBag.copy();
        copy.firstPlayerToken = firstPlayerToken;

        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        double score = scores.get(playerId).getValue();

        Wall wall = playerWalls.get(playerId);
        score += wall.getCompletedRows() * ((AzulGameParameters)gameParameters).getBonusPoints();
        score += wall.getCompletedColumns() * ((AzulGameParameters)gameParameters).getBonusPoints();

        score += floorLines.get(playerId).getValue() *
                ((AzulGameParameters)gameParameters).getPenaltyPerFloorTile();

        return score / 100.0;  // Normalize to [-1, 1] range
    }

    @Override
    public double getGameScore(int playerId) {
        return scores.get(playerId).getValue();
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof AzulGameState)) return false;
        AzulGameState other = (AzulGameState) o;
        return playerWalls.equals(other.playerWalls) &&
                patternLines.equals(other.patternLines) &&
                floorLines.equals(other.floorLines) &&
                scores.equals(other.scores) &&
                factories.equals(other.factories) &&
                centerPool.equals(other.centerPool) &&
                tileBag.equals(other.tileBag) &&
                discardBag.equals(other.discardBag) &&
                firstPlayerToken == other.firstPlayerToken;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hash(playerWalls, patternLines, floorLines, scores,
                factories, centerPool, tileBag, discardBag, firstPlayerToken);
        return result;
    }
}