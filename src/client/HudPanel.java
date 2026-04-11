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

    // UI components
    private final JLabel titleLabel = new JLabel("ChronoArena");
    private final JLabel playerNameLabel = new JLabel("Player: -");
    private final JLabel playerIdLabel = new JLabel("ID: -");
    private final JProgressBar timeBar = new JProgressBar();
    private final JLabel matchStatusLabel = new JLabel("Match: -");
    private final DefaultListModel<String> scoreboardModel = new DefaultListModel<>();
    private final JList<String> scoreboardList = new JList<>(scoreboardModel);
    private final JTextArea serverNoticeArea = new JTextArea();

    public HudPanel(ClientState clientState) {
        this.clientState = clientState;
        setPreferredSize(new Dimension(300, 700));
        setBackground(new Color(30, 30, 30));
        setForeground(Color.WHITE);
        setLayout(new BorderLayout(8, 8));

        buildUi();
        startRefreshTimer();
    }

    private void buildUi() {
        // Title
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(titleLabel, BorderLayout.NORTH);

        // center content
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel info = new JPanel(new GridLayout(0, 1));
        info.setOpaque(false);
        playerNameLabel.setForeground(Color.WHITE);
        playerIdLabel.setForeground(Color.WHITE);
        matchStatusLabel.setForeground(Color.WHITE);
        info.add(playerNameLabel);
        info.add(playerIdLabel);
        info.add(matchStatusLabel);
        center.add(info);

        center.add(Box.createVerticalStrut(8));

        JLabel controlsLabel = new JLabel("Controls: Move W/A/S/D, Freeze: Space/F");
        controlsLabel.setForeground(new Color(200, 200, 200));
        controlsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        center.add(controlsLabel);

        center.add(Box.createVerticalStrut(12));

        // time/progress
        timeBar.setStringPainted(true);
        timeBar.setMinimum(0);
        timeBar.setMaximum(1);
        timeBar.setValue(0);
        timeBar.setPreferredSize(new Dimension(260, 20));
        center.add(timeBar);

        center.add(Box.createVerticalStrut(12));

        JLabel scoreboardLabel = new JLabel("Scoreboard");
        scoreboardLabel.setForeground(Color.WHITE);
        scoreboardLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        center.add(scoreboardLabel);

        scoreboardList.setForeground(Color.WHITE);
        scoreboardList.setBackground(new Color(40, 40, 40));
        scoreboardList.setVisibleRowCount(8);
        JScrollPane scroller = new JScrollPane(scoreboardList);
        scroller.setPreferredSize(new Dimension(260, 200));
        center.add(scroller);

        center.add(Box.createVerticalStrut(12));

        JLabel noticeLabel = new JLabel("Server Notice");
        noticeLabel.setForeground(Color.WHITE);
        noticeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        center.add(noticeLabel);

        serverNoticeArea.setEditable(false);
        serverNoticeArea.setLineWrap(true);
        serverNoticeArea.setWrapStyleWord(true);
        serverNoticeArea.setBackground(new Color(40, 40, 40));
        serverNoticeArea.setForeground(Color.WHITE);
        serverNoticeArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        serverNoticeArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane noticeScroll = new JScrollPane(serverNoticeArea);
        noticeScroll.setPreferredSize(new Dimension(260, 120));
        center.add(noticeScroll);

        add(center, BorderLayout.CENTER);
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
                timeBar.setString("-");
                timeBar.setValue(0);
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

            // time bar
            int total = Math.max(1, Constants.MATCH_DURATION_SECONDS);
            int left = Math.max(0, snapshot.getTimeLeftSeconds());
            int value = Math.min(total, left);
            timeBar.setMaximum(total);
            timeBar.setValue(value);
            timeBar.setString(left + "s left");

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