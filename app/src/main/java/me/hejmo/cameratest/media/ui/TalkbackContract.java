package me.hejmo.cameratest.media.ui;

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

    }

    interface View extends IView<Presenter>{

    }
}
