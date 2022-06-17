package dji.ux.beta.core.v4;

/**
 * Model class to hold specs of an ImageView's appearance
 * All specs should be relative to the parent View
 */
public class ImageAppearance extends Appearance {

    public ImageAppearance(int positionX, int positionY, int width, int height, int viewID) {
        super(positionX, positionY, width, height, viewID);
    }

    private ImageAppearance(Builder builder) {
        super(builder);
    }

    static public abstract class Builder<T extends ImageAppearance> extends Appearance.Builder<T>
    {
        public Builder(final ImageAppearance imageAppearance) {
            super(imageAppearance);
        }

        public abstract T build();
    }

    public Builder<?> builder()
    {
        return new Builder<ImageAppearance>(this)
        {
            @Override
            public ImageAppearance build()
            {
                return new ImageAppearance(this);
            }
        };
    }
}
