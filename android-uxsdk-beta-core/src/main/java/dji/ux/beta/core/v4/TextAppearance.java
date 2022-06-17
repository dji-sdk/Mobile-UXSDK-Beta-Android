package dji.ux.beta.core.v4;

import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Model class to hold specs of a TextView's appearance
 * All specs should be relative to the parent view
 */
public class TextAppearance extends Appearance {

    // A String that would occupy the whole desired width of the TextView
    @NonNull private String measuringText;
    private int maxLines = 1;
    private static final float FONT_CHANGE_THRESHOLD = 0.75f;

    public TextAppearance(int positionX,
                          int positionY,
                          int width,
                          int height,
                          int viewID,
                          @NonNull String measuringText,
                          String font) {
        super(positionX, positionY, width, height, viewID);
        this.measuringText = measuringText;
    }

    public TextAppearance(int positionX,
                          int positionY,
                          int width,
                          int height,
                          int viewID,
                          @NonNull String measuringText,
                          String font,
                          int maxLines) {
        this(positionX, positionY, width, height, viewID, measuringText, font);
        this.maxLines = maxLines;
    }

    private TextAppearance(Builder builder){
        super(builder);
        this.measuringText = builder.measuringText;
        //this.font = builder.font;
    }

    /**
     * Adjusts TextView size to ideal size
     */
    public void adjustTextViewSize(TextView textView) {
        float ratio = 1, ratioWidth = 1;

        // Calculate ratio to fit width
        {
            float idealWidth = textView.getPaint().measureText(measuringText) / maxLines;
            float originalWidth = textView.getMeasuredWidth() - (textView.getPaddingLeft() + textView.getPaddingRight());
            ratioWidth = originalWidth / idealWidth;
        }
        // Calculate ratio to fit height
        {
            float viewHeight = textView.getMeasuredHeight() - (textView.getPaddingTop() + textView.getPaddingBottom());
            float lineHeight = textView.getLineHeight();
            if (lineHeight > viewHeight) {
                ratio = viewHeight / lineHeight;
            }
        }

        // Take the smaller of the two ratios so that text never gets cropped
        ratio = Math.min(ratio, ratioWidth);

        // Change TextSize
        float originalSize = textView.getTextSize();
        float newSize = originalSize * ratio;
        if (newSize > 0 && Math.abs(newSize - originalSize) > FONT_CHANGE_THRESHOLD) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
        }
    }

    static public abstract class Builder<T extends TextAppearance> extends Appearance.Builder<T>
    {
        private String measuringText;
        //private String font;

        public Builder(final TextAppearance textAppearance) {
            super(textAppearance);
            this.measuringText = textAppearance.measuringText;
            //this.font = textAppearance.font;
        }

        public Builder<T> measuringText(String measuringText )
        {
            this.measuringText = measuringText;
            return this;
        }

        public Builder<T> font(String font )
        {
            //this.font = font;
            return this;
        }

        public abstract T build();
    }

    @Override
    public Builder<?> builder()
    {
        return new Builder<TextAppearance>(this)
        {
            @Override
            public TextAppearance build()
            {
                return new TextAppearance(this);
            }
        };
    }
}