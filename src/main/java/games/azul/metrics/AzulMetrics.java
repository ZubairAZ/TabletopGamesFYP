package games.azul.metrics;

import core.AbstractGameState;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import games.azul.AzulGameState;
import games.azul.components.Wall;
import core.components.Counter;

import java.util.*;

import static evaluation.metrics.Event.GameEvent.GAME_OVER;

public class AzulMetrics {
    private static final int[] FLOOR_PENALTIES = {-1, -1, -2, -2, -2, -3, -3};

    public static class WallCompletion extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AzulGameState state = (AzulGameState) e.state;
            int completedRows = 0;
            int completedColumns = 0;
            int completedColorSets = 0;
            
            for (int player = 0; player < state.getNPlayers(); player++) {
                Wall wall = state.getPlayerWalls().get(player);
                completedRows += wall.getCompletedRows();
                completedColumns += wall.getCompletedColumns();
                completedColorSets += wall.getCompletedColorSets();
            }
            
            records.put("CompletedRows", completedRows);
            records.put("CompletedColumns", completedColumns);
            records.put("CompletedColorSets", completedColorSets);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("CompletedRows", Integer.class);
            columns.put("CompletedColumns", Integer.class);
            columns.put("CompletedColorSets", Integer.class);
            return columns;
        }
    }

    public static class FloorPenalty extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AzulGameState state = (AzulGameState) e.state;
            int totalPenalty = 0;
            for (int player = 0; player < state.getNPlayers(); player++) {
                Counter floorLine = state.getFloorLine(player);
                int floorTiles = floorLine.getValue();
                int penalty = 0;
                for (int i = 0; i < floorTiles && i < FLOOR_PENALTIES.length; i++) {
                    penalty += FLOOR_PENALTIES[i];
                }
                totalPenalty += penalty;
            }
            records.put("TotalPenalty", totalPenalty);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("TotalPenalty", Integer.class);
            return columns;
        }
    }

    public static class TilesPlaced extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AzulGameState state = (AzulGameState) e.state;
            int totalTiles = 0;
            for (int player = 0; player < state.getNPlayers(); player++) {
                Wall wall = state.getPlayerWalls().get(player);
                for (int row = 0; row < 5; row++) {
                    for (int col = 0; col < 5; col++) {
                        if (wall.isTilePlaced(row, col)) totalTiles++;
                    }
                }
            }
            records.put("TotalTiles", totalTiles);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("TotalTiles", Integer.class);
            return columns;
        }
    }
} 