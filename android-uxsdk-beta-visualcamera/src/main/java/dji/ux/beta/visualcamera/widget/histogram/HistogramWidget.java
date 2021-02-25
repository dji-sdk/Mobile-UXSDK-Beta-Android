/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.ux.beta.visualcamera.widget.histogram;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.thirdparty.io.reactivex.Flowable;
import dji.ux.beta.core.base.DJISDKModel;
import dji.ux.beta.core.base.SchedulerProvider;
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget;
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore;
import dji.ux.beta.core.util.ArrayUtil;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.visualcamera.R;

/**
 * Shows the histogram along with a button for closing the widget. The histogram is a visualization
 * of pixel exposure, with each bar representing the number of pixels exposed in a particular tone.
 * <p>
 * Interaction:
 * Tapping the close button will disable the histogram and hide the widget.
 */
public class HistogramWidget extends ConstraintLayoutWidget implements View.OnTouchListener {

    //region Constants
    private static final String TAG = "HistogramWidget";
    //endregion

    //region Views
    private LineChartView chartView;
    private ImageView closeButtonImageView;
    //endregion

    //region Fields
    private HistogramWidgetModel widgetModel;
    private boolean isDragSupported = true;
    private boolean mIsDragging = false;
    private int mDeltaX = 0;
    private int mDeltaY = 0;
    //endregion

    //region Constructor
    public HistogramWidget(@NonNull Context context) {
        super(context);
    }

    public HistogramWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HistogramWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_histogram, this);

        chartView = findViewById(R.id.fpv_camera_chart_line);
        closeButtonImageView = findViewById(R.id.button_close);
        closeButtonImageView.setOnClickListener(view -> setHistogramEnabled(false));
        setOnTouchListener(this);

        if (!isInEditMode()) {
            widgetModel = new HistogramWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getHistogramEnabled()
                .observeOn(SchedulerProvider.ui())
                .subscribe(enabled -> setVisibility(enabled ? VISIBLE : GONE)));

        addReaction(widgetModel.getLightValues()
                .observeOn(SchedulerProvider.ui())
                .subscribe(data -> chartView.setData(ArrayUtil.toPrimitive(data))));
    }
    //endregion

    //region Reaction helpers
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isDragSupported) {
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mIsDragging = true;
                    mDeltaX = (int) event.getX();
                    mDeltaY = (int) event.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mIsDragging) {
                        setX(event.getRawX() - mDeltaX);
                        setY(event.getRawY() - mDeltaY);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mIsDragging) {
                        mIsDragging = false;
                    }

                    break;
                default:
                    break;
            }
        }

        return true;
    }

    /**
     * Gets the histogram enabled state.
     *
     * @return A Flowable that will emit a boolean when the enabled state of the histogram changes.
     */
    public Flowable<Boolean> getHistogramEnabled() {
        return widgetModel.getHistogramEnabled();
    }

    /**
     * Enables or disables the histogram feature and then sets the visibility of the widget when
     * successful.
     *
     * @param enabled True to enable the histogram, false to disable.
     */
    public void setHistogramEnabled(boolean enabled) {
        addDisposable(widgetModel.setHistogramEnabled(enabled)
                .observeOn(SchedulerProvider.ui())
                .subscribe(() -> setVisibility(enabled ? VISIBLE : GONE),
                        logErrorConsumer(TAG, "setHistogramEnabled: ")));
    }
    //endregion

    //region Customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_histogram_ratio);
    }

    /**
     * Get the current index of the camera the widget model is reacting to.
     *
     * @return current camera index.
     */
    @NonNull
    public SettingDefinitions.CameraIndex getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    /**
     * Set the index of the camera for which the widget model should react.
     *
     * @param cameraIndex camera index.
     */
    public void setCameraIndex(@NonNull SettingDefinitions.CameraIndex cameraIndex) {
        if (!isInEditMode()) {
            widgetModel.setCameraIndex(cameraIndex);
        }
    }

    /**
     * Line color of the histogram.
     *
     * @return An int value of histogram line color.
     */
    @ColorInt
    public int getHistogramLineColor() {
        return chartView.getLineColor();
    }

    /**
     * Set line color of the histogram.
     *
     * @param histogramLineColor An int value of histogram line color.
     */
    public void setHistogramLineColor(@ColorInt int histogramLineColor) {
        chartView.setLineColor(histogramLineColor);
    }

    /**
     * Fill color of the histogram.
     *
     * @return An int value of histogram fill color.
     */
    @ColorInt
    public int getHistogramFillColor() {
        return chartView.getFillColor();
    }

    /**
     * Set fill color of the histogram.
     *
     * @param histogramFillColor An int value of the histogram fill color.
     */
    public void setHistogramFillColor(@ColorInt int histogramFillColor) {
        chartView.setFillColor(histogramFillColor);
    }

    /**
     * Grid color of the histogram.
     *
     * @return An int value of the histogram grid color.
     */
    @ColorInt
    public int getHistogramGridColor() {
        return chartView.getGridColor();
    }

    /**
     * Set grid color of the histogram.
     *
     * @param histogramGridColor An int value of the histogram grid color.
     */
    public void setHistogramGridColor(@ColorInt int histogramGridColor) {
        chartView.setGridColor(histogramGridColor);
    }

    /**
     * Background color of the histogram.
     *
     * @return An int value of the histogram background color.
     */
    @ColorInt
    public int getHistogramBackgroundColor() {
        return chartView.getBackgroundColor();
    }

    /**
     * Set background color of the histogram.
     *
     * @param histogramBackgroundColor An int value of the histogram background color.
     */
    public void setHistogramBackgroundColor(@ColorInt int histogramBackgroundColor) {
        chartView.setBackgroundColor(histogramBackgroundColor);
    }

    /**
     * Set whether the grid should be drawn behind the chart.
     *
     * @param shouldDrawGrid `true` if the grid should be drawn, `false` if the grid should be hidden.
     */
    public void setShouldDrawGrid(boolean shouldDrawGrid) {
        chartView.setDrawGrid(shouldDrawGrid);
    }

    /**
     * Get whether the grid should be drawn behind the chart.
     *
     * @return `true` if the grid should be drawn, `false` if the grid should be hidden.
     */
    public boolean shouldDrawGrid() {
        return chartView.getDrawGrid();
    }

    /**
     * Set whether the line should be drawn in a cubic bezier curve.
     *
     * @param shouldDrawCubic `true` if the line should be drawn in a cubic bezier curve, `false`
     *                        if the line should be drawn linearly.
     */
    public void setShouldDrawCubic(boolean shouldDrawCubic) {
        chartView.setDrawCubic(shouldDrawCubic);
    }

    /**
     * Get whether the line should be drawn in a cubic bezier curve.
     *
     * @return `true` if the line should be drawn in a cubic bezier curve, `false` if the line
     * should be drawn linearly.
     */
    public boolean shouldDrawCubic() {
        return chartView.getDrawCubic();
    }

    /**
     * Set whether histogram should show the close button.
     *
     * @param shouldShowCloseButton `true` if histogram should show the close button.
     */
    public void setShouldShowCloseButton(boolean shouldShowCloseButton) {
        closeButtonImageView.setVisibility(shouldShowCloseButton ? VISIBLE : GONE);
    }

    /**
     * Return `true` if histogram should show the close button.
     *
     * @return A boolean value to check if histogram needs to show the close button.
     */
    public boolean shouldShowCloseButton() {
        return closeButtonImageView.getVisibility() == VISIBLE;
    }

    /**
     * Return `true` if histogram dragging is enabled.
     *
     * @return A boolean value to check if histogram can be dragged around the screen.
     */
    public boolean isDraggable() {
        return isDragSupported;
    }

    /**
     * Set whether histogram dragging is enabled. It is enabled by default.
     *
     * @param draggable `true` if histogram can be dragged around the screen.
     */
    public void setDraggable(boolean draggable) {
        isDragSupported = draggable;
    }

    /**
     * Get the drawable resource for the close icon.
     *
     * @return Drawable for the close icon.
     */
    @Nullable
    public Drawable getCloseIcon() {
        return closeButtonImageView.getDrawable();
    }

    /**
     * Set the resource ID for the close icon.
     *
     * @param resourceId Integer ID of the drawable resource.
     */
    public void setCloseIcon(@DrawableRes int resourceId) {
        setCloseIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the close icon.
     *
     * @param icon Drawable resource for the image.
     */
    public void setCloseIcon(@Nullable Drawable icon) {
        closeButtonImageView.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the close icon's background.
     *
     * @return Drawable for the close icon's background.
     */
    @Nullable
    public Drawable getCloseIconBackground() {
        return closeButtonImageView.getBackground();
    }

    /**
     * Set the resource ID for the close icon's background.
     *
     * @param resourceId Integer ID of the background resource.
     */
    public void setCloseIconBackground(@DrawableRes int resourceId) {
        closeButtonImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the close icon's background.
     *
     * @param icon Drawable resource for the background.
     */
    public void setCloseIconBackground(@Nullable Drawable icon) {
        closeButtonImageView.setBackground(icon);
    }

    //endregion

    //Region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HistogramWidget);

        SettingDefinitions.CameraIndex index = SettingDefinitions.CameraIndex.find(typedArray.getInt(R.styleable.HistogramWidget_uxsdk_cameraIndex, 0));
        setCameraIndex(index);

        @ColorInt int histogramLineColor = typedArray.getColor(R.styleable.HistogramWidget_uxsdk_histogramLineColor, getResources().getColor(R.color.uxsdk_background));
        setHistogramLineColor(histogramLineColor);

        @ColorInt int histogramFillColor = typedArray.getColor(R.styleable.HistogramWidget_uxsdk_histogramFillColor, getResources().getColor(R.color.uxsdk_white_75_percent));
        setHistogramFillColor(histogramFillColor);

        @ColorInt int histogramGridColor = typedArray.getColor(R.styleable.HistogramWidget_uxsdk_histogramGridColor, getResources().getColor(R.color.uxsdk_white_40_percent));
        setHistogramGridColor(histogramGridColor);

        @ColorInt int histogramBackgroundColor = typedArray.getColor(R.styleable.HistogramWidget_uxsdk_histogramBackgroundColor, getResources().getColor(R.color.uxsdk_black_47_percent));
        setHistogramBackgroundColor(histogramBackgroundColor);

        boolean shouldDrawGrid = typedArray.getBoolean(R.styleable.HistogramWidget_uxsdk_shouldDrawGrid, true);
        setShouldDrawGrid(shouldDrawGrid);

        boolean shouldDrawCubic = typedArray.getBoolean(R.styleable.HistogramWidget_uxsdk_shouldDrawCubic, true);
        setShouldDrawCubic(shouldDrawCubic);

        boolean shouldShowCloseButton = typedArray.getBoolean(R.styleable.HistogramWidget_uxsdk_shouldShowCloseButton, true);
        setShouldShowCloseButton(shouldShowCloseButton);

        boolean draggable = typedArray.getBoolean(R.styleable.HistogramWidget_uxsdk_draggable, true);
        setDraggable(draggable);

        Drawable closeIcon = typedArray.getDrawable(R.styleable.HistogramWidget_uxsdk_closeIcon);
        if (closeIcon != null) {
            setCloseIcon(closeIcon);
        }

        Drawable closeIconBackground = typedArray.getDrawable(R.styleable.HistogramWidget_uxsdk_closeIconBackground);
        if (closeIconBackground != null) {
            setCloseIconBackground(closeIconBackground);
        }

        typedArray.recycle();
    }
    //endregion
}
