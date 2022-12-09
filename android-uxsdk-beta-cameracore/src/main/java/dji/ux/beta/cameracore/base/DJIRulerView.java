package dji.ux.beta.cameracore.base;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.Scroller;

import dji.ux.beta.cameracore.R;

public class DJIRulerView extends View {
    private static final String TAG = "DJIRulerView";
    protected static final int DEFAULT_INTERVAL = 10;
    protected static final int DEFAULT_NUMBER = 13;
    protected int width = 0;
    protected int height = 0;
    protected Paint drawPaint = null;
    protected Drawable selectDrawable = null;
    protected int scaleColor = 0;
    protected int scalePadding = 0;
    protected float density = 0.0F;
    protected int minVelocity = 0;
    protected int maxVelocity = 0;
    protected Scroller scroller = null;
    protected VelocityTracker velocityTracker = null;
    protected int offsetY = 0;
    protected int lastTouchY = 0;
    protected final RectF tmpRect = new RectF();
    protected int maxSize = 2000;
    protected int curSize = 0;
    protected int interval = 10;
    protected OnRulerScrollListener onScrollListener = null;
    protected OnRulerChangeListener onChangeListener = null;

    public DJIRulerView(Context var1, AttributeSet var2) {
        super(var1, var2);
        this.initDatas(var1);
        if (!this.isInEditMode()) {
            this.initDefaultAttrs();
        }
    }

    private void obtainTracker() {
        if (this.velocityTracker == null) {
            this.velocityTracker = VelocityTracker.obtain();
        }

    }

    private void recycleTracker() {
        VelocityTracker var1;
        if ((var1 = this.velocityTracker) != null) {
            var1.clear();
            this.velocityTracker.recycle();
            this.velocityTracker = null;
        }

    }

    private void requestInterceptEvent() {
        ViewParent var1;
        if ((var1 = this.getParent()) != null) {
            var1.requestDisallowInterceptTouchEvent(true);
        }

    }

    private void onOffsetChanged(int var1) {
        int var2;
        if ((var1 = (int) ((float) var1 / this.density)) != (var2 = this.curSize)) {
            this.curSize = var1;
            OnRulerChangeListener var3;
            if ((var3 = this.onChangeListener) != null) {
                var3.onChanged(this, var1, var2, true);
            }
        }

    }

    private void scrollOverY(int var1) {
        int var2 = (int) ((float) (this.maxSize + 1) * this.density);
        this.offsetY += var1;
        if ((var1 = this.offsetY) < 0) {
            this.offsetY = 0;
        } else if (var1 > var2) {
            this.offsetY = var2;
        }

        this.onOffsetChanged(this.offsetY);
        this.postInvalidate();
    }

    private int recalAlpha(float var1, float var2) {
        float var10000 = Math.abs(var1 + this.density / 2.0F - var2);
        return (int) (((1.0F - var10000 / var2) * (1.0F - var10000 / var2) * 0.95F + 0.05F) * 255.0F);
    }

    public void setOnScrollListener(OnRulerScrollListener var1) {
        this.onScrollListener = var1;
    }

    public void setOnChangeListener(OnRulerChangeListener var1) {
        this.onChangeListener = var1;
    }

    public void setMaxSize(int var1) {
        if (var1 != this.maxSize) {
            this.maxSize = var1;
            int var2;
            if ((var2 = this.curSize) > var1) {
                this.curSize = var1;
                OnRulerChangeListener var3;
                if ((var3 = this.onChangeListener) != null) {
                    var3.onChanged(this, var1, var2, false);
                }

                this.offsetY = (int) ((float) (var1 + 1) * this.density);
            }

            this.postInvalidate();
        }

    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public boolean isAtMin() {
        return this.curSize == 0;
    }

    public boolean isAtMax() {
        return this.curSize == this.maxSize;
    }

    public void setCurSizeNow(int var1) {
        int var2 = this.curSize;
        this.curSize = var1;
        OnRulerChangeListener var3;
        if ((var3 = this.onChangeListener) != null) {
            var3.onChanged(this, var1, var2, false);
        }

        this.offsetY = (int) ((float) var1 * this.density);
        this.postInvalidate();
    }

    public void setCurSize(int var1) {
        if (var1 != this.curSize) {
            int var2;
            if (var1 <= (var2 = this.maxSize)) {
                if (var1 < 0) {
                    var2 = 0;
                } else {
                    var2 = var1;
                }
            }

            var1 = (int) ((float) Math.abs(this.curSize - var2) * 1.0F / 8.0F + 1.0F);
            this.post(new ScrollRunnable(this.curSize, var2, var1));
        }

    }

    public int getCurSize() {
        return this.curSize;
    }

    public int stepUp(int var1) {
        int var2;
        int var3;
        if ((var2 = this.curSize) < (var3 = this.maxSize)) {
            if ((var1 += var2) > var3) {
                var1 = var3;
            }

            this.post(new ScrollRunnable(this.curSize, var1));
        } else {
            var1 = var2;
        }

        return var1;
    }

    public int stepDown(int var1) {
        int var2;
        if ((var2 = this.curSize) > 0) {
            if ((var1 = var2 - var1) < 0) {
                var1 = 0;
            }

            this.post(new ScrollRunnable(this.curSize, var1));
        } else {
            var1 = var2;
        }

        return var1;
    }

    public void stepNext() {
        int var1;
        int var2;
        if ((var1 = this.curSize) < (var2 = this.maxSize)) {
            if ((var1 += this.interval) > var2) {
                var1 = var2;
            }

            this.post(new ScrollRunnable(this.curSize, var1));
        }

    }

    public void stepPrev() {
        int var1;
        if ((var1 = this.curSize) > 0) {
            if ((var1 -= this.interval) < 0) {
                var1 = 0;
            }

            this.post(new ScrollRunnable(this.curSize, var1));
        }

    }

    protected void initDefaultAttrs() {
        Resources var10002 = this.getResources();
        this.scaleColor = var10002.getColor(R.color.white);
        this.scalePadding = var10002.getDimensionPixelSize(R.dimen.uxsdk_gen_corner_radius);
        this.drawPaint.setColor(this.scaleColor);
    }

    protected void initDatas(Context var1) {
        DJIRulerView var10000 = this;
        this.scroller = new Scroller(var1);
        this.drawPaint = new Paint();
        this.drawPaint.setAntiAlias(true);
        this.drawPaint.setStyle(Style.FILL);
        ViewConfiguration var2;
        this.minVelocity = (var2 = ViewConfiguration.get(var1)).getScaledMinimumFlingVelocity();
        var10000.maxVelocity = var2.getScaledMaximumFlingVelocity();
    }

    protected void onMeasure(int var1, int var2) {
        int var10002 = var1;
        var1 = MeasureSpec.getSize(var2);
        this.setMeasuredDimension(MeasureSpec.getSize(var10002), var1);
        float var4;
        if ((var4 = this.getResources().getDisplayMetrics().density * 2.0F) < 4.0F) {
            var4 = 4.0F;
        }

        int var10001 = var1;
        var1 = 12;

        float var3;
        for (this.density = (var3 = (float) var10001 * 1.0F) / (float) (this.interval * var1 + 1); this.density > var4; this.density = var3 / (float) ((var1 += 2) * this.interval + 1)) {
        }

    }

    protected void onSizeChanged(int var1, int var2, int var3, int var4) {
        super.onSizeChanged(var1, var2, var3, var4);
        this.width = var1;
        this.height = var2;
        Drawable var6;
        if ((var6 = this.selectDrawable) != null) {
            DJIRulerView var10000 = this;
            DJIRulerView var10001 = this;
            int var5 = var6.getIntrinsicHeight();
            var3 = var10001.selectDrawable.getIntrinsicWidth();
            var10000.selectDrawable.setBounds((var1 - var3) / 2, (var2 - var5) / 2, (var1 + var3) / 2, (var2 + var5) / 2);
        }

    }

    public void computeScroll() {
        if (this.scroller.computeScrollOffset()) {
            this.offsetY = this.scroller.getCurrY();
            this.onOffsetChanged(this.offsetY);
            OnRulerScrollListener var1;
            if (this.scroller.isFinished() && (var1 = this.onScrollListener) != null) {
                var1.onScrollingFinished(this);
            }

            this.postInvalidateOnAnimation();
        }

    }

    public boolean onTouchEvent(MotionEvent var1) {
        this.obtainTracker();
        this.velocityTracker.addMovement(var1);
        int var10001;
        int var4;
        OnRulerScrollListener var5;
        switch (var1.getAction()) {
            case 0:
                this.requestInterceptEvent();
                if (!this.scroller.isFinished()) {
                    this.scroller.abortAnimation();
                }

                this.lastTouchY = (int) var1.getY();
                if ((var5 = this.onScrollListener) != null) {
                    var5.onScrollingStarted(this);
                }
                break;
            case 1:
            case 3:
                this.velocityTracker.computeCurrentVelocity(1000, (float) this.maxVelocity);
                if (Math.abs(var4 = (int) this.velocityTracker.getYVelocity()) > this.minVelocity) {
                    int var2 = (int) ((float) (this.maxSize + 1) * this.density);
                    var10001 = var4;
                    var4 = this.offsetY;
                    int var3 = -var10001;
                    this.scroller.fling(0, var4, 0, var3, 0, 0, 0, var2);
                } else if ((var5 = this.onScrollListener) != null) {
                    var5.onScrollingFinished(this);
                }

                this.recycleTracker();
                break;
            case 2:
                var4 = (int) var1.getY();
                var10001 = this.lastTouchY - var4;
                this.lastTouchY = var4;
                this.scrollOverY(var10001);
        }

        return true;
    }

    protected void onDraw(Canvas var1) {
        Drawable var2;
        if ((var2 = this.selectDrawable) != null) {
            var2.draw(var1);
        }

        float var12;
        float var10000 = var12 = (float) this.offsetY;
        float var3 = (float) (this.maxSize + 1);
        float var4;
        float var10001 = var4 = this.density;
        var3 *= var4;
        var4 = (float) this.height / 2.0F;
        int var5;
        int var6;
        if ((var6 = (int) (var10000 / var10001 % (float) (var5 = this.interval))) != 0) {
            var6 = var5 - var6;
        }

        float var7 = (float) (var5 = this.scalePadding);
        float var13 = (float) (this.width - var5);
        float var8;
        var10000 = var8 = this.density;
        int var15 = var6;
        float var14 = var8 / 2.0F;
        var8 = (float) var15 * var8;

        for (float var9 = var10000 / 2.0F; var8 < this.height; var8 += (float) this.interval * this.density) {
            float var10;
            float var11;
            if (var4 <= (var10 = var8 + var12) + (var11 = this.density) && var10 + var9 <= var3 + var4) {
                this.tmpRect.set(var7, var8, var13, var8 + var11);
                this.drawPaint.setAlpha(this.recalAlpha(var8, var4));
                var1.drawRoundRect(this.tmpRect, var14, var14, this.drawPaint);
            }
        }

    }

    public interface OnRulerChangeListener {
        void onChanged(DJIRulerView var1, int var2, int var3, boolean var4);
    }

    public interface OnRulerScrollListener {
        void onScrollingStarted(DJIRulerView var1);

        void onScrollingFinished(DJIRulerView var1);
    }

    private final class ScrollRunnable implements Runnable {
        private int mStartSize;
        private int mEndSize;
        private int mStep;
        private boolean mbAdd;

        private ScrollRunnable(int var2, int var3) {
            this(var2, var3, 2);
        }

        private ScrollRunnable(int var2, int var3, int var4) {
            this.mStartSize = 0;
            this.mEndSize = 0;
            this.mStep = 2;
            this.mbAdd = false;
            this.mStartSize = var2;
            this.mEndSize = var3;
            this.mStep = var4;
            if (var2 < var3) {
                this.mbAdd = true;
            }

        }

        public void run() {
            DJIRulerView var10000;
            label46:
            {
                int var2;
                DJIRulerView var6;
                label38:
                {
                    int var1;
                    OnRulerChangeListener var4;
                    label37:
                    {
                        int var3;
                        DJIRulerView var5;
                        if (this.mbAdd) {
                            if ((var1 = this.mEndSize) > this.mStartSize + (var2 = this.mStep) + 1) {
                                var10000 = var6 = DJIRulerView.this;
                                var10000.curSize += var2;
                                if ((var2 = var10000.curSize) < var1) {
                                    break label38;
                                }

                                var6.curSize = var1;
                                if ((var4 = var6.onChangeListener) == null) {
                                    break label46;
                                }
                                break label37;
                            }

                            var10000 = var5 = DJIRulerView.this;
                            var3 = var5.curSize;
                            var10000.curSize = var1;
                            if ((var4 = var10000.onChangeListener) == null) {
                                break label46;
                            }
                        } else {
                            if ((var1 = this.mEndSize) + (var2 = this.mStep) + 1 < this.mStartSize) {
                                var10000 = var6 = DJIRulerView.this;
                                var10000.curSize -= var2;
                                if ((var2 = var10000.curSize) > var1) {
                                    break label38;
                                }

                                var6.curSize = var1;
                                if ((var4 = var6.onChangeListener) == null) {
                                    break label46;
                                }
                                break label37;
                            }

                            var10000 = var5 = DJIRulerView.this;
                            var3 = var5.curSize;
                            var10000.curSize = var1;
                            if ((var4 = var10000.onChangeListener) == null) {
                                break label46;
                            }
                        }

                        var4.onChanged(var5, var1, var3, true);
                        break label46;
                    }

                    var4.onChanged(var6, var1, var2, true);
                    break label46;
                }

                var6.offsetY = (int) ((float) var2 * var6.density);
                var6.invalidate();
                DJIRulerView.this.postDelayed(this, 10L);
                return;
            }

            var10000 = DJIRulerView.this;
            var10000.offsetY = (int) ((float) this.mEndSize * var10000.density);
            var10000.postInvalidate();
        }
    }
}