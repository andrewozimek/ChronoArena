package client;

import common.PropertiesLoader;

import javax.swing.*;
import java.util.Properties;

public class ClientMain {

    public static void main(String[] args) {
        String propertiesPath = "config/client.properties";
        if (args.length > 0) {
            propertiesPath = args[0];
        }

        Properties properties = PropertiesLoader.load(propertiesPath);

        String defaultHost = PropertiesLoader.getRequired(properties, "server.host");
        int defaultTcpPort = PropertiesLoader.getRequiredInt(properties, "server.tcp.port");
        int defaultUdpPort = PropertiesLoader.getRequiredInt(properties, "server.udp.port");
        String defaultPlayerName = PropertiesLoader.getRequired(properties, "client.player.name");
        int defaultLocalUdpPort = PropertiesLoader.getInt(properties, "client.local.udp.port", 0);

        final GameClient[] gameClientHolder = new GameClient[1];

        SwingUtilities.invokeLater(() -> {
            ConnectDialog dialog = new ConnectDialog(
                    null,
                    defaultHost,
                    defaultTcpPort,
                    defaultUdpPort,
                    defaultPlayerName,
                    defaultLocalUdpPort
            );
            dialog.setVisible(true);

            if (!dialog.isSubmitted()) {
                System.exit(0);
                return;
            }

            ConnectDialog.ConnectInfo info = dialog.getConnectInfo();
            GameClient gameClient = new GameClient(
                    info.getServerHost(),
                    info.getTcpPort(),
                    info.getUdpPort(),
                    info.getPlayerName(),
                    info.getLocalUdpPort()
            );

            gameClientHolder[0] = gameClient;

            Runtime.getRuntime().addShutdownHook(new Thread(gameClient::shutdown));

            try {
                gameClient.connect();

                GameFrame frame = new GameFrame(gameClient);
                frame.setVisible(true);
                frame.requestFocusInWindow();

                System.out.println("Connected successfully as player " + gameClient.getClientState().getLocalPlayerId());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Client failed: " + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE
                );
                e.printStackTrace();
                gameClient.shutdown();
                System.exit(1);
            }
        });
    }
}