package games.azul.components;

import core.CoreConstants;
import core.components.Component;

public class AzulTile extends Component {
    public enum TileType { BLUE, YELLOW, RED, BLACK, WHITE }
    final TileType type;

    public AzulTile(TileType type) {
        super(CoreConstants.ComponentType.CARD);
        this.type = type;
    }

    public TileType getTileType() { return type; }
    @Override public AzulTile copy() { return new AzulTile(type); }
    @Override public int hashCode() { return type.hashCode(); }
    @Override public String toString() { return type.toString().substring(0, 1); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AzulTile)) return false;
        return type == ((AzulTile) o).type;
    }
}