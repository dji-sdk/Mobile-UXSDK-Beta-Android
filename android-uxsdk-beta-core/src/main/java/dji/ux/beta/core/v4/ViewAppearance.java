package dji.ux.beta.core.v4;

import androidx.annotation.AnyRes;

/**
 * Model class to hold specs of a View's appearance
 * All specs should be relative to the parent view
 */
public class ViewAppearance extends Appearance {

    public ViewAppearance(int positionX, int positionY, int width, int height, @AnyRes int viewID) {
        super(positionX, positionY, width, height, viewID);
    }

    private ViewAppearance(Builder builder) {
        super(builder);
    }

    static public abstract class Builder<T extends ViewAppearance> extends Appearance.Builder<T>
    {
        public Builder(final ViewAppearance viewAppearance) {
            super(viewAppearance);
        }

        public abstract T build();
    }

    public Builder<?> builder()
    {
        return new Builder<ViewAppearance>(this)
        {
            @Override
            public ViewAppearance build()
            {
                return new ViewAppearance(this);
            }
        };
    }
}
