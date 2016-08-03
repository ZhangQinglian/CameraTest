package me.hejmo.cameratest.media;

/**
 * @author qinglian.zhang
 * 2016-7-18
 */
public class Contract {

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
}
