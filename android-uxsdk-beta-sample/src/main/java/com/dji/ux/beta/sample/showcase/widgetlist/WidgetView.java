/*
 * Copyright (c) 2018-2021 DJI
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

package com.dji.ux.beta.sample.showcase.widgetlist;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.util.StringUtils;
import com.dji.ux.beta.sample.view.SettingsDrawerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget;

/**
 * A view with a single widget and an indicator of the current size of the widget. This widget
 * can be resized using pinch-to-zoom and the size indicator updates to match the current
 * dimensions of the widget.
 */
public class WidgetView extends ConstraintLayout {

    private static final int LOG_MAX_LINES = 500;

    //region Views
    @BindView(R.id.widget_container)
    protected LinearLayout containerView;
    @BindView(R.id.textview_aspect_ratio)
    protected TextView aspectRatioTextView;
    @BindView(R.id.textview_current_size)
    protected TextView currentSizeTextView;
    @BindView(R.id.settings_scroll_view)
    protected SettingsDrawerView scrollView;
    @BindView(R.id.btn_hooks)
    protected ImageView hooksButton;
    @BindView(R.id.textview_logs)
    protected TextView logsTextView;
    private ViewGroup widget;
    //endregion

    //region Fields
    private WidgetViewHolder widgetViewHolder;
    private ScaleGestureDetector scaleGestureDetector;
    private int originalHeight;
    private int originalWidth;
    private float scaleFactor = 1.0f;
    private CompositeDisposable compositeDisposable;
    private int logLineCount = 0;
    //endregion

    //region Lifecycle
    public WidgetView(Context context) {
        super(context);
    }

    public WidgetView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Initializes the view with the given {@link WidgetViewHolder} object and returns the widget
     * that is part of this WidgetView.
     *
     * @param widgetViewHolder An instance of {@link WidgetViewHolder}.
     * @return The initialized widget.
     */
    public ViewGroup init(WidgetViewHolder widgetViewHolder) {
        this.widgetViewHolder = widgetViewHolder;
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        inflate(getContext(), R.layout.view_widget, this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        setLayoutParams(params);
        ButterKnife.bind(this);
        widget = widgetViewHolder.getWidget(getContext());
        if (widget != null) {
            containerView.addView(widget);
            final ViewTreeObserver obs = widget.getViewTreeObserver();
            obs.addOnPreDrawListener(() -> {
                if (originalHeight == 0 && originalWidth == 0) {
                    originalHeight = widget.getHeight();
                    originalWidth = widget.getWidth();
                }
                return true;
            });
        }
        aspectRatioTextView.setText(widgetViewHolder.getIdealDimensionRatioString());

        return widget;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        compositeDisposable = new CompositeDisposable();
        if (widgetViewHolder.getHooks() != null) {
            compositeDisposable.add(widgetViewHolder.getHooks()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::addHookLog));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        currentSizeTextView.setText(widgetViewHolder.getWidgetSize());
    }

    @OnTouch(R.id.widget_container)
    public boolean onWidgetTouched(ViewGroup widget, MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    @OnClick(R.id.btn_hooks)
    public void onHooksClick() {
        scrollView.movePanel(hooksButton);
    }
    //endregion

    /**
     * A gesture listener that scales the widget.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            scaleFactor *= scaleGestureDetector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
            if (widget != null) {
                ViewGroup.LayoutParams layoutParams = widget.getLayoutParams();
                layoutParams.height = (int) (originalHeight * scaleFactor);
                layoutParams.width = (int) (originalWidth * scaleFactor);
                widget.setLayoutParams(layoutParams);
                if (widget instanceof FPVInteractionWidget) {
                    ((FPVInteractionWidget) widget).adjustAspectRatio(layoutParams.width, layoutParams.height);
                }
                currentSizeTextView.setText(widgetViewHolder.getWidgetSize());
            }
            return true;
        }
    }

    /**
     * Adds a hook and its properties to the hook log
     *
     * @param hook The hook to add to the log
     */
    private void addHookLog(Object hook) {
        String hookString = hook.toString();

        String line = trimQualifiers(new Rewriter().rewrite(hookString));

        logsTextView.append(line);
        int numLines = StringUtils.countMatches(line, '\n');

        Editable text = logsTextView.getEditableText();
        for (int i = 0; i < numLines; i++) {
            if (logLineCount == LOG_MAX_LINES) {
                text.delete(0, text.toString().indexOf('\n') + 1);
            } else {
                logLineCount++;
            }
        }
    }

    /**
     * Trims extra text from a kotlin object's toString result
     *
     * @param input The string to trim
     * @return The trimmed string
     */
    private String trimQualifiers(String input) {
        String trimmedString = input;
        while (trimmedString.contains("dji.ux.beta")) {
            int index = trimmedString.indexOf("dji.ux.beta");
            int dollarIndex = trimmedString.indexOf("$", trimmedString.indexOf("$") + 1) + 1;
            int atIndex = trimmedString.indexOf("@");
            int endIndex = trimmedString.indexOf("\n", atIndex + 1);
            trimmedString = trimmedString.substring(0, index)
                    + trimmedString.substring(dollarIndex, atIndex)
                    + trimmedString.substring(endIndex);
        }
        return trimmedString;
    }

    /**
     * Rewrites a kotlin data class's toString result into multiple lines
     */
    public static class Rewriter {
        private int level;
        private StringBuilder output;

        public String rewrite(String input) {
            reset(input.length());
            for (char character : input.toCharArray()) {
                switch (character) {
                    case '(':
                        level++;
                        appendNewLine();
                        break;
                    case ')':
                        level--;
                        break;
                    case ',':
                        appendNewLine();
                        break;
                    default:
                        output.append(character);
                        break;
                }
            }
            output.append("\n");
            return output.toString();
        }

        public void reset(int suggestedLength) {
            level = 0;
            output = new StringBuilder(suggestedLength);
        }

        private void appendNewLine() {
            output.append("\n");
            if (level > 0) {
                for (int i = 0; i < level; i++) {
                    output.append("\u00A0");
                }
            }
        }
    }

}
