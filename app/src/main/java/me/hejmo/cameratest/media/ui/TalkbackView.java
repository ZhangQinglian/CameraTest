package me.hejmo.cameratest.media.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * @author qinglian.zhang
 */

public class TalkbackView extends FrameLayout {

    private View childFront;

    private int height;

    private int width;


    private FrameLayout.LayoutParams childFrontLP ;

    public TalkbackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if(hasNavbar()){
                height = getHeight() - getNavbarH() - 26;
                }else {
                    height = getHeight();
                }
                width = getWidth();

                Log.d("scott","width = " + width + "    heght = " + height);
                getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        childFront = getChildAt(1);
        childFrontLP = (LayoutParams) childFront.getLayoutParams();
        childFront.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        childFront.setOnTouchListener(mTouch);

        childFrontLP.setMargins(childFrontLP.leftMargin + animationPadding,childFrontLP.topMargin + animationPadding,childFrontLP.rightMargin,childFrontLP.bottomMargin);
        childFront.setLayoutParams(childFrontLP);
    }

    private final int animationPadding = 20;

    private void doAnimaiton() {
        int left = childFrontLP.leftMargin;
        int top = childFrontLP.topMargin;
        int right = width - (left + childFront.getWidth());
        int bottom = height - (top + childFront.getHeight());
        Log.d("scott","left = " + left + "  top = " + top + " right = " + right + "  bottom = " + bottom);
        int strangePaddingLeft = width-animationPadding-childFront.getWidth();
        int strangePaddingTop = height-animationPadding-childFront.getHeight();
        if(left-animationPadding<0 && top-animationPadding<0){
            animat2(left,top,animationPadding,animationPadding);
            return ;
        }
        if(right-animationPadding<0 && top-animationPadding<0){
            animat2(left,top,strangePaddingLeft,animationPadding);
            return ;
        }
        if(left-animationPadding<0 && bottom-animationPadding<0){
            animat2(left,top,animationPadding,strangePaddingTop);
            return ;
        }
        if(right-animationPadding<0 && bottom-animationPadding<0){
            animat2(left,top,strangePaddingLeft,strangePaddingTop);
            return ;
        }
        if(left-animationPadding<0){
            animat2(left,top,animationPadding,top);
            return ;
        }
        if(right-animationPadding<0){
            animat2(left,top,strangePaddingLeft,top);
            return ;
        }
        if(top - animationPadding<0){
            animat2(left,top,left,animationPadding);
            return ;
        }
        if(bottom - animationPadding<0){
            animat2(left,top,left,strangePaddingTop);
            return ;
        }
        if(left<top && left<right&& left<bottom){
            animat2(left,top,animationPadding,top);
            return ;
        }
        if(right<left && right<bottom && right<top){
            animat2(left,top,strangePaddingLeft,top);
            return ;
        }
        if(top < right&& top<left&&top<bottom){
            animat2(left,top,left,animationPadding);
            return ;
        }
        if(bottom < right&& bottom<left&&bottom<top){
            animat2(left,top,left,strangePaddingTop);
            return ;
        }
        if((left == right)){
            animat2(left,top,animationPadding,top);
            return ;
        }
        if((top == bottom) ){
            animat2(left,top,left,animationPadding);
        }
    }

    private void animat2(final int left,final int top, final int armLeft, final int armTop) {
        ValueAnimator animator = ValueAnimator.ofInt(0,100);
        animator.setDuration(300);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                int deltaLeft = (int) ((armLeft-left)/100.0*value);
                int deltaTop = (int) ((armTop-top)/100.0*value);
                childFrontLP.setMargins(left + deltaLeft,top + deltaTop,childFrontLP.rightMargin,childFrontLP.bottomMargin);
                childFront.setLayoutParams(childFrontLP);
            }
        });
        animator.start();
    }

    private boolean hasNavbar(){
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
        boolean hasHardwareButtons = hasBackKey && hasHomeKey;
        return !hasHardwareButtons;
    }

    private int getNavbarH(){
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    OnTouchListener mTouch = new OnTouchListener() {

        float startX;
        float startY;
        float movingX;
        float movingY;
        float deltaX;
        float deltaY;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    break;

                case MotionEvent.ACTION_UP:
                    doAnimaiton();
                    break;

                case MotionEvent.ACTION_MOVE:
                    movingX = event.getX();
                    movingY = event.getY();
                    deltaX = movingX - startX;
                    deltaY = movingY -startY;
                    childFrontLP.setMargins(childFrontLP.leftMargin + (int)deltaX,childFrontLP.topMargin + (int)deltaY,childFrontLP.rightMargin,childFrontLP.bottomMargin);
                    childFront.setLayoutParams(childFrontLP);
                    break;
            }
            return true;
        }
    };

}
