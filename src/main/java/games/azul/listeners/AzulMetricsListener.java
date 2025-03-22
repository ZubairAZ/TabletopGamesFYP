package games.azul.listeners;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import evaluation.listeners.IGameListener;
import games.azul.AzulGameState;
import games.azul.components.Wall;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IDataLogger;
import evaluation.metrics.tablessaw.DataTableSaw;
import evaluation.metrics.GameMetrics;
import games.azul.metrics.AzulMetrics;

import java.util.*;

import static evaluation.metrics.IDataLogger.ReportDestination.ToFile;
import static evaluation.metrics.IDataLogger.ReportType.Summary;

public class AzulMetricsListener extends MetricsGameListener {
    public AzulMetricsListener() {
        super(ToFile, 
            new IDataLogger.ReportType[]{Summary}, 
            new AbstractMetric[]{
                new GameMetrics.GameScore(),
                new GameMetrics.FinalScore(),
                new GameMetrics.StateSpace(),
                new GameMetrics.ComputationTimes(),
                new GameMetrics.Decisions(),
                new GameMetrics.ActionsReduced(),
                new GameMetrics.OrdinalPosition(),
                new GameMetrics.Winner(),
                new AzulMetrics.WallCompletion(),
                new AzulMetrics.FloorPenalty(),
                new AzulMetrics.TilesPlaced()
            }
        );
    }

    public AzulMetricsListener(AbstractMetric[] metrics) {
        super(ToFile, new IDataLogger.ReportType[]{Summary}, metrics);
    }

    @Override
    public void init(Game game, int nPlayersPerGame, Set<String> playerNames) {
        super.init(game, nPlayersPerGame, playerNames);
        // Add all relevant event types
        eventsOfInterest.add(Event.GameEvent.GAME_OVER);
        eventsOfInterest.add(Event.GameEvent.ACTION_TAKEN);
        eventsOfInterest.add(Event.GameEvent.ACTION_CHOSEN);
        eventsOfInterest.add(Event.GameEvent.ROUND_OVER);
        // Set output directory to Azul's out directory
        setOutputDirectory("src", "main", "java", "games", "azul", "out");
    }

    @Override
    public void onEvent(Event e) {
        // Run metrics based on their event types
        for (AbstractMetric metric : metrics.values()) {
            if (metric.getDefaultEventTypes().contains(e.type)) {
                metric.run(this, e);
            }
        }
    }
} 