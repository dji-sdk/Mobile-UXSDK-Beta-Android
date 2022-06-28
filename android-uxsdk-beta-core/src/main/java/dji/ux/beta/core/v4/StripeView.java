package dji.ux.beta.core.v4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import dji.ux.beta.core.R;
import dji.ux.beta.core.util.DisplayUtil;

public class StripeView extends View {

    final float BAR_HEIGHT = 9.0f;
    final float BAR_WIDTH = 4.0f;
    final float BAR_STROKE_HEIGHT = 6.0f;
    final float BAR_STROKE_WIDTH = 2.0f;
    final float BAR_STROKE_CYCLE_RADIUS = 1.0f;

    final int DEFAULT_MAX_UNIT = 3;
    final int DEFAULT_MAX_NUM_BARS_PER_UNIT = 3;

    private Bitmap shortLineBmp = null;
    private Bitmap shortHighLightLineBmp = null;
    private Bitmap longLineBmp = null;
    private Bitmap longHighLightLineBmp = null;

    private final Paint paint = new Paint();

    private int selectedPos;
    private int maxUnit = DEFAULT_MAX_UNIT;
    private int maxBarPerUnit = DEFAULT_MAX_NUM_BARS_PER_UNIT;
    private int centerPos;
    private float widthScale = 1;
    private float heightScale = 1;

    public StripeView(Context context) {
        super(context);
    }

    public StripeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StripeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public int getSelectedPosition() {
        return selectedPos;
    }

    public void setSelectedPosition(final int pos) {
        if (selectedPos != pos) {
            selectedPos = pos;
            postInvalidate();
        }
    }

    /**
     * For the EV using, the MaxUnit mostly is 3 which is [-3, +3], but user can set i in case
     * the new camera EV supports [-5, +5].
     */
    public void setMaxUnit(int max) {
        maxUnit = max;
    }

    public int getMaxUnit() {
        return maxUnit;
    }

    /**
     * For the EV, The max strips number is 3 since it likes [0, 0.3, 0.7, 1, 1.3, 1.7, 2], from 0 to 1,
     * there are 3 bars.  But user can set it for other situation.
     */
    public void setMaxNumberOfBarsPerUnit(int max) {
        maxBarPerUnit = max;
    }

    public int getMaxNumberOfBarsPerUnit() {
        return maxBarPerUnit;
    }

    public int getDesignedWidth() {
        int totalBarsNum = getMaxUnit() * getMaxNumberOfBarsPerUnit() * 2 + 1;
        float designedWidth = totalBarsNum * BAR_WIDTH;
        return (int) DisplayUtil.dipToPx(getContext(), designedWidth);
    }

    public int getDesignedHeight() {
        return (int) DisplayUtil.dipToPx(getContext(), BAR_HEIGHT);
    }

    /**
     * The position value will be the index of the arrary values which matched with each bar of the stripe view.
     * The zeroPostion is the center bar which is match to the index of middle value of the array.
     * Such as [-1.7, -1.3, 0, 1.3, 1.7], the zero postion will be 2 which is the index of 0 value.
     */
    public void setZeroPosition(int center) {
        centerPos = center;
    }

    private Bitmap createLine(boolean isLongLine, boolean isHighlight) {
        Bitmap b = Bitmap.createBitmap((int) DisplayUtil.dipToPx(getContext(), BAR_WIDTH),
                (int) DisplayUtil.dipToPx(getContext(), BAR_HEIGHT),
                Bitmap.Config.ARGB_8888);
        b.eraseColor(Color.TRANSPARENT);

        Canvas c = new Canvas(b);
        Paint p = new Paint();
        p.setAntiAlias(true);
        if (isHighlight) {
            p.setColor(getResources().getColor(R.color.uxsdk_white));
        } else {
            p.setColor(getResources().getColor(R.color.uxsdk_white_60_percent));
        }

        p.setStyle(Paint.Style.FILL);

        if (isLongLine) {
            c.drawCircle(DisplayUtil.dipToPx(getContext(), BAR_WIDTH / 2), // cycle center coordinate X
                    DisplayUtil.dipToPx(getContext(), BAR_STROKE_CYCLE_RADIUS), // cycle center coordinate Y
                    DisplayUtil.dipToPx(getContext(), BAR_STROKE_CYCLE_RADIUS), // radius
                    p);
            c.drawRect(DisplayUtil.dipToPx(getContext(), (BAR_WIDTH - BAR_STROKE_WIDTH) / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_HEIGHT - BAR_STROKE_HEIGHT),
                    DisplayUtil.dipToPx(getContext(), BAR_WIDTH / 2 + BAR_STROKE_WIDTH / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_HEIGHT),
                    p);
        } else {
            c.drawRect(DisplayUtil.dipToPx(getContext(), BAR_WIDTH / 2 - BAR_STROKE_WIDTH / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_HEIGHT / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_WIDTH / 2 + BAR_STROKE_WIDTH / 2),
                    DisplayUtil.dipToPx(getContext(), BAR_HEIGHT),
                    p);
        }

        return b;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            return;
        }

        shortLineBmp = createLine(false, false);
        shortHighLightLineBmp = createLine(false, true);
        longLineBmp = createLine(true, false);
        longHighLightLineBmp = createLine(true, true);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        setWillNotDraw(false);
    }

    private Bitmap getBitmap(final int index, final int barNumOnSide, final Bitmap normal, final boolean isLeft) {
        Bitmap bmp = normal;

        int convertedIndex;
        if (isLeft) {
            convertedIndex = centerPos - index;
        } else {
            convertedIndex = centerPos + index;
        }

        if (selectedPos < centerPos && isLeft) {
            if (convertedIndex >= selectedPos) {
                if (normal == shortLineBmp) {
                    bmp = shortHighLightLineBmp;
                } else if (normal == longLineBmp) {
                    bmp = longHighLightLineBmp;
                }
            }
        } else if (selectedPos > centerPos && !isLeft) {
            if (convertedIndex <= selectedPos) {
                if (normal == shortLineBmp) {
                    bmp = shortHighLightLineBmp;
                } else if (normal == longLineBmp) {
                    bmp = longHighLightLineBmp;
                }
            }
        }
        return bmp;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        float left = 0;
        float width = longHighLightLineBmp.getWidth();
        float height = longHighLightLineBmp.getHeight();

        int barNumPerUnit = getMaxNumberOfBarsPerUnit();

        float newWidth = width * widthScale;
        float newHeight = height * heightScale;

        int barNumOnSide = getMaxNumberOfBarsPerUnit() * getMaxUnit();
        Rect src = new Rect(0, 0, (int) width, (int) height);
        Rect dest = new Rect((int) left, 0, (int) (left + newWidth), (int) newHeight);
        // Draw left side of the middle
        for (int i = 0; i < getMaxUnit(); i++) {
            for (int j = 0; j < barNumPerUnit; j++) {
                Bitmap barImg = getBitmap((barNumOnSide - i * barNumPerUnit - j),
                        barNumOnSide,
                        (j == 0 ? longLineBmp : shortLineBmp),
                        true);
                dest = new Rect((int) left, 0, (int) (left + newWidth), (int) newHeight);
                canvas.drawBitmap(barImg, src, dest, paint);
                left += newWidth;
            }
        }

        // Draw middle
        dest = new Rect((int) left, 0, (int) (left + newWidth), (int) newHeight);
        //if (selectedPos != null) {
        canvas.drawBitmap(longHighLightLineBmp, src, dest, paint);
        //} else {
        //    canvas.drawBitmap(longLineBmp, src, dest, paint);
        //}
        left += width;

        // Draw right side of the middle
        for (int i = 0; i < getMaxUnit(); i++) {
            for (int j = 0; j < barNumPerUnit; j++) {
                Bitmap barImg = getBitmap((i * barNumPerUnit + j + 1),
                        barNumOnSide,
                        (j + 1 != barNumPerUnit ? shortLineBmp : longLineBmp),
                        false);
                dest = new Rect((int) left, 0, (int) (left + newWidth), (int) newHeight);
                canvas.drawBitmap(barImg, src, dest, paint);
                left += newWidth;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);

        widthScale = (float) width / getDesignedWidth();

        int height = MeasureSpec.getSize(heightMeasureSpec);

        heightScale = (float) height / getDesignedHeight();

        setMeasuredDimension(width, height);
    }
}
