package games.azul;

import core.AbstractParameters;

import java.util.Objects;

public class AzulGameParameters extends AbstractParameters {
    // Game specific parameters
    private final int nTilesPerFactory;  // Tiles per factory display
    private final int nTilesPerType;  // Number of tiles per color
    private final int nRows;  // Number of pattern lines
    private final int nColumns;  // Size of wall grid
    private final int rowBonusPoints;  // Points for completing a row (2)
    private final int columnBonusPoints;  // Points for completing a column (7)
    private final int colorSetBonusPoints;  // Points for completing a color set (10)
    private final int penaltyPerFloorTile;  // Penalty points per floor tile
    public String dataPath = "data/azul/";  // Path to game data
    private int nPlayers = 2;  // Default number of players

    public AzulGameParameters(long seed) {
        super();
        this.setRandomSeed(seed);
        // Default values for a standard game of Azul
        this.nTilesPerFactory = 4;
        this.nTilesPerType = 20;  // 20 of each color
        this.nRows = 5;
        this.nColumns = 5;
        this.rowBonusPoints = 2;
        this.columnBonusPoints = 7;
        this.colorSetBonusPoints = 10;
        this.penaltyPerFloorTile = -1;
    }
    
    public AzulGameParameters(String dataPath) {
        super();
        this.dataPath = dataPath;
        // Default values for a standard game of Azul
        this.nTilesPerFactory = 4;
        this.nTilesPerType = 20;  // 20 of each color
        this.nRows = 5;
        this.nColumns = 5;
        this.rowBonusPoints = 2;
        this.columnBonusPoints = 7;
        this.colorSetBonusPoints = 10;
        this.penaltyPerFloorTile = -1;
    }
    
    public AzulGameParameters(String dataPath, long seed) {
        super();
        this.setRandomSeed(seed);
        this.dataPath = dataPath;
        // Default values for a standard game of Azul
        this.nTilesPerFactory = 4;
        this.nTilesPerType = 20;  // 20 of each color
        this.nRows = 5;
        this.nColumns = 5;
        this.rowBonusPoints = 2;
        this.columnBonusPoints = 7;
        this.colorSetBonusPoints = 10;
        this.penaltyPerFloorTile = -1;
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
    public String getDataPath() { return dataPath; }
    public void setDataPath(String dataPath) { this.dataPath = dataPath; }
    public int getNPlayers() { return nPlayers; }
    public void setNPlayers(int nPlayers) { this.nPlayers = nPlayers; }

    @Override
    protected AbstractParameters _copy() {
        AzulGameParameters params = new AzulGameParameters(System.currentTimeMillis());
        params.setMaxRounds(getMaxRounds());
        params.setTimeoutRounds(getTimeoutRounds());
        params.setThinkingTimeMins(getThinkingTimeMins());
        params.dataPath = this.dataPath;
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
                nPlayers == other.nPlayers &&
                Objects.equals(dataPath, other.dataPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nTilesPerFactory, nTilesPerType,
                nRows, nColumns, rowBonusPoints, columnBonusPoints,
                colorSetBonusPoints, penaltyPerFloorTile,
                dataPath, nPlayers);
    }
    
    public Object instantiate() {
        return new AzulGameState(this, nPlayers);
    }
}