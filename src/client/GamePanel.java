package client;

import common.Constants;
import common.GameSnapshot;
import common.ItemState;
import common.ItemType;
import common.PlayerState;
import common.Position;
import common.ZoneStateModel;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import common.MapLayout;

public class GamePanel extends JPanel {

    private final ClientState clientState;
    private BufferedImage[] tiles;
    private BufferedImage freeze;
    private BufferedImage energy;
    private BufferedImage mapBackground;

    private final int TILE_WIDTH = 50;
    private final int TILE_HEIGHT = 50;

    public GamePanel(ClientState clientState) {
        this.clientState = clientState;
        setPreferredSize(new Dimension(Constants.MAP_WIDTH, Constants.MAP_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        loadImages();
        createBackground();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            enableAntialiasing(g2);

            GameSnapshot snapshot = clientState.getLatestSnapshot();
            if (snapshot == null) {
                drawCenteredMessage(g2, "Waiting for snapshot...");
                return;
            }

            g2.drawImage(mapBackground, 0, 0, null);
            //drawArenaGrid(g2);
            drawZones(g2, snapshot.getZones());
            drawItems(g2, snapshot.getItems());
            drawPlayers(g2, snapshot.getPlayers());

            if (snapshot.isMatchEnded()) {
                String detail = snapshot.getServerNotice() == null || snapshot.getServerNotice().isBlank()
                        ? "Match Ended"
                        : snapshot.getServerNotice();
                drawOverlayMessage(g2, "Match Ended", detail);
            } else if (!snapshot.isMatchRunning()) {
                drawOverlayMessage(g2, "Waiting for players...", "Need at least 2 players to start");
            }

        } finally {
            g2.dispose();
        }
    }

    private void enableAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawArenaGrid(Graphics2D g2) {
        g2.setColor(new Color(40, 40, 40));

        for (int x = 0; x <= Constants.MAP_WIDTH; x += 50) {
            g2.drawLine(x, 0, x, Constants.MAP_HEIGHT);
        }

        for (int y = 0; y <= Constants.MAP_HEIGHT; y += 50) {
            g2.drawLine(0, y, Constants.MAP_WIDTH, y);
        }
    }

    private void drawZones(Graphics2D g2, List<ZoneStateModel> zones) {
        for (ZoneStateModel zone : zones) {
            Color fill;
            switch (zone.getState()) {
                case CONTROLLED:
                    fill = new Color(80, 180, 100, 120);
                    break;
                case CAPTURING:
                    fill = new Color(80, 160, 220, 120);
                    break;
                case CONTESTED:
                    fill = new Color(220, 160, 60, 140);
                    break;
                case UNCLAIMED:
                default:
                    fill = new Color(120, 120, 120, 90);
                    break;
            }

            int cx = zone.getX() + zone.getWidth() / 2;
            int cy = zone.getY() + zone.getHeight() / 2;
            int r = Math.min(zone.getWidth(), zone.getHeight()) / 2;

            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(cx - r + 4, cy - r + 4, r * 2, r * 2);

            g2.setColor(fill);
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            String zid = zone.getZoneId();
            int tw = fm.stringWidth(zid);
            g2.drawString(zid, cx - tw / 2, cy - 6);

            String ownerText = "Owner: " + (zone.getOwnerPlayerId() == null ? "-" : "#" + zone.getOwnerPlayerId());
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int otw = g2.getFontMetrics().stringWidth(ownerText);
            g2.drawString(ownerText, cx - otw / 2, cy + 12);

            String stateText = "State: " + zone.getState().name();
            int stw = g2.getFontMetrics().stringWidth(stateText);
            g2.drawString(stateText, cx - stw / 2, cy + 28);
        }
    }

    private void drawItems(Graphics2D g2, List<ItemState> items) {
        for (ItemState item : items) {
            Position p = item.getPosition();
            if (p == null) {
                continue;
            }
 
            double phase = (System.currentTimeMillis() % 1000) / 1000.0;
            float scale = 1.0f + (float) (0.12 * Math.sin(phase * Math.PI * 2 + (p.getX() + p.getY()) * 0.01));
            int base = 10;
            int size = Math.max(6, (int) (base * 2 * scale));
 
            if (item.getItemType() == ItemType.ENERGY) {
                // MERGED: glow effect + image rendering when energy image is available
                if (energy != null) {
                    int imgSize = Math.max(20, (int) (28 * scale));
 
                    Composite old = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                    g2.setColor(new Color(255, 220, 80));
                    g2.fillOval(p.getX() - imgSize / 2 - 6, p.getY() - imgSize / 2 - 6, imgSize + 12, imgSize + 12);
                    g2.setComposite(old);
 
                    g2.drawImage(energy, p.getX() - imgSize / 2, p.getY() - imgSize / 2, imgSize, imgSize, null);
                } else {
                    g2.setColor(new Color(255, 220, 60));
                    g2.fillOval(p.getX() - size / 2, p.getY() - size / 2, size, size);
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, size / 2)));
                    g2.drawString("E", p.getX() - (size / 4), p.getY() + (size / 4));
                }
            } else {
                if (freeze != null) {
                    int imgSize = Math.max(20, (int) (28 * scale));
 
                    Composite old = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                    g2.setColor(new Color(130, 220, 255));
                    g2.fillOval(p.getX() - imgSize / 2 - 6, p.getY() - imgSize / 2 - 6, imgSize + 12, imgSize + 12);
                    g2.setComposite(old);
 
                    g2.drawImage(freeze, p.getX() - imgSize / 2, p.getY() - imgSize / 2, imgSize, imgSize, null);
                } else {
                    g2.setColor(new Color(130, 220, 255));
                    g2.fillOval(p.getX() - size / 2, p.getY() - size / 2, size, size);
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, size / 2)));
                    g2.drawString("F", p.getX() - (size / 4), p.getY() + (size / 4));
                }
            }
        }
    }

    private void drawPlayers(Graphics2D g2, List<PlayerState> players) {
        for (PlayerState player : players) {
            Position pos = player.getPosition();
            if (pos == null) {
                continue;
            }

            boolean isLocal = player.getPlayerId() == clientState.getLocalPlayerId();

            if (!player.isConnected()) {
                g2.setColor(new Color(120, 120, 120));
            } else if (player.isFrozen()) {
                g2.setColor(new Color(100, 180, 255));
            } else if (isLocal) {
                g2.setColor(new Color(255, 200, 70));
            } else {
                g2.setColor(new Color(200, 90, 120));
            }

            if (isLocal) {
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                g2.setColor(new Color(255, 220, 100));
                int gx = pos.getX() - 12;
                int gy = pos.getY() - 12;
                int gw = Constants.PLAYER_SIZE + 24;
                int gh = Constants.PLAYER_SIZE + 24;
                g2.fill(new Ellipse2D.Float(gx, gy, gw, gh));
                g2.setComposite(old);
            }

            g2.fillRoundRect(
                    pos.getX(),
                    pos.getY(),
                    Constants.PLAYER_SIZE,
                    Constants.PLAYER_SIZE,
                    10,
                    10
            );

            g2.setColor(Color.WHITE);
            g2.drawRoundRect(
                    pos.getX(),
                    pos.getY(),
                    Constants.PLAYER_SIZE,
                    Constants.PLAYER_SIZE,
                    10,
                    10
            );

            String nameText = player.getPlayerName() + " (#" + player.getPlayerId() + ")";
            String scoreText = "Score: " + player.getScore();

            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics nameFm = g2.getFontMetrics();
            int nameWidth = nameFm.stringWidth(nameText);
            int nameX = pos.getX() + (Constants.PLAYER_SIZE / 2) - (nameWidth / 2);
            int nameY = pos.getY() - 10;

            // dark rounded label behind player name
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(nameX - 6, nameY - 12, nameWidth + 12, 18, 10, 10);

            g2.setColor(Color.WHITE);
            g2.drawString(nameText, nameX, nameY);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            FontMetrics scoreFm = g2.getFontMetrics();
            int scoreWidth = scoreFm.stringWidth(scoreText);
            int scoreX = pos.getX() + (Constants.PLAYER_SIZE / 2) - (scoreWidth / 2);
            int scoreY = pos.getY() + Constants.PLAYER_SIZE + 16;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(scoreX - 5, scoreY - 11, scoreWidth + 10, 16, 10, 10);

            g2.setColor(Color.WHITE);
            g2.drawString(scoreText, scoreX, scoreY);

            if (player.isFreezePowerAvailable()) {
                String freezeText = "Freeze Ready";
                int freezeWidth = scoreFm.stringWidth(freezeText);
                int freezeX = pos.getX() + (Constants.PLAYER_SIZE / 2) - (freezeWidth / 2);
                int freezeY = pos.getY() + Constants.PLAYER_SIZE + 31;

                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(freezeX - 5, freezeY - 11, freezeWidth + 10, 16, 10, 10);

                g2.setColor(new Color(120, 240, 255));
                g2.drawString(freezeText, freezeX, freezeY);
            } else if (player.getFreezeCooldownUntilEpochMs() > System.currentTimeMillis()) {
                String freezeText = "Cooldown";
                int freezeWidth = scoreFm.stringWidth(freezeText);
                int freezeX = pos.getX() + (Constants.PLAYER_SIZE / 2) - (freezeWidth / 2);
                int freezeY = pos.getY() + Constants.PLAYER_SIZE + 31;

                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(freezeX - 5, freezeY - 11, freezeWidth + 10, 16, 10, 10);

                g2.setColor(new Color(190, 190, 255));
                g2.drawString(freezeText, freezeX, freezeY);
            }
        }
    }

    private void drawCenteredMessage(Graphics2D g2, String text) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = getHeight() / 2;
        g2.drawString(text, x, y);
    }

    private void drawOverlayMessage(Graphics2D g2, String title, String subtitle) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(getWidth() / 2 - 240, getHeight() / 2 - 60, 480, 120, 22, 22);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics titleFm = g2.getFontMetrics();
        int titleX = (getWidth() - titleFm.stringWidth(title)) / 2;
        int titleY = getHeight() / 2 - 2;
        g2.drawString(title, titleX, titleY);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        FontMetrics subFm = g2.getFontMetrics();
        int subX = (getWidth() - subFm.stringWidth(subtitle)) / 2;
        int subY = getHeight() / 2 + 28;
        g2.drawString(subtitle, subX, subY);
    }

    private void loadImages() {
        tiles = new BufferedImage[6];
        try {
            tiles[0] = resizeImage(ImageIO.read(GamePanel.class.getResourceAsStream("/client/resources/sand1.PNG")));
            tiles[1] = resizeImage(ImageIO.read(GamePanel.class.getResourceAsStream("/client/resources/sand2.PNG")));
            tiles[2] = resizeImage(ImageIO.read(GamePanel.class.getResourceAsStream("/client/resources/sand3.PNG")));
            tiles[3] = resizeImage(ImageIO.read(GamePanel.class.getResourceAsStream("/client/resources/water_top.PNG")));
            tiles[4] = resizeImage(ImageIO.read(GamePanel.class.getResourceAsStream("/client/resources/water1.PNG")));
            tiles[5] = resizeImage(ImageIO.read(GamePanel.class.getResourceAsStream("/client/resources/water2.PNG")));
            energy   = ImageIO.read(GamePanel.class.getResourceAsStream("/client/resources/energy.png"));
            freeze = ImageIO.read(GamePanel.class.getResourceAsStream("/client/resources/freeze.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage resizeImage(BufferedImage image) {
        BufferedImage resized = new BufferedImage(TILE_WIDTH, TILE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, 0, 0, TILE_WIDTH, TILE_HEIGHT, null);
        g.dispose();
        return resized;
    }

    private void createBackground() {
        int w = Constants.MAP_WIDTH;
        int h = Constants.MAP_HEIGHT;
        BufferedImage background = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = background.createGraphics();

        for (int y = 0; y < Constants.MAP_HEIGHT; y += 50) {
            for (int x = 0; x < Constants.MAP_WIDTH; x += 50) {
                BufferedImage tile;
                switch (MapLayout.map[y / 50][x / 50]) {
                    case 1:  tile = tiles[0]; break;
                    case 2:  tile = tiles[1]; break;
                    case 3:  tile = tiles[2]; break;
                    case 4:  tile = tiles[3]; break;
                    case 5:  tile = tiles[4]; break;
                    default: tile = tiles[5]; break;
                }
                g.drawImage(tile, x, y, null);
            }
        }
        g.dispose();
        mapBackground = background;
    }
}