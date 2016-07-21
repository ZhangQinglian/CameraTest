package me.hejmo.cameratest.media.talkback;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import me.hejmo.cameratest.media.Contract;

/**
 * @author qinglian.zhang
 */
public class Initiator extends ITalkback {



    private ServerSocket mServerSocket ;

    public Initiator(TalkbackCallback callback,String role) {
        super(callback,role);
    }

    @Override
    public void close() throws IOException {
        stopTalkback();
        if(mServerSocket != null){
            mServerSocket.close();
            mServerSocket = null;
        }
    }

    @Override
    public void run() {
        Log.d("talkback", "Initiator running");
        try {
            mServerSocket = new ServerSocket(Contract.TALK_BACK_PORT);
            Log.d("talkback", "Initiator socket accept before");
            Socket socket = mServerSocket.accept();
            Log.d("talkback", "Initiator socket accept after");
            onConnected(socket);
            startTalkback();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
