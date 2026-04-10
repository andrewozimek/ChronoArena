package server;

import common.Constants;
import common.JoinRequest;
import common.JoinResponse;
import common.TcpMessage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class TcpClientHandler implements Runnable {

    private final Socket clientSocket;
    private final GameServer gameServer;

    public TcpClientHandler(Socket clientSocket, GameServer gameServer) {
        this.clientSocket = clientSocket;
        this.gameServer = gameServer;
    }

    @Override
    public void run() {
        int playerId = -1;

        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            outputStream.flush();

            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            Object raw = inputStream.readObject();
            
            if (!(raw instanceof TcpMessage)) {
                outputStream.writeObject(TcpMessage.of(
                        Constants.MESSAGE_TYPE_JOIN_RESPONSE,
                        new JoinResponse(false, -1, null, "Invalid join message")
                ));
                outputStream.flush();
                return;
            }

            TcpMessage tcpMessage = (TcpMessage) raw;
            if (!Constants.MESSAGE_TYPE_JOIN_REQUEST.equals(tcpMessage.getType())
                    || !(tcpMessage.getPayload() instanceof JoinRequest)) {
                outputStream.writeObject(TcpMessage.of(
                        Constants.MESSAGE_TYPE_JOIN_RESPONSE,
                        new JoinResponse(false, -1, null, "Expected JOIN_REQUEST")
                ));
                outputStream.flush();
                return;
            }


        } catch (Exception e) {
        }


    }


    private String sanatizePlayerName(String name){
        if(name == null || name.isBlank()){
            return "Player";
        }
        String trimmed = name.trim();
        if(trimmed.length() > 16){
            return trimmed.substring(0,16);
        }
        return trimmed;
    }

}
