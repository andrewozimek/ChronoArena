package client;

import javax.swing.*;
import java.awt.*;

public class ConnectDialog extends JDialog {

    public static class ConnectInfo {
        private final String serverHost;
        private final int tcpPort;
        private final int udpPort;
        private final String playerName;
        private final int localUdpPort;

        public ConnectInfo(String serverHost, int tcpPort, int udpPort, String playerName, int localUdpPort) {
            this.serverHost = serverHost;
            this.tcpPort = tcpPort;
            this.udpPort = udpPort;
            this.playerName = playerName;
            this.localUdpPort = localUdpPort;
        }

        public String getServerHost() {
            return serverHost;
        }

        public int getTcpPort() {
            return tcpPort;
        }

        public int getUdpPort() {
            return udpPort;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getLocalUdpPort() {
            return localUdpPort;
        }
    }

    private JTextField hostField;
    private JTextField tcpPortField;
    private JTextField udpPortField;
    private JTextField playerNameField;
    private JTextField localUdpPortField;

    private ConnectInfo connectInfo;
    private boolean submitted = false;

    public ConnectDialog(Frame owner, String defaultHost, int defaultTcpPort, int defaultUdpPort, String defaultPlayerName, int defaultLocalUdpPort) {
        super(owner, "Connect to ChronoArena", true);
        buildUi(defaultHost, defaultTcpPort, defaultUdpPort, defaultPlayerName, defaultLocalUdpPort);
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi(String defaultHost, int defaultTcpPort, int defaultUdpPort, String defaultPlayerName, int defaultLocalUdpPort) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));

        hostField = new JTextField(defaultHost);
        tcpPortField = new JTextField(String.valueOf(defaultTcpPort));
        udpPortField = new JTextField(String.valueOf(defaultUdpPort));
        playerNameField = new JTextField(defaultPlayerName);
        localUdpPortField = new JTextField(String.valueOf(defaultLocalUdpPort));

        form.add(new JLabel("Server IP / Host:"));
        form.add(hostField);

        form.add(new JLabel("TCP Port:"));
        form.add(tcpPortField);

        form.add(new JLabel("UDP Port:"));
        form.add(udpPortField);

        form.add(new JLabel("Player Name:"));
        form.add(playerNameField);

        form.add(new JLabel("Local UDP Port (0 = auto):"));
        form.add(localUdpPortField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton connectButton = new JButton("Connect");
        JButton cancelButton = new JButton("Cancel");

        connectButton.addActionListener(e -> onConnect());
        cancelButton.addActionListener(e -> onCancel());

        buttons.add(cancelButton);
        buttons.add(connectButton);

        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(connectButton);
    }

    private void onConnect() {
        try {
            String host = hostField.getText().trim();
            int tcpPort = Integer.parseInt(tcpPortField.getText().trim());
            int udpPort = Integer.parseInt(udpPortField.getText().trim());
            String playerName = playerNameField.getText().trim();
            int localUdpPort = Integer.parseInt(localUdpPortField.getText().trim());

            if (host.isBlank()) {
                throw new IllegalArgumentException("Server host cannot be empty.");
            }
            if (playerName.isBlank()) {
                throw new IllegalArgumentException("Player name cannot be empty.");
            }

            connectInfo = new ConnectInfo(host, tcpPort, udpPort, playerName, localUdpPort);
            submitted = true;
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Invalid input: " + e.getMessage(),
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void onCancel() {
        submitted = false;
        dispose();
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public ConnectInfo getConnectInfo() {
        return connectInfo;
    }
}