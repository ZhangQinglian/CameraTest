package me.hejmo.cameratest.camera;

import android.util.Log;

/**
 * Created by scott on 4/5/16.
 * @author zhangqinglian
 */
public class CameraFrame {

    private byte[] _data;

    private long _timeStamp;

    public CameraFrame(byte[] _data, long _timeStamp) {
        this._data = _data;
        this._timeStamp = _timeStamp;
    }

    public byte[] getData() {
        return _data;
    }

    public void setData(byte[] data) {
        this._data = data;
    }


    public static byte[] getSizeFromShort(int len){
        int temp = len;
        byte[] targets = new byte[4];
        targets[0] = (byte) (temp & 0xff);// 最低位
        targets[1] = (byte) ((temp >> 8) & 0xff);// 次低位
        targets[2] = (byte) ((temp >> 16) & 0xff);// 次高位
        targets[3] = (byte) (temp >>> 24);// 最高位,无符号右移。
        return targets;
    }

    public static int getSizeFromBytes(byte[] bytes){
        int r = (bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00) // | 表示安位或
                | ((bytes[2] << 24) >>> 8) | (bytes[3] << 24);
        return r;
    }
    public long getTimeStamp() {
        return _timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this._timeStamp = timeStamp;
    }
}
