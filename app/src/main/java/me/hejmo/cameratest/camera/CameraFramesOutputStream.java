package me.hejmo.cameratest.camera;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by scott on 4/5/16.
 * @author zhangqinglian
 */
public class CameraFramesOutputStream extends OutputStream{

    private Socket mOutputSocket;

    @Override
    public void write(int oneByte) throws IOException {

    }

    public CameraFramesOutputStream(Socket out) {
        mOutputSocket = out;
    }

    @Override
    public void close() throws IOException {
        flush();
        mOutputSocket.getOutputStream().flush();
        mOutputSocket.getOutputStream().close();
        mOutputSocket.close();
        mOutputSocket = null;
    }

    @Override
    public void flush() throws IOException {
        if(mOutputSocket != null){
            mOutputSocket.getOutputStream().flush();
        }
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        if(mOutputSocket != null){
            mOutputSocket.getOutputStream().write(buffer);
        }
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        super.write(buffer, offset, count);
    }

}
