package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import dji.ux.beta.core.R;


/**
 * A seekBar that can be scrubbed left or right
 */
public class SeekBar extends BaseFrameLayout implements View.OnTouchListener, View.OnClickListener {

    //region Properties
    private static final String TAG = "SeekBar";
    private BaseWidgetAppearances widgetAppearances;
    private ImageView seekbarTrack;
    private ImageView currentSeekbarThumb;
    private TextView seekbarText;
    private List<OnSeekBarChangeListener> onSeekBarChangeListener;
    private int progressMax;
    private int currentProgress;
    // Null status will be used
    private int previousProgress;

    private float boundaryLeft;
    private float boundaryRight;
    private float xThumbStartCenter;
    private float xMoveStart;
    private ImageView seekbarThumbEnable;
    private ImageView seekbarThumbDisable;

    //Min and Max
    private boolean minValueVisibility = false;
    private boolean maxValueVisibility = false;
    private TextView seekbarMinText;
    private TextView seekbarMaxText;

    //Plus and minus
    private boolean minusVisibility = false;
    private boolean plusVisibility = false;
    private ImageView seekbarMinus;
    private ImageView seekbarPlus;

    // Baseline
    private boolean baselineVisibility = false;
    private int baselineProgress = -1;
    private ImageView seekbarRecommendedBar;

    //region Life Cycle
    public SeekBar(Context context) {
        this(context, null, 0);
    }

    //endregion

    public SeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        super.initView(context, attrs, defStyleAttr);
        seekbarTrack = (ImageView) findViewById(R.id.imageview_track);
        seekbarThumbEnable = (ImageView) findViewById(R.id.imageview_thumb);
        seekbarThumbDisable = (ImageView) findViewById(R.id.imageview_thumb_disable);
        seekbarText = (TextView) findViewById(R.id.textview_value);
        seekbarMinText = (TextView) findViewById(R.id.textview_min_value);
        seekbarMaxText = (TextView) findViewById(R.id.textview_max_value);
        seekbarRecommendedBar = (ImageView) findViewById(R.id.imageview_recommended_bar);
        seekbarMinus = (ImageView) findViewById(R.id.imageview_minus);
        seekbarPlus = (ImageView) findViewById(R.id.imageview_plus);
    }

    @Override
    protected BaseWidgetAppearances getWidgetAppearances() {
        if (widgetAppearances == null) {
            widgetAppearances = new SeekBarAppearances();
        }
        return widgetAppearances;
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        boundaryLeft = seekbarTrack.getLeft();
        boundaryRight = seekbarTrack.getRight();
        currentSeekbarThumb = seekbarThumbEnable;
        updateTextAndThumbInProgress(currentProgress);
    }

    // region Seekbar Public Methods
    public synchronized int getMax() {
        return progressMax;
    }

    //endregion

    public synchronized void setMax(int max) {
        progressMax = max;
    }

    public void enable(boolean status) {

        seekbarText.setEnabled(status);

        if (status) {
            currentSeekbarThumb = seekbarThumbEnable;

            seekbarThumbEnable.setVisibility(VISIBLE);
            seekbarThumbDisable.setVisibility(INVISIBLE);
            setOnTouchListener(this);
            seekbarMinus.setOnClickListener(this);
            seekbarPlus.setOnClickListener(this);
        } else {
            setOnTouchListener(null);
            seekbarMinus.setOnClickListener(null);
            seekbarPlus.setOnClickListener(null);

            currentSeekbarThumb = seekbarThumbDisable;
            seekbarThumbEnable.setVisibility(INVISIBLE);
            seekbarThumbDisable.setVisibility(VISIBLE);
        }
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        if (onSeekBarChangeListener == null) {
            onSeekBarChangeListener = new LinkedList<>();
        }
        onSeekBarChangeListener.add(l);
    }

    public void setText(String text) {
        seekbarText.setText(text);
    }

    public void setLeftBoundaryText(String text) {
        seekbarMinText.setText(text);
    }

    public void setRightBoundaryText(String text) {
        seekbarMaxText.setText(text);
    }

    public int getProgress() {
        return currentProgress;
    }

    public synchronized void setProgress(int progress) {
        if (progress >= progressMax) {
            progress = progressMax;
        } else if (progress < 0) {
            progress = 0;
        }
        currentProgress = progress;
        if (onSeekBarChangeListener != null) {
            for (int i = 0; i < onSeekBarChangeListener.size(); i++) {
                onSeekBarChangeListener.get(i).onProgressChanged(this, currentProgress);
            }
        }
    }

    public void restorePreviousProgress() {
        //if (previousProgress != null) {
        setProgress(previousProgress);
        updateTextAndThumbInProgress(previousProgress);
        //}
    }

    public void updateTextAndThumbInProgress(int progress) {
        float newX = boundaryLeft + getIncrement() * progress;
        updateTextAndThumbPosition(newX);
    }

    public void updateTextAndThumbPosition(float newX) {
        if (newX < boundaryLeft) {
            newX = boundaryLeft;
        } else if (newX > boundaryRight) {
            newX = boundaryRight;
        }

        setSeekbarTextPosition(newX);
        setSeekbarThumbPosition(newX);
    }

    private void setSeekbarTextPosition(float newX) {
        seekbarText.setX(newX - seekbarText.getWidth() / 2);
    }

    private void setSeekbarThumbPosition(float newX) {
        seekbarThumbDisable.setX(newX - seekbarThumbDisable.getWidth() / 2);
        seekbarThumbEnable.setX(newX - seekbarThumbEnable.getWidth() / 2);
    }

    //region Seekbar Internal Methods
    // Make the unit smaller to make the room for the last item.
    private float getIncrement() {
        return (boundaryRight - boundaryLeft) / progressMax;
    }


    //endregion

    //region OnTouchListener
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                onStartTracking(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                onEndTracking();
                break;

            case MotionEvent.ACTION_MOVE:
                // perform scrolling
                onTrackMoving(event);
                break;

        }

        return true;
    }
    //endregion

    private void onStartTracking(MotionEvent event) {
        xMoveStart = event.getX();
        xThumbStartCenter = currentSeekbarThumb.getX() + currentSeekbarThumb.getWidth() / 2;

        previousProgress = currentProgress;
        if (onSeekBarChangeListener != null) {
            for (int i = 0; i < onSeekBarChangeListener.size(); i++) {
                onSeekBarChangeListener.get(i).onStartTrackingTouch(this, currentProgress);
            }
        }
    }
    //endregion

    //region OnDragListener

    private void onTrackMoving(MotionEvent event) {
        float xDelta = event.getX() - xMoveStart;
        float newX = xThumbStartCenter + xDelta;
        // Drag and drop, the thumb and text view need to move smoothly
        //updateTextAndThumbPosition(newX);
        setProgress((int) ((newX - boundaryLeft) / getIncrement()));
    }

    private void onEndTracking() {
        if (onSeekBarChangeListener != null) {
            for (int i = 0; i < onSeekBarChangeListener.size(); i++) {
                onSeekBarChangeListener.get(i).onStopTrackingTouch(this, currentProgress);
            }
        }
    }

    //region OnClickListener
    @Override
    public void onClick(View v) {
        if (onSeekBarChangeListener != null) {
            if (v.equals(seekbarMinus)) {
                for (int i = 0; i < onSeekBarChangeListener.size(); i++) {
                    onSeekBarChangeListener.get(i).onMinusClicked(this);
                }
            } else if (v.equals(seekbarPlus)) {
                for (int i = 0; i < onSeekBarChangeListener.size(); i++) {
                    onSeekBarChangeListener.get(i).onPlusClicked(this);
                }
            }
        }
    }
    //endregion

    public boolean isMinValueVisible() {
        return minValueVisibility;
    }
    //endregion

    public void setMinValueVisibility(boolean minValueVisibility) {
        this.minValueVisibility = minValueVisibility;
        seekbarMinText.setVisibility(minValueVisibility ? VISIBLE : GONE);
    }

    public boolean isMaxValueVisible() {
        return maxValueVisibility;
    }

    public void setMaxValueVisibility(boolean maxValueVisibility) {
        this.maxValueVisibility = maxValueVisibility;
        seekbarMaxText.setVisibility(maxValueVisibility ? VISIBLE : GONE);
    }

    public boolean isMinusVisible() {
        return minusVisibility;
    }

    public void setMinusVisibility(boolean minusVisibility) {
        this.minusVisibility = minusVisibility;
        seekbarMinus.setVisibility(minusVisibility ? VISIBLE : GONE);
    }

    public boolean isPlusVisible() {
        return plusVisibility;
    }

    public void setPlusVisibility(boolean plusVisibility) {
        this.plusVisibility = plusVisibility;
        seekbarPlus.setVisibility(plusVisibility ? VISIBLE : GONE);
    }

    public boolean isBaselineVisibility() {
        return baselineVisibility;
    }

    public void setBaselineVisibility(boolean baselineVisibility) {
        this.baselineVisibility = baselineVisibility;
        seekbarRecommendedBar.setVisibility(baselineVisibility ? VISIBLE : GONE);
    }

    public int getBaselineProgress() {
        return baselineProgress;
    }

    public void setBaselineProgress(int baselineProgress) {
        this.baselineProgress = baselineProgress;
        float newX = boundaryLeft + getIncrement() * baselineProgress;
        seekbarRecommendedBar.setX(newX - seekbarRecommendedBar.getWidth() / 2);
    }

    /**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    public interface OnSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param progress The current progress level. This will be in the range 0..max where max
         *                 was set by {@link #setMax(int)}. (The default value for max is 100.)
         */
        void onProgressChanged(SeekBar object, int progress);

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seekbar.
         */
        void onStartTrackingTouch(SeekBar object, int progress);

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seekbar.
         */
        void onStopTrackingTouch(SeekBar object, int progress);

        /**
         * Notification that the user has clicked the plus symbol. Clients should use this
         * to increment the value and update the seekBar.
         */
        void onPlusClicked(SeekBar object);

        /**
         * Notification that the user has clicked the minus symbol. Clients should use this
         * to decrement the value and update the seekBar.
         */
        void onMinusClicked(SeekBar object);
    }
}
