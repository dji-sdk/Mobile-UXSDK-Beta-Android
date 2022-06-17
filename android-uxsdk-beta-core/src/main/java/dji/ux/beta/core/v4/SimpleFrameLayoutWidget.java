package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dji.keysdk.DJIKey;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.Subscriber;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Func1;
import dji.thirdparty.rx.subscriptions.CompositeSubscription;
import dji.ux.beta.core.base.DJISDKModel;

/**
 * Base class for widget view that does not need Appearances
 */
public abstract class SimpleFrameLayoutWidget extends FrameLayout implements Widget, TrackableWidget {

    //region Properties
    protected static final String TAG = "OldFrameLayout";
    private UIStyle widgetStyle = UIStyle.PLAIN;
    private List<DJIKey> dependentKeys;
    protected Context context;
    protected CompositeSubscription subscription = new CompositeSubscription();
    protected int keyIndex;
    protected int subKeyIndex;
    protected boolean isAttachedToWindow;
    //endregion

    //region Default Constructor
    public SimpleFrameLayoutWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        // Avoid the rendering exception in the Android Studio Preview view.

        initView(context, attrs, defStyle);
        if (isInEditMode()) {
            return;
        }
    }

    public void setWidgetStyle(UIStyle style) {
        this.widgetStyle = style;
    }

    @Override
    public boolean shouldTrack() {
        return false;
    }
    //endregion

    //region Logic UI thread update.

    @Override
    public void destroy() {
        unRegisterDependentKeys();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Avoid the rendering exception in the Android Studio Preview view.
        if (isInEditMode()) {
            return;
        }

        initKey();
        this.registerDependentKeys();
        this.isAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
        super.onDetachedFromWindow();
        this.destroy();
        this.isAttachedToWindow = false;
    }

    @Override
    public Observable<Boolean> transformValueObservable(final Object value, final DJIKey key) {
        return Observable.just(key).flatMap(new Func1<DJIKey, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(DJIKey djisdkCacheKey) {
                transformValue(value, key);
                return Observable.just(true);
            }
        }).observeOn(AndroidSchedulers.mainThread());
    }


    @Override
    public Observable<Boolean> updateWidgetObservable(final DJIKey key) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                updateWidget(key);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                }
            }
        });
    }

    @Override
    public float aspectRatio() {
        return 1;
    }
    //endregion

    //region DependentKeys Control
    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
    }

    @Override
    public boolean addDependentKey(DJIKey eachDependentKey) {
        if (dependentKeys == null) {
            // registerDependentKeys contains asynchronous registration of keys, two threads operating ArrayList at the
            // same time may cause crash, use CopyOnWriteArrayList instead
            dependentKeys = new CopyOnWriteArrayList<>();
        }
        if (!dependentKeys.contains(eachDependentKey)) {
            dependentKeys.add(eachDependentKey);
            return true;
        }
        return false;
    }

    @Override
    public void addDependentKeyWithRegister(DJIKey eachDependentKey) {
        DJISDKModelV4.getInstance().registerDependentKeys(new ArrayList<>(Collections.singletonList(eachDependentKey)), this);
    }

    @Override
    public List<DJIKey> getDependentKeys() {
        return dependentKeys;
    }

    @Override
    public void registerDependentKeys() {
        if (getDependentKeys() != null) {
            DJISDKModelV4.getInstance().registerDependentKeys(getDependentKeys(), this);
        }
    }

    @Override
    public void updateWidget(DJIKey key) {
        // Do nothing by default.
    }

    @Override
    public int getKeyIndex() {
        return keyIndex;
    }

    @Override
    public int getSubKeyIndex() {
        return subKeyIndex;
    }

    @Override
    public void updateKeyOnIndex(int keyIndex ,int subKeyIndex) {
        this.keyIndex = keyIndex;
        this.subKeyIndex = subKeyIndex;
        unRegisterDependentKeys();
        if (getDependentKeys() != null) {
            getDependentKeys().clear();
        }
        initKey();
        registerDependentKeys();
    }

    @Override
    public void unRegisterDependentKeys() {
        DJISDKModelV4.getInstance().unregisterDependentKeysForWidget(this);
    }

    @Override
    public void unRegisterDependentKey(DJIKey key) {
        DJISDKModelV4.getInstance().unregisterDependentKeyOnWidget(this, key);
    }

    //endregion

    protected void useThermalKeyIndex() {
        if (DJISDKManager.getInstance().getProduct() instanceof Aircraft) {
            Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
            List<Camera> cameraList = aircraft.getCameras();
            if (cameraList != null && !cameraList.isEmpty()) {
                for (Camera c : cameraList) {
                    if (c.getComponentIndexForIRInXT2() == c.getIndex()) {
                        keyIndex = c.getIndex();
                        break;
                    }
                }
            }
        }
    }
}
