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
    public enum GamePhase implements IGamePhase { 
        TILE_PICKING, 
        WALL_TILING
    }

    private GamePhase gamePhase;
    private final List<Wall> playerWalls = new ArrayList<>();
    private final List<PatternLine[]> patternLines = new ArrayList<>();
    private final List<Counter> floorLines = new ArrayList<>();
    private final List<Counter> scores = new ArrayList<>();
    private final List<GridBoard<AzulTile>> factories = new ArrayList<>();
    private List<AzulTile> centerPool;
    private Deck<AzulTile> tileBag, discardBag;
    private int firstPlayer;

    public AzulGameState(AbstractParameters params, int nPlayers) {
        super(params, nPlayers);
        gamePhase = GamePhase.TILE_PICKING;
        firstPlayer = 0;
        setGamePhase(gamePhase);
        setTurnOwner(firstPlayer);
        roundCounter = 0;
        turnCounter = 0;
    }

    // Getters and Setters
    public GamePhase getPhase() { return gamePhase; }
    public void setPhase(GamePhase phase) { 
        this.gamePhase = phase;
        setGamePhase(phase);
        if (phase == GamePhase.TILE_PICKING) {
            roundCounter++;
        }
    }
    public List<Wall> getPlayerWalls() { return playerWalls; }
    public Wall getPlayerWall(int player) { return playerWalls.get(player); }
    public List<PatternLine[]> getPatternLines() { return patternLines; }
    public PatternLine[] getPatternLines(int player) { return patternLines.get(player); }
    public List<Counter> getFloorLines() { return floorLines; }
    public Counter getFloorLine(int player) { return floorLines.get(player); }
    public List<Counter> getScores() { return scores; }
    public Counter getScore(int player) { return scores.get(player); }
    public List<GridBoard<AzulTile>> getFactories() { return factories; }
    public List<AzulTile> getCenterPool() { return centerPool; }
    public void setCenterPool(List<AzulTile> centerPool) { this.centerPool = centerPool; }
    public Deck<AzulTile> getTileBag() { return tileBag; }
    public void setTileBag(Deck<AzulTile> tileBag) { this.tileBag = tileBag; }
    public Deck<AzulTile> getDiscardBag() { return discardBag; }
    public void setDiscardBag(Deck<AzulTile> discardBag) { this.discardBag = discardBag; }
    public int getFirstPlayer() { return firstPlayer; }
    public void setFirstPlayer(int player) { firstPlayer = player; }

    @Override
    protected GameType _getGameType() { return GameType.Azul; }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerWalls);
        for (PatternLine[] lines : patternLines) components.addAll(Arrays.asList(lines));
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
        AzulGameState copy = new AzulGameState(gameParameters, getNPlayers());
        copy.gamePhase = gamePhase;

        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerWalls.add(playerWalls.get(i).copy());
            copy.patternLines.add(new PatternLine[5]);
            for (int j = 0; j < 5; j++) copy.patternLines.get(i)[j] = patternLines.get(i)[j].copy();
            copy.floorLines.add(floorLines.get(i).copy());
            copy.scores.add(scores.get(i).copy());
        }

        for (GridBoard<AzulTile> factory : factories) copy.factories.add(factory.copy());

        copy.centerPool = new ArrayList<>();
        for (AzulTile tile : centerPool) copy.centerPool.add(tile.copy());
        copy.tileBag = (Deck<AzulTile>) ((playerId != -1) ? tileBag.copy(playerId) : tileBag.copy());
        copy.discardBag = (Deck<AzulTile>) discardBag.copy();
        copy.firstPlayer = firstPlayer;
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        double score = scores.get(playerId).getValue();
        Wall wall = playerWalls.get(playerId);
        AzulGameParameters params = (AzulGameParameters) gameParameters;
        return (score + wall.getCompletedRows() * params.getRowBonusPoints() +
                wall.getCompletedColumns() * params.getColumnBonusPoints() +
                wall.getCompletedColorSets() * params.getColorSetBonusPoints() +
                floorLines.get(playerId).getValue() * params.getPenaltyPerFloorTile()) / 100.0;
    }

    @Override
    public double getGameScore(int playerId) { return scores.get(playerId).getValue(); }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AzulGameState)) return false;
        AzulGameState other = (AzulGameState) o;
        return gamePhase == other.gamePhase && firstPlayer == other.firstPlayer &&
               Objects.equals(playerWalls, other.playerWalls) &&
               Objects.equals(patternLines, other.patternLines) &&
               Objects.equals(floorLines, other.floorLines) &&
               Objects.equals(scores, other.scores) &&
               Objects.equals(factories, other.factories) &&
               Objects.equals(centerPool, other.centerPool) &&
               Objects.equals(tileBag, other.tileBag) &&
               Objects.equals(discardBag, other.discardBag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gamePhase, playerWalls, patternLines, floorLines, scores,
                          factories, centerPool, tileBag, discardBag, firstPlayer);
    }

    public boolean isFirstPlayerTokenInCenter() {
        return centerPool.stream().anyMatch(t -> t.getTileType() == null);
    }

    public void removeFirstPlayerTokenFromCenter() {
        centerPool.removeIf(t -> t.getTileType() == null);
    }

    public void addFirstPlayerTokenToCenter() {
        centerPool.add(new AzulTile(null) {
            @Override public AzulTile.TileType getTileType() { return null; }
            @Override public String toString() { return "First Player Token"; }
        });
    }
}