package client;

import common.Constants;
import common.GameSnapshot;
import common.PlayerState;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class HudPanel extends JPanel {

    private final ClientState clientState;
    private final IntConsumer voteCallback;

    private final JLabel titleLabel = new JLabel("ChronoArena");
    private final JLabel fpsLabel = new JLabel("FPS: -");
    private final JLabel playerNameLabel = new JLabel("Player: -");
    private final JLabel playerIdLabel = new JLabel("ID: -");
    private final CircularTimer circularTimer = new CircularTimer();
    private final JLabel matchStatusLabel = new JLabel("Match: -");
    private final DefaultListModel<String> scoreboardModel = new DefaultListModel<>();
    private final JList<String> scoreboardList = new JList<>(scoreboardModel);
    private final JTextArea serverNoticeArea = new JTextArea();

    private final JPanel votePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
    private final JLabel voteHintLabel = new JLabel("Vote match length:");

    private static final Color PANEL_BG = new Color(25, 28, 34);
    private static final Color CARD_BG = new Color(34, 38, 46);
    private static final Color CARD_ALT = new Color(28, 32, 40);
    private static final Color LINE = new Color(85, 95, 110);
    private static final Color TEXT = new Color(240, 240, 245);
    private static final Color MUTED = new Color(180, 185, 195);
    private static final Color LOCAL_HIGHLIGHT = new Color(68, 88, 145);

    public HudPanel(ClientState clientState, IntConsumer voteCallback) {
        this.clientState = clientState;
        this.voteCallback = voteCallback;

        setPreferredSize(new Dimension(310, 700));
        setBackground(PANEL_BG);
        setForeground(TEXT);
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        buildUi();
        startRefreshTimer();
    }

    private void buildUi() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(true);
        top.setBackground(new Color(30, 34, 42));
        top.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(LINE, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(TEXT);

        fpsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fpsLabel.setForeground(MUTED);

        top.add(titleLabel, BorderLayout.WEST);
        top.add(fpsLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel playerPanel = createCard("Player");
        playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));

        styleInfoLabel(playerNameLabel, 16, true);
        styleInfoLabel(playerIdLabel, 15, false);
        styleInfoLabel(matchStatusLabel, 15, false);

        playerPanel.add(playerNameLabel);
        playerPanel.add(Box.createVerticalStrut(4));
        playerPanel.add(playerIdLabel);
        playerPanel.add(Box.createVerticalStrut(2));
        playerPanel.add(matchStatusLabel);

        center.add(playerPanel);
        center.add(Box.createVerticalStrut(10));

        JLabel controlsLabel = new JLabel("Move: W/A/S/D   Freeze: Space/F");
        controlsLabel.setForeground(MUTED);
        controlsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        controlsLabel.setBorder(new EmptyBorder(0, 4, 4, 4));
        controlsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(controlsLabel);
        center.add(Box.createVerticalStrut(8));

        JPanel timePanel = createCard("Match Time");
        timePanel.setLayout(new BorderLayout());
        circularTimer.setPreferredSize(new Dimension(150, 150));

        JPanel timerWrap = new JPanel();
        timerWrap.setOpaque(false);
        timerWrap.setLayout(new BoxLayout(timerWrap, BoxLayout.Y_AXIS));
        circularTimer.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerWrap.add(Box.createVerticalStrut(6));
        timerWrap.add(circularTimer);
        timerWrap.add(Box.createVerticalStrut(8));
        timePanel.add(timerWrap, BorderLayout.CENTER);

        center.add(timePanel);
        center.add(Box.createVerticalStrut(10));

        JPanel votingCard = createCard("Vote");
        votingCard.setLayout(new BoxLayout(votingCard, BoxLayout.Y_AXIS));

        voteHintLabel.setForeground(MUTED);
        voteHintLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        voteHintLabel.setBorder(new EmptyBorder(0, 2, 6, 2));

        votePanel.setOpaque(false);

        addVoteButton(votePanel, 60);
        addVoteButton(votePanel, 90);
        addVoteButton(votePanel, 120);
        addVoteButton(votePanel, 180);

        votingCard.add(voteHintLabel);
        votingCard.add(votePanel);

        center.add(votingCard);
        center.add(Box.createVerticalStrut(10));

        scoreboardList.setForeground(TEXT);
        scoreboardList.setBackground(CARD_ALT);
        scoreboardList.setSelectionBackground(new Color(70, 75, 90));
        scoreboardList.setSelectionForeground(TEXT);
        scoreboardList.setFixedCellHeight(22);
        scoreboardList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        scoreboardList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        scoreboardList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                c.setBorder(new EmptyBorder(2, 6, 2, 6));

                if (!isSelected) {
                    c.setBackground(index % 2 == 0 ? CARD_ALT : CARD_BG);
                    c.setForeground(TEXT);
                }

                if (value instanceof String s) {
                    String localPrefix = "#" + clientState.getLocalPlayerId();
                    if (s.startsWith(localPrefix)) {
                        c.setBackground(LOCAL_HIGHLIGHT);
                        c.setForeground(Color.WHITE);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                }

                return c;
            }
        });

        JScrollPane scoreScroll = new JScrollPane(scoreboardList);
        scoreScroll.setPreferredSize(new Dimension(275, 180));
        scoreScroll.setBorder(BorderFactory.createEmptyBorder());
        scoreScroll.getViewport().setBackground(CARD_ALT);

        JPanel scorePanel = createCard("Scoreboard");
        scorePanel.setLayout(new BorderLayout());
        scorePanel.add(scoreScroll, BorderLayout.CENTER);

        center.add(scorePanel);
        center.add(Box.createVerticalStrut(10));

        serverNoticeArea.setEditable(false);
        serverNoticeArea.setLineWrap(true);
        serverNoticeArea.setWrapStyleWord(true);
        serverNoticeArea.setBackground(CARD_ALT);
        serverNoticeArea.setForeground(TEXT);
        serverNoticeArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        serverNoticeArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane noticeScroll = new JScrollPane(serverNoticeArea);
        noticeScroll.setPreferredSize(new Dimension(275, 120));
        noticeScroll.setBorder(BorderFactory.createEmptyBorder());
        noticeScroll.getViewport().setBackground(CARD_ALT);

        JPanel noticePanel = createCard("Server Notice");
        noticePanel.setLayout(new BorderLayout());
        noticePanel.add(noticeScroll, BorderLayout.CENTER);

        center.add(noticePanel);

        add(center, BorderLayout.CENTER);
    }

    private void addVoteButton(JPanel panel, int seconds) {
        JButton button = new JButton(seconds + "s");
        button.setFocusPainted(false);
        button.setBackground(new Color(62, 84, 130));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        button.addActionListener(e -> {
            if (voteCallback != null) {
                voteCallback.accept(seconds);
            }
        });
        panel.add(button);
    }

    private JPanel createCard(String title) {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(CARD_BG);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(LINE, 1, true),
                title
        );
        border.setTitleColor(new Color(220, 225, 235));
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 13));

        panel.setBorder(BorderFactory.createCompoundBorder(
                border,
                new EmptyBorder(8, 8, 8, 8)
        ));

        return panel;
    }

    private void styleInfoLabel(JLabel label, int size, boolean bold) {
        label.setForeground(TEXT);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setBorder(new EmptyBorder(2, 2, 2, 2));
    }

    public void setFps(int fps) {
        SwingUtilities.invokeLater(() -> fpsLabel.setText("FPS: " + fps));
    }

    private class CircularTimer extends JComponent {
        private int totalSeconds = 1;
        private int leftSeconds = 0;
        private float pulse = 0f;
        private final Timer anim;

        CircularTimer() {
            setOpaque(false);
            anim = new Timer(80, e -> {
                pulse += 0.08f;
                if (pulse > Float.MAX_VALUE - 1) {
                    pulse = 0f;
                }
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
                int size = Math.min(w, h) - 8;
                int cx = w / 2;
                int cy = h / 2;

                float pct = Math.max(0f, Math.min(1f, (float) leftSeconds / (float) totalSeconds));

                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillOval(cx - size / 2, cy - size / 2, size, size);

                int thickness = Math.max(12, size / 10);

                Stroke oldStroke = g2.getStroke();
                g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                g2.setColor(new Color(70, 78, 90));
                g2.drawArc(cx - size / 2, cy - size / 2, size, size, 90, -360);

                Color col;
                if (pct > 0.66f) {
                    col = blend(new Color(90, 210, 110), new Color(220, 210, 90), (pct - 0.66f) / 0.34f);
                } else if (pct > 0.33f) {
                    col = blend(new Color(220, 210, 90), new Color(235, 150, 90), (pct - 0.33f) / 0.33f);
                } else {
                    col = blend(new Color(235, 150, 90), new Color(220, 85, 85), pct / 0.33f);
                }

                if (leftSeconds <= Math.min(5, totalSeconds / 6)) {
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER,
                            0.88f + 0.12f * (float) Math.abs(Math.sin(pulse))
                    ));
                }

                int arc = (int) (-360f * pct);
                g2.setColor(col);
                g2.drawArc(cx - size / 2, cy - size / 2, size, size, 90, arc);
                g2.setStroke(oldStroke);
                g2.setComposite(AlphaComposite.SrcOver);

                g2.setColor(TEXT);
                g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(20, size / 6)));
                String label = leftSeconds + "s";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(label);
                int th = fm.getAscent();
                g2.drawString(label, cx - tw / 2, cy + th / 3);

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
                matchStatusLabel.setText("Match: Waiting");
                scoreboardModel.clear();
                serverNoticeArea.setText("Waiting for game state...");
                votePanel.setVisible(false);
                voteHintLabel.setVisible(false);
                return;
            }

            playerNameLabel.setText("Player: " + clientState.getLocalPlayerName());
            playerIdLabel.setText("ID: " + clientState.getLocalPlayerId());

            String matchStatus;
            if (snapshot.isMatchEnded()) {
                matchStatus = "Ended";
            } else if (snapshot.isMatchRunning()) {
                matchStatus = "Running";
            } else {
                matchStatus = "Waiting";
            }
            matchStatusLabel.setText("Match: " + matchStatus);

            int total = Math.max(1, Constants.MATCH_DURATION_SECONDS);
            int left = Math.max(0, snapshot.getTimeLeftSeconds());
            circularTimer.setTime(total, left);

            List<PlayerState> players = new ArrayList<>(snapshot.getPlayers());
            players.sort(Comparator.comparingInt(PlayerState::getScore).reversed()
                    .thenComparingInt(PlayerState::getPlayerId));

            scoreboardModel.clear();
            for (PlayerState p : players) {
                String label = String.format("#%d %-12s %4d", p.getPlayerId(), p.getPlayerName(), p.getScore());
                scoreboardModel.addElement(label);
            }

            serverNoticeArea.setText(snapshot.getServerNotice() == null ? "-" : snapshot.getServerNotice());

            boolean showVoting = !snapshot.isMatchRunning() && !snapshot.isMatchEnded();
            votePanel.setVisible(showVoting);
            voteHintLabel.setVisible(showVoting);
        });
    }
}