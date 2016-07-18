package me.hejmo.cameratest.media.mediacodec;

/**
 * Created by vladlichonos on 6/5/15.
 */
public interface VideoCodec {

    String VIDEO_FORMAT = "video/avc";
    int VIDEO_FRAME_PER_SECOND = 15;
    int VIDEO_I_FRAME_INTERVAL = 2;
    int VIDEO_BITRATE = 3000 * 1000;
}
