package games.azul.components;

import core.CoreConstants;
import core.components.Component;
import java.util.Arrays;

public class PatternLine extends Component {
    private final AzulTile[] tiles;
    private final int capacity;
    private int count;
    private AzulTile.TileType currentType;

    public PatternLine(int size) {
        super(CoreConstants.ComponentType.BOARD);
        this.capacity = size;
        this.tiles = new AzulTile[size];
        this.count = 0;
        this.currentType = null;
    }

    public boolean canAdd(AzulTile tile) {
        if (count >= capacity) return false;
        return count == 0 || currentType == tile.getTileType();
    }

    public boolean add(AzulTile tile) {
        if (!canAdd(tile)) return false;
        if (count == 0) currentType = tile.getTileType();
        tiles[count++] = tile;
        return true;
    }

    public boolean isFull() {
        return count == capacity;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void clear() {
        Arrays.fill(tiles, null);
        count = 0;
        currentType = null;
    }

    public AzulTile[] getTiles() {
        return tiles;
    }

    public int getCount() {
        return count;
    }

    public AzulTile.TileType getCurrentType() {
        return currentType;
    }

    @Override
    public PatternLine copy() {
        PatternLine copy = new PatternLine(capacity);
        for (int i = 0; i < count; i++) {
            copy.tiles[i] = (AzulTile) tiles[i].copy();
        }
        copy.count = count;
        copy.currentType = currentType;
        copyComponentTo(copy);
        return copy;
    }
}