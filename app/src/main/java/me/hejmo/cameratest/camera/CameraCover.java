package me.hejmo.cameratest.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by scott on 4/8/16.
 * @author zhangqinglian
 */
public class CameraCover extends View {

    private Paint paint;

    private Handler mHandler = new Handler();

    private int mCurrentCoverAlpha = 255;

    public CameraCover(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void init(){
        paint = new Paint();

    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawColor(Color.argb(mCurrentCoverAlpha,0,0,0));
        float x = getX();
        float y = getY();
        int w = getWidth();
        int h = getHeight();
    }

    public void startShutter(){
        mHandler.post(new StartShutterWorker());
    }

    private class StartShutterWorker implements Runnable{

        @Override
        public void run() {
            ValueAnimator valueAnimator = new ValueAnimator();
            valueAnimator.setIntValues(0, 400);
            valueAnimator.setDuration(600);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    mCurrentCoverAlpha = (Integer) (animation.getAnimatedValue());
                    if (200 - mCurrentCoverAlpha < 0) {
                        mCurrentCoverAlpha = 400 - mCurrentCoverAlpha;
                    }
                    invalidate();
                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setVisibility(VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mCurrentCoverAlpha = 255;
                    setVisibility(INVISIBLE);
                }
            });
            valueAnimator.start();

        }
    }

}
