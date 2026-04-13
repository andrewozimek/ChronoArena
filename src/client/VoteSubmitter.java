package client;

@FunctionalInterface
public interface VoteSubmitter {
    void submitVote(int durationSeconds);
}