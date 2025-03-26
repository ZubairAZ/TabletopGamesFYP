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

public class AzulExperimentRunner {
    private static final int NUM_GAMES = 50;
    private static final long RANDOM_SEED = 42;
    private static final int THINKING_TIME_MS = 100;
    private static final int MAX_ROUNDS = 1000;

    public static void main(String[] args) {
        List<AbstractPlayer> allPlayers = createPlayerList();
        AbstractPlayer ruleBasedPlayer = allPlayers.get(0);
        
        AzulGameParameters gameParams = new AzulGameParameters(RANDOM_SEED);
        
        Map<RunArg, Object> config = new HashMap<>();
        config.put(RunArg.matchups, NUM_GAMES);
        config.put(RunArg.mode, "fixed");
        config.put(RunArg.budget, THINKING_TIME_MS);
        config.put(RunArg.iterations, MAX_ROUNDS);
        config.put(RunArg.byTeam, false);
        config.put(RunArg.seed, RANDOM_SEED);
        
        for (int i = 1; i < allPlayers.size(); i++) {
            AbstractPlayer opponent = allPlayers.get(i);
            String matchupName = "RuleBased_vs_" + opponent.toString();
            
            List<AbstractPlayer> matchupPlayers = new ArrayList<>();
            matchupPlayers.add(ruleBasedPlayer);
            matchupPlayers.add(opponent);
            
            runFixedMatchup(matchupPlayers, gameParams, config, matchupName);
        }
    }
    
    private static List<AbstractPlayer> createPlayerList() {
        Random rnd = new Random(RANDOM_SEED);
        List<AbstractPlayer> players = new ArrayList<>();
        
        List<AbstractPlayer> dummyPlayers = new ArrayList<>();
        dummyPlayers.add(new RandomPlayer(new Random(RANDOM_SEED)));
        dummyPlayers.add(new RandomPlayer(new Random(RANDOM_SEED)));
        
        Game tempGame = Game.runOne(GameType.Azul, null, dummyPlayers, RANDOM_SEED, false, null, null, 0);
        
        AzulRuleBasedPlayer ruleBasedPlayer = new AzulRuleBasedPlayer(new Random(RANDOM_SEED));
        ruleBasedPlayer.setForwardModel(tempGame.getForwardModel());
        ruleBasedPlayer.setName("RuleBased");
        players.add(ruleBasedPlayer);
        
        RandomPlayer randomPlayer = new RandomPlayer(new Random(RANDOM_SEED));
        randomPlayer.setForwardModel(tempGame.getForwardModel());
        randomPlayer.setName("Random");
        players.add(randomPlayer);
        
        OSLAPlayer oslaPlayer = new OSLAPlayer(new Random(RANDOM_SEED));
        oslaPlayer.setForwardModel(tempGame.getForwardModel());
        oslaPlayer.setName("OSLA");
        players.add(oslaPlayer);
        
        MCTSParams mctsParams = new MCTSParams();
        mctsParams.rolloutLength = 10;
        mctsParams.maxTreeDepth = 20;
        MCTSPlayer mctsPlayerSimple = new MCTSPlayer(mctsParams);
        mctsPlayerSimple.setForwardModel(tempGame.getForwardModel());
        mctsPlayerSimple.setName("MCTS-Simple");
        players.add(mctsPlayerSimple);
        
        MCTSParams mctsParamsHigh = new MCTSParams();
        mctsParamsHigh.rolloutLength = 30;
        mctsParamsHigh.maxTreeDepth = 40;
        MCTSPlayer mctsPlayerAdvanced = new MCTSPlayer(mctsParamsHigh);
        mctsPlayerAdvanced.setForwardModel(tempGame.getForwardModel());
        mctsPlayerAdvanced.setName("MCTS-Advanced");
        players.add(mctsPlayerAdvanced);
        
        return players;
    }
    
    private static void runFixedMatchup(List<AbstractPlayer> players, AbstractParameters params,
                                      Map<RunArg, Object> config, String experimentName) {
        List<AbstractPlayer> freshPlayers = new ArrayList<>();
        Random rnd = new Random(RANDOM_SEED);
        
        List<AbstractPlayer> dummyPlayers = new ArrayList<>();
        dummyPlayers.add(new RandomPlayer(new Random(rnd.nextInt())));
        dummyPlayers.add(new RandomPlayer(new Random(rnd.nextInt())));
        
        Game tempGame = Game.runOne(GameType.Azul, null, dummyPlayers, rnd.nextLong(), false, null, null, 0);
        
        AbstractPlayer p0 = players.get(0);
        if (p0 instanceof AzulRuleBasedPlayer) {
            AzulRuleBasedPlayer freshPlayer = new AzulRuleBasedPlayer(new Random(rnd.nextInt()));
            freshPlayer.setForwardModel(tempGame.getForwardModel());
            freshPlayer.setName(p0.toString());
            freshPlayers.add(freshPlayer);
        }
        
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
            MCTSParams mctsParams = ((MCTSPlayer) p1).getParameters() instanceof MCTSParams ?
                    (MCTSParams) ((MCTSPlayer) p1).getParameters().copy() :
                    new MCTSParams();
            MCTSPlayer freshPlayer = new MCTSPlayer(mctsParams);
            freshPlayer.setForwardModel(tempGame.getForwardModel());
            freshPlayer.setName(p1.toString());
            freshPlayers.add(freshPlayer);
        }
        
        RoundRobinTournament tournament = new RoundRobinTournament(
                freshPlayers, GameType.Azul, 2, params, config
        );
        
        File outputDir = createOutputDirectory(experimentName);
        String listenerFile = "metrics/azul.json";
        IGameListener metricsListener = IGameListener.createListener(listenerFile);
        metricsListener.setOutputDirectory(new String[]{outputDir.getAbsolutePath()});
        tournament.addListener(metricsListener);
        
        tournament.addListener(new IGameListener() {
            @Override
            public void onEvent(evaluation.metrics.Event event) {}
            
            @Override
            public Game getGame() { return null; }
            
            @Override
            public void setGame(Game game) {}
            
            @Override
            public void report() {}
        });
        
        tournament.run();
    }
    
    private static File createOutputDirectory(String experimentName) {
        String dirPath = "results/azul_experiments/" + experimentName;
        File outputDir = new File(dirPath);
        outputDir.getParentFile().mkdirs();
        
        if (outputDir.exists()) {
            deleteDirectory(outputDir);
        }
        
        outputDir.mkdir();
        return outputDir;
    }
    
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
} 