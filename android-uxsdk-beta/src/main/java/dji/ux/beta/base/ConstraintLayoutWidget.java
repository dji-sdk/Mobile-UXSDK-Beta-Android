/*
 * Copyright (c) 2018-2019 DJI
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
 */

package dji.ux.beta.base;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;

import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.thirdparty.io.reactivex.functions.Consumer;
import dji.ux.beta.util.RxUtil;

/**
 * This is a base class for widgets requiring ConstraintLayout.
 */
public abstract class ConstraintLayoutWidget extends ConstraintLayout {
    //region Constants
    protected static final String TAG = "ConstraintLayoutWidget";
    protected static final int INVALID_RESOURCE = -1;
    protected static final int INVALID_COLOR = 0;
    protected static final float INVALID_DIMENSION = 0f;
    //endregion
    //region Fields
    private CompositeDisposable reactionDisposables;
    private CompositeDisposable compositeDisposable;
    //endregion

    //region Constructors
    public ConstraintLayoutWidget(@NonNull Context context) {
        super(context);
        initView(context, null, 0);
    }

    public ConstraintLayoutWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs, 0);
    }

    public ConstraintLayoutWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        reactionDisposables = new CompositeDisposable();
        compositeDisposable = new CompositeDisposable();
        reactToModelChanges();
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterReactions();
        disposeAll();
        super.onDetachedFromWindow();
    }

    /**
     * Invoked after the view is created, during the initialization of the class.
     * FindViewById load, AttributeSet in here.
     *
     * @param context      Context
     * @param attrs        Attribute set
     * @param defStyleAttr Style attribute
     */
    protected abstract void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr);

    /**
     * Call addReaction here to bind to the model.
     */
    protected abstract void reactToModelChanges();

    /**
     * Add a disposable which is automatically disposed with the view's lifecycle.
     *
     * @param disposable the disposable to add
     */
    protected void addDisposable(@NonNull Disposable disposable) {
        if (compositeDisposable != null) {
            compositeDisposable.add(disposable);
        }
    }
    //endregion

    //region Customization

    /**
     * Ideal dimension ratio in the format width:height.
     *
     * @return dimension ratio string.
     */
    @NonNull
    public abstract String getIdealDimensionRatioString();
    //endregion

    //region Reactions

    /**
     * Add a reaction which is automatically disposed with the view's lifecycle.
     *
     * @param reaction the reaction to add.
     */
    protected void addReaction(@NonNull Disposable reaction) {
        if (reactionDisposables == null) {
            throw new IllegalStateException("Called this method only from reactToModelChanges.");
        }

        reactionDisposables.add(reaction);
    }

    private void unregisterReactions() {
        if (reactionDisposables != null) {
            reactionDisposables.dispose();
            reactionDisposables = null;
        }
    }

    private void disposeAll() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
    }
    //endregion

    //region Helpers

    /**
     * Get a throwable error consumer for the given error.
     *
     * @param tag     Tag for the log
     * @param message Message to be logged
     * @return Throwable consumer
     */
    protected Consumer<Throwable> logErrorConsumer(@NonNull String tag, @NonNull String message) {
        return RxUtil.logErrorConsumer(tag, message);
    }
    //endregion
}