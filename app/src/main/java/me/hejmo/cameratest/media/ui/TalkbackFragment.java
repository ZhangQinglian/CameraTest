package me.hejmo.cameratest.media.ui;


import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.zqlite.android.logly.Logly;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

import me.hejmo.cameratest.R;
import me.hejmo.cameratest.camera.CameraHolder;
import me.hejmo.cameratest.camera.CameraUtils;
import me.hejmo.cameratest.media.AspectFrameLayout;
import me.hejmo.cameratest.media.Contract;
import me.hejmo.cameratest.media.MediaActivity;
import me.hejmo.cameratest.media.gles.EglCore;
import me.hejmo.cameratest.media.gles.FullFrameRect;
import me.hejmo.cameratest.media.gles.Texture2dProgram;
import me.hejmo.cameratest.media.gles.WindowSurface;
import me.hejmo.cameratest.media.mediacodec.VideoDecoder;
import me.hejmo.cameratest.media.mediacodec.VideoEncoder;
import me.hejmo.cameratest.media.talkback.ITalkback;
import me.hejmo.cameratest.media.talkback.Initiator;
import me.hejmo.cameratest.media.talkback.Responder;
import me.hejmo.cameratest.media.talkback.VideoEncodeConfig;
import me.hejmo.cameratest.media.talkback.VideoEncodeFrame;

import static me.hejmo.cameratest.media.Contract.DESIRED_PREVIEW_FPS;
import static me.hejmo.cameratest.media.Contract.VIDEO_HEIGHT;
import static me.hejmo.cameratest.media.Contract.VIDEO_WIDTH;
import static me.hejmo.cameratest.media.MediaActivity.TAG;

/**
 * @author qinglian.zhang
 */
public class TalkbackFragment extends Fragment implements TalkbackContract.View{

    //callback
    private FrameAvailableListener mFrameAvailableListener = new FrameAvailableListener();
    private DisplaySurfaceLifeCallback mDisplaySurfaceCallback = new DisplaySurfaceLifeCallback();

    //media
    private EglCore mEglCore;
    //显示绘制Camera采集的数据
    private WindowSurface mDisplayWindowSurface;

    private SurfaceView mDisplaySV;

    //用于接收Camera的preview数据
    private SurfaceTexture mCameraTexture;

    //用于显示视频对话方的视频数据
    private SurfaceView mReceiveSurfaceView;

    private FullFrameRect mFullFrameBlit;
    private final float[] mTmpMatrix = new float[16];
    private int mTextureId;
    private int mFrameNum;

    private Camera mCamera;
    private int mCameraPreviewThousandFps;

    //Encoder的input surface
    private WindowSurface mEncoderSurface;

    private MainHandler mHandler;
    private float mSecondsOfVideo;

    private VideoEncoder mEncoder;
    private VideoDecoder mDecoder;

    //视频对话
    private ITalkback mTalkback;
    private String mRole;
    private String mIP;

    private VideoEncodeConfig mConfig = null;

    private View mView;
    private TalkbackContract.Presenter mPresenter;

    @Override
    public void setPresenter(TalkbackContract.Presenter presenter) {
        mPresenter = presenter;
    }

    private static class MainHandler extends Handler {
        public static final int MSG_FRAME_AVAILABLE = 1;

        private WeakReference<TalkbackFragment> mWeakActivity;

        public MainHandler(TalkbackFragment activity) {
            mWeakActivity = new WeakReference<TalkbackFragment>(activity);
        }


        @Override
        public void handleMessage(Message msg) {
            TalkbackFragment activity = mWeakActivity.get();
            if (activity == null) {
                Logly.d(TAG, "Got message for dead activity");
                return;
            }

            switch (msg.what) {
                case MSG_FRAME_AVAILABLE: {
                    activity.drawFrame();
                    break;
                }

                default:
                    throw new RuntimeException("Unknown message " + msg.what);
            }
        }
    }

    public TalkbackFragment() {
        // Required empty public constructor
    }


    public static TalkbackFragment newInstance(String role, String ip) {
        TalkbackFragment fragment = new TalkbackFragment();
        Bundle args = new Bundle();
        args.putString(Contract.ROLE, role);
        if(Contract.RESPONDER.equals(role)){
            args.putString(Contract.IP, ip);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mRole = getArguments().getString(Contract.ROLE);
            if(Contract.RESPONDER.equals(mRole)){
                mIP = getArguments().getString(Contract.IP);
            }
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        Logly.d(TAG, "onStart()");
        mDisplaySV.setVisibility(View.VISIBLE);
        mReceiveSurfaceView.setVisibility(View.VISIBLE);

    }

    @Override
    public void onResume() {
        super.onResume();
        Logly.d(TAG, "onResume");

    }

    @Override
    public void onPause() {
        super.onPause();
        Logly.d(TAG, "onPause");


        Logly.d(TAG, "onPause() done");

    }

    @Override
    public void onStop() {
        super.onStop();
        Logly.d(TAG, "onStop()");
        mDisplaySV.setVisibility(View.INVISIBLE);
        mReceiveSurfaceView.setVisibility(View.INVISIBLE);
        mTalkback.pause();
        releaseCamera();

        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }
        if (mDisplayWindowSurface != null) {
            mDisplayWindowSurface.release();
            mDisplayWindowSurface = null;
        }
        if (mFullFrameBlit != null) {
            mFullFrameBlit.release(false);
            mFullFrameBlit = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    @Override
    public void onDestroy() {
        Logly.d(TAG, "mediaActivity destroy");
        super.onDestroy();
        try {
            mTalkback.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView =  inflater.inflate(R.layout.fragment_talkback, container, false);
        mDisplaySV = (SurfaceView) mView.findViewById(R.id.display_surface);
        mDisplaySV.getHolder().addCallback(mDisplaySurfaceCallback);
        mDisplaySV.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mDisplaySV.setZOrderOnTop(true);
        mHandler = new MainHandler(this);

        mSecondsOfVideo = 0.0f;

        mEncoder = new MyEncoder();
        mDecoder = new VideoDecoder();

        mReceiveSurfaceView = (SurfaceView) mView.findViewById(R.id.receive_surface);
        mReceiveSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Logly.d(TAG, "receive SV create");
                // todo:decoder的生命周期考虑跟随RecevierSV
                mDecoder.start();
                if(mConfig != null){
                    mDecoder.configure(mReceiveSurfaceView.getHolder().getSurface(),
                            mConfig.width,
                            mConfig.height,
                            mConfig.data,
                            mConfig.offset,
                            mConfig.size);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Logly.d(TAG, "Receiver SV destroyed");
                //todo: decoder生命周期建议考虑跟随ReceiverSV
                mDecoder.stop();
            }
        });

        //show ip
        Toast.makeText(getContext(), "ip : " + getIp(), Toast.LENGTH_SHORT).show();
        startTalkback();
        return mView ;
    }

    private ITalkback.TalkbackCallback talkbackCallback = new ITalkback.TalkbackCallback() {

        @Override
        public void onConfig(VideoEncodeConfig config) {
            Log.d("scott","decoder config");
            //configure只会有一次,这里安全不做处理
            mDecoder.configure(mReceiveSurfaceView.getHolder().getSurface(),
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

            mDecoder.decodeSample(frame.data,
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

    private void startTalkback() {
        if (mRole.equals(Contract.INITIATOR)) {
            mTalkback = new Initiator(talkbackCallback,mRole);
            Executors.newSingleThreadExecutor().execute(mTalkback);
        }
        if (mRole.equals(Contract.RESPONDER)) {
            mTalkback = new Responder(talkbackCallback,mRole,mIP);
            Executors.newSingleThreadExecutor().execute(mTalkback);
        }
    }

    private void openCamera(int desiredWidth, int desiredHeight, int desiredFps) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        mCamera = CameraHolder.getInstance(getContext()).openCamera();
        Camera.Parameters parms = mCamera.getParameters();

        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);
        mCameraPreviewThousandFps = CameraUtils.chooseFixedPreviewFps(parms, desiredFps * 1000);
        parms.setRecordingHint(true);

        //parms.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        //parms.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parms.setRotation(0);
        //如果需要自动对焦，这句一定要加
        mCamera.cancelAutoFocus();

        mCamera.setParameters(parms);
        mCamera.setDisplayOrientation(360-CameraUtils.getFrontCameraDegree());

        Camera.Size cameraPreviewSize = parms.getPreviewSize();
        String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height +
                " @" + (mCameraPreviewThousandFps / 1000.0f) + "fps";
        Log.i("media", "Camera config: " + previewFacts);

        // Set the preview aspect ratio.

        AspectFrameLayout layout = (AspectFrameLayout) mView.findViewById(R.id.continuousCapture_afl_display);
        layout.setAspectRatio((double) cameraPreviewSize.height / cameraPreviewSize.width);
        AspectFrameLayout layout_mirror = (AspectFrameLayout) mView.findViewById(R.id.continuousCapture_afl_receive);
        layout_mirror.setAspectRatio((double) cameraPreviewSize.height / cameraPreviewSize.width);

    }


    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d("media", "releaseCamera -- done");
        }
    }

    private void drawFrame() {
        //Log.d(TAG, "drawFrame");
        if (mEglCore == null) {
            Log.d("media", "Skipping drawFrame after shutdown");
            return;
        }

        // Latch the next frame from the camera.
        mDisplayWindowSurface.makeCurrent();
        mCameraTexture.updateTexImage();
        mCameraTexture.getTransformMatrix(mTmpMatrix);

        // Fill the SurfaceView with it.
        SurfaceView sv = (SurfaceView) mView.findViewById(R.id.display_surface);
        int viewWidth = sv.getWidth();
        int viewHeight = sv.getHeight();
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        mFullFrameBlit.drawFrame(mTextureId, mTmpMatrix);
        drawExtra(mFrameNum, viewWidth, viewHeight);
        mDisplayWindowSurface.swapBuffers();

        mEncoderSurface.makeCurrent();
        GLES20.glViewport(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
        mFullFrameBlit.drawFrame(mTextureId, mTmpMatrix);
        drawExtra(mFrameNum, VIDEO_WIDTH, VIDEO_HEIGHT);
        // mCircEncoder.frameAvailableSoon();
        mEncoderSurface.setPresentationTime(mCameraTexture.getTimestamp());
        mEncoderSurface.swapBuffers();
        // Send it to the video encoder.


        mFrameNum++;
    }

    private static void drawExtra(int frameNum, int width, int height) {
        // We "draw" with the scissor rect and clear calls.  Note this uses window coordinates.
        int val = frameNum % 3;
        switch (val) {
            case 0:
                GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
                break;
            case 1:
                GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                break;
            case 2:
                GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
                break;
        }

        int xpos = (int) (width * ((frameNum % 100) / 100.0f));
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 0, width / 32, height / 32);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    private class FrameAvailableListener implements SurfaceTexture.OnFrameAvailableListener {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mHandler.sendEmptyMessage(MainHandler.MSG_FRAME_AVAILABLE);
        }
    }

    private class DisplaySurfaceLifeCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Logly.d(TAG, "Display SV create holder=" + holder);

            // Set up everything that requires an EGL context.
            //
            // We had to wait until we had a surface because you can't make an EGL context current
            // without one, and creating a temporary 1x1 pbuffer is a waste of time.
            //
            // The display surface that we use for the SurfaceView, and the encoder surface we
            // use for video, use the same EGL context.
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
            mDisplayWindowSurface = new WindowSurface(mEglCore, holder.getSurface(), false);
            mDisplayWindowSurface.makeCurrent();

            mFullFrameBlit = new FullFrameRect(
                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            mTextureId = mFullFrameBlit.createTextureObject();
            mCameraTexture = new SurfaceTexture(mTextureId);
            mCameraTexture.setOnFrameAvailableListener(mFrameAvailableListener);

            Logly.d(TAG, "set camera preview");
            try {
                openCamera(holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height(), DESIRED_PREVIEW_FPS);
                mCamera.setPreviewTexture(mCameraTexture);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            // todo:encoder的生命周期考虑跟随DisplaySV
            mEncoder.start();
            //当双方socket连接成功并开始读取数据后再开始镜头预览,以此确保数据的实时性。
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mEncoder.getInputSurface() != null) {
                        mCamera.startPreview();
                        mEncoderSurface = new WindowSurface(mEglCore, mEncoder.getInputSurface(), true);
                    } else {
                        mHandler.postDelayed(this, 100);
                    }
                }
            });
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Logly.d(TAG, "Display SV changedfmt=" + format + " size=" + width + "x" + height +
                    " holder=" + holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Logly.d(TAG, "Display SV Destroyed holder=" + holder);
            //todo: encoder生命周期建议考虑跟随DisplaySV
            mEncoder.stop();

        }
    }

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

    private String getIp() {
        WifiManager wm = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wm.getConnectionInfo();
        return intToIp(info.getIpAddress());
    }

    public String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }
}
