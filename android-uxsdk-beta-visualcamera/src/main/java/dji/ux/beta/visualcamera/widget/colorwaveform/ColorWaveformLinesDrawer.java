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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dji.log.DJILog;

/**
 * Draws the lines on the color waveform view.
 */
public class ColorWaveformLinesDrawer implements ColorWaveformView.WaveformDrawer {

    //region constants
    private static final String TAG = "WaveformLinesDrawer";
    //endregion

    //region Fields
    private Canvas canvas;
    private byte[] data;
    private int videoWidth;
    private float left;
    private float bottom;
    private float horInterval;
    private float verInterval;
    private ExecutorService threadPool;
    private CountDownLatch rowCdl;

    private ColorChannel colorChannel;
    private Paint paint;
    private int rowSampleInterval;
    private int columnSampleInterval;
    //endregion

    /**
     * A channel that represents which waveform line is being drawn.
     */
    public enum ColorChannel {
        Exp(-1), R(0), G(1), B(2), A(3);
        private final int value;

        public int value() {
            return value;
        }

        ColorChannel(int value) {
            this.value = value;
        }
    }

    public ColorWaveformLinesDrawer(@NonNull ColorChannel colorChannel, @ColorInt int color, int rowSampleInterval, int columnSampleInterval) {
        this.colorChannel = colorChannel;
        this.paint = new Paint();
        this.rowSampleInterval = rowSampleInterval;
        this.columnSampleInterval = columnSampleInterval;
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        paint.setStrokeWidth(1);
        paint.setColor(color);
        paint.setAntiAlias(false);
    }

    public ColorWaveformLinesDrawer(@NonNull ColorChannel colorChannel, @ColorInt int color) {
        this(colorChannel, color, 1, 1);
    }

    @Override
    public void init() {
        if (threadPool != null) {
            shutdownThreadPoolExecutor();
        }
        threadPool = new ThreadPoolExecutor(7, 7, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    //One good way to shut down the ExecutorService (which is also recommended by Oracle)
    private void shutdownThreadPoolExecutor() {
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
            threadPool = null;
        }
    }

    @Override
    public void unInit() {
        shutdownThreadPoolExecutor();
    }

    @Override
    public void draw(@NonNull Canvas canvas, float paintWidth, @NonNull byte[] data, int videoWidth, int videoHeight, float left, float top, float right, float bottom) {
        ExecutorService tempThreadPool = threadPool;
        if (tempThreadPool == null) {
            return;
        }
        this.canvas = canvas;
        this.data = Arrays.copyOf(data, data.length);
        this.paint.setStrokeWidth(paintWidth);
        this.videoWidth = videoWidth;
        this.left = left;
        this.bottom = bottom;
        horInterval = columnSampleInterval * (right - left) / videoWidth;
        verInterval = (bottom - top) / 256;
        rowCdl = new CountDownLatch(videoHeight / rowSampleInterval);
        for (int i = 0; i < videoHeight; i += rowSampleInterval) {
            tempThreadPool.execute(new DrawRowTask(i));
        }
        try {
            rowCdl.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            DJILog.e(TAG, e.getMessage());
        }
    }

    private class DrawRowTask implements Runnable {
        private int rowNum;

        public DrawRowTask(int rowNum) {
            this.rowNum = rowNum;
        }

        @Override
        public void run() {
            int lastValue = -1;
            int value;
            int preposedPixelNum = rowNum * videoWidth;
            for (int j = 0; j < videoWidth / columnSampleInterval; j++) {
                value = (colorChannel.value() < 0 ? data[(preposedPixelNum + j * columnSampleInterval)] : data[(preposedPixelNum + j * columnSampleInterval) * 4 + colorChannel.value()]) & 0xff;
                if (lastValue >= 0) {
                    canvas.drawLine(left + horInterval * j, bottom - lastValue * verInterval, left + horInterval * (j + 1), bottom - value * verInterval, paint);
                }
                lastValue = value;
            }
            if (rowCdl != null) {
                rowCdl.countDown();
            }
        }
    }
}
