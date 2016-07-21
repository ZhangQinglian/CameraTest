package me.hejmo.cameratest.media;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.zqlite.android.logly.Logly;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

import me.hejmo.cameratest.R;
import me.hejmo.cameratest.camera.CameraHolder;
import me.hejmo.cameratest.camera.CameraUtils;
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

import static me.hejmo.cameratest.media.Contract.*;

public class MediaActivity extends AppCompatActivity {


    public static final Logly.Tag TAG = new Logly.Tag(Logly.FLAG_THREAD_NAME, "scott", Logly.DEBUG);

    //callback
    private FrameAvailableListener mFrameAvailableListener = new FrameAvailableListener();
    private SurfaceLifeCallback mDisplaySurfaceCallback = new SurfaceLifeCallback();

    //media
    private EglCore mEglCore;
    //显示绘制Camera采集的数据
    private WindowSurface mDisplaySurface;

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

    private static class MainHandler extends Handler {
        public static final int MSG_FRAME_AVAILABLE = 1;

        private WeakReference<MediaActivity> mWeakActivity;

        public MainHandler(MediaActivity activity) {
            mWeakActivity = new WeakReference<MediaActivity>(activity);
        }


        @Override
        public void handleMessage(Message msg) {
            MediaActivity activity = mWeakActivity.get();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        //get role
        String role = getIntent().getExtras().getString("role");
        Log.d("talkback","role = " + role);
        startTalkback(role);

        SurfaceView dispalySV = (SurfaceView) findViewById(R.id.display_surface);
        dispalySV.getHolder().addCallback(mDisplaySurfaceCallback);

        mHandler = new MainHandler(this);

        mSecondsOfVideo = 0.0f;

        mEncoder = new MyEncoder();
        mDecoder = new VideoDecoder();

        mReceiveSurfaceView = (SurfaceView) findViewById(R.id.receive_surface);
        mReceiveSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d("scott", "receive surface create");
                mDecoder.start();
                mEncoder.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        //show ip
        Toast.makeText(this,"ip : " + getIp(),Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ideally, the frames from the camera are at the same resolution as the input to
        // the video encoder so we don't have to scale.
        openCamera(VIDEO_WIDTH, VIDEO_HEIGHT, DESIRED_PREVIEW_FPS);
    }

    @Override
    protected void onPause() {
        super.onPause();

        releaseCamera();


        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }
        if (mDisplaySurface != null) {
            mDisplaySurface.release();
            mDisplaySurface = null;
        }
        if (mFullFrameBlit != null) {
            mFullFrameBlit.release(false);
            mFullFrameBlit = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        Log.d("media", "onPause() done");

    }

    @Override
    protected void onDestroy() {
        Log.d("talkback","mediaActivity destroy");
        super.onDestroy();
        try {
            mTalkback.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ITalkback.TalkbackCallback talkbackCallback = new ITalkback.TalkbackCallback() {
        @Override
        public void onTalkbackConnected() {

        }

        @Override
        public void onTalkbackStart() {
            //当双方socket连接成功并开始读取数据后再开始镜头预览,以此确保数据的实时性。
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mEncoder.getInputSurface() != null){
                        mCamera.startPreview();
                        mEncoderSurface = new WindowSurface(mEglCore, mEncoder.getInputSurface(), true);
                    }else{
                        mHandler.postDelayed(this,100);
                    }
                }
            });
        }
    };
    private void startTalkback(String role){
        Log.d("talkback","talkback role is :" + role);
        if(role.equals("initiator")){
            mTalkback = new Initiator(talkbackCallback);
            Executors.newSingleThreadExecutor().execute(mTalkback);
        }
        if(role.equals("responder")){
            mTalkback = new Responder(talkbackCallback);
            Executors.newSingleThreadExecutor().execute(mTalkback);
        }
    }
    private void openCamera(int desiredWidth, int desiredHeight, int desiredFps) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        mCamera = CameraHolder.getInstance(this).openCamera();
        Camera.Parameters parms = mCamera.getParameters();

        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);
        mCameraPreviewThousandFps = CameraUtils.chooseFixedPreviewFps(parms, desiredFps * 1000);
        parms.setRecordingHint(true);

        parms.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        parms.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parms.setRotation(0);
        //如果需要自动对焦，这句一定要加
        mCamera.cancelAutoFocus();

        mCamera.setParameters(parms);
        mCamera.setDisplayOrientation(90);

        Camera.Size cameraPreviewSize = parms.getPreviewSize();
        String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height +
                " @" + (mCameraPreviewThousandFps / 1000.0f) + "fps";
        Log.i("media", "Camera config: " + previewFacts);

        // Set the preview aspect ratio.
        AspectFrameLayout layout = (AspectFrameLayout) findViewById(R.id.continuousCapture_afl_display);
        layout.setAspectRatio((double) cameraPreviewSize.height / cameraPreviewSize.width);

        AspectFrameLayout layout_mirror = (AspectFrameLayout) findViewById(R.id.continuousCapture_afl_receive);
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
        mDisplaySurface.makeCurrent();
        mCameraTexture.updateTexImage();
        mCameraTexture.getTransformMatrix(mTmpMatrix);

        // Fill the SurfaceView with it.
        SurfaceView sv = (SurfaceView) findViewById(R.id.display_surface);
        int viewWidth = sv.getWidth();
        int viewHeight = sv.getHeight();
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        mFullFrameBlit.drawFrame(mTextureId, mTmpMatrix);
        drawExtra(mFrameNum, viewWidth, viewHeight);
        mDisplaySurface.swapBuffers();

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

    private class SurfaceLifeCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d("media", "surfaceCreated holder=" + holder);

            // Set up everything that requires an EGL context.
            //
            // We had to wait until we had a surface because you can't make an EGL context current
            // without one, and creating a temporary 1x1 pbuffer is a waste of time.
            //
            // The display surface that we use for the SurfaceView, and the encoder surface we
            // use for video, use the same EGL context.
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
            mDisplaySurface = new WindowSurface(mEglCore, holder.getSurface(), false);
            mDisplaySurface.makeCurrent();

            mFullFrameBlit = new FullFrameRect(
                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            mTextureId = mFullFrameBlit.createTextureObject();
            mCameraTexture = new SurfaceTexture(mTextureId);
            mCameraTexture.setOnFrameAvailableListener(mFrameAvailableListener);

            Log.d("media", "starting camera preview");
            try {
                mCamera.setPreviewTexture(mCameraTexture);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }



        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("media", "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                    " holder=" + holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("media", "surfaceDestroyed holder=" + holder);
            mEncoder.stop();
            mDecoder.stop();
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
            byte[] mBuffer = new byte[0];
            if (mBuffer.length < info.size) {
                mBuffer = new byte[info.size];
            }
            data.position(info.offset);
            data.limit(info.offset + info.size);
            data.get(mBuffer, 0, info.size);

            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                // this is the first and only config sample, which contains information about codec
                // like H.264, that let's configure the decoder

//                mDecoder.configure(mReceiveSurfaceView.getHolder().getSurface(),
//                        VIDEO_WIDTH,
//                        VIDEO_HEIGHT,
//                        mBuffer,
//                        0,
//                        info.size);
                Log.d("talkback","++++++++ w = " + VIDEO_WIDTH + " h: " + VIDEO_HEIGHT + " s: " + info.size + " o: " + 0 );
                VideoEncodeConfig config = new VideoEncodeConfig(VIDEO_WIDTH,VIDEO_HEIGHT,info.size,0,mBuffer);
                mTalkback.addVideoEncodeConfigure(config);
            } else {
                // pass byte[] to decoder's queue to render asap
//                mDecoder.decodeSample(mBuffer,
//                        0,
//                        info.size,
//                        info.presentationTimeUs,
//                        info.flags);
                Log.d("talkback","++++++++ raw  s: " + info.size + " o: " + 0 + " presentTime = " + info.presentationTimeUs );
                VideoEncodeFrame frame = new VideoEncodeFrame(info.size,0,info.flags,info.presentationTimeUs,mBuffer);
                mTalkback.addVideoEncodeFrame(frame);
            }
        }
    }

    private String getIp(){
        WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wm.getConnectionInfo();
        return intToIp(info.getIpAddress());
    }

    public String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }
}
