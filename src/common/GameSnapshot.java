package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameSnapshot implements Serializable {

	private static final long serialVersionUID = 1L;

	private long serverTimeMs;

	private MatchPhase matchPhase;
	private String serverNotice;
	private int timeLeftSeconds;
	private Integer winnerPlayerId;

	private int lobbyTimeLeftSeconds;
	private Map<Integer, Integer> voteCounts;

	private List<PlayerState> players = new ArrayList<>();
	private List<ZoneStateModel> zones = new ArrayList<>();
	private List<ItemState> items = new ArrayList<>();

	public GameSnapshot() {
	}

	public void setLobbyTimeLeftSeconds(int sec){
		this.lobbyTimeLeftSeconds = sec;
	}

	public void setMatchPhase(MatchPhase matchPhase){
		this.matchPhase = matchPhase;
	}

	public int getLobbyTimeLeftSeconds(){
		return this.lobbyTimeLeftSeconds;
	}

	public void setVoteCounts(Map<Integer, Integer> votes){
		this.voteCounts = votes;
	}

	public Map<Integer, Integer> getVoteCounts(){
		return this.voteCounts;
	}

	public long getServerTimeMs() {
		return serverTimeMs;
	}

	public void setServerTimeMs(long serverTimeMs) {
		this.serverTimeMs = serverTimeMs;
	}

	public boolean isMatchRunning() {
		return matchPhase == MatchPhase.RUNNING;
	}

	public boolean isMatchEnded() {
		return matchPhase == MatchPhase.ENDING;
	}

	public void setMatchRunning(boolean matchRunning) {
		if(matchRunning)
    		this.matchPhase = MatchPhase.RUNNING;
	}

	public void setMatchEnded(boolean matchEnded) {
		if(matchEnded)
	    	this.matchPhase = MatchPhase.ENDING;
	}

	public boolean isMatchLobby(){
		return this.matchPhase == MatchPhase.LOBBY;
	}

	public void setMatchLobby(){
		this.matchPhase = MatchPhase.LOBBY;
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