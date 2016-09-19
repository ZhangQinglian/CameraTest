package me.hejmo.cameratest.media.mediacodec;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.hejmo.cameratest.media.Contract;

/**
 * Created by vladlichonos on 6/5/15.
 */
public class VideoDecoder implements VideoCodec {

    Worker mWorker;

    public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
        if (mWorker != null) {
            mWorker.decodeSample(data, offset, size, presentationTimeUs, flags);
        }
    }

    public void configure(Surface surface, int width, int height, byte[] csd0, int offset, int size) {
        if (mWorker != null) {
            mWorker.configure(surface, width, height, ByteBuffer.wrap(csd0, offset, size));
        }
    }

    public void start() {
        if (mWorker == null) {
            mWorker = new Worker();
            mWorker.setRunning(true);
            mWorker.start();
        }
    }

    public void stop() {
        Log.d("scott","decoder stop");
        if (mWorker != null) {
            mWorker.setRunning(false);
            mWorker = null;
        }
    }

    public boolean isRunning(){
        if(mWorker != null){
            return mWorker.getRunning();
        }
        return false;
    }

    class Worker extends Thread {

        volatile boolean mRunning;
        MediaCodec mCodec;
        // todo: 考虑放在VideoDecoder中,因为整个视频周期只configure一次,但Worker可能会因为视频暂停被初始化多次
        volatile boolean mConfigured;
        long mTimeoutUs;

        public Worker() {
            mTimeoutUs = 10000l;
        }

        public void setRunning(boolean running) {
            mRunning = running;
        }

        public boolean getRunning(){
            return mRunning;
        }
        public void configure(Surface surface, int width, int height, ByteBuffer csd0) {
            if (mConfigured) {
                throw new IllegalStateException("Decoder is already configured");
            }
            MediaFormat format = MediaFormat.createVideoFormat(Contract.VIDEO_FORMAT, width, height);
            // little tricky here, csd-0 is required in order to configure the codec properly
            // it is basically the first sample from encoder with flag: BUFFER_FLAG_CODEC_CONFIG
            format.setByteBuffer("csd-0", csd0);
            try {
                mCodec = MediaCodec.createDecoderByType(Contract.VIDEO_FORMAT);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create codec", e);
            }
            mCodec.configure(format, surface, null, 0);
            mCodec.start();
            mConfigured = true;
        }

        @SuppressLint("NewApi")
        @SuppressWarnings("deprecation")
        public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
            if (mConfigured && mRunning) {
                Log.d("scott"," decoder new frame");
                int index = mCodec.dequeueInputBuffer(mTimeoutUs);
                if (index >= 0) {
                    ByteBuffer buffer;
                    // since API 21 we have new API to use
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        buffer = mCodec.getInputBuffers()[index];
                        buffer.clear();
                    } else {
                        buffer = mCodec.getInputBuffer(index);
                    }
                    if (buffer != null) {
                        buffer.put(data, offset, size);
                        mCodec.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (mRunning) {
                    if (mConfigured) {
                        int index = mCodec.dequeueOutputBuffer(info, mTimeoutUs);
                        if (index >= 0) {
                            // setting true is telling system to render frame onto Surface
                            mCodec.releaseOutputBuffer(index, true);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    } else {
                        // just waiting to be configured, then decode and render
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            } finally {
                if (mConfigured) {
                    mCodec.stop();
                    mCodec.release();
                }
            }
        }
    }
}
