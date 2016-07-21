package me.hejmo.cameratest.media.talkback;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author qinglian.zhang
 */
public class VideoEncodeFrame {

    public final int size;

    public final int offset;

    public final int flag;

    public final long presentTime;

    public final byte[] data;

    public VideoEncodeFrame( int s, int o,int f,long t,byte[] d){

        this.size = s;
        this.offset = o;
        this.flag = f;
        this.presentTime = t;
        this.data = d;
    }

    public byte[] getBytes(){
        int totalLen = 21 + size;
        ByteBuffer buffer = ByteBuffer.allocate(totalLen);
        buffer.clear();
        buffer.put(ITalkback.VIDEO_ENCODE_FRAME);      //1
        buffer.put(getBytesFromInt(size));             //4
        buffer.put(getBytesFromInt(offset));           //4
        buffer.put(getBytesFromInt(flag));             //4
        buffer.put(getBytesFromLong(presentTime));     //8
        buffer.put(data,offset,size);
        buffer.position(0);
        buffer.limit(buffer.capacity());

        byte[] totals = new byte[totalLen];
        buffer.get(totals,0,totalLen);
        return totals;
    }

    @Override
    public String toString() {
        return " s : " + size + " o : " + offset + "  flag : " + flag + "   presentT : " + presentTime + " data len : " + data.length;
    }

    public static VideoEncodeFrame getFrame(InputStream is){


        int s = VideoEncodeFrame.getIntFromIS(is);
        int o = VideoEncodeFrame.getIntFromIS(is);
        int f = VideoEncodeFrame.getIntFromIS(is);
        long t = VideoEncodeFrame.getLongFromIS(is);
        byte[] tempData = new byte[s];
        int totalLen = 0;
        int p = 0;
        while(totalLen<s){
            try {
                p += is.read(tempData,p,s-p);
                totalLen+=p;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        VideoEncodeFrame videoEncodeConfig = new VideoEncodeFrame(s,o,f,t,tempData);
        return videoEncodeConfig;
    }

    private static int getIntFromIS(InputStream is){
        byte[] intBuffer = new byte[4];
        int len = 0;
        int p = 0;
        while(len < 4){
            try {
                p += is.read(intBuffer,p,4-p);
                len +=p;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return VideoEncodeFrame.getSizeFromBytes(intBuffer);
    }

    private static long getLongFromIS(InputStream is){
        byte[] longBuffer = new byte[8];
        int len = 0;
        int p = 0;
        while(len < 8){
            try {
                p += is.read(longBuffer,p,8-p);
                len +=p;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return VideoEncodeFrame.bytesToLong(longBuffer);
    }

    public static byte[] getBytesFromInt(int len){
        int temp = len;
        byte[] targets = new byte[4];
        targets[0] = (byte) (temp & 0xff);// 最低位
        targets[1] = (byte) ((temp >> 8) & 0xff);// 次低位
        targets[2] = (byte) ((temp >> 16) & 0xff);// 次高位
        targets[3] = (byte) (temp >>> 24);// 最高位,无符号右移。
        return targets;
    }

    private static ByteBuffer buffer = ByteBuffer.allocate(8);

    public static byte[] getBytesFromLong(long x){
        buffer.clear();
        buffer.putLong(0, x);
        return buffer.array();
    }
    public static long bytesToLong(byte[] bytes) {
        buffer.clear();
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    public static int getSizeFromBytes(byte[] bytes){
        int r = (bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00) // | 表示安位或
                | ((bytes[2] << 24) >>> 8) | (bytes[3] << 24);
        return r;
    }
}
