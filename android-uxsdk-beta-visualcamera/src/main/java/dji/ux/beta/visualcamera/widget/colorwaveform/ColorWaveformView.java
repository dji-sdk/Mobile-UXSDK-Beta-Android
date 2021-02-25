/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.ux.beta.visualcamera.widget.colorwaveform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import dji.common.camera.ColorWaveformSettings.ColorWaveformDisplayMode;
import dji.common.camera.ColorWaveformSettings.ColorWaveformDisplayState;
import dji.log.DJILog;
import dji.sdk.codec.DJICodecManager;
import dji.ux.beta.visualcamera.R;

/**
 * Displays a color waveform view which gathers data from the DJICodecManager and displays it.
 */
public class ColorWaveformView extends AppCompatImageView {
    //region constants
    private static final String TAG = "ColorWaveformView";
    private static final int MSG_FETCH_RGBA_DATA = 0;
    private static final int MSG_GENERATE_BITMAP = 1;
    private static final int MSG_SWITCH_COLOR_EXP = 2;

    private static final int UPDATE_MIN_INTERVAL = 0;
    private static final int SAMPLE_MAX_HEIGHT = 96;
    private static final int BITMAP_MAX_HEIGHT = 360;
    private static final float DRAW_AREA_HORIZONTAL_RATIO = 1;
    private static final float DRAW_AREA_VERTICAL_RATIO = 1;
    private static final int BITMAP_POOL_SIZE = 3;

    private static final int DRAWERS_ROW_SAMPLE_INTERVAL = 2;
    private static final int DRAWERS_COLUMN_SAMPLE_INTERVAL = 2;
    //endregion

    //region Fields
    private BitmapPool bitmapPool;
    private boolean isRunning = false;
    private long lastDrawDuration = -1;
    private BitmapWrapper curBitmapWrapper;
    private List<WaveformDrawer> drawerList = new LinkedList<>();
    private byte[] data;
    private int videoWidth;
    private int videoHeight;
    private HandlerThread waveformThread;
    private Handler waveformHandler;
    private ColorWaveformDisplayMode displayMode = ColorWaveformDisplayMode.MIX;
    private ColorWaveformDisplayState displayState = ColorWaveformDisplayState.EXPOSURE;
    private DJICodecManager codecManager = null;
    private WaveformDrawer expDrawer;
    private WaveformDrawer[] rgbDrawers;

    private int horizontalLineNum;
    private float horizontalLineWidth;
    @ColorInt
    private int horizontalLineColor;
    //endregion

    //region Constructor
    public ColorWaveformView(@NonNull Context context) {
        super(context);
        init();
    }

    public ColorWaveformView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorWaveformView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        horizontalLineNum = 5;
        horizontalLineWidth = 1f;
        horizontalLineColor = getResources().getColor(R.color.uxsdk_white_50_percent);
    }
    //endregion

    //region inner classes and interfaces

    /**
     * Draws the waveform.
     */
    public interface WaveformDrawer {

        /**
         * Initializes the waveform drawer.
         */
        void init();

        /**
         * Performs cleanup for the waveform drawer.
         */
        void unInit();

        /**
         * Draws a waveform line on the canvas.
         *
         * @param canvas      The canvas to draw on.
         * @param paintWidth  The width of the line to draw.
         * @param data        The data from the video decoder.
         * @param videoWidth  The width of the video.
         * @param videoHeight The height of the video.
         * @param left        The left position of the draw area.
         * @param top         The top position of the draw area.
         * @param right       The right position of the draw area.
         * @param bottom      The bottom position of the draw area.
         */
        void draw(@NonNull Canvas canvas, float paintWidth, @NonNull byte[] data, int videoWidth, int videoHeight, float left, float top, float right, float bottom);
    }

    static class BitmapWrapper {
        private Bitmap bitmap;
        private Canvas canvas;

        public BitmapWrapper(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.canvas = new Canvas(bitmap);
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public Canvas getCanvas() {
            return canvas;
        }
    }

    static class BitmapPool {
        private BlockingQueue<BitmapWrapper> bitmapWrapperQueue;
        private List<BitmapWrapper> outQueue;
        private int width;
        private int height;

        public synchronized void init(int width, int height, int size) {
            bitmapWrapperQueue = new ArrayBlockingQueue<>(size);
            outQueue = new ArrayList<>(size);
            this.width = width;
            this.height = height;
            for (int i = 0; i < size; i++) {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                BitmapWrapper bitmapWrapper = new BitmapWrapper(bitmap);
                if (!bitmapWrapperQueue.offer(bitmapWrapper)) {
                    DJILog.e(TAG, "BitmapPool: constructor can't offer to queue: size: " + bitmapWrapperQueue.size());
                }
            }
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        private synchronized BitmapWrapper getBitmap(long timeoutInMs) {
            if (bitmapWrapperQueue == null) {
                return null;
            }
            try {
                BitmapWrapper bitmapWrapper = bitmapWrapperQueue.poll(timeoutInMs, TimeUnit.MILLISECONDS);
                if (bitmapWrapper != null) {
                    outQueue.add(bitmapWrapper);
                    return bitmapWrapper;
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                DJILog.e(TAG, "bitmap wrapper pool getBitmap error: ", e);
            }
            return null;

        }

        private synchronized boolean isFromBitmapPool(BitmapWrapper bitmapWrapper) {
            if (outQueue == null) {
                return false;
            }

            if (bitmapWrapper != null) {
                return outQueue.contains(bitmapWrapper);
            } else {
                return false;
            }
        }

        private synchronized boolean releaseBitmap(BitmapWrapper bitmapWapper) {
            if (bitmapWrapperQueue == null) {
                _releaseBitmap(bitmapWapper);
                return false;
            }
            if (bitmapWapper != null) {
                if (outQueue.contains(bitmapWapper)) {
                    if (bitmapWrapperQueue.offer(bitmapWapper)) {
                        outQueue.remove(bitmapWapper);
                        return true;
                    } else {
                        _releaseBitmap(bitmapWapper);
                        return false;
                    }
                } else {
                    _releaseBitmap(bitmapWapper);
                    return false;
                }
            } else {
                return false;
            }

        }

        private void _releaseBitmap(BitmapWrapper bitmapWrapper) {
            if (bitmapWrapper != null && bitmapWrapper.getBitmap() != null && !bitmapWrapper.getBitmap().isRecycled()) {
                bitmapWrapper.getBitmap().recycle();
            }
        }

        private synchronized void release() {
            if (bitmapWrapperQueue != null) {
                for (BitmapWrapper bitmapWrapper : bitmapWrapperQueue) {
                    _releaseBitmap(bitmapWrapper);
                }
            }
            bitmapWrapperQueue = null;
            outQueue = null;
        }
    }
    //endregion

    //region state

    /**
     * Gets the display state of the waveform.
     *
     * @return The display state of the waveform.
     */
    @NonNull
    public ColorWaveformDisplayState getDisplayState() {
        return displayState;
    }

    /**
     * Sets the display state of the waveform.
     *
     * @param displayState The display state to set.
     */
    public void setDisplayState(@NonNull ColorWaveformDisplayState displayState) {
        if (displayState != this.displayState) {
            this.displayState = displayState;

            //re-init drawers in waveform thread to avoid concurrent modification exception
            if (waveformHandler.hasMessages(MSG_SWITCH_COLOR_EXP)) {
                waveformHandler.removeMessages(MSG_SWITCH_COLOR_EXP);
            }
            waveformHandler.sendMessage(waveformHandler.obtainMessage(MSG_SWITCH_COLOR_EXP));
        }
    }

    /**
     * Gets the display mode of the waveform.
     *
     * @return The display mode of the waveform.
     */
    @NonNull
    public ColorWaveformDisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Sets the display mode of the waveform.
     *
     * @param displayMode The display mode to set.
     */
    public void setDisplayMode(@NonNull ColorWaveformDisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    /**
     * Sets the codec manager. This must be set before the waveform can display any data.
     *
     * @param codecManager The codec manager to set.
     */
    public void setCodecManager(@Nullable DJICodecManager codecManager) {
        this.codecManager = codecManager;
    }
    //endregion

    //region Customization

    /**
     * Gets the number of horizontal lines displayed on the waveform view.
     *
     * @return The number of horizontal lines.
     */
    public int getHorizontalLineNum() {
        return horizontalLineNum;
    }

    /**
     * Sets the number of horizontal lines displayed on the waveform view.
     *
     * @param horizontalLineNum The number of horizontal lines.
     */
    public void setHorizontalLineNum(int horizontalLineNum) {
        this.horizontalLineNum = horizontalLineNum;
    }

    /**
     * Gets the color of the horizontal lines displayed on the waveform view.
     *
     * @return The color of the horizontal lines.
     */
    @ColorInt
    public int getHorizontalLineColor() {
        return horizontalLineColor;
    }

    /**
     * Sets the color of the horizontal lines displayed on the waveform view.
     *
     * @param horizontalLineColor The color of the horizontal lines.
     */
    public void setHorizontalLineColor(@ColorInt int horizontalLineColor) {
        this.horizontalLineColor = horizontalLineColor;
    }

    /**
     * Gets the width of the horizontal lines displayed on the waveform view.
     *
     * @return The width of the horizontal lines.
     */
    public float getHorizontalLineWidth() {
        return horizontalLineWidth;
    }

    /**
     * Sets the width of the horizontal lines displayed on the waveform view.
     *
     * @param horizontalLineWidth The width of the horizontal lines.
     */
    public void setHorizontalLineWidth(float horizontalLineWidth) {
        this.horizontalLineWidth = horizontalLineWidth;
    }
    //endregion

    //region waveform thread helpers
    private void startWaveformThread() {
        stopWaveformThread();
        waveformThread = new HandlerThread("Waveform_Thread");
        waveformThread.start();
        waveformHandler = new Handler(waveformThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_FETCH_RGBA_DATA:
                        if (isRunning && codecManager != null && codecManager.isDecoderOK() && getWidth() > 0 && getHeight() > 0) {
                            int width = getWidth() / 4;
                            int height = getHeight() / 4;
                            if (height > SAMPLE_MAX_HEIGHT) {
                                float ratio = (float) width / (float) height;
                                height = SAMPLE_MAX_HEIGHT;
                                width = (int) (ratio * height);
                            }
                            byte[] bArray =
                                    displayState == ColorWaveformDisplayState.EXPOSURE ?
                                            codecManager.getYuvData(width, height) :
                                            codecManager.getRgbaData(width, height);
                            if (bArray != null && bArray.length > 0) {
                                setData(bArray, width, height);
                            } else {
                                sendEmptyMessageDelayed(MSG_FETCH_RGBA_DATA, 1000);
                                DJILog.e(TAG, "handleMessage: get rgba data failed");
                            }
                        } else {
                            if (isRunning) {
                                sendEmptyMessageDelayed(MSG_FETCH_RGBA_DATA, 1000);
                            }
                        }
                        break;
                    case MSG_GENERATE_BITMAP:
                        final long tStartSetData = System.currentTimeMillis();
                        int bmHeight = BITMAP_MAX_HEIGHT;
                        int bmWidth = bmHeight * 16 / 9;
                        if (bitmapPool == null) {
                            bitmapPool = new BitmapPool();
                            bitmapPool.init(bmWidth, bmHeight, BITMAP_POOL_SIZE);
                        }
                        final BitmapWrapper bitmapWrapper = generateWaveformDisplay(bmWidth, bmHeight, horizontalLineNum, horizontalLineColor, horizontalLineWidth);
                        if (bitmapWrapper != null) {
                            lastDrawDuration = System.currentTimeMillis() - tStartSetData;
                            ColorWaveformView.this.post(() -> {
                                setImageBitmap(bitmapWrapper.getBitmap());
                                if (curBitmapWrapper != null) {
                                    recycleBitmapWrapper(curBitmapWrapper);
                                }
                                curBitmapWrapper = bitmapWrapper;
                            });
                            if (isRunning && !hasMessages(MSG_FETCH_RGBA_DATA)) {
                                sendEmptyMessageDelayed(MSG_FETCH_RGBA_DATA, lastDrawDuration > UPDATE_MIN_INTERVAL ? 0 : UPDATE_MIN_INTERVAL - lastDrawDuration);
                            }
                            break;
                        }
                        if (isRunning && !hasMessages(MSG_FETCH_RGBA_DATA)) {
                            sendEmptyMessageDelayed(MSG_FETCH_RGBA_DATA, UPDATE_MIN_INTERVAL);
                        }
                        break;
                    case MSG_SWITCH_COLOR_EXP:
                        initDrawers();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void stopWaveformThread() {
        if (waveformHandler != null) {
            waveformHandler.removeCallbacksAndMessages(null);
            waveformHandler = null;
        }
        if (waveformThread != null) {
            waveformThread.quitSafely();
            waveformThread = null;
        }
    }

    private void setData(byte[] data, int width, int height) {
        this.data = data;
        this.videoWidth = width;
        this.videoHeight = height;
        waveformHandler.sendEmptyMessage(MSG_GENERATE_BITMAP);
    }

    private void startUpdate() {
        isRunning = true;
        if (waveformHandler == null) {
            startWaveformThread();
        }
        waveformHandler.sendEmptyMessage(MSG_FETCH_RGBA_DATA);
    }

    private void stopUpdate() {
        isRunning = false;
    }
    //endregion

    //region drawer helpers
    private void addDrawer(WaveformDrawer drawer) {
        drawerList.add(drawer);
    }

    private void addDrawers(WaveformDrawer[] drawers) {
        drawerList.addAll(Arrays.asList(drawers));
    }

    private void clearDrawers() {
        drawerList.clear();
    }

    private void initDrawers() {
        if (expDrawer == null) {
            expDrawer = new ColorWaveformLinesDrawer(ColorWaveformLinesDrawer.ColorChannel.Exp, 0x14ffffff);
            expDrawer.init();
        }
        if (rgbDrawers == null) {
            ColorWaveformLinesDrawer redDrawer = new ColorWaveformLinesDrawer(ColorWaveformLinesDrawer.ColorChannel.R, 0x28ff0000, DRAWERS_ROW_SAMPLE_INTERVAL, DRAWERS_COLUMN_SAMPLE_INTERVAL);
            ColorWaveformLinesDrawer greenDrawer = new ColorWaveformLinesDrawer(ColorWaveformLinesDrawer.ColorChannel.G, 0x2800ff00, DRAWERS_ROW_SAMPLE_INTERVAL, DRAWERS_COLUMN_SAMPLE_INTERVAL);
            ColorWaveformLinesDrawer blueDrawer = new ColorWaveformLinesDrawer(ColorWaveformLinesDrawer.ColorChannel.B, 0x280000ff, DRAWERS_ROW_SAMPLE_INTERVAL, DRAWERS_COLUMN_SAMPLE_INTERVAL);
            rgbDrawers = new WaveformDrawer[]{redDrawer, greenDrawer, blueDrawer};
            for (WaveformDrawer rgbDrawer : rgbDrawers) {
                rgbDrawer.init();
            }
        }
        clearDrawers();
        if (displayState == ColorWaveformDisplayState.EXPOSURE) {
            addDrawer(expDrawer);
        } else {
            addDrawers(rgbDrawers);
        }
    }

    private void unInitDrawers() {
        if (expDrawer != null) {
            expDrawer.unInit();
            expDrawer = null;
        }
        if (rgbDrawers != null) {
            for (WaveformDrawer rgbDrawer : rgbDrawers) {
                rgbDrawer.unInit();
            }
            rgbDrawers = null;
        }
        clearDrawers();
    }
    //endregion

    //region bitmap helpers
    private void recycleBitmapWrapper(BitmapWrapper bitmapWrapper) {
        if (bitmapWrapper != null) {
            if (bitmapPool != null && bitmapPool.isFromBitmapPool(bitmapWrapper)) {
                bitmapPool.releaseBitmap(bitmapWrapper);
            } else {
                if (!bitmapWrapper.getBitmap().isRecycled()) {
                    bitmapWrapper.getBitmap().recycle();
                }
            }
        }
    }

    private BitmapWrapper generateWaveformDisplay(int width, int height, int horLineNum, @ColorInt int horLineColor, float horLineWidth) {
        if (width <= 0 || height <= 0) {
            return null;
        }

        BitmapWrapper bitmapWrapper;
        if (bitmapPool != null && bitmapPool.getWidth() == width && bitmapPool.getHeight() == height) {
            bitmapWrapper = bitmapPool.getBitmap(200);
            if (bitmapWrapper == null) {
                bitmapWrapper = new BitmapWrapper(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
            }
        } else {
            bitmapWrapper = new BitmapWrapper(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
        }

        Canvas canvas = bitmapWrapper.getCanvas();
        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(clearPaint);

        canvas.drawARGB(25, 0xff, 0xff, 0xff);

        float left = (1 - DRAW_AREA_HORIZONTAL_RATIO) * width;
        float right = width * DRAW_AREA_HORIZONTAL_RATIO;
        float top = (1 - DRAW_AREA_VERTICAL_RATIO) * height;
        float bottom = height * DRAW_AREA_VERTICAL_RATIO;

        float topLineY = 0;
        float bottomLineY = bottom - topLineY;
        float lineYInterval = (bottomLineY - topLineY) / (horLineNum - 1);

        Paint linePaint = new Paint();
        linePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        linePaint.setStrokeWidth(horLineWidth);
        linePaint.setColor(horLineColor);
        for (int i = 0; i < horLineNum; i++) {
            float lineY = topLineY + i * lineYInterval;
            canvas.drawLine(left, lineY, right, lineY, linePaint);
        }

        float paintWidth = height / 205f;
        if (data != null) {
            switch (displayMode) {
                case MIX:
                    for (final WaveformDrawer drawer : drawerList) {
                        drawer.draw(canvas, paintWidth, data, videoWidth, videoHeight, left, top, right, bottom);
                    }
                    break;
                case SEPARATE:
                    int drawerNum = drawerList.size();
                    if (drawerNum > 0) {
                        float horInterval = (right - left) / drawerNum;
                        for (int i = 0; i < drawerNum; i++) {
                            drawerList.get(i).draw(canvas, paintWidth, data, videoWidth, videoHeight, left + i * horInterval, top, left + (i + 1) * horInterval, bottom);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return bitmapWrapper;
    }
    //endregion

    //region Lifecycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (drawerList.size() == 0) {
            initDrawers();
        }
        startWaveformThread();
        if (getVisibility() == VISIBLE) {
            startUpdate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopUpdate();
        stopWaveformThread();
        unInitDrawers();
        if (bitmapPool != null) {
            bitmapPool.release();
            bitmapPool = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            startUpdate();
        } else {
            stopUpdate();
        }
    }
    //endregion
}
