package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

import java.util.List;

import dji.keysdk.DJIKey;
import dji.thirdparty.rx.Observable;

/**
 * Common interface of all the widget classes.
 * Each Widget base class should implement this
 * and extend from a View class that it needs
 */
public interface Widget extends KeyIndexManager {

    //region Life Cycle

    /**
     * Adds all dependent keys in here
     */
    void initKey();

    /**
     * invoke when the view create, add view, findViewById load, AttributeSet in here
     */
    void initView(Context context, AttributeSet attrs, int defStyleAttr);

    /**
     * Deallocates all resources
     * Call this method at the end of Widget life-cycle
     */
    void destroy();
    //endregion

    //region View Logic

    /**
     * RXJava Observable that executes {@link #transformValue}
     */
    Observable<Boolean> transformValueObservable(Object value, DJIKey key);

    /**
     * Manipulate the raw value receiving from Cache layer to transform it to
     * something meaningful for the Widget
     */
    void transformValue(Object value, DJIKey key);

    /**
     * RXJava Observable that executes {@link #updateWidget}
     */
    Observable<Boolean> updateWidgetObservable(DJIKey key);

    /**
     * All the UI related updates are done here
     */
    void updateWidget(DJIKey key);

    /**
     * Returns the ratio of width over height of the widget
     */
    float aspectRatio();
    //endregion

    boolean addDependentKey(DJIKey eachDependentKey);

    void addDependentKeyWithRegister(DJIKey eachDependentKey);
    /**
     * @return all the {@link DJIKey} that this View depends on
     */
    List<DJIKey> getDependentKeys();

    /**
     * Lets who ever is in charge know about View's dependent keys
     */
    void registerDependentKeys();

    void unRegisterDependentKeys();

    void unRegisterDependentKey(DJIKey key);

}

