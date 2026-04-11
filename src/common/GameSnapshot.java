package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameSnapshot implements Serializable {

	private static final long serialVersionUID = 1L;

	private long serverTimeMs;
	private boolean matchRunning;
	private boolean matchEnded;
	private String serverNotice;
	private int timeLeftSeconds;
	private Integer winnerPlayerId;

	private List<PlayerState> players = new ArrayList<>();
	private List<ZoneStateModel> zones = new ArrayList<>();
	private List<ItemState> items = new ArrayList<>();

	public GameSnapshot() {
	}

	public long getServerTimeMs() {
		return serverTimeMs;
	}

	public void setServerTimeMs(long serverTimeMs) {
		this.serverTimeMs = serverTimeMs;
	}

	public boolean isMatchRunning() {
		return matchRunning;
	}

	public void setMatchRunning(boolean matchRunning) {
		this.matchRunning = matchRunning;
	}

	public boolean isMatchEnded() {
		return matchEnded;
	}

	public void setMatchEnded(boolean matchEnded) {
		this.matchEnded = matchEnded;
	}

	public String getServerNotice() {
		return serverNotice;
	}

	public void setServerNotice(String serverNotice) {
		this.serverNotice = serverNotice;
	}

	public int getTimeLeftSeconds() {
		return timeLeftSeconds;
	}

	public void setTimeLeftSeconds(int timeLeftSeconds) {
		this.timeLeftSeconds = timeLeftSeconds;
	}

	public Integer getWinnerPlayerId() {
		return winnerPlayerId;
	}

	public void setWinnerPlayerId(Integer winnerPlayerId) {
		this.winnerPlayerId = winnerPlayerId;
	}

	public List<PlayerState> getPlayers() {
		return players;
	}

	public void setPlayers(List<PlayerState> players) {
		this.players = players == null ? new ArrayList<>() : players;
	}

	public List<ZoneStateModel> getZones() {
		return zones;
	}

	public void setZones(List<ZoneStateModel> zones) {
		this.zones = zones == null ? new ArrayList<>() : zones;
	}

	public List<ItemState> getItems() {
		return items;
	}

	public void setItems(List<ItemState> items) {
		this.items = items == null ? new ArrayList<>() : items;
	}
}