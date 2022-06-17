package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import dji.ux.beta.core.R;

/**
 * Created by Robert.Liu on 26/10/2016.
 */

public class ParentChildrenViewAnimator extends ViewAnimator {

    private Animation leftInAnim;
    private Animation leftOutAnim;
    private Animation rightInAnim;
    private Animation rightOutAnim;
    private RootViewCallback rootViewCallback;

    public interface RootViewCallback {
        void onRootViewIsShown(boolean isShown);
    }

    public void setRootViewCallback(RootViewCallback rootViewCallback) {
        this.rootViewCallback = rootViewCallback;
    }

    //region Default Constructors
    public ParentChildrenViewAnimator(Context context) {
        super(context);
    }

    public ParentChildrenViewAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    //endregion

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (rightInAnim == null) {
            leftInAnim = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_left_in);
            leftOutAnim = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_left_out);
            rightInAnim = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_right_in);
            rightOutAnim = AnimationUtils.loadAnimation(getContext(), R.anim.uxsdk_slide_right_out);
        }
    }

    @Override
    public void setDisplayedChild(int whichChild) {
        if (rootViewCallback != null) {
            rootViewCallback.onRootViewIsShown(whichChild == 0);
        }

        int lastIndex = getDisplayedChild();

        if (lastIndex > whichChild) {
            setInAnimation(leftInAnim);
            setOutAnimation(rightOutAnim);
        } else if (lastIndex < whichChild) {
            setInAnimation(rightInAnim);
            setOutAnimation(leftOutAnim);
        } else {
            setInAnimation(null);
            setOutAnimation(null);
        }
        super.setDisplayedChild(whichChild);
    }
}
