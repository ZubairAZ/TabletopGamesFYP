package games.azul.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import games.azul.AzulGameState;
import games.azul.components.Wall;

import java.util.*;

/**
 * Metrics specific to the Azul game implementation.
 * Tracks various statistics about game play including:
 * - Completed rows, columns, and color sets
 * - Floor penalties incurred
 * - Wall tile placement patterns
 */
public class AzulMetrics {

    /**
     * Tracks the number of completed rows on each player's wall
     */
    public static class CompletedRows extends AbstractMetric {
        
        public CompletedRows() {
            super();
        }
        
        public CompletedRows(Event.GameEvent... args) {
            super(args);
        }
        
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AzulGameState state = (AzulGameState) e.state;
            
            for (int i = 0; i < state.getNPlayers(); i++) {
                Wall playerWall = state.getPlayerWall(i);
                records.put("Player-" + i, playerWall.getCompletedRows());
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            
            return true;
        }
        
        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Integer.class);
                columns.put("PlayerName-" + i, String.class);
            }
            return columns;
        }
    }
    
    /**
     * Tracks the number of completed columns on each player's wall
     */
    public static class CompletedColumns extends AbstractMetric {
        
        public CompletedColumns() {
            super();
        }
        
        public CompletedColumns(Event.GameEvent... args) {
            super(args);
        }
        
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AzulGameState state = (AzulGameState) e.state;
            
            for (int i = 0; i < state.getNPlayers(); i++) {
                Wall playerWall = state.getPlayerWall(i);
                records.put("Player-" + i, playerWall.getCompletedColumns());
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            
            return true;
        }
        
        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Integer.class);
                columns.put("PlayerName-" + i, String.class);
            }
            return columns;
        }
    }
    
    /**
     * Tracks the number of completed color sets on each player's wall
     */
    public static class CompletedColorSets extends AbstractMetric {
        
        public CompletedColorSets() {
            super();
        }
        
        public CompletedColorSets(Event.GameEvent... args) {
            super(args);
        }
        
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AzulGameState state = (AzulGameState) e.state;
            
            for (int i = 0; i < state.getNPlayers(); i++) {
                Wall playerWall = state.getPlayerWall(i);
                records.put("Player-" + i, playerWall.getCompletedColorSets());
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            
            return true;
        }
        
        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Integer.class);
                columns.put("PlayerName-" + i, String.class);
            }
            return columns;
        }
    }
    
    /**
     * Tracks the total floor penalties incurred by each player
     */
    public static class FloorPenalties extends AbstractMetric {
        
        public FloorPenalties() {
            super();
        }
        
        public FloorPenalties(Event.GameEvent... args) {
            super(args);
        }
        
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AzulGameState state = (AzulGameState) e.state;
            
            for (int i = 0; i < state.getNPlayers(); i++) {
                records.put("Player-" + i, state.getFloorLine(i).getValue());
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            
            return true;
        }
        
        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.GAME_OVER));
        }
        
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Integer.class);
                columns.put("PlayerName-" + i, String.class);
            }
            return columns;
        }
    }
    
    /**
     * Tracks the total number of tiles placed on each player's wall
     */
    public static class WallTileCount extends AbstractMetric {
        
        public WallTileCount() {
            super();
        }
        
        public WallTileCount(Event.GameEvent... args) {
            super(args);
        }
        
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AzulGameState state = (AzulGameState) e.state;
            
            for (int i = 0; i < state.getNPlayers(); i++) {
                Wall playerWall = state.getPlayerWall(i);
                int totalTiles = 0;
                
                for (int row = 0; row < 5; row++) {
                    for (int col = 0; col < 5; col++) {
                        if (playerWall.isTilePlaced(row, col)) {
                            totalTiles++;
                        }
                    }
                }
                
                records.put("Player-" + i, totalTiles);
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            
            return true;
        }
        
        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.GAME_OVER));
        }
        
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Integer.class);
                columns.put("PlayerName-" + i, String.class);
            }
            return columns;
        }
    }
} 