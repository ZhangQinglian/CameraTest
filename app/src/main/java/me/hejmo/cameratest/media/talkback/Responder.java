package me.hejmo.cameratest.media.talkback;


import android.util.Log;

import java.io.IOException;
import java.net.Socket;

import me.hejmo.cameratest.media.Contract;

/**
 * @author qinglian.zhang
 */
public class Responder extends ITalkback {


    public Responder(TalkbackCallback callback) {
        super(callback);
    }

    @Override
    public void close() throws IOException {
        stopTalkback();

    }

    @Override
    public void run() {

        Log.d("talkback","Responder is running");
        String ip = "172.16.10.49";
        try {
            mSocket = new Socket(ip, Contract.TALK_BACK_PORT);
            Log.d("talkback","connect socket successful");
            onConnected(mSocket);
            startTalkback();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
