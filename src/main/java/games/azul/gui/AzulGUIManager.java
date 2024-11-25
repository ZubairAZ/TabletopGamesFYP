package games.azul.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.Counter;
import core.components.GridBoard;
import games.azul.AzulGameState;
import games.azul.components.AzulTile;
import games.azul.components.PatternLine;
import games.azul.components.Wall;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import java.util.HashMap;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;

public class AzulGUIManager extends AbstractGUIManager {
    // Dimensions for components
    private final static int TILE_SIZE = 40;
    private final static int FACTORY_SIZE = TILE_SIZE * 2;
    private final static int PATTERN_LINE_WIDTH = TILE_SIZE * 5;
    private final static int WALL_SIZE = TILE_SIZE * 5;

    // Game state views
    private JPanel mainGameBoard;
    private JPanel[] playerBoards;
    private JPanel factoriesPanel;
    private JPanel centerPoolPanel;

    public AzulGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanPlayerId) {
        super(parent, game, ac, humanPlayerId);
        if (game == null) return;

        if (parent != null) {
            width = 1000;
            height = 800;

            // Main layout
            parent.setLayout(new BorderLayout());

            // Game info panel at top
            JPanel infoPanel = createGameStateInfoPanel("Azul", game.getGameState(), width, defaultInfoPanelHeight);
            parent.add(infoPanel, BorderLayout.NORTH);

            // Main game area
            mainGameBoard = new JPanel();
            mainGameBoard.setLayout(new BoxLayout(mainGameBoard, BoxLayout.Y_AXIS));

            // Create panel for factories and center pool
            JPanel topArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

            // Add factories
            factoriesPanel = new JPanel();
            factoriesPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            factoriesPanel.setBorder(BorderFactory.createTitledBorder("Factories"));
            topArea.add(factoriesPanel);

            // Add center pool
            centerPoolPanel = new JPanel();
            centerPoolPanel.setLayout(new GridBagLayout());
            centerPoolPanel.setBorder(BorderFactory.createTitledBorder("Center Pool"));
            topArea.add(centerPoolPanel);

            mainGameBoard.add(topArea);

            // Player boards
            AzulGameState state = (AzulGameState) game.getGameState();
            int rows = (state.getNPlayers() > 2) ? 2 : 1;
            int cols = (state.getNPlayers() > 2) ? 2 : state.getNPlayers();

            JPanel playerBoardsPanel = new JPanel(new GridLayout(rows, cols, 10, 10));
            playerBoards = new JPanel[state.getNPlayers()];

            for (int i = 0; i < state.getNPlayers(); i++) {
                playerBoards[i] = createPlayerBoard(i);
                playerBoardsPanel.add(playerBoards[i]);
            }

            JScrollPane playerScroll = new JScrollPane(playerBoardsPanel);
            mainGameBoard.add(playerScroll);

            parent.add(mainGameBoard, BorderLayout.CENTER);

            // Action panel at bottom
            JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, true, null, null, null);
            parent.add(actionPanel, BorderLayout.SOUTH);
        }
    }
    private JPanel createPlayerBoard(int playerId) {
        JPanel board = new JPanel(new BorderLayout(5, 5));
        board.setBorder(BorderFactory.createTitledBorder("Player " + playerId));

        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout(0, 10));

        // Upper section for pattern lines and wall
        JPanel upperSection = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Pattern Lines section with its own border
        JPanel patternLines = createPatternLines();
        patternLines.setBorder(BorderFactory.createTitledBorder("Pattern Lines"));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        upperSection.add(patternLines, gbc);

        // Wall section with its own border
        JPanel wall = createWallPanel();
        wall.setBorder(BorderFactory.createTitledBorder("Wall"));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        upperSection.add(wall, gbc);

        mainContainer.add(upperSection, BorderLayout.CENTER);

        // Floor line section centered under pattern lines and wall
        JPanel floorLine = createFloorLine();
        floorLine.setBorder(BorderFactory.createTitledBorder("Floor Line"));
        JPanel floorLineWrapper = new JPanel();
        floorLineWrapper.setLayout(new BoxLayout(floorLineWrapper, BoxLayout.X_AXIS));
        floorLineWrapper.add(Box.createHorizontalGlue());
        floorLineWrapper.add(floorLine);
        floorLineWrapper.add(Box.createHorizontalGlue());
        floorLineWrapper.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        mainContainer.add(floorLineWrapper, BorderLayout.SOUTH);
        board.add(mainContainer, BorderLayout.CENTER);

        return board;
    }
    private JPanel createPatternLines() {
        JPanel wrapper = new JPanel(new GridBagLayout());  // Changed to GridBagLayout
        JPanel patternLines = new JPanel(new GridBagLayout());

        Dimension slotSize = new Dimension(TILE_SIZE, TILE_SIZE);

        // Create pattern lines aligned with wall (5x5 grid)
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = col;
                gbc.gridy = row;
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.fill = GridBagConstraints.NONE;

                JPanel slot = new JPanel();
                slot.setPreferredSize(slotSize);
                slot.setMinimumSize(slotSize);
                slot.setMaximumSize(slotSize);
                slot.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                slot.setOpaque(true);

                // Only show slots that form the staircase pattern
                slot.setVisible(col >= 4 - row);

                patternLines.add(slot, gbc);
            }
        }

        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.anchor = GridBagConstraints.EAST;
        wrapper.add(patternLines, wrapperGbc);
        return wrapper;
    }
    private JPanel createWallPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        JPanel wallPanel = new JPanel(new GridBagLayout());
        Dimension slotSize = new Dimension(TILE_SIZE, TILE_SIZE);

        // Create 5x5 wall grid with same alignment as pattern lines
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = col;
                gbc.gridy = row;
                gbc.insets = new Insets(2, 2, 2, 2);

                JPanel slot = new JPanel();
                slot.setPreferredSize(slotSize);
                slot.setMinimumSize(slotSize);
                slot.setMaximumSize(slotSize);
                slot.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                // Set initial semi-transparent colors based on wall pattern
                Color baseColor = getTileColor(Wall.WALL_PATTERN[row][col]);
                slot.setBackground(new Color(
                        baseColor.getRed(),
                        baseColor.getGreen(),
                        baseColor.getBlue(),
                        64
                ));

                wallPanel.add(slot, gbc);
            }
        }

        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.anchor = GridBagConstraints.WEST;
        wrapper.add(wallPanel, wrapperGbc);
        return wrapper;
    }
    private JPanel createFloorLine() {
        JPanel floorLinePanel = new JPanel(new GridLayout(1, 7, 2, 2));
        Dimension slotSize = new Dimension(TILE_SIZE, TILE_SIZE);

        for (int i = 0; i < 7; i++) {
            JPanel slot = new JPanel();
            slot.setPreferredSize(slotSize);
            slot.setMinimumSize(slotSize);
            slot.setMaximumSize(slotSize);
            slot.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            slot.setOpaque(true);
            floorLinePanel.add(slot);
        }

        // Wrap in panel to center it
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.add(floorLinePanel);
        return wrapper;
    }
    @Override
    public int getMaxActionSpace() {
        return 100;  // Maximum possible actions in Azul
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        AzulGameState state = (AzulGameState) gameState;
        updateFactories(state);
        updateCenterPool(state);
        for (int i = 0; i < state.getNPlayers(); i++) {
            updatePlayerBoard(i, state);
        }
    }
    private void updateFactories(AzulGameState state) {
        factoriesPanel.removeAll();
        for (GridBoard<AzulTile> factory : state.getFactories()) {
            JPanel factoryView = new JPanel(new GridLayout(2, 2, 2, 2));  // Added 2-pixel gaps
            factoryView.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));  // Added padding
            factoryView.setPreferredSize(new Dimension(FACTORY_SIZE, FACTORY_SIZE));

            for (int i = 0; i < factory.getHeight(); i++) {
                for (int j = 0; j < factory.getWidth(); j++) {
                    JPanel tilePanel = createTilePanel(factory.getElement(i, j));
                    tilePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    factoryView.add(tilePanel);
                }
            }
            factoriesPanel.add(Box.createHorizontalStrut(5));  // Add gap between factories
            factoriesPanel.add(factoryView);
        }
        factoriesPanel.revalidate();
        factoriesPanel.repaint();
    }
    private void updateCenterPool(AzulGameState state) {
        centerPoolPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);

        JPanel gridPanel = new JPanel(new GridLayout(3, 7, 2, 2));
        List<AzulTile> centerPool = state.getCenterPool();

        // Create slots and fill with tiles
        Dimension tileDim = new Dimension(TILE_SIZE, TILE_SIZE);
        for (int i = 0; i < 21; i++) {
            JPanel slot = new JPanel();
            slot.setPreferredSize(tileDim);
            slot.setMinimumSize(tileDim);
            slot.setMaximumSize(tileDim);
            slot.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            if (i < centerPool.size()) {
                slot.setBackground(getTileColor(centerPool.get(i).getTileType()));
            } else {
                slot.setBackground(null);
            }

            gridPanel.add(slot);
        }

        centerPoolPanel.add(gridPanel, gbc);
        centerPoolPanel.revalidate();
        centerPoolPanel.repaint();
    }

    private JPanel createTilePanel(AzulTile tile) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(TILE_SIZE, TILE_SIZE));
        if (tile != null) {
            panel.setBackground(getTileColor(tile.getTileType()));
        }
        return panel;
    }

    private Color getTileColor(AzulTile.TileType type) {
        if (type == null) return Color.LIGHT_GRAY;
        return switch (type) {
            case BLUE -> Color.BLUE;
            case RED -> Color.RED;
            case YELLOW -> Color.YELLOW;
            case BLACK -> Color.BLACK;
            case WHITE -> Color.WHITE;
        };
    }

    private void updatePlayerBoard(int playerId, AzulGameState state) {
        JPanel board = playerBoards[playerId];
        JPanel container = (JPanel) board.getComponent(0);
        JPanel upperSection = (JPanel) container.getComponent(0);

        // Get pattern lines and wall
        JPanel patternLines = (JPanel) ((JPanel) upperSection.getComponent(0)).getComponent(0);
        JPanel wall = (JPanel) ((JPanel) upperSection.getComponent(1)).getComponent(0);

        // Get floor line
        JPanel floorLineWrapper = (JPanel) container.getComponent(1);
        JPanel floorLine = (JPanel) ((JPanel) floorLineWrapper.getComponent(1)).getComponent(0);

        // Update pattern lines
        PatternLine[] lines = state.getPatternLines(playerId);
        for (int row = 0; row < 5; row++) {
            AzulTile[] tiles = lines[row].getTiles();  // Remove reverse order
            int lineLength = row + 1;

            for (int col = 0; col < lineLength; col++) {
                Component[] components = patternLines.getComponents();
                for (Component c : components) {
                    GridBagConstraints gbc = ((GridBagLayout)patternLines.getLayout()).getConstraints(c);
                    if (c instanceof JPanel && gbc.gridy == row && gbc.gridx == (4 - lineLength + col + 1)) {
                        JPanel slot = (JPanel)c;
                        if (col < tiles.length && tiles[col] != null) {
                            slot.setBackground(getTileColor(tiles[col].getTileType()));
                        } else {
                            slot.setBackground(null);
                        }
                    }
                }
            }
        }

        // Update wall with proper opacity
        Wall playerWall = state.getPlayerWall(playerId);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                JPanel slot = (JPanel) wall.getComponent(i * 5 + j);
                Color baseColor = getTileColor(Wall.WALL_PATTERN[i][j]);
                if (playerWall.isTilePlaced(i, j)) {
                    slot.setBackground(baseColor);
                } else {
                    slot.setBackground(new Color(
                            baseColor.getRed(),
                            baseColor.getGreen(),
                            baseColor.getBlue(),
                            64
                    ));
                }
            }
        }

        // Update floor line
        Counter floorLineCounter = state.getFloorLine(playerId);
        for (int i = 0; i < 7; i++) {
            JPanel slot = (JPanel) floorLine.getComponent(i);
            if (i < floorLineCounter.getValue()) {
                slot.setBackground(Color.GRAY);
            } else {
                slot.setBackground(null);
            }
        }
    }
}