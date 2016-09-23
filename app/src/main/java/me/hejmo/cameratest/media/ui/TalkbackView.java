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

    private int totalHeight;

    private int totalWidth;

    private int frontMiniH;

    private int frontMiniW;


    private FrameLayout.LayoutParams childFrontLP ;

    public TalkbackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if(hasNavbar()){
                totalHeight = getHeight() - getNavbarH() - 26;
                }else {
                    totalHeight = getHeight();
                }
                totalWidth = getWidth();
                frontMiniH = childFront.getHeight();
                frontMiniW = childFront.getWidth();
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
        int right = totalWidth - (left + childFront.getWidth());
        int bottom = totalHeight - (top + childFront.getHeight());
        Log.d("scott","left = " + left + "  top = " + top + " right = " + right + "  bottom = " + bottom);
        int strangePaddingLeft = totalWidth -animationPadding-childFront.getWidth();
        int strangePaddingTop = totalHeight -animationPadding-childFront.getHeight();
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
        float childW;
        float childH;

        float moveX1 ;
        float moveX2 ;
        float moveY1 ;
        float moveY2 ;
        float deltaDiagonal = -1;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    childW = childFront.getWidth();
                    childH = childFront.getHeight();

                    break;

                case MotionEvent.ACTION_UP:
                    doAnimaiton();
                    deltaDiagonal = -1;
                    break;

                case MotionEvent.ACTION_MOVE:
                    int count = event.getPointerCount();
                    Log.d("scott"," motion count = " + count);
                    if(count == 1){
                        movingX = event.getX();
                        movingY = event.getY();
                        deltaX = movingX - startX;
                        deltaY = movingY -startY;
                        childFrontLP.setMargins(childFrontLP.leftMargin + (int)deltaX,childFrontLP.topMargin + (int)deltaY,childFrontLP.rightMargin,childFrontLP.bottomMargin);
                        childFront.setLayoutParams(childFrontLP);
                    }
                    if(count == 2){
                        moveX1 = event.getX(0);
                        moveX2 = event.getX(1);
                        moveY1 = event.getY(0);
                        moveY2 = event.getY(1);
                        if(deltaDiagonal == -1){
                            deltaDiagonal = (int) Math.sqrt(
                                    Math.pow((moveX2-moveX1),2)
                                            +Math.pow((moveY2-moveY1),2));
                        }
                        int delta = (int) ((int) Math.sqrt(
                                                        Math.pow((moveX2-moveX1),2)
                                                        +Math.pow((moveY2-moveY1),2)) - deltaDiagonal);
                        Log.d("scott","delta = " + delta);
                        float p = childW/childH;
                        int tmpW = (int) (childW+delta/2);
                        int tmpH = (int)(childH + delta/2/p);
                        if(tmpW >= frontMiniW && tmpW <= totalWidth/3*2 &&
                                tmpH >= frontMiniH && tmpH <= totalHeight/3*2   ){
                            childFrontLP.width = tmpW;
                            childFrontLP.height = tmpH;
                            childFront.setLayoutParams(childFrontLP);
                        }
                    }

                    break;
            }
            return true;
        }
    };


}
