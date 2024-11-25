package games.azul.components;

import core.CoreConstants;
import core.components.Component;
import games.azul.components.AzulTile.TileType;

import java.awt.*;

public class Wall extends Component {
    private final boolean[][] tiles;  // [row][col]
    public static final TileType[][] WALL_PATTERN = {
            {TileType.BLUE, TileType.YELLOW, TileType.RED, TileType.BLACK, TileType.WHITE},
            {TileType.WHITE, TileType.BLUE, TileType.YELLOW, TileType.RED, TileType.BLACK},
            {TileType.BLACK, TileType.WHITE, TileType.BLUE, TileType.YELLOW, TileType.RED},
            {TileType.RED, TileType.BLACK, TileType.WHITE, TileType.BLUE, TileType.YELLOW},
            {TileType.YELLOW, TileType.RED, TileType.BLACK, TileType.WHITE, TileType.BLUE}
    };

    public Wall() {
        super(CoreConstants.ComponentType.BOARD, "Wall");
        tiles = new boolean[5][5];
    }

    public boolean canPlaceTile(int row, TileType color) {
        int col = getColumnForColor(row, color);
        return col != -1 && !tiles[row][col];
    }

    public void placeTile(int row, TileType color) {
        int col = getColumnForColor(row, color);
        if (col != -1) {
            tiles[row][col] = true;
        }
    }

    public int getColumnForColor(int row, TileType color) {
        for (int col = 0; col < 5; col++) {
            if (WALL_PATTERN[row][col] == color) return col;
        }
        return -1;
    }

    public int getCompletedRows() {
        int count = 0;
        for (int row = 0; row < 5; row++) {
            boolean complete = true;
            for (int col = 0; col < 5; col++) {
                if (!tiles[row][col]) {
                    complete = false;
                    break;
                }
            }
            if (complete) count++;
        }
        return count;
    }

    public int getCompletedColumns() {
        int count = 0;
        for (int col = 0; col < 5; col++) {
            boolean complete = true;
            for (int row = 0; row < 5; row++) {
                if (!tiles[row][col]) {
                    complete = false;
                    break;
                }
            }
            if (complete) count++;
        }
        return count;
    }

    public int getCompletedColorSets() {
        int count = 0;
        for (TileType color : TileType.values()) {
            boolean complete = true;
            for (int row = 0; row < 5; row++) {
                int col = getColumnForColor(row, color);
                if (!tiles[row][col]) {
                    complete = false;
                    break;
                }
            }
            if (complete) count++;
        }
        return count;
    }

    public boolean isTilePlaced(int row, int col) {
        return tiles[row][col];
    }

    @Override
    public Wall copy() {
        Wall copy = new Wall();
        for (int i = 0; i < 5; i++) {
            System.arraycopy(tiles[i], 0, copy.tiles[i], 0, 5);
        }
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wall)) return false;
        if (!super.equals(o)) return false;
        Wall other = (Wall) o;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (tiles[i][j] != other.tiles[i][j]) return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                result = 31 * result + (tiles[i][j] ? 1 : 0);
            }
        }
        return result;
    }
}