package client;

import common.GameSnapshot;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

public class GameFrame extends JFrame {

    private final GameClient gameClient;
    private final GamePanel gamePanel;
    private final HudPanel hudPanel;

    public GameFrame(GameClient gameClient) {
        this.gameClient = gameClient;
        this.gamePanel = new GamePanel(gameClient.getClientState());
        this.hudPanel = new HudPanel(gameClient.getClientState());

        setTitle("ChronoArena");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        buildLayout();
        attachListeners();
        startRepaintLoop();

        pack();
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        // top area with simple title and quick controls
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JLabel title = new JLabel("ChronoArena");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(245, 245, 245));
        topBar.setBackground(new Color(45, 45, 45));
        topBar.add(title, BorderLayout.WEST);

        // root layout
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(30, 30, 30));
        root.add(topBar, BorderLayout.NORTH);
        root.add(gamePanel, BorderLayout.CENTER);
        root.add(hudPanel, BorderLayout.EAST);

        // status bar
        JLabel statusBar = new JLabel("Not connected");
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(40, 40, 40));
        statusBar.setForeground(Color.WHITE);
        root.add(statusBar, BorderLayout.SOUTH);

        // menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            gameClient.shutdown();
            dispose();
            System.exit(0);
        });
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        setContentPane(root);

        // periodically update status bar from client state
        Timer statusTimer = new Timer(500, e -> {
            SwingUtilities.invokeLater(() -> {
                boolean connected = gameClient.getClientState().isConnected();
                GameSnapshot snap = gameClient.getClientState().getLatestSnapshot();
                String status = connected ? (snap == null ? "Connected — waiting for game" : "Connected — in game") : "Not connected";
                statusBar.setText(status);
            });
        });
        statusTimer.start();
    }

    private void attachListeners() {
        InputController inputController = new InputController(gameClient.getUdpSender());
        addKeyListener(inputController);
        gamePanel.addKeyListener(inputController);
        setFocusable(true);
        gamePanel.setFocusable(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameClient.shutdown();
                dispose();
                System.exit(0);
            }
        });
    }

    private void startRepaintLoop() {
        Timer timer = new Timer(50, e -> {
            gamePanel.repaint();
            hudPanel.repaint();
        });
        timer.start();
    }
}