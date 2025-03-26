package games.azul;

import core.AbstractParameters;

import java.util.Objects;

public class AzulGameParameters extends AbstractParameters {
    // Game specific parameters
    private final int nTilesPerFactory = 4;
    private final int nTilesPerType = 20;
    private final int nRows = 5;
    private final int nColumns = 5;
    private final int rowBonusPoints = 2;
    private final int columnBonusPoints = 7;
    private final int colorSetBonusPoints = 10;
    private final int penaltyPerFloorTile = -1;
    private int nPlayers = 2;  // Default number of players

    public AzulGameParameters(long seed) {
        super();
        this.setRandomSeed(seed);
    }

    // Getters for all parameters
    public int getNTilesPerFactory() { return nTilesPerFactory; }
    public int getNTilesPerType() { return nTilesPerType; }
    public int getNRows() { return nRows; }
    public int getNColumns() { return nColumns; }
    public int getRowBonusPoints() { return rowBonusPoints; }
    public int getColumnBonusPoints() { return columnBonusPoints; }
    public int getColorSetBonusPoints() { return colorSetBonusPoints; }
    public int getPenaltyPerFloorTile() { return penaltyPerFloorTile; }
    public int getNPlayers() { return nPlayers; }
    public void setNPlayers(int nPlayers) { this.nPlayers = nPlayers; }

    @Override
    protected AbstractParameters _copy() {
        AzulGameParameters params = new AzulGameParameters(System.currentTimeMillis());
        params.setMaxRounds(getMaxRounds());
        params.setTimeoutRounds(getTimeoutRounds());
        params.setThinkingTimeMins(getThinkingTimeMins());
        params.nPlayers = this.nPlayers;
        return params;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AzulGameParameters)) return false;
        AzulGameParameters other = (AzulGameParameters) o;
        return nTilesPerFactory == other.nTilesPerFactory &&
                nTilesPerType == other.nTilesPerType &&
                nRows == other.nRows &&
                nColumns == other.nColumns &&
                rowBonusPoints == other.rowBonusPoints &&
                columnBonusPoints == other.columnBonusPoints &&
                colorSetBonusPoints == other.colorSetBonusPoints &&
                penaltyPerFloorTile == other.penaltyPerFloorTile &&
                nPlayers == other.nPlayers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nTilesPerFactory, nTilesPerType,
                nRows, nColumns, rowBonusPoints, columnBonusPoints,
                colorSetBonusPoints, penaltyPerFloorTile,
                nPlayers);
    }
    
    public Object instantiate() {
        return new AzulGameState(this, nPlayers);
    }
}