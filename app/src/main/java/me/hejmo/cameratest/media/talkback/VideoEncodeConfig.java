package me.hejmo.cameratest.media.talkback;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author qinglian.zhang
 */
public class VideoEncodeConfig {

    public final int width;

    public final int height;

    public final int size;

    public final int offset;

    public final byte[] data;

    public VideoEncodeConfig(int w, int h, int s, int o, byte[] d) {
        this.width = w;
        this.height = h;
        this.size = s;
        this.offset = o;
        this.data = d;
    }

    public byte[] getBytes() {
        int totalLen = 17 + size;
        ByteBuffer buffer = ByteBuffer.allocate(totalLen);
        buffer.clear();
        buffer.put(ITalkback.VIDEO_ENCODE_CONFIGURE);  //1
        buffer.put(getBytesFromInt(width));            //4
        buffer.put(getBytesFromInt(height));           //4
        buffer.put(getBytesFromInt(size));             //4
        buffer.put(getBytesFromInt(offset));           //4
        buffer.put(data, offset, size);
        buffer.position(0);
        buffer.limit(buffer.capacity());

        byte[] totals = new byte[totalLen];
        buffer.get(totals, 0, totalLen);
        return totals;
    }

    @Override
    public String toString() {
        return "w : " + width + " h : " + height + " s : " + size + " o : " + offset + " data len : " + data.length;
    }

    public static VideoEncodeConfig getConfig(InputStream is) {

        int w = VideoEncodeConfig.getIntFromIS(is);
        int h = VideoEncodeConfig.getIntFromIS(is);
        int s = VideoEncodeConfig.getIntFromIS(is);
        int o = VideoEncodeConfig.getIntFromIS(is);
        byte[] tempData = new byte[s];
        int totalLen = 0;
        int len;
        while (totalLen < s) {
            try {
                len = is.read(tempData, totalLen, s - totalLen);
                totalLen += len;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        VideoEncodeConfig videoEncodeConfig = new VideoEncodeConfig(w, h, s, o, tempData);
        return videoEncodeConfig;
    }

    private static int getIntFromIS(InputStream is) {
        byte[] intBuffer = new byte[4];
        int totalLen = 0;
        int len = 0;
        while (totalLen < 4) {
            try {
                len = is.read(intBuffer, totalLen, 4 - totalLen);
                totalLen += len;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return VideoEncodeConfig.getSizeFromBytes(intBuffer);
    }

    public static byte[] getBytesFromInt(int len) {
        int temp = len;
        byte[] targets = new byte[4];
        targets[0] = (byte) (temp & 0xff);// 最低位
        targets[1] = (byte) ((temp >> 8) & 0xff);// 次低位
        targets[2] = (byte) ((temp >> 16) & 0xff);// 次高位
        targets[3] = (byte) (temp >>> 24);// 最高位,无符号右移。
        return targets;
    }

    public static int getSizeFromBytes(byte[] bytes) {
        int r = (bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00) // | 表示安位或
                | ((bytes[2] << 24) >>> 8) | (bytes[3] << 24);
        return r;
    }
}
