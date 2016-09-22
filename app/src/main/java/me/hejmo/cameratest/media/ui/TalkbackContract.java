package me.hejmo.cameratest.media.ui;

import android.view.Surface;

import java.io.IOException;

import me.hejmo.cameratest.mvpc.IContract;
import me.hejmo.cameratest.mvpc.IPresenter;
import me.hejmo.cameratest.mvpc.IView;

/**
 * @author qinglian.zhang
 */

public class TalkbackContract implements IContract {

    /**
     * 视屏编解码的宽度
     */
    public static final int VIDEO_WIDTH = 640;
    /**
     * 视屏编解码的高度
     */
    public static final int VIDEO_HEIGHT = 480;

    /**
     * 视频帧率
     */
    public static final int DESIRED_PREVIEW_FPS = 8;


    public static final String VIDEO_FORMAT = "video/avc";
    public static final int VIDEO_I_FRAME_INTERVAL = 2;
    public static final int VIDEO_BITRATE = 1000 * 800;

    /**
     * 视频对话socket的端口号
     */
    public static final int TALK_BACK_PORT = 12001;

    public static final String ROLE = "role";

    public static final String INITIATOR = "initiator";

    public static final String RESPONDER = "responder";

    public static final String IP = "ip";

    interface Presenter extends IPresenter{
        void startDecoder();
        void stopDecoder();
        void startEncoder();
        void stopEncoder();
        Surface getEncoderInputSurface();
        void configureDecoder(Surface surface, int width, int height, byte[] csd0, int offset, int size);
        void configureDecoderAgain();
        void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags);
        void startTalkback(String role,String ip);
        void pauseTalkbakc();
        void closeTalkback() throws IOException;
    }

    interface View extends IView<Presenter>{
        Surface getReceiveSV();
    }
}
