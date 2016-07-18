package me.hejmo.cameratest.media;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.zqlite.android.logly.Logly;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import me.hejmo.cameratest.R;
import me.hejmo.cameratest.camera.CameraHolder;
import me.hejmo.cameratest.camera.CameraUtils;
import me.hejmo.cameratest.media.gles.EglCore;
import me.hejmo.cameratest.media.gles.FullFrameRect;
import me.hejmo.cameratest.media.gles.Texture2dProgram;
import me.hejmo.cameratest.media.gles.WindowSurface;
import me.hejmo.cameratest.media.mediacodec.VideoDecoder;
import me.hejmo.cameratest.media.mediacodec.VideoEncoder;

import static me.hejmo.cameratest.media.Contract.*;

public class MediaActivity extends AppCompatActivity {


    public static final Logly.Tag TAG = new Logly.Tag(Logly.FLAG_THREAD_NAME, "media", Logly.DEBUG);

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

        SurfaceView dispalySV = (SurfaceView) findViewById(R.id.display_surface);
        dispalySV.getHolder().addCallback(mDisplaySurfaceCallback);

        mHandler = new MainHandler(this);

        mSecondsOfVideo = 0.0f;

        mEncoder = new MyEncoder();
        mDecoder = new VideoDecoder();

        mReceiveSurfaceView = (SurfaceView) findViewById(R.id.receive_surface);

        mDecoder.start();
        mEncoder.start();
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

        // Send it to the video encoder.
        mEncoderSurface.makeCurrent();
        GLES20.glViewport(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
        mFullFrameBlit.drawFrame(mTextureId, mTmpMatrix);
        drawExtra(mFrameNum, VIDEO_WIDTH, VIDEO_HEIGHT);
        // mCircEncoder.frameAvailableSoon();
        mEncoderSurface.setPresentationTime(mCameraTexture.getTimestamp());
        mEncoderSurface.swapBuffers();

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
            Log.d("media", "frame available");
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
            mCamera.startPreview();
            mEncoderSurface = new WindowSurface(mEglCore, mEncoder.getInputSurface(), true);

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("media", "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                    " holder=" + holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("media", "surfaceDestroyed holder=" + holder);
        }
    }

    class MyEncoder extends VideoEncoder {

        byte[] mBuffer = new byte[0];

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
            if (mBuffer.length < info.size) {
                mBuffer = new byte[info.size];
            }
            Logly.d(TAG, "frame size = " + info.size);
            data.position(info.offset);
            data.limit(info.offset + info.size);
            data.get(mBuffer, 0, info.size);

            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                // this is the first and only config sample, which contains information about codec
                // like H.264, that let's configure the decoder
                mDecoder.configure(mReceiveSurfaceView.getHolder().getSurface(),
                        VIDEO_WIDTH,
                        VIDEO_HEIGHT,
                        mBuffer,
                        0,
                        info.size);
            } else {
                // pass byte[] to decoder's queue to render asap
                mDecoder.decodeSample(mBuffer,
                        0,
                        info.size,
                        info.presentationTimeUs,
                        info.flags);
            }
        }
    }
}
