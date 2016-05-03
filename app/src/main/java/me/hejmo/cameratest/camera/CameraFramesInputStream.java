package me.hejmo.cameratest.camera;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by scott on 4/5/16.
 * @author zhangqinglian
 *
 * 照片输入流
 */
public class CameraFramesInputStream extends InputStream{

    private Socket mInputSocket;

    public CameraFramesInputStream(Socket socket) {
        mInputSocket = socket;
    }

    @Override
    public void close() throws IOException {
        mInputSocket.getInputStream().close();
        mInputSocket.close();
        mInputSocket = null;
    }

    @Override
    public int read() throws IOException {
        return mInputSocket.getInputStream().read();
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return super.skip(byteCount);
    }

    @Override
    public int available() throws IOException {
        return super.available();
    }

    @Override
    public void mark(int readlimit) {
        super.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return super.markSupported();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return mInputSocket.getInputStream().read(buffer);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return mInputSocket.getInputStream().read(buffer, byteOffset, byteCount);
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
    }

}
