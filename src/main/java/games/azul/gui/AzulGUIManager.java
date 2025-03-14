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

import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class AzulGUIManager extends AbstractGUIManager {
    // Dimensions for components
    private final static int TILE_SIZE = 40;
    private final static int FACTORY_SIZE = TILE_SIZE * 2;

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

            // Create a split pane for game board and action panel
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setResizeWeight(0.7); // 70% for game board, 30% for action panel
            
            // Main game area
            mainGameBoard = new JPanel();
            mainGameBoard.setLayout(new BoxLayout(mainGameBoard, BoxLayout.Y_AXIS));

            // Create panel for factories and centre pool
            JPanel topArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

            // Add factories
            factoriesPanel = new JPanel();
            factoriesPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            factoriesPanel.setBorder(BorderFactory.createTitledBorder("Factories"));
            topArea.add(factoriesPanel);

            // Add centre pool
            centerPoolPanel = new JPanel();
            centerPoolPanel.setLayout(new GridBagLayout());
            centerPoolPanel.setBorder(BorderFactory.createTitledBorder("Centre Pool"));
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
            
            // Add game board to the left side of split pane
            splitPane.setLeftComponent(new JScrollPane(mainGameBoard));

            // Action panel on the right side with vertical scrolling
            JPanel actionContainer = new JPanel();
            actionContainer.setLayout(new BoxLayout(actionContainer, BoxLayout.Y_AXIS));
            
            // Add a title for the action panel
            JLabel actionTitle = new JLabel("Available Actions");
            actionTitle.setFont(new Font(actionTitle.getFont().getName(), Font.BOLD, 14));
            actionTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            actionTitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
            actionContainer.add(actionTitle);
            
            // Create the action panel with vertical layout
            JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width / 3 - 20, height - 50, true, true, null, null, null);
            actionContainer.add(actionPanel);
            
            // Add some padding at the bottom
            actionContainer.add(Box.createVerticalStrut(10));
            
            // Create a scroll pane for the action container
            JScrollPane actionScroll = new JScrollPane(actionContainer);
            actionScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            actionScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            actionScroll.setBorder(BorderFactory.createEmptyBorder());
            
            // Add the action scroll pane to the right side of the split pane
            splitPane.setRightComponent(actionScroll);
            
            // Add split pane to the centre of the main layout
            parent.add(splitPane, BorderLayout.CENTER);
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

        // Floor line section centred under pattern lines and wall
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
        JPanel wrapper = new JPanel(new GridBagLayout());
        JPanel patternLines = new JPanel(new GridBagLayout());

        Dimension slotSize = new Dimension(TILE_SIZE, TILE_SIZE);

        // Create pattern lines aligned with wall (5x5 grid)
        for (int row = 0; row < 5; row++) {
            // Add row label to the left of each pattern line
            GridBagConstraints labelGbc = new GridBagConstraints();
            labelGbc.gridx = 0;
            labelGbc.gridy = row;
            labelGbc.insets = new Insets(2, 5, 2, 10);
            labelGbc.anchor = GridBagConstraints.EAST;
            
            JLabel rowLabel = new JLabel("Line " + row + ":");
            rowLabel.setFont(new Font(rowLabel.getFont().getName(), Font.BOLD, 12));
            patternLines.add(rowLabel, labelGbc);
            
            for (int col = 0; col < 5; col++) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = col + 1;  // Shift right to make room for labels
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

                // Set initial semi-transparent colours based on wall pattern
                Color baseColour = getTileColour(Wall.WALL_PATTERN[row][col]);
                slot.setBackground(new Color(
                        baseColour.getRed(),
                        baseColour.getGreen(),
                        baseColour.getBlue(),
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
        JPanel floorLinePanel = new JPanel(new GridLayout(2, 7, 2, 0));
        Dimension slotSize = new Dimension(TILE_SIZE, TILE_SIZE);
        
        // Add penalty labels above the floor line slots
        // Show the actual individual penalties for each position, not cumulative
        int[] penalties = {-1, -1, -2, -2, -2, -3, -3};
        for (int i = 0; i < 7; i++) {
            JLabel penaltyLabel = new JLabel(String.valueOf(penalties[i]), JLabel.CENTER);
            penaltyLabel.setForeground(Color.RED);
            penaltyLabel.setFont(new Font(penaltyLabel.getFont().getName(), Font.BOLD, 12));
            floorLinePanel.add(penaltyLabel);
        }

        // Add floor line slots
        for (int i = 0; i < 7; i++) {
            JPanel slot = new JPanel();
            slot.setPreferredSize(slotSize);
            slot.setMinimumSize(slotSize);
            slot.setMaximumSize(slotSize);
            slot.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            slot.setOpaque(true);
            floorLinePanel.add(slot);
        }

        // Wrap in panel to centre it
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.add(floorLinePanel);
        return wrapper;
    }
    
    @Override
    public int getMaxActionSpace() {
        return 100;  // Maximum possible actions in Azul
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        // Call the parent implementation first
        super.updateActionButtons(player, gameState);
        
        // Then customise the buttons for better visibility
        if (actionButtons != null) {
            for (ActionButton button : actionButtons) {
                if (button.isVisible()) {
                    // Set preferred size for better visibility
                    button.setPreferredSize(new Dimension(width / 3 - 50, 50));
                    button.setMaximumSize(new Dimension(width / 3 - 50, 50));
                    
                    // Make text more readable
                    button.setFont(new Font(button.getFont().getName(), Font.PLAIN, 12));
                    
                    // Ensure text wrapping for long action descriptions
                    if (button.getText().length() > 30) {
                        String text = button.getText();
                        StringBuilder wrappedText = new StringBuilder("<html>");
                        int charsPerLine = 30;
                        
                        for (int i = 0; i < text.length(); i += charsPerLine) {
                            wrappedText.append(text.substring(i, Math.min(i + charsPerLine, text.length())));
                            if (i + charsPerLine < text.length()) {
                                wrappedText.append("<br>");
                            }
                        }
                        
                        wrappedText.append("</html>");
                        button.setText(wrappedText.toString());
                    }
                    
                    // Improve button appearance
                    button.setHorizontalAlignment(SwingConstants.LEFT);
                    button.setMargin(new Insets(5, 5, 5, 5));
                    button.setFocusPainted(false);
                }
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        AzulGameState state = (AzulGameState) gameState;
        updateFactories(state);
        updateCentrePool(state);
        
        // Update all player boards
        for (int i = 0; i < state.getNPlayers(); i++) {
            updatePlayerBoard(i, state);
        }
        
        // Add a visual indicator for the current player
        highlightCurrentPlayer(state);
    }
    
    /**
     * Adds a visual indicator to show which player's turn it is
     */
    private void highlightCurrentPlayer(AzulGameState state) {
        int currentPlayer = state.getCurrentPlayer();
        
        // Update all player boards with appropriate highlighting
        for (int i = 0; i < playerBoards.length; i++) {
            JPanel board = playerBoards[i];
            boolean isCurrentPlayer = (i == currentPlayer);
            
            // Set a distinctive border for the current player
            board.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(
                    isCurrentPlayer ? new Color(0, 150, 0) : Color.GRAY, 
                    isCurrentPlayer ? 3 : 1
                ),
                "Player " + i + 
                (isCurrentPlayer ? " (Current)" : "") + 
                " - Score: " + state.getScore(i).getValue()
            ));
            
            // Add a background tint to the current player's board
            if (isCurrentPlayer) {
                board.setBackground(new Color(240, 255, 240)); // Light green tint
            } else {
                board.setBackground(UIManager.getColor("Panel.background"));
            }
        }
    }
    
    private void updateFactories(AzulGameState state) {
        factoriesPanel.removeAll();
        
        // Add a grid layout for factories with labels
        JPanel factoriesGrid = new JPanel(new GridLayout(0, 3, 10, 10));
        
        for (int i = 0; i < state.getFactories().size(); i++) {
            // Create a panel for each factory with its label
            JPanel factoryWithLabel = new JPanel(new BorderLayout(5, 5));
            
            // Add factory label
            JLabel factoryLabel = new JLabel("Factory " + i, JLabel.CENTER);
            factoryLabel.setFont(new Font(factoryLabel.getFont().getName(), Font.BOLD, 12));
            factoryWithLabel.add(factoryLabel, BorderLayout.NORTH);
            
            // Create the factory display
            GridBoard<AzulTile> factory = state.getFactories().get(i);
            JPanel factoryView = new JPanel(new GridLayout(2, 2, 2, 2));
            factoryView.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            factoryView.setPreferredSize(new Dimension(FACTORY_SIZE, FACTORY_SIZE));

            for (int row = 0; row < factory.getHeight(); row++) {
                for (int col = 0; col < factory.getWidth(); col++) {
                    JPanel tilePanel = createTilePanel(factory.getElement(row, col));
                    tilePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    factoryView.add(tilePanel);
                }
            }
            
            factoryWithLabel.add(factoryView, BorderLayout.CENTER);
            factoriesGrid.add(factoryWithLabel);
        }
        
        factoriesPanel.add(factoriesGrid);
        factoriesPanel.revalidate();
        factoriesPanel.repaint();
    }
    
    private void updateCentrePool(AzulGameState state) {
        centerPoolPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);

        JPanel gridPanel = new JPanel(new GridLayout(3, 7, 2, 2));
        List<AzulTile> centrePool = state.getCenterPool();

        // Create slots and fill with tiles
        Dimension tileDim = new Dimension(TILE_SIZE, TILE_SIZE);
        for (int i = 0; i < 21; i++) {
            JPanel slot = new JPanel();
            slot.setPreferredSize(tileDim);
            slot.setMinimumSize(tileDim);
            slot.setMaximumSize(tileDim);
            slot.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            if (i < centrePool.size()) {
                AzulTile tile = centrePool.get(i);
                if (tile.getTileType() == null) {
                    // This is the first player token
                    slot.setBackground(new Color(255, 215, 0)); // Gold colour
                    JLabel tokenLabel = new JLabel("F", JLabel.CENTER);
                    tokenLabel.setFont(new Font(tokenLabel.getFont().getName(), Font.BOLD, 16));
                    tokenLabel.setForeground(Color.BLACK);
                    slot.add(tokenLabel);
                } else {
                    slot.setBackground(getTileColour(tile.getTileType()));
                }
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
            panel.setBackground(getTileColour(tile.getTileType()));
        }
        return panel;
    }

    private Color getTileColour(AzulTile.TileType type) {
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
            AzulTile[] tiles = lines[row].getTiles();
            int lineLength = row + 1;

            for (int col = 0; col < lineLength; col++) {
                Component[] components = patternLines.getComponents();
                for (Component c : components) {
                    if (c instanceof JPanel) {
                        GridBagConstraints gbc = ((GridBagLayout)patternLines.getLayout()).getConstraints(c);
                        // Adjust for the label column (gridx is now offset by 1)
                        if (gbc.gridy == row && gbc.gridx == (col + 1 + (4 - lineLength + 1))) {
                            JPanel slot = (JPanel)c;
                            if (col < tiles.length && tiles[col] != null) {
                                slot.setBackground(getTileColour(tiles[col].getTileType()));
                            } else {
                                slot.setBackground(null);
                            }
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
                Color baseColour = getTileColour(Wall.WALL_PATTERN[i][j]);
                if (playerWall.isTilePlaced(i, j)) {
                    slot.setBackground(baseColour);
                } else {
                    slot.setBackground(new Color(
                            baseColour.getRed(),
                            baseColour.getGreen(),
                            baseColour.getBlue(),
                            64
                    ));
                }
            }
        }

        // Update floor line - skip the first row which contains penalty labels
        Counter floorLineCounter = state.getFloorLine(playerId);
        int floorTileCount = floorLineCounter.getValue();
        boolean isFirstPlayer = (playerId == state.getFirstPlayer());
        
        // Update floor line tiles with more visible indicators
        // Get only the tile slots (second row of the grid)
        for (int i = 0; i < 7; i++) {
            // Get the slot at position 7+i (after the 7 penalty labels)
            JPanel slot = (JPanel) floorLine.getComponent(7 + i);
            slot.removeAll(); // Clear any existing components
            
            if (i < floorTileCount) {
                // Special handling for the first player token
                if (isFirstPlayer && i == 0) {
                    slot.setBackground(new Color(255, 215, 0)); // Gold colour for first player token
                    JLabel tokenLabel = new JLabel("F", JLabel.CENTER);
                    tokenLabel.setFont(new Font(tokenLabel.getFont().getName(), Font.BOLD, 16));
                    tokenLabel.setForeground(Color.BLACK);
                    slot.add(tokenLabel);
                    slot.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                } else {
                    slot.setBackground(Color.DARK_GRAY);
                    slot.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                }
            } else {
                slot.setBackground(null);
                slot.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            }
            
            slot.revalidate();
            slot.repaint();
        }
    }
}