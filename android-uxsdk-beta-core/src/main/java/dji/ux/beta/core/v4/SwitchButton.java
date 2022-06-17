package dji.ux.beta.core.v4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import dji.ux.beta.core.R;

/**
 * Switch widget
 */
public class SwitchButton extends BaseFrameLayout implements  View.OnTouchListener {

    //region Properties
    private BaseWidgetAppearances widgetAppearances;
    private static final int PROGRESS_MAX = 10;
    private static final int MOVE_SENSITIVE_THRESHOLD = 10;
    private ImageView thumbView;
    private boolean isChecked;
    private OnCheckedChangeListener onCheckedChangeListener;
    private ImageView trackView;
    private float moveStartX;
    private int boundaryLeft;
    private int boundaryRight;
    private int currentProgress;
    private int stepSize;
    private float thumbStartX;
    private float xDelta;
    private Paint trackPaint;
    private Paint backgroundPaint;
    private Paint thumbPaint;

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedListener(OnCheckedChangeListener listener) {
        onCheckedChangeListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changed.
     */
    public interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param isChecked The new checked state of buttonView.
         */
        void onCheckedChanged(boolean isChecked);
    }
    //endregion

    //region Default Constructors
    public SwitchButton(Context context) {
        super(context, null, 0);
    }

    public SwitchButton(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public SwitchButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region UI Life Cycle
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        this.setWillNotDraw(false);
        trackView = (ImageView) findViewById(R.id.layout_track);
        thumbView = (ImageView) findViewById(R.id.imageview_left);

        initTrackPaints(context, trackView);
        setOnTouchListener(this);
        trackView.setSelected(false);
        setChecked(false);
        currentProgress = 0;
        setClickable(true);
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        boundaryLeft = trackView.getLeft();
        boundaryRight = trackView.getRight() - thumbView.getWidth();
        if (isChecked) {
            thumbView.setX(boundaryRight);
        }
        stepSize = (int) Math.floor((boundaryRight - boundaryLeft) / PROGRESS_MAX);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new SwitchButtonAppearances();
        }
        return widgetAppearances;
    }

    private void initTrackPaints(Context context, ImageView track) {
        int progressColor = context.getResources().getColor(R.color.uxsdk_green);
        int backgroundColor = context.getResources().getColor(R.color.uxsdk_gray);
        int thumbColor = context.getResources().getColor(R.color.uxsdk_white);

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(progressColor);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(thumbColor);
        thumbPaint.setStyle(Paint.Style.STROKE);
        thumbPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        trackView.setVisibility(INVISIBLE);
        thumbView.setVisibility(INVISIBLE);

        float diam = trackView.getHeight();
        trackPaint.setStrokeWidth(diam);
        backgroundPaint.setStrokeWidth(diam);
        thumbPaint.setStrokeWidth(diam);

        float centerY = trackView.getY() + diam/2;

        canvas.drawLine(diam/2,
                centerY,
                trackView.getWidth() - diam/2,
                centerY,
                backgroundPaint);

        canvas.drawLine(diam/2,
                centerY,
                thumbView.getX() + diam/2,
                centerY,
                trackPaint);

        canvas.drawPoint(
                thumbView.getX() + diam/2,
                centerY,
                thumbPaint);
    }


    public void onClick(View v) {
        if (isClickable()) {
            setChecked(!isChecked);

            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(isChecked);
            }
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    /**
     * <p>Changes the checked state of this button.</p>
     *
     * @param checked true to check the button, false to uncheck it
     */
    public synchronized void setChecked(final boolean checked) {
        if (!isEnabled()) {
            return;
        }
        // This method is the final UI statement change,should only handle the UI change.
        // no any other logic, such as listener callback.
        isChecked = checked;
        setProgress(checked ? PROGRESS_MAX : 0);
        thumbView.setX(checked ? boundaryRight : boundaryLeft);
        postInvalidate();
    }

    //endregion

    //region OnTouchListener
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!isEnabled() || !isClickable()) {
            return false;
        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                onStartTracking(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // supports both user's click action and slide action.
                if (Math.abs(xDelta) < MOVE_SENSITIVE_THRESHOLD) {
                    onClick(v);
                } else {
                    onEndTracking();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // perform scrolling
                onTrackMoving(event);
                break;
        }

        return true;
    }
    //endregion

    //region OnDragListener

    private void onStartTracking(MotionEvent event) {
        trackView.setSelected(false);
        moveStartX = event.getX();
        thumbStartX = thumbView.getX();
    }

    private void onTrackMoving(MotionEvent event) {
        xDelta = event.getX() - moveStartX;
        float newX = thumbStartX + xDelta;
        // Drag and drop, the thumb and text view need to move smoothly
        updateThumbPosition(newX);

        if (stepSize != 0) {
            setProgress((int) (newX - boundaryLeft) / stepSize);
        }

        invalidate();
    }

    private void onEndTracking() {
        setChecked(currentProgress >= PROGRESS_MAX / 2);
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onCheckedChanged(isChecked);
        }
    }

    private void updateThumbPosition(float newX) {
        if (newX < boundaryLeft) {
            newX = boundaryLeft;
        } else if (newX > boundaryRight) {
            newX = boundaryRight;
        }

        thumbView.setX(newX);
    }

    void setProgress(int progress) {

        if (progress >= PROGRESS_MAX) {
            progress = PROGRESS_MAX;
        } else if (progress < 0) {
            progress = 0;
        }

        if (progress != currentProgress) {
            currentProgress = progress;
        }

    }

    //endregion
}
