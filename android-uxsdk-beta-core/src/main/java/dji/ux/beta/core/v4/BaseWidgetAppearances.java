package dji.ux.beta.core.v4;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

/**
 * Base class that applies widget appearance calculations
 */
public abstract class BaseWidgetAppearances {
    private float scaleFactor;

    protected abstract @NonNull
    ViewAppearance getMainAppearance();

    public abstract @NonNull Appearance[] getElementAppearances();

    public View inflate(LayoutInflater inflater, ViewGroup viewGroup) {
        return inflater.inflate(getMainAppearance().getViewID(), viewGroup);
    }

    public void calculateAppearance(int containerWidth, int containerHeight) {
        ViewAppearance mainAppearance = getMainAppearance();
        final int originalWidth = mainAppearance.getWidth();
        final int originalHeight = mainAppearance.getHeight();
        if ((1.0f * containerHeight / originalHeight) < (1.0f * containerWidth / originalWidth)) {
            scaleFactor = (1.0f * containerHeight / originalHeight);
        } else {
            scaleFactor = (1.0f * containerWidth / originalWidth);
        }
    }

    public void adjustPosition(View childView, Appearance childAppearance) {
        ViewAppearance mainAppearance = getMainAppearance();

        final int left = (int) ((childAppearance.getPositionX() - mainAppearance.getPositionX()) * scaleFactor);
        final int top =  (int) ((childAppearance.getPositionY() - mainAppearance.getPositionY()) * scaleFactor);
        final int right = left + getAbsoluteWidth(childAppearance);
        final int bottom = top + getAbsoluteHeight(childAppearance);

        childView.layout(left, top, right, bottom);
    }

    /**
     * Calculates and returns absolute Width based on dimensions of Parent
     */
    public int getAbsoluteWidth(Appearance childAppearance) {
        return (int) (childAppearance.getWidth() * scaleFactor);
    }

    /**
     * Calculates and returns absolute Height based on dimensions of Parent
     */
    public int getAbsoluteHeight(Appearance childAppearance) {
        return (int) (childAppearance.getHeight() * scaleFactor);
    }

    public float aspectRatio() {
        ViewAppearance mainAppearance = getMainAppearance();
        return (float) mainAppearance.getWidth() / (float) mainAppearance.getHeight();
    }

    public int getWidthFromHeight(int height) {
        ViewAppearance mainAppearance = getMainAppearance();
        return height * mainAppearance.getWidth() / mainAppearance.getHeight();
    }

    public int getHeightFromWidth(int width) {
        ViewAppearance mainAppearance = getMainAppearance();
        return width * mainAppearance.getHeight() / mainAppearance.getWidth();
    }
}
