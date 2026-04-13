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
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

public class GamePanel extends JPanel {

    private final ClientState clientState;

    private BufferedImage sand1;
    private BufferedImage sand2;
    private BufferedImage sand3;
    private BufferedImage water1;
    private BufferedImage water2;
    private BufferedImage waterTop;
    private BufferedImage energyImage;

    private final Random random = new Random(42);
    private int[][] tileMap;

    public GamePanel(ClientState clientState) {
        this.clientState = clientState;
        setPreferredSize(new Dimension(Constants.MAP_WIDTH, Constants.MAP_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        loadResources();
        buildTileMap();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            enableAntialiasing(g2);

            drawTiledBackground(g2);

            GameSnapshot snapshot = clientState.getLatestSnapshot();
            if (snapshot == null) {
                drawCenteredMessage(g2, "Waiting for snapshot...");
                return;
            }

            drawArenaGrid(g2);
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

    private void loadResources() {
        sand1 = loadImage("/client/resources/sand1.PNG");
        sand2 = loadImage("/client/resources/sand2.PNG");
        sand3 = loadImage("/client/resources/sand3.PNG");
        water1 = loadImage("/client/resources/water1.PNG");
        water2 = loadImage("/client/resources/water2.PNG");
        waterTop = loadImage("/client/resources/water_top.PNG");
        energyImage = loadImage("/client/resources/energy.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("Resource not found: " + path);
                return null;
            }
            return ImageIO.read(url);
        } catch (Exception e) {
            System.err.println("Failed to load resource: " + path + " -> " + e.getMessage());
            return null;
        }
    }

    private void buildTileMap() {
        int tileSize = 64;
        int cols = (Constants.MAP_WIDTH / tileSize) + 2;
        int rows = (Constants.MAP_HEIGHT / tileSize) + 2;
        tileMap = new int[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tileMap[r][c] = random.nextInt(3);
            }
        }
    }

    private void enableAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawTiledBackground(Graphics2D g2) {
        int tileSize = 64;

        BufferedImage[] sandTiles = new BufferedImage[]{sand1, sand2, sand3};

        if (sand1 != null || sand2 != null || sand3 != null) {
            for (int y = 0; y < Constants.MAP_HEIGHT; y += tileSize) {
                for (int x = 0; x < Constants.MAP_WIDTH; x += tileSize) {
                    int row = y / tileSize;
                    int col = x / tileSize;
                    int idx = tileMap[row][col];

                    BufferedImage tile = sandTiles[idx];
                    if (tile != null) {
                        g2.drawImage(tile, x, y, tileSize, tileSize, null);
                    } else {
                        g2.setColor(new Color(35, 30, 20));
                        g2.fillRect(x, y, tileSize, tileSize);
                    }
                }
            }
        } else {
            g2.setColor(new Color(20, 20, 20));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        drawWaterDecor(g2);
        drawVignette(g2);
    }

    private void drawWaterDecor(Graphics2D g2) {
        if (waterTop != null) {
            for (int x = 0; x < Constants.MAP_WIDTH; x += 128) {
                g2.drawImage(waterTop, x, 0, 128, 48, null);
            }
        }

        if (water1 != null) {
            g2.drawImage(water1, 40, Constants.MAP_HEIGHT - 140, 140, 90, null);
        }
        if (water2 != null) {
            g2.drawImage(water2, Constants.MAP_WIDTH - 220, 80, 170, 100, null);
        }
    }

    private void drawVignette(Graphics2D g2) {
        Paint old = g2.getPaint();

        GradientPaint topFade = new GradientPaint(
                0, 0, new Color(0, 0, 0, 70),
                0, 140, new Color(0, 0, 0, 0)
        );
        g2.setPaint(topFade);
        g2.fillRect(0, 0, getWidth(), 140);

        GradientPaint bottomFade = new GradientPaint(
                0, getHeight() - 140, new Color(0, 0, 0, 0),
                0, getHeight(), new Color(0, 0, 0, 80)
        );
        g2.setPaint(bottomFade);
        g2.fillRect(0, getHeight() - 140, getWidth(), 140);

        g2.setPaint(old);
    }

    private void drawArenaGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 35));

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
            g2.drawString(zid, cx - tw / 2, cy - 8);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String ownerText = "Owner: " + (zone.getOwnerPlayerId() == null ? "-" : "#" + zone.getOwnerPlayerId());
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

            if (item.getItemType() == ItemType.ENERGY && energyImage != null) {
                int size = Math.max(20, (int) (28 * scale));

                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                g2.setColor(new Color(255, 220, 80));
                g2.fillOval(p.getX() - size / 2 - 6, p.getY() - size / 2 - 6, size + 12, size + 12);
                g2.setComposite(old);

                g2.drawImage(energyImage, p.getX() - size / 2, p.getY() - size / 2, size, size, null);
            } else {
                int base = 10;
                int size = Math.max(6, (int) (base * 2 * scale));

                if (item.getItemType() == ItemType.ENERGY) {
                    g2.setColor(new Color(255, 220, 60));
                    g2.fillOval(p.getX() - size / 2, p.getY() - size / 2, size, size);
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, size / 2)));
                    g2.drawString("E", p.getX() - (size / 4), p.getY() + (size / 4));
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

            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(
                    player.getPlayerName() + " (#" + player.getPlayerId() + ")",
                    pos.getX() - 10,
                    pos.getY() - 8
            );

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(
                    "Score: " + player.getScore(),
                    pos.getX() - 4,
                    pos.getY() + Constants.PLAYER_SIZE + 15
            );

            if (player.isFreezePowerAvailable()) {
                g2.setColor(new Color(120, 240, 255));
                g2.drawString("Freeze Ready", pos.getX() - 4, pos.getY() + Constants.PLAYER_SIZE + 29);
            } else if (player.getFreezeCooldownUntilEpochMs() > System.currentTimeMillis()) {
                g2.setColor(new Color(190, 190, 255));
                g2.drawString("Cooldown", pos.getX() - 4, pos.getY() + Constants.PLAYER_SIZE + 29);
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
}