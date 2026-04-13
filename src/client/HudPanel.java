package client;

import common.Constants;
import common.GameSnapshot;
import common.PlayerState;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

public class HudPanel extends JPanel {

    private final ClientState clientState;
    private final VoteSubmitter voteSubmitter;

    // UI components
    private final JLabel titleLabel = new JLabel("ChronoArena");
    private final JLabel fpsLabel = new JLabel("FPS: -");
    private final JLabel playerNameLabel = new JLabel("Player: -");
    private final JLabel playerIdLabel = new JLabel("ID: -");
    private final CircularTimer circularTimer = new CircularTimer();
    private final JLabel matchStatusLabel = new JLabel("Match: -");
    private final DefaultListModel<String> scoreboardModel = new DefaultListModel<>();
    private final JList<String> scoreboardList = new JList<>(scoreboardModel);
    private final JTextArea serverNoticeArea = new JTextArea();

    // Lobby vote panel
    private JPanel lobbyVotePanel;
    private JLabel lobbyCountdownLabel;
    private final JButton[] voteButtons = new JButton[4];
    private volatile int playerVoteSeconds = -1;

    public HudPanel(ClientState clientState, VoteSubmitter voteSubmitter) {
        this.clientState = clientState;
        this.voteSubmitter = voteSubmitter;
        setPreferredSize(new Dimension(300, 700));
        setBackground(new Color(30, 30, 30));
        setForeground(Color.WHITE);
        setLayout(new BorderLayout(8, 8));

        buildUi();
        startRefreshTimer();
    }

    private void buildUi() {
        // Title + FPS row
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        fpsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fpsLabel.setForeground(new Color(200, 200, 200));
        fpsLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    JPanel top = new JPanel(new BorderLayout());
    // distinct top area color
    top.setOpaque(true);
    top.setBackground(new Color(35, 38, 45));
        top.add(titleLabel, BorderLayout.WEST);
        top.add(fpsLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // center content with titled sections for clarity
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Player info section
    JPanel playerPanel = new JPanel();
    playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
    // blue-ish player section
    playerPanel.setOpaque(true);
    playerPanel.setBackground(new Color(38, 56, 92));
    playerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)), "Player"));
        playerNameLabel.setForeground(Color.WHITE);
        playerIdLabel.setForeground(Color.WHITE);
        matchStatusLabel.setForeground(Color.WHITE);
        playerNameLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        playerIdLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 4, 6));
        matchStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
        playerPanel.add(playerNameLabel);
        playerPanel.add(playerIdLabel);
        playerPanel.add(matchStatusLabel);
        center.add(playerPanel);

        center.add(Box.createVerticalStrut(8));

        // Controls (small note)
        JLabel controlsLabel = new JLabel("Controls: Move W/A/S/D  Freeze: Space/F");
        controlsLabel.setForeground(new Color(200, 200, 200));
        controlsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        controlsLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        center.add(controlsLabel);

    // Match timer section (circular)
    JPanel timePanel = new JPanel(new GridBagLayout());
    // red-ish time section
    timePanel.setOpaque(true);
    timePanel.setBackground(new Color(92, 46, 46));
    timePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)), "Match Time"));
    circularTimer.setPreferredSize(new Dimension(140, 140));
    timePanel.setLayout(new BorderLayout());
    JPanel timerWrap = new JPanel();
    timerWrap.setOpaque(false);
    timerWrap.setLayout(new BoxLayout(timerWrap, BoxLayout.Y_AXIS));
    circularTimer.setAlignmentX(Component.CENTER_ALIGNMENT);
    timerWrap.add(Box.createVerticalStrut(6));
    timerWrap.add(circularTimer);
    timerWrap.add(Box.createVerticalStrut(8));
        // Removed numeric timer label
    timerWrap.add(Box.createVerticalStrut(6));
    timePanel.add(timerWrap, BorderLayout.CENTER);
    center.add(timePanel);
    timePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalStrut(8));

        // Scoreboard section
        scoreboardList.setForeground(Color.WHITE);
        scoreboardList.setBackground(new Color(40, 40, 40));
        scoreboardList.setVisibleRowCount(8);
        scoreboardList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String s) {
                    String localPrefix = "#" + clientState.getLocalPlayerId();
                    if (s.startsWith(localPrefix)) {
                        c.setBackground(new Color(80, 80, 140));
                        c.setForeground(Color.WHITE);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        });
        JScrollPane scroller = new JScrollPane(scoreboardList);
        scroller.setPreferredSize(new Dimension(260, 180));
    JPanel scorePanel = new JPanel(new BorderLayout());
    scorePanel.setOpaque(true);
    scorePanel.setBackground(new Color(46, 92, 60));
    scorePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)), "Scoreboard"));
    scroller.getViewport().setBackground(new Color(46, 92, 60));
    scorePanel.add(scroller, BorderLayout.CENTER);
        center.add(scorePanel);

        center.add(Box.createVerticalStrut(8));

        // Server notice
        serverNoticeArea.setEditable(false);
        serverNoticeArea.setLineWrap(true);
        serverNoticeArea.setWrapStyleWord(true);
        serverNoticeArea.setBackground(new Color(40, 40, 40));
        serverNoticeArea.setForeground(Color.WHITE);
        serverNoticeArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        serverNoticeArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane noticeScroll = new JScrollPane(serverNoticeArea);
        noticeScroll.setPreferredSize(new Dimension(260, 120));
    JPanel noticePanel = new JPanel(new BorderLayout());
    // purple-ish notice section
    noticePanel.setOpaque(true);
    noticePanel.setBackground(new Color(80, 50, 100));
    noticePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)), "Server Notice"));
    noticeScroll.getViewport().setBackground(new Color(80, 50, 100));
    noticePanel.add(noticeScroll, BorderLayout.CENTER);
        center.add(noticePanel);

        // Lobby vote panel (hidden by default, shown during LOBBY phase)
        lobbyVotePanel = buildLobbyVotePanel();
        center.add(Box.createVerticalStrut(8));
        center.add(lobbyVotePanel);

        add(center, BorderLayout.CENTER);
    }

    private JPanel buildLobbyVotePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(new Color(55, 45, 80));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(120, 90, 180)), "Vote: Match Duration"));
        panel.setVisible(false);

        lobbyCountdownLabel = new JLabel("Lobby closes in: --s", SwingConstants.CENTER);
        lobbyCountdownLabel.setForeground(new Color(200, 180, 255));
        lobbyCountdownLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lobbyCountdownLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        panel.add(lobbyCountdownLabel, BorderLayout.NORTH);

        int[] durations = {30, 60, 120, 300};
        String[] labels  = {"30 sec", "1 min", "2 min", "5 min"};

        JPanel buttonRow = new JPanel(new GridLayout(2, 2, 6, 6));
        buttonRow.setOpaque(false);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        for (int i = 0; i < 4; i++) {
            int durationSecs = durations[i];
            JButton btn = new JButton(labels[i]);
            btn.setFont(new Font("SansSerif", Font.BOLD, 13));
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(80, 60, 120));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(120, 90, 180), 1),
                    BorderFactory.createEmptyBorder(6, 4, 6, 4)));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> {
                if (voteSubmitter != null) {
                    voteSubmitter.submitVote(durationSecs);
                }
                playerVoteSeconds = durationSecs;
                updateVoteButtonStyles();
            });

            voteButtons[i] = btn;
            buttonRow.add(btn);
        }

        panel.add(buttonRow, BorderLayout.CENTER);
        return panel;
    }

    /** Highlights the selected vote button and dims the others. */
    private void updateVoteButtonStyles() {
        int[] durations = {30, 60, 120, 300};
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < voteButtons.length; i++) {
                if (voteButtons[i] == null) continue;
                boolean chosen = (durations[i] == playerVoteSeconds);
                voteButtons[i].setBackground(chosen ? new Color(120, 80, 200) : new Color(80, 60, 120));
                voteButtons[i].setForeground(chosen ? Color.WHITE : new Color(200, 200, 200));
                voteButtons[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                chosen ? new Color(180, 140, 255) : new Color(120, 90, 180), chosen ? 2 : 1),
                        BorderFactory.createEmptyBorder(6, 4, 6, 4)));
            }
        });
    }

    public void setFps(int fps) {
        SwingUtilities.invokeLater(() -> fpsLabel.setText("FPS: " + fps));
    }

    // Custom circular timer component
    private class CircularTimer extends JComponent {
        private int totalSeconds = 1;
        private int leftSeconds = 0;
        private float pulse = 0f;
        private final Timer anim;

        CircularTimer() {
            setOpaque(false);
            anim = new Timer(80, e -> {
                // animate pulse for subtle movement
                pulse += 0.08f;
                if (pulse > Float.MAX_VALUE - 1) pulse = 0f;
                repaint();
            });
            anim.start();
        }

        void setTime(int total, int left) {
            this.totalSeconds = Math.max(1, total);
            this.leftSeconds = Math.max(0, left);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int size = Math.min(w, h);
                int cx = w / 2;
                int cy = h / 2;

                float pct = Math.max(0f, Math.min(1f, (float) leftSeconds / (float) totalSeconds));

                // background ring
                int ring = Math.max(8, size / 10);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillOval(cx - size/2, cy - size/2, size, size);

                // compute color gradient (green -> yellow -> red)
                Color col;
                if (pct > 0.66f) {
                    col = blend(new Color(80, 200, 100), new Color(200, 200, 80), (pct - 0.66f) / 0.34f);
                } else if (pct > 0.33f) {
                    col = blend(new Color(200, 200, 80), new Color(220, 140, 80), (pct - 0.33f) / 0.33f);
                } else {
                    col = blend(new Color(220, 140, 80), new Color(200, 80, 80), pct / 0.33f);
                }

                // pulse modulation when low
                if (leftSeconds <= Math.min(5, totalSeconds / 6)) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f + 0.1f * (float)Math.abs(Math.sin(pulse))));
                }

                // draw arc
                int outer = size - 8;
                int thickness = Math.max(10, ring);
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(col);
                int start = 90;
                int arc = (int) (-360f * pct);
                g2.drawArc(cx - outer/2, cy - outer/2, outer, outer, start, arc);
                g2.setStroke(old);

                // central label
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(18, size/6)));
                String label = leftSeconds + "s";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(label);
                int th = fm.getAscent();
                g2.drawString(label, cx - tw/2, cy + th/3);

            } finally {
                g2.dispose();
            }
        }

        private Color blend(Color a, Color b, float t) {
            t = Math.max(0f, Math.min(1f, t));
            int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
            int g = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
            int bl = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
            return new Color(r, g, bl);
        }
    }

    private void startRefreshTimer() {
        Timer t = new Timer(500, e -> refreshFromState());
        t.start();
    }

    private void refreshFromState() {
    SwingUtilities.invokeLater(() -> {
            GameSnapshot snapshot = clientState.getLatestSnapshot();
            if (snapshot == null) {
                playerNameLabel.setText("Player: " + clientState.getLocalPlayerName());
                playerIdLabel.setText("ID: " + clientState.getLocalPlayerId());
                matchStatusLabel.setText("Match: waiting");
                scoreboardModel.clear();
                serverNoticeArea.setText("Waiting for game state...");
                lobbyVotePanel.setVisible(false);
                return;
            }

            playerNameLabel.setText("Player: " + clientState.getLocalPlayerName());
            playerIdLabel.setText("ID: " + clientState.getLocalPlayerId());

            boolean inLobby = snapshot.isMatchLobby();

            String matchStatus;
            if (snapshot.isMatchEnded()) {
                matchStatus = "Ended";
            } else if (snapshot.isMatchRunning()) {
                matchStatus = "Running";
            } else {
                matchStatus = "Lobby";
            }
            matchStatusLabel.setText("Match: " + matchStatus);

            // Show/hide lobby vote panel
            if (inLobby) {
                lobbyVotePanel.setVisible(true);
                int lobbyLeft = snapshot.getTimeLeftSeconds(); // reused field during lobby
                lobbyCountdownLabel.setText("Lobby closes in: " + Math.max(0, lobbyLeft) + "s");
                updateVoteButtonStyles();
                // Reset vote choice if the lobby has just re-opened (e.g. new round)
            } else {
                lobbyVotePanel.setVisible(false);
                if (snapshot.isMatchRunning()) {
                    // Clear remembered vote so it's fresh next lobby
                    playerVoteSeconds = -1;
                }
            }

            // match timer — only meaningful while running
            int total = Math.max(1, Constants.MATCH_DURATION_SECONDS);
            int left = Math.max(0, snapshot.getTimeLeftSeconds());
            circularTimer.setTime(total, left);

            // scoreboard
            List<PlayerState> players = new ArrayList<>(snapshot.getPlayers());
            players.sort(Comparator.comparingInt(PlayerState::getScore).reversed()
                    .thenComparingInt(PlayerState::getPlayerId));
            scoreboardModel.clear();
            for (PlayerState p : players) {
                String label = String.format("#%d %-12s %4d", p.getPlayerId(), p.getPlayerName(), p.getScore());
                scoreboardModel.addElement(label);
            }

            serverNoticeArea.setText(snapshot.getServerNotice() == null ? "-" : snapshot.getServerNotice());
        });
    }
}