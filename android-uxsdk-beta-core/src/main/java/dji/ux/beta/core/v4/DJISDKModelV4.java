package dji.ux.beta.core.v4;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import dji.common.error.DJIError;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.GetCallback;
import dji.keysdk.callback.KeyListener;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.Subscription;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Func1;
import dji.thirdparty.rx.schedulers.Schedulers;

/**
 * Encapsulates communication with SDKCache here.
 * Also holds all common logic of Widget to reduce code duplication
 */
public class DJISDKModelV4 {

    //region Properties
    private static final String TAG = "DJISDKModel";

    // For the case that the KeyManager is null,
    // SDKModel needs to start and timer and wait until the KeyManager is ready.
    private volatile Subscription timerSubscription;
    private Observable<Long> timer = Observable.timer(100, TimeUnit.MILLISECONDS).doOnSubscribe(() -> {
        if (KeyManager.getInstance() != null) {
            startPendingJob();
        }
    }).repeat().subscribeOn(Schedulers.computation());
    // Wait list of all the dependent keys from all widget
    private Map<Widget, List<DJIKey>> pendingWidgetDependentKeysMap;

    private Map<Widget, Map<DJIKey, KeyListener>> widgetKeyListenerMap;
    //endregion

    //region DependentKeys Control

    /**
     * Call this method when KeyManager is ready
     */
    private void startPendingJob() {
        // Stop the timer first
        stopTimerIfRunning();
        // Register all pending Widget
        if (pendingWidgetDependentKeysMap != null && !pendingWidgetDependentKeysMap.isEmpty()) {
            for (Widget widget : pendingWidgetDependentKeysMap.keySet()) {
                startListeningOnKeys(pendingWidgetDependentKeysMap.remove(widget), widget);
            }
        }
    }

    /**
     * Registers a list of dependent keys for Widget
     */
    public void registerDependentKeys(List<DJIKey> dependentKeys, Widget djiViewWidget) {
        if (KeyManager.getInstance() == null) {
            pendingWidgetDependentKeysMap.put(djiViewWidget, dependentKeys);
            startTimerIfNeeded();
        } else {
            startListeningOnKeys(dependentKeys, djiViewWidget);
        }
    }

    private void startTimerIfNeeded() {
        if (timerSubscription == null || timerSubscription.isUnsubscribed()) {
            timerSubscription = timer.subscribe();
        }
    }

    private void stopTimerIfRunning() {
        if (timerSubscription != null && !timerSubscription.isUnsubscribed()) {
            timerSubscription.unsubscribe();
            timerSubscription = null;
        }
    }

    /**
     * Get value of the key for Widget
     */
    public void getValueOfKey(DJIKey key, Widget djiViewWidget) {
        if (KeyManager.getInstance() != null) {
            getValueOnKeyObserver(key, djiViewWidget);
        }
    }

    /**
     * Pass the list of keys to {@link dji.sdksharedlib.DJISDKCache}
     */
    private void startListeningOnKeys(final List<DJIKey> dependentKeys, final Widget djiWidget) {
        Observable.from(dependentKeys)
                .flatMap((Func1<DJIKey, Observable<Boolean>>) dependentKey -> startListeningOnKey(dependentKey, djiWidget))
                .subscribeOn(Schedulers.computation())
                .subscribe();
    }

    /**
     * Pass the specified Widget's {@link DJIKey} to {@link dji.sdksharedlib.DJISDKCache}
     * When data arrives, calls {@link Widget#transformValueObservable} first
     * and then calls {@link Widget#updateWidgetObservable}
     */
    private Observable<Boolean> startListeningOnKey(final DJIKey dependentKey, final Widget djiWidget) {
        return Observable.just(dependentKey).flatMap((Func1<DJIKey, Observable<Boolean>>) djisdkCacheKey -> {
            KeyManager manager = KeyManager.getInstance();
            if (djisdkCacheKey != null && manager != null) {
                // Get current value
                manager.getValue(djisdkCacheKey, new GetCallback() {
                    @Override
                    public void onSuccess(Object value) {
                        if (value != null) {
                            djiWidget.updateWidgetObservable(djisdkCacheKey)
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .startWith(djiWidget.transformValueObservable(value, djisdkCacheKey))
                                    .subscribe();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull DJIError error) {

                    }
                });

                // Start listening to changes
                KeyListener newListener = (oldValue, newValue) -> {
                    if (newValue != null) {
                        djiWidget.updateWidgetObservable(djisdkCacheKey)
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .startWith(djiWidget.transformValueObservable(newValue, djisdkCacheKey))
                                .subscribe();
                    }
                };

                synchronized (this) {
                    Map<DJIKey, KeyListener> keyListenerMap = widgetKeyListenerMap.get(djiWidget);
                    if (keyListenerMap == null) {
                        keyListenerMap = new ConcurrentHashMap<>();
                    }
                    KeyListener oldListener = keyListenerMap.get(djisdkCacheKey);
                    if (oldListener != null){
                        manager.removeListener(oldListener);
                    }
                    keyListenerMap.put(djisdkCacheKey, newListener);
                    widgetKeyListenerMap.put(djiWidget, keyListenerMap);
                    manager.addListener(djisdkCacheKey, newListener);
                }
            }
            return Observable.just(true);
        }).subscribeOn(Schedulers.computation());
    }

    /**
     * Removes data watching for all dependentKeys of a given widget.
     */
    public void unregisterDependentKeysForWidget(final Widget widget) {
        // Remove from waiting list if it is
        pendingWidgetDependentKeysMap.remove(widget);
        // Stop listening to cache
        stopListeningObservable(widget).subscribe();
    }

    public void unregisterDependentKeyOnWidget(final Widget widget, DJIKey key) {
        Map<DJIKey, KeyListener> currentList = widgetKeyListenerMap.get(widget);
        if (currentList != null) {
            KeyListener eachListener = currentList.remove(key);
            if (KeyManager.getInstance() != null && eachListener != null) {
                KeyManager.getInstance().removeListener(eachListener);
            }
        }
    }

    private Observable<Boolean> stopListeningObservable(final Widget widget) {
        if (widget == null || widget.getDependentKeys() == null) {
            return Observable.just(true);
        }

        return Observable.just(true).doOnSubscribe(() -> {
            Map<DJIKey, KeyListener> currentList = widgetKeyListenerMap.remove(widget);
            if (currentList != null) {
                for (KeyListener eachListener : currentList.values()) {
                    if (KeyManager.getInstance() != null) {
                        KeyManager.getInstance().removeListener(eachListener);
                    }
                }
            }
        });
    }

    //endregion

    //region Life Cycle

    private DJISDKModelV4() {
        pendingWidgetDependentKeysMap = new ConcurrentHashMap<>();
        widgetKeyListenerMap = new ConcurrentHashMap<>();
    }

    /**
     * A Lazy Singleton Holder
     * To ensure instance is not initialized until the JVM determines that SingletonHolder must be executed
     */
    private static class SingletonHolder {
        private static DJISDKModelV4 instance = new DJISDKModelV4();
    }

    public static DJISDKModelV4 getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * Get the value of the key, and transfer the value to widget in same process of push data.
     */
    private Observable<Boolean> getValueOnKeyObserver(DJIKey dependentKey, final Widget djiWidget) {

        Observable<Boolean> observable =
                Observable.just(dependentKey).flatMap(new Func1<DJIKey, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(final DJIKey djisdkCacheKey) {
                        // Get current value
                        KeyManager.getInstance().getValue(djisdkCacheKey, new GetCallback() {
                            @Override
                            public void onSuccess(Object value) {
                                if (value != null) {
                                    djiWidget.updateWidgetObservable(djisdkCacheKey)
                                            .subscribeOn(AndroidSchedulers.mainThread())
                                            .startWith(djiWidget.transformValueObservable(value, djisdkCacheKey))
                                            .subscribe();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull DJIError error) {

                            }
                        });
                        return Observable.just(true);
                    }
                }).subscribeOn(Schedulers.computation());

        return observable;
    }

    //endregion
}
