package server;

import common.Constants;
import common.GameSnapshot;
import common.ItemState;
import common.ItemType;
import common.MatchPhase;
import common.PlayerActionType;
import common.PlayerState;
import common.Position;
import common.UdpActionMessage;
import common.ZoneControlState;
import common.ZoneStateModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameStateManager {
    private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();
    private final Map<String, ZoneStateModel> zones = new ConcurrentHashMap<>();
    private final Map<String, ItemState> items = new ConcurrentHashMap<>();

    private final SpawnManager spawnManager;

    private volatile MatchPhase phase = MatchPhase.WAITING;
    private volatile long matchStartTimeMs;
    private volatile long lobbyStartTimeMs;
    private volatile int matchDurationSeconds = Constants.MATCH_DURATION_SECONDS;
    private volatile boolean matchEnded;
    private volatile String serverNotice;

    private final Map<Integer, Integer> playerVotes = new ConcurrentHashMap<>();

    private volatile long lastItemSpawnTimeMs;

    public GameStateManager(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
        this.matchStartTimeMs = 0L;
        this.lobbyStartTimeMs = 0L;
        this.matchEnded = false;
        this.serverNotice = "Waiting for players...";
        this.lastItemSpawnTimeMs = 0L;

        initializeZones();
    }

    private void initializeZones() {
        ZoneStateModel zoneA = new ZoneStateModel("ZONE-A", 180, 150, 140, 140);
        ZoneStateModel zoneB = new ZoneStateModel("ZONE-B", 650, 350, 140, 140);

        zones.put(zoneA.getZoneId(), zoneA);
        zones.put(zoneB.getZoneId(), zoneB);
    }

    public synchronized void startLobby() {
        if (phase != MatchPhase.WAITING) {
            return;
        }
        this.phase = MatchPhase.LOBBY;
        this.lobbyStartTimeMs = System.currentTimeMillis();
        this.playerVotes.clear();
        this.serverNotice = "Lobby open — vote for match duration!";
    }

    public synchronized void tickLobby() {
        if (phase != MatchPhase.LOBBY) {
            return;
        }
        long elapsed = (System.currentTimeMillis() - lobbyStartTimeMs) / 1000L;
        if (elapsed >= Constants.LOBBY_DURATION_SECONDS) {
            resolveLobby();
        }
    }

    private void resolveLobby() {
        Map<Integer, Integer> tally = new HashMap<>();
        for (int duration : playerVotes.values()) {
            tally.merge(duration, 1, Integer::sum);
        }

        if (tally.isEmpty()) {
            matchDurationSeconds = Constants.MATCH_DURATION_SECONDS;
        } else {
            matchDurationSeconds = tally.entrySet().stream()
                    .max(Map.Entry.<Integer, Integer>comparingByValue()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .get()
                    .getKey();
        }

        startMatch();
    }

    public synchronized void startMatch() {
        if (phase == MatchPhase.RUNNING) {
            return;
        }
        this.phase = MatchPhase.RUNNING;
        this.matchStartTimeMs = System.currentTimeMillis();
        this.matchEnded = false;
        this.serverNotice = "Match started! Duration: " + matchDurationSeconds + "s";
        this.lastItemSpawnTimeMs = System.currentTimeMillis();
    }

    public void submitVote(int playerId, int durationSeconds) {
        // only accept votes during the lobby; one vote per player (last write wins)
        if (phase != MatchPhase.LOBBY) {
            return;
        }
        playerVotes.put(playerId, durationSeconds);
        System.out.println("Player " + playerId + " voted for " + durationSeconds + "s");
    }

    public MatchPhase getPhase() {
        return phase;
    }

    public synchronized void addPlayer(int playerId, String playerName, Position spawn) {
        PlayerState player = new PlayerState(playerId, playerName, spawn);
        players.put(playerId, player);
        serverNotice = playerName + " joined the game";
    }

    public synchronized void markPlayerDisconnected(int playerId) {
        PlayerState player = players.get(playerId);
        if (player != null) {
            player.setConnected(false);
            serverNotice = player.getPlayerName() + " disconnected";
            releaseOwnedZones(playerId);
        }
    }

    public synchronized void removePlayerCompletely(int playerId) {
        PlayerState removed = players.remove(playerId);
        if (removed != null) {
            releaseOwnedZones(playerId);
            serverNotice = removed.getPlayerName() + " removed from match";
        }
    }

    private void releaseOwnedZones(int playerId) {
        for (ZoneStateModel zone : zones.values()) {
            if (zone.getOwnerPlayerId() != null && zone.getOwnerPlayerId() == playerId) {
                zone.setOwnerPlayerId(null);
                zone.setContenderPlayerId(null);
                zone.setState(ZoneControlState.UNCLAIMED);
                zone.setCaptureStartTimeMs(0L);
                zone.setGraceExpiryTimeMs(0L);
            }
            if (zone.getContenderPlayerId() != null && zone.getContenderPlayerId() == playerId) {
                zone.setContenderPlayerId(null);
                if (zone.getOwnerPlayerId() == null) {
                    zone.setState(ZoneControlState.UNCLAIMED);
                }
            }
        }
    }

    public synchronized boolean hasPlayer(int playerId) {
        return players.containsKey(playerId);
    }

    public synchronized PlayerState getPlayer(int playerId) {
        PlayerState player = players.get(playerId);
        return player == null ? null : player.copy();
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized List<PlayerState> getPlayersSorted() {
        List<PlayerState> list = new ArrayList<>();
        for (PlayerState player : players.values()) {
            list.add(player.copy());
        }
        list.sort(Comparator.comparingInt(PlayerState::getPlayerId));
        return list;
    }

    public synchronized Position getSpawnForNewPlayer(int playerIndex) {
        return spawnManager.getSpawnForPlayerIndex(playerIndex);
    }

    public synchronized List<ZoneStateModel> getZoneCopies() {
        List<ZoneStateModel> zoneCopies = new ArrayList<>();
        for (ZoneStateModel zone : zones.values()) {
            zoneCopies.add(zone.copy());
        }
        return zoneCopies;
    }

    public synchronized void processActions(List<QueuedPlayerAction> actions, KillSwitchManager killSwitchManager) {
        if (phase != MatchPhase.RUNNING || matchEnded) {
            return;
        }

        long now = System.currentTimeMillis();

        for (QueuedPlayerAction queuedAction : actions) {
            UdpActionMessage action = queuedAction.getActionMessage();
            int playerId = action.getPlayerId();

            if (killSwitchManager.isKilled(playerId)) {
                continue;
            }

            PlayerState player = players.get(playerId);
            if (player == null || !player.isConnected()) {
                continue;
            }

            resolveExpiredFreeze(player, now);

            if (player.isFrozen() && action.getActionType() != PlayerActionType.STOP) {
                continue;
            }

            switch (action.getActionType()) {
                case MOVE_UP:
                    movePlayer(player, 0, -Constants.PLAYER_SPEED);
                    break;
                case MOVE_DOWN:
                    movePlayer(player, 0, Constants.PLAYER_SPEED);
                    break;
                case MOVE_LEFT:
                    movePlayer(player, -Constants.PLAYER_SPEED, 0);
                    break;
                case MOVE_RIGHT:
                    movePlayer(player, Constants.PLAYER_SPEED, 0);
                    break;
                case FREEZE_ATTACK:
                    attemptFreezeAttack(playerId, now);
                    break;
                case STOP:
                default:
                    break;
            }
        }

        updateZones(now);
        collectItems(now);
        spawnItemsIfNeeded(now);
        endMatchIfExpired(now);
    }

    private void resolveExpiredFreeze(PlayerState player, long now) {
        if (player.isFrozen() && now >= player.getFrozenUntilEpochMs()) {
            player.setFrozen(false);
            player.setFrozenUntilEpochMs(0L);
        }
    }

    private void movePlayer(PlayerState player, int deltaX, int deltaY) {
        Position current = player.getPosition();
        int newX = clamp(current.getX() + deltaX, 0, Constants.MAP_WIDTH - Constants.PLAYER_SIZE);
        int newY = clamp(current.getY() + deltaY, 0, Constants.MAP_HEIGHT - Constants.PLAYER_SIZE);
        current.setX(newX);
        current.setY(newY);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void attemptFreezeAttack(int attackerId, long now) {
        PlayerState attacker = players.get(attackerId);
        if (attacker == null || !attacker.isConnected()) {
            return;
        }

        if (!attacker.isFreezePowerAvailable()) {
            return;
        }

        if (attacker.getFreezeCooldownUntilEpochMs() > now) {
            return;
        }

        PlayerState nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (PlayerState target : players.values()) {
            if (target.getPlayerId() == attackerId || !target.isConnected()) {
                continue;
            }

            double distance = attacker.getPosition().distanceTo(target.getPosition());
            if (distance <= Constants.FREEZE_RANGE && distance < nearestDistance) {
                nearestDistance = distance;
                nearestTarget = target;
            }
        }

        if (nearestTarget != null) {
            nearestTarget.setFrozen(true);
            nearestTarget.setFrozenUntilEpochMs(now + Constants.FREEZE_DURATION_MS);

            attacker.setFreezePowerAvailable(false);
            attacker.setFreezeCooldownUntilEpochMs(now + Constants.FREEZE_COOLDOWN_MS);

            serverNotice = attacker.getPlayerName() + " froze " + nearestTarget.getPlayerName();
        }
    }

    private void updateZones(long now) {
        for (ZoneStateModel zone : zones.values()) {
            List<PlayerState> inside = getPlayersInsideZone(zone);

            if (inside.isEmpty()) {
                handleZoneNoPlayers(zone, now);
                continue;
            }

            if (inside.size() > 1) {
                zone.setState(ZoneControlState.CONTESTED);
                zone.setContenderPlayerId(null);
                zone.setCaptureStartTimeMs(0L);
                zone.setGraceExpiryTimeMs(0L);
                continue;
            }

            PlayerState player = inside.get(0);

            if (zone.getOwnerPlayerId() != null && zone.getOwnerPlayerId() == player.getPlayerId()) {
                zone.setState(ZoneControlState.CONTROLLED);
                zone.setGraceExpiryTimeMs(0L);

                if (now - zone.getLastPointAwardTimeMs() >= Constants.ZONE_POINT_AWARD_INTERVAL_MS) {
                    player.addScore(Constants.ZONE_POINT_AWARD);
                    zone.setLastPointAwardTimeMs(now);
                }
                continue;
            }

            if (zone.getOwnerPlayerId() != null && zone.getOwnerPlayerId() != player.getPlayerId()) {
                zone.setState(ZoneControlState.CONTESTED);
                zone.setContenderPlayerId(player.getPlayerId());
                zone.setCaptureStartTimeMs(0L);
                zone.setGraceExpiryTimeMs(0L);
                continue;
            }

            if (zone.getContenderPlayerId() == null || zone.getContenderPlayerId() != player.getPlayerId()) {
                zone.setContenderPlayerId(player.getPlayerId());
                zone.setCaptureStartTimeMs(now);
                zone.setState(ZoneControlState.CAPTURING);
            } else if (now - zone.getCaptureStartTimeMs() >= Constants.ZONE_CAPTURE_TIME_MS) {
                zone.setOwnerPlayerId(player.getPlayerId());
                zone.setContenderPlayerId(null);
                zone.setState(ZoneControlState.CONTROLLED);
                zone.setLastPointAwardTimeMs(now);
                zone.setGraceExpiryTimeMs(0L);
                serverNotice = player.getPlayerName() + " captured " + zone.getZoneId();
            }
        }
    }

    private List<PlayerState> getPlayersInsideZone(ZoneStateModel zone) {
        List<PlayerState> inside = new ArrayList<>();
        for (PlayerState player : players.values()) {
            if (!player.isConnected()) {
                continue;
            }
            if (zone.contains(player.getPosition())) {
                inside.add(player);
            }
        }
        inside.sort(Comparator.comparingInt(PlayerState::getPlayerId));
        return inside;
    }

    private void handleZoneNoPlayers(ZoneStateModel zone, long now) {
        if (zone.getOwnerPlayerId() != null) {
            if (zone.getGraceExpiryTimeMs() == 0L) {
                zone.setGraceExpiryTimeMs(now + Constants.ZONE_GRACE_TIME_MS);
            } else if (now >= zone.getGraceExpiryTimeMs()) {
                zone.setOwnerPlayerId(null);
                zone.setState(ZoneControlState.UNCLAIMED);
                zone.setContenderPlayerId(null);
                zone.setCaptureStartTimeMs(0L);
                zone.setGraceExpiryTimeMs(0L);
            }
        } else {
            zone.setState(ZoneControlState.UNCLAIMED);
            zone.setContenderPlayerId(null);
            zone.setCaptureStartTimeMs(0L);
            zone.setGraceExpiryTimeMs(0L);
        }
    }

    private void collectItems(long now) {
        Iterator<ItemState> iterator = items.values().iterator();

        while (iterator.hasNext()) {
            ItemState item = iterator.next();
            if (!item.isActive()) {
                iterator.remove();
                continue;
            }

            PlayerState collector = findFirstPlayerTouchingItem(item);
            if (collector == null) {
                continue;
            }

            if (item.getItemType() == ItemType.ENERGY) {
                collector.addScore(Constants.ENERGY_ITEM_SCORE);
                serverNotice = collector.getPlayerName() + " collected energy";
            } else if (item.getItemType() == ItemType.FREEZE) {
                collector.setFreezePowerAvailable(true);
                serverNotice = collector.getPlayerName() + " picked freeze power";
            }

            item.setActive(false);
            iterator.remove();
        }
    }

    private PlayerState findFirstPlayerTouchingItem(ItemState item) {
        List<PlayerState> candidates = new ArrayList<>();

        for (PlayerState player : players.values()) {
            if (!player.isConnected()) {
                continue;
            }

            if (player.getPosition().distanceTo(item.getPosition()) <= 30) {
                candidates.add(player);
            }
        }

        candidates.sort(Comparator.comparingInt(PlayerState::getPlayerId));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private void spawnItemsIfNeeded(long now) {
        if (items.size() >= Constants.MAX_ITEMS_ON_MAP) {
            return;
        }

        if (now - lastItemSpawnTimeMs >= Constants.ITEM_SPAWN_INTERVAL_MS) {
            ItemState item = spawnManager.spawnRandomItem(getZoneCopies());
            items.put(item.getItemId(), item);
            lastItemSpawnTimeMs = now;
        }
    }

    private void endMatchIfExpired(long now) {
        long elapsedSeconds = (now - matchStartTimeMs) / 1000L;
        if (elapsedSeconds >= matchDurationSeconds) {
            phase = MatchPhase.ENDING;
            matchEnded = true;

            PlayerState winner = players.values()
                    .stream()
                    .max(Comparator.comparingInt(PlayerState::getScore)
                            .thenComparingInt(PlayerState::getPlayerId))
                    .orElse(null);

            if (winner != null) {
                serverNotice = "Winner: " + winner.getPlayerName() + " with " + winner.getScore() + " points";
            } else {
                serverNotice = "Match ended";
            }
        }
    }

    public synchronized GameSnapshot createSnapshot() {
        GameSnapshot snapshot = new GameSnapshot();
        long now = System.currentTimeMillis();

        snapshot.setServerTimeMs(now);
        snapshot.setMatchPhase(phase);
        snapshot.setMatchRunning(phase == MatchPhase.RUNNING);
        snapshot.setMatchEnded(phase == MatchPhase.ENDING);
        snapshot.setServerNotice(serverNotice);

        if (phase == MatchPhase.LOBBY) {
            long elapsed = (now - lobbyStartTimeMs) / 1000L;
            int lobbyLeft = Math.max(0, Constants.LOBBY_DURATION_SECONDS - (int) elapsed);
            snapshot.setTimeLeftSeconds(lobbyLeft);

            // build vote counts for display: duration -> number of votes
            Map<Integer, Integer> tally = new HashMap<>();
            for (int duration : playerVotes.values()) {
                tally.merge(duration, 1, Integer::sum);
            }
            snapshot.setVoteCounts(tally);
        } else {
            int timeLeft = matchDurationSeconds;
            if (phase == MatchPhase.RUNNING) {
                long elapsed = (now - matchStartTimeMs) / 1000L;
                timeLeft = Math.max(0, matchDurationSeconds - (int) elapsed);
            } else if (phase == MatchPhase.ENDING) {
                timeLeft = 0;
            }
            snapshot.setTimeLeftSeconds(timeLeft);
            snapshot.setVoteCounts(null);
        }

        List<PlayerState> playerCopies = new ArrayList<>();
        Integer winnerId = null;
        PlayerState winner = null;

        for (PlayerState player : players.values()) {
            playerCopies.add(player.copy());
            if (winner == null || player.getScore() > winner.getScore()
                    || (player.getScore() == winner.getScore() && player.getPlayerId() < winner.getPlayerId())) {
                winner = player;
            }
        }

        playerCopies.sort(Comparator.comparingInt(PlayerState::getPlayerId));
        snapshot.setPlayers(playerCopies);

        if (matchEnded && winner != null) {
            winnerId = winner.getPlayerId();
        }
        snapshot.setWinnerPlayerId(winnerId);

        List<ZoneStateModel> zoneCopies = new ArrayList<>();
        for (ZoneStateModel zone : zones.values()) {
            zoneCopies.add(zone.copy());
        }
        zoneCopies.sort(Comparator.comparing(ZoneStateModel::getZoneId));
        snapshot.setZones(zoneCopies);

        List<ItemState> itemCopies = new ArrayList<>();
        for (ItemState item : items.values()) {
            itemCopies.add(item.copy());
        }
        itemCopies.sort(Comparator.comparing(ItemState::getItemId));
        snapshot.setItems(itemCopies);

        return snapshot;
    }
}