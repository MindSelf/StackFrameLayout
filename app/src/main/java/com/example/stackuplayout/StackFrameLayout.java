package com.example.stackuplayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by zhaolexi on 2020-01-15.
 */
public class StackFrameLayout extends FrameLayout {

    private static final int DEFAULT_GAP = 36;
    private static final float DEFAULT_ALPHA = 0.6F;
    private static final float DEFAULT_SCALE = 0.9F;
    private static final int DEFAULT_DURATION = 200;

    private Adapter.Observer mObserver;
    private Adapter mAdapter;

    private AnimatorSet mAnimatorForward, mAnimatorBack;

    private int mGap;
    private float mBackViewAlpha;
    private float mBackViewScale;
    private long mDuration;

    private float mTranslationY;

    public StackFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public StackFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.StackFrameLayout);
        mGap = ta.getDimensionPixelSize(R.styleable.StackFrameLayout_stack_gap, DEFAULT_GAP);
        mBackViewAlpha = ta.getFloat(R.styleable.StackFrameLayout_stack_backview_alpha, DEFAULT_ALPHA);
        mBackViewScale = ta.getFloat(R.styleable.StackFrameLayout_stack_backview_scale, DEFAULT_SCALE);
        mDuration = ta.getInteger(R.styleable.StackFrameLayout_stack_duration, DEFAULT_DURATION);
        ta.recycle();
        mTranslationY = Resources.getSystem().getDisplayMetrics().heightPixels; //一个足以让View跑出屏幕外的translation
    }

    private void forward() {

        if (mAnimatorForward != null && mAnimatorForward.isRunning()) {
            mAnimatorForward.cancel();
        }

        mAnimatorForward = initAnimatorSet();
        AnimatorSet.Builder builder = null;

        int count = getChildCount();

        //remove backView
        if (count > 1) {
            View backView = getChildAt(0);
            mAdapter.destroyView(StackFrameLayout.this, backView);
            count--;
        }

        //frontView -> backView
        if (count > 0) {
            View frontView = getChildAt(count - 1);
            ObjectAnimator translationY = ObjectAnimator.ofFloat(frontView, "translationY", frontView.getTranslationY(), -mGap);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(frontView, "alpha", frontView.getAlpha(), mBackViewAlpha);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(frontView, "scaleX", frontView.getScaleX(), mBackViewScale);
            builder = mAnimatorForward.play(translationY).with(alpha).with(scaleX);
        }

        //add frontView
        View enterView = instantiateView(-1);
        mAdapter.updateFrontView(enterView);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(enterView, "translationY", mTranslationY, 0);
        if (builder != null) {
            builder.with(translationY);
        } else {
            mAnimatorForward.play(translationY);
        }

        mAnimatorForward.start();
    }

    private void back(boolean hasMore) {

        if (mAnimatorBack != null && mAnimatorBack.isRunning()) {
            mAnimatorBack.cancel();
        }

        int count = getChildCount();
        if (count == 0) {
            return;
        }

        mAnimatorBack = initAnimatorSet();
        AnimatorSet.Builder builder;

        //remove frontView
        final View exitView = getChildAt(count - 1);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(exitView, "translationY", exitView.getTranslationY(), mTranslationY);
        translationY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAdapter.destroyView(StackFrameLayout.this, exitView);
            }
        });
        builder = mAnimatorBack.play(translationY);

        //backView -> frontView
        if (count > 1) {
            View frontView = getChildAt(count - 2);
            ObjectAnimator translationY1 = ObjectAnimator.ofFloat(frontView, "translationY", frontView.getTranslationY(), 0);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(frontView, "alpha", mBackViewAlpha, 1);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(frontView, "scaleX", mBackViewScale, 1);
            builder.with(translationY1).with(alpha).with(scaleX);
        }

        if (hasMore) {
            View backView = instantiateView(0);
            mAdapter.updateBehindView(backView);
            backView.setScaleX(mBackViewScale);
            backView.setTranslationY(-mGap);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(backView, "alpha", 0, mBackViewAlpha);
            builder.with(alpha);
        }

        mAnimatorBack.start();
    }

    private View instantiateView(int index) {
        View newView = mAdapter.instantiateView(this, index);
        //reset transform
        newView.setTranslationY(0);
        newView.setAlpha(1);
        newView.setScaleX(1);
        newView.setScaleY(1);
        return newView;
    }

    private AnimatorSet initAnimatorSet() {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(mDuration);
        return animatorSet;
    }

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
        if (mObserver == null) {
            mObserver = new Adapter.Observer() {
                @Override
                public void onChanged() {
                    //更新可见的两个View
                    int count = getChildCount();
                    if (count > 0) {
                        mAdapter.updateFrontView(getChildAt(count - 1));
                    }
                    if (count > 1) {
                        mAdapter.updateBehindView(getChildAt(count - 2));
                    }
                }

                @Override
                public void onPush() {
                    forward();
                }

                @Override
                public void onPop(boolean hasMore) {
                    back(hasMore);
                }
            };
        }
        mAdapter.addObserver(mObserver);
    }

    public boolean isAnimating() {
        return (mAnimatorBack != null && mAnimatorBack.isRunning())
                || (mAnimatorForward != null && mAnimatorForward.isRunning());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAdapter != null) {
            mAdapter.removeObserver(mObserver);
        }
        if (mAnimatorForward != null && mAnimatorForward.isRunning()) {
            mAnimatorForward.cancel();
        }
        if (mAnimatorBack != null && mAnimatorBack.isRunning()) {
            mAnimatorBack.cancel();
        }
    }

    public static abstract class Adapter<V extends View,M> {

        private List<Observer> observers = new ArrayList<>();
        private List<View> viewCache = new ArrayList<>();
        private Stack<M> data;
        private int lastSize;

        public void setLinkRecordModel(@NonNull Stack<M> newData) {

            data = newData;
            int newSize = newData.size();

            for (Observer observer : observers) {
                if (lastSize < newSize) {
                    observer.onPush();
                } else if (lastSize > newSize) {
                    observer.onPop(data.size() >= 2);
                } else {
                    observer.onChanged();
                }
            }

            lastSize = newSize;
        }

        public void addObserver(Observer observer) {
            if (!observers.contains(observer)) {
                observers.add(observer);
            }
        }

        public void removeObserver(Observer observer) {
            observers.remove(observer);
        }

        private void updateFrontView(View frontView) {
            if (!data.isEmpty()) {
                bindView((V) frontView, data.peek());
            }
        }

        private void updateBehindView(View backView) {
            if (data.size() > 1) {
                bindView((V) backView, data.get(data.size() - 2));
            }
        }

        protected View instantiateView(ViewGroup container, int index) {
            View child;
            if (viewCache.isEmpty()) {
                child = createView(container);
            } else {
                child = viewCache.remove(0);
            }
            container.addView(child, index);
            return child;
        }

        protected void destroyView(ViewGroup container, View view) {
            container.removeView(view);
            viewCache.add(view);
        }

        abstract View createView(ViewGroup parent);

        abstract void bindView(V view, M model);

        public interface Observer {

            void onChanged();

            void onPush();

            /**
             * @param hasMore 后面还有没有数据
             */
            void onPop(boolean hasMore);
        }
    }

}
