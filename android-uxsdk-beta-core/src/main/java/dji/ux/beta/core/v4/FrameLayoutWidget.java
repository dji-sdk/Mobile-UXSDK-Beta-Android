package dji.ux.beta.core.v4;

import android.content.Context;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import dji.common.error.DJIError;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.SetCallback;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.Subscriber;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Func1;
import dji.thirdparty.rx.schedulers.Schedulers;
import dji.ux.beta.core.base.DJISDKModel;

/**
 * Base class for most widgets that also need Appearances
 */
public abstract class FrameLayoutWidget extends BaseFrameLayout implements Widget {

    //region Properties
    protected static final String TAG = "FrameLayoutWidget";
    private UIStyle widgetStyle = UIStyle.PLAIN;
    private List<DJIKey> dependentKeys;
    protected int keyIndex;
    protected int subKeyIndex;
    //endregion

    //region Default Constructor
    public FrameLayoutWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setWidgetStyle(UIStyle style) {
        this.widgetStyle = style;
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
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.destroy();
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

    //endregion

    //region DependentKeys Control
    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
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
    public void unRegisterDependentKeys() {
        DJISDKModelV4.getInstance().unregisterDependentKeysForWidget(this);
    }

    @Override
    public void unRegisterDependentKey(DJIKey key) {
        DJISDKModelV4.getInstance().unregisterDependentKeyOnWidget(this, key);
    }

    @Override
    public void updateKeyOnIndex(int keyIndex, int subKeyIndex) {
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
    public boolean addDependentKey(DJIKey eachDependentKey) {
        if (dependentKeys == null) {
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

    //endregion

    //region doAction
    protected Observable<Boolean> performActionByKey(final DJIKey key) {
        if (KeyManager.getInstance() == null) {
            return Observable.error(new NullPointerException("KeyManager is null."));
        }

        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                KeyManager.getInstance().performAction(key, new ActionCallback() {
                    @Override
                    public void onSuccess() {
                        subscriber.onStart();
                        subscriber.onNext(true);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onFailure(@NonNull DJIError error) {
                        subscriber.onError(new Throwable(error.getDescription()));
                    }
                });
            }
        }).subscribeOn(Schedulers.computation());
    }

    protected Observable<Boolean> setValueByKey(final DJIKey key, final Object value) {
        if (KeyManager.getInstance() == null) {
            return Observable.error(new NullPointerException("KeyManager is null."));
        }

        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                KeyManager.getInstance().setValue(key, value, new SetCallback() {
                    @Override
                    public void onSuccess() {
                        subscriber.onStart();
                        subscriber.onNext(true);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onFailure(@NonNull DJIError error) {
                        subscriber.onError(new Throwable(error.getDescription()));
                    }
                });
            }
        }).subscribeOn(Schedulers.computation());
    }
    //endregion
}
