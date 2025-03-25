package games.azul.experiments;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import evaluation.RunArg;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import games.azul.AzulGameParameters;
import games.azul.players.AzulRuleBasedPlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.io.File;
import java.util.*;

/**
 * Runs experiments comparing the AzulRuleBasedPlayer against other AI players in the TAG framework.
 * Collects metrics on game outcomes, player performance, and Azul-specific statistics.
 */
public class AzulExperimentRunner {

    // Change these configurations as needed
    private static final int NUM_GAMES = 50;  // Number of games to play for each matchup
    private static final long RANDOM_SEED = 42;  // Seed for reproducibility
    private static final boolean VERBOSE_MODE = false;  // Set to true for more detailed output
    private static final int THINKING_TIME_MS = 100;  // Maximum thinking time per move in milliseconds
    private static final int MAX_ROUNDS = 1000;  // Maximum number of rounds before forcing end

    public static void main(String[] args) {
        // Define players to test
        List<AbstractPlayer> allPlayers = createPlayerList();
        AbstractPlayer ruleBasedPlayer = allPlayers.get(0);
        
        // Set up parameters
        AzulGameParameters gameParams = new AzulGameParameters(RANDOM_SEED);
        
        // Create tournament configurations
        Map<RunArg, Object> config = new HashMap<>();
        config.put(RunArg.matchups, NUM_GAMES);  // Number of games per matchup
        config.put(RunArg.verbose, VERBOSE_MODE);
        config.put(RunArg.mode, "fixed");  // Fixed mode so players stay in their positions
        config.put(RunArg.budget, THINKING_TIME_MS);  // Add thinking time budget
        config.put(RunArg.iterations, MAX_ROUNDS);  // Add maximum game length
        config.put(RunArg.byTeam, false);
        config.put(RunArg.seed, RANDOM_SEED);
        
        System.out.println("=== Starting Azul Tournament Experiments ===");
        System.out.println("Games per matchup: " + NUM_GAMES);
        System.out.println("Players: " + getPlayerNames(allPlayers));
        System.out.println("Note: Players will be initialized with forward models for accurate evaluation");
        System.out.println("RuleBased player will always be in position 0 (first player)");
        
        // Run head-to-head matchups separately for each opponent
        for (int i = 1; i < allPlayers.size(); i++) {
            AbstractPlayer opponent = allPlayers.get(i);
            String matchupName = "RuleBased_vs_" + opponent.toString();
            
            List<AbstractPlayer> matchupPlayers = new ArrayList<>();
            matchupPlayers.add(ruleBasedPlayer);
            matchupPlayers.add(opponent);
            
            System.out.println("\n--- Running matchup: " + matchupName + " ---");
            runFixedMatchup(matchupPlayers, gameParams, config, matchupName);
        }
        
        System.out.println("=== Experiments Complete ===");
    }
    
    /**
     * Creates the list of AI players to test
     */
    private static List<AbstractPlayer> createPlayerList() {
        Random rnd = new Random(RANDOM_SEED);
        List<AbstractPlayer> players = new ArrayList<>();
        
        // First create a temporary game to get the forward model
        // Create some dummy players for initialization
        List<AbstractPlayer> dummyPlayers = new ArrayList<>();
        dummyPlayers.add(new RandomPlayer(new Random(RANDOM_SEED)));
        dummyPlayers.add(new RandomPlayer(new Random(RANDOM_SEED)));
        
        // Run a temporary game to get the forward model
        System.out.println("Initializing forward model for players...");
        String paramString = null; // Use default parameters
        Game tempGame = Game.runOne(GameType.Azul, paramString, dummyPlayers, RANDOM_SEED, false, null, null, 0);
        
        // Our rule-based player - with generic name for metrics compatibility
        AzulRuleBasedPlayer ruleBasedPlayer = new AzulRuleBasedPlayer(new Random(RANDOM_SEED));
        ruleBasedPlayer.setForwardModel(tempGame.getForwardModel());
        ruleBasedPlayer.setName("RuleBased"); // Use a simple name for metrics
        players.add(ruleBasedPlayer);
        
        // Simple random player - standard name for metrics compatibility
        RandomPlayer randomPlayer = new RandomPlayer(new Random(RANDOM_SEED));
        randomPlayer.setForwardModel(tempGame.getForwardModel());
        randomPlayer.setName("Random"); // Use a simple name for metrics
        players.add(randomPlayer);
        
        // OSLA player - standard name for metrics compatibility
        OSLAPlayer oslaPlayer = new OSLAPlayer(new Random(RANDOM_SEED));
        oslaPlayer.setForwardModel(tempGame.getForwardModel());
        oslaPlayer.setName("OSLA"); // Use a simple name for metrics
        players.add(oslaPlayer);
        
        // MCTS player with low rollout and thinking time
        MCTSParams mctsParams = new MCTSParams();
        mctsParams.rolloutLength = 10;
        mctsParams.maxTreeDepth = 20;
        MCTSPlayer mctsPlayerSimple = new MCTSPlayer(mctsParams);
        mctsPlayerSimple.setForwardModel(tempGame.getForwardModel());
        mctsPlayerSimple.setName("MCTS-Simple");
        players.add(mctsPlayerSimple);
        
        // MCTS player with higher rollout
        MCTSParams mctsParamsHigh = new MCTSParams();
        mctsParamsHigh.rolloutLength = 30;
        mctsParamsHigh.maxTreeDepth = 40;
        MCTSPlayer mctsPlayerAdvanced = new MCTSPlayer(mctsParamsHigh);
        mctsPlayerAdvanced.setForwardModel(tempGame.getForwardModel());
        mctsPlayerAdvanced.setName("MCTS-Advanced");
        players.add(mctsPlayerAdvanced);
        
        return players;
    }
    
