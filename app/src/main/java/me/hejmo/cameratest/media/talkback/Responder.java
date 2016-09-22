package me.hejmo.cameratest.media.talkback;


import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import static me.hejmo.cameratest.media.ui.TalkbackContract.*;

/**
 * @author qinglian.zhang
 */
public class Responder extends ITalkback {

    private String ip;

    public Responder(TalkbackCallback callback,String role,String ip) {
        super(callback,role);
        this.ip = ip;
    }

    @Override
    public void close() throws IOException {
        stopTalkback();

    }

    @Override
    public void run() {

        Log.d("talkback","Responder is running");
        try {
            mSocket = new Socket(ip, TALK_BACK_PORT);
            Log.d("talkback","connect socket successful");
            onConnected(mSocket);
            startTalkback();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
