package me.hejmo.cameratest.media.ui;

import android.media.MediaCodec;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

import me.hejmo.cameratest.media.mediacodec.VideoDecoder;
import me.hejmo.cameratest.media.mediacodec.VideoEncoder;
import me.hejmo.cameratest.media.talkback.ITalkback;
import me.hejmo.cameratest.media.talkback.Initiator;
import me.hejmo.cameratest.media.talkback.Responder;
import me.hejmo.cameratest.media.talkback.VideoEncodeConfig;
import me.hejmo.cameratest.media.talkback.VideoEncodeFrame;

import static me.hejmo.cameratest.media.ui.TalkbackContract.INITIATOR;
import static me.hejmo.cameratest.media.ui.TalkbackContract.RESPONDER;
import static me.hejmo.cameratest.media.ui.TalkbackContract.VIDEO_HEIGHT;
import static me.hejmo.cameratest.media.ui.TalkbackContract.VIDEO_WIDTH;

/**
 * @author qinglian.zhang
 */

public class TalkbackPresenter implements TalkbackContract.Presenter {

    private TalkbackContract.View mView;

    private VideoEncoder mEncoder;
    private VideoDecoder mDecoder;
    private ITalkback mTalkback;
    private VideoEncodeConfig mConfig = null;

    public TalkbackPresenter(TalkbackContract.View view){
        mView = view;
        view.setPresenter(this);
    }

    @Override
    public void start() {
        mEncoder = new MyEncoder();
        mDecoder = new VideoDecoder();
    }

    @Override
    public void startDecoder() {
        mDecoder.start();
    }

    @Override
    public void stopDecoder() {
        mDecoder.stop();
    }

    @Override
    public void startEncoder() {
        mEncoder.start();
    }

    @Override
    public void stopEncoder() {
        mEncoder.stop();
    }

    @Override
    public Surface getEncoderInputSurface() {
        return mEncoder.getInputSurface();
    }

    @Override
    public void configureDecoder(Surface surface, int width, int height, byte[] csd0, int offset, int size) {
        mDecoder.configure(surface,
                width,
                height,
                csd0,
                offset,
                size);
    }

    @Override
    public void configureDecoderAgain() {
        if(mConfig != null){
            configureDecoder(mView.getReceiveSV(),
                    mConfig.width,
                    mConfig.height,
                    mConfig.data,
                    mConfig.offset,
                    mConfig.size);
        }
    }

    @Override
    public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
        mDecoder.decodeSample(data,
                offset,
                size,
                presentationTimeUs,
                flags);
    }

    @Override
    public void startTalkback(String role,String ip) {
        if (role.equals(INITIATOR)) {
            mTalkback = new Initiator(talkbackCallback,role);
            Executors.newSingleThreadExecutor().execute(mTalkback);
        }
        if (role.equals(RESPONDER)) {
            mTalkback = new Responder(talkbackCallback,role,ip);
            Executors.newSingleThreadExecutor().execute(mTalkback);
        }
    }

    @Override
    public void pauseTalkbakc() {
        mTalkback.pause();
    }

    @Override
    public void closeTalkback() throws IOException {
        mTalkback.close();
    }

    private ITalkback.TalkbackCallback talkbackCallback = new ITalkback.TalkbackCallback() {

        @Override
        public void onConfig(VideoEncodeConfig config) {
            Log.d("scott","decoder config");
            //configure只会有一次,这里安全不做处理
            configureDecoder(mView.getReceiveSV(),
                    config.width,
                    config.height,
                    config.data,
                    config.offset,
                    config.size);
            mConfig = config;
        }

        @Override
        public void onNewFrame(VideoEncodeFrame frame) {
            // todo: 在decode前需要判断decoder是否已经start

            decodeSample(frame.data,
                    frame.offset,
                    frame.size,
                    frame.presentTime,
                    frame.flag);
        }

        @Override
        public void onTalkbackConnected() {

        }

        @Override
        public void onTalkbackStart() {

        }
    };

    class MyEncoder extends VideoEncoder {


        public MyEncoder() {
            super(VIDEO_WIDTH, VIDEO_HEIGHT);
        }

        // Both of onSurfaceCreated and onSurfaceDestroyed are called from codec's thread,
        // non-UI thread

        @Override
        protected void onSurfaceCreated(Surface surface) {
        }

        @Override
        protected void onSurfaceDestroyed(Surface surface) {
        }

        @Override
        protected void onEncodedSample(MediaCodec.BufferInfo info, ByteBuffer data) {
            // Here we could have just used ByteBuffer, but in real life case we might need to
            // send sample over network, etc. This requires byte[]
            byte[] buffer = new byte[info.size];

            data.position(info.offset);
            data.limit(info.offset + info.size);
            data.get(buffer, 0, info.size);

            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                // this is the first and only config sample, which contains information about codec
                // like H.264, that let's configure the decoder

                VideoEncodeConfig config = new VideoEncodeConfig(VIDEO_WIDTH, VIDEO_HEIGHT, info.size, 0, buffer);
                mTalkback.addVideoEncodeConfigure(config);
            } else {
                // pass byte[] to decoder's queue to render asap

                VideoEncodeFrame frame = new VideoEncodeFrame(info.size, 0, info.flags, info.presentationTimeUs, buffer);
                mTalkback.addVideoEncodeFrame(frame);
            }
        }
    }
}