    /**
     * Runs a fixed matchup between two players with the first player always in position 0
     */
    private static void runFixedMatchup(List<AbstractPlayer> players, AbstractParameters params,
                                      Map<RunArg, Object> config, String experimentName) {
        System.out.println("Setting up fixed matchup with " + players.get(0) + " (P0) vs " + players.get(1) + " (P1)");
        
        // Create new instances of players with proper forward model initialization
        List<AbstractPlayer> freshPlayers = new ArrayList<>();
        Random rnd = new Random(RANDOM_SEED);
        
        // First create a temporary game to get the forward model
        List<AbstractPlayer> dummyPlayers = new ArrayList<>();
        dummyPlayers.add(new RandomPlayer(new Random(rnd.nextInt())));
        dummyPlayers.add(new RandomPlayer(new Random(rnd.nextInt())));
        
        // Run a temporary game to get the forward model
        String tempParamString = null; // Use default parameters 
        Game tempGame = Game.runOne(GameType.Azul, tempParamString, dummyPlayers, rnd.nextLong(), false, null, null, 0);
        
        // Create fresh rule-based player (position 0)
        AbstractPlayer p0 = players.get(0);
        if (p0 instanceof AzulRuleBasedPlayer) {
            AzulRuleBasedPlayer freshPlayer = new AzulRuleBasedPlayer(new Random(rnd.nextInt()));
            freshPlayer.setForwardModel(tempGame.getForwardModel());
            freshPlayer.setName(p0.toString());
            freshPlayers.add(freshPlayer);
        }
        
        // Create fresh opponent (position 1)
        AbstractPlayer p1 = players.get(1);
        if (p1 instanceof RandomPlayer) {
            RandomPlayer freshPlayer = new RandomPlayer(new Random(rnd.nextInt()));
            freshPlayer.setForwardModel(tempGame.getForwardModel());
            freshPlayer.setName(p1.toString());
            freshPlayers.add(freshPlayer);
        } else if (p1 instanceof OSLAPlayer) {
            OSLAPlayer freshPlayer = new OSLAPlayer(new Random(rnd.nextInt()));
            freshPlayer.setForwardModel(tempGame.getForwardModel());
            freshPlayer.setName(p1.toString());
            freshPlayers.add(freshPlayer);
        } else if (p1 instanceof MCTSPlayer) {
            // Create fresh MCTS parameters
            MCTSParams mctsParams;
            if (((MCTSPlayer) p1).getParameters() instanceof MCTSParams) {
                mctsParams = (MCTSParams) ((MCTSPlayer) p1).getParameters().copy();
            } else {
                mctsParams = new MCTSParams();
                mctsParams.rolloutLength = 10;
                mctsParams.maxTreeDepth = 20;
            }
            MCTSPlayer freshPlayer = new MCTSPlayer(mctsParams);
            freshPlayer.setForwardModel(tempGame.getForwardModel());
            freshPlayer.setName(p1.toString());
            freshPlayers.add(freshPlayer);
        }
        
        // Run tournament with fixed player positions
        RoundRobinTournament tournament = new RoundRobinTournament(
                freshPlayers, GameType.Azul, 2, params, config
        );
        
        // Add metrics listener
        File outputDir = createOutputDirectory(experimentName);
        String listenerFile = "metrics/azul.json";
        IGameListener metricsListener = IGameListener.createListener(listenerFile);
        metricsListener.setOutputDirectory(new String[]{outputDir.getAbsolutePath()});
        tournament.addListener(metricsListener);
        
        // Add progress tracking listener
        tournament.addListener(new IGameListener() {
            private int gamesCompleted = 0;
            private Game game;
            
            @Override
            public void onEvent(evaluation.metrics.Event event) {
                if (event.type == evaluation.metrics.Event.GameEvent.GAME_OVER) {
                    gamesCompleted++;
                    System.out.printf("Matchup %s: Game %d/%d completed\n", 
                        experimentName, gamesCompleted, NUM_GAMES);
                }
            }
            
            @Override
            public Game getGame() {
                return game;
            }
            
            @Override
            public void setGame(Game game) {
                this.game = game;
            }
            
            @Override
            public void report() {
                // No reporting needed for progress tracking
            }
        });
        
        // Run and show summary
        tournament.run();
        System.out.println("Results saved to: " + outputDir.getAbsolutePath());
    }
    
    /**
     * Creates an output directory for experiment results
     */
    private static File createOutputDirectory(String experimentName) {
        // Create directory in results/azul_experiments/
        String dirPath = "results/azul_experiments/" + experimentName;
        File outputDir = new File(dirPath);
        
        // Ensure parent directories exist
        outputDir.getParentFile().mkdirs();
        
        // Delete existing directory if it exists
        if (outputDir.exists()) {
            deleteDirectory(outputDir);
        }
        
        // Create fresh directory
        outputDir.mkdir();
        
        return outputDir;
    }
    
    /**
     * Recursively deletes a directory and all its contents
     */
    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    
    /**
     * Gets a list of player names for display
     */
    private static String getPlayerNames(List<AbstractPlayer> players) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(players.get(i).toString());
        }
        return sb.toString();
    }
    
    /**
     * Runs a single game between two players for debugging purposes
     */
    private static void runSingleGame(AbstractPlayer p1, AbstractPlayer p2, AbstractParameters params) {
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        
        String paramString = null;  // Use default params
        Game game = Game.runOne(GameType.Azul, paramString, players, System.currentTimeMillis(), false, null, null, 0);
        
        // Print results
        System.out.println("Game over. Final scores:");
        System.out.println(p1.getClass().getSimpleName() + ": " + game.getGameState().getGameScore(0));
        System.out.println(p2.getClass().getSimpleName() + ": " + game.getGameState().getGameScore(1));
    }
} 