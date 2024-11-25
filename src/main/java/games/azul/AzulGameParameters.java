package games.azul;

import core.AbstractParameters;

import java.util.Objects;

public class AzulGameParameters extends AbstractParameters {
    // Game specific parameters
    private final int nTilesPerFactory;  // Tiles per factory display
    private final int nTilesPerType;  // Number of tiles per color
    private final int nRows;  // Number of pattern lines
    private final int nColumns;  // Size of wall grid
    private final int bonusPoints;  // Points for completing a row/column/set
    private final int penaltyPerFloorTile;  // Penalty points per floor tile

    public AzulGameParameters(long seed) {
        super();
        this.setRandomSeed(seed);
        // Default values for a standard game of Azul
        this.nTilesPerFactory = 4;
        this.nTilesPerType = 20;  // 20 of each color
        this.nRows = 5;
        this.nColumns = 5;
        this.bonusPoints = 2;
        this.penaltyPerFloorTile = -1;
    }

    // Getters for all parameters
    public int getNTilesPerFactory() { return nTilesPerFactory; }
    public int getNTilesPerType() { return nTilesPerType; }
    public int getNRows() { return nRows; }
    public int getNColumns() { return nColumns; }
    public int getBonusPoints() { return bonusPoints; }
    public int getPenaltyPerFloorTile() { return penaltyPerFloorTile; }

    @Override
    protected AbstractParameters _copy() {
        AzulGameParameters params = new AzulGameParameters(System.currentTimeMillis());
        params.setMaxRounds(getMaxRounds());
        params.setTimeoutRounds(getTimeoutRounds());
        params.setThinkingTimeMins(getThinkingTimeMins());
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
                bonusPoints == other.bonusPoints &&
                penaltyPerFloorTile == other.penaltyPerFloorTile;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nTilesPerFactory, nTilesPerType,
                nRows, nColumns, bonusPoints, penaltyPerFloorTile);
    }
}