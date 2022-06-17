package dji.ux.beta.core.v4;

/**
 * Model class to hold specs of an appearance
 * All specs should be relative to the parent View
 */
public class Appearance {
    private int positionX;
    private int positionY;
    private int width;
    private int height;
    private int viewID;

    public Appearance(int viewID) {
        this.viewID = viewID;
    }

    public Appearance(int positionX, int positionY, int width, int height, int viewID) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.viewID = viewID;
    }

    protected Appearance(Builder builder){
        this(builder.positionX, builder.positionY, builder.width, builder.height, builder.viewID);
    }

    //region Getter
    public int getViewID() {
        return viewID;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    static public abstract class Builder<T extends Appearance>
    {
        private int positionX;
        private int positionY;
        private int width;
        private int height;
        private int viewID;

        public Builder(final Appearance appearance) {
            this.positionX  = appearance.getPositionX();
            this.positionY  = appearance.getPositionY();
            this.width = appearance.getWidth();
            this.height = appearance.getHeight();
            this.viewID = appearance.getViewID();
        }

        public Builder<T> positionX(int positionX) {
            this.positionX = positionX;
            return this;
        }

        public Builder<T> positionY(int positionY) {
            this.positionY = positionY;
            return this;
        }

        public Builder<T> width(int width) {
            this.width = width;
            return this;
        }

        public Builder<T> height(int height) {
            this.height = height;
            return this;
        }

        public Builder<T> viewID(int viewID) {
            this.viewID = viewID;
            return this;
        }

        public abstract T build();
    }

    public Builder<?> builder()
    {
        return new Builder<Appearance>(this)
        {
            @Override
            public Appearance build()
            {
                return new Appearance(this);
            }
        };
    }

    //endregion
}
