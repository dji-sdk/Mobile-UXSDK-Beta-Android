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

package dji.ux.beta.core.util;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;

import java.io.File;

public class AudioRecorderHandler {

    private static final int FREQ = 44100;
    private static final int MAX_DATA_LENGTH = 160;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private short[] buffer = null;
    private File lastCacheFile = null;

    public AudioRecorderHandler(Context context) {
        if (context == null) {
            throw new RuntimeException("Context could not be null!");
        }
    }


    public void startRecord(AudioRecordingCallback audioRecordingCallback) {
        RecordTask task = new RecordTask(audioRecordingCallback);
        task.execute();
    }


    public void stopRecord() {
        isRecording = false;
    }


    public boolean deleteLastRecordFile() {
        boolean success = false;
        if (lastCacheFile != null && lastCacheFile.exists()) {
            success = lastCacheFile.delete();
        }
        return success;
    }


    public interface AudioRecordingCallback {

        void onRecording(byte[] data);


        void onStopRecord(String savedPath);
    }

    private class RecordTask extends AsyncTask<String, Integer, String> {

        private AudioRecordingCallback audioRecordingCallback;

        public RecordTask(AudioRecordingCallback audioRecordingCallback) {
            this.audioRecordingCallback = audioRecordingCallback;
        }

        @Override
        protected void onPreExecute() {
            int channelInConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSize = AudioRecord.getMinBufferSize(FREQ,
                    channelInConfig, audioEncoding);
            if (audioRecord == null) {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        FREQ, channelInConfig, audioEncoding, bufferSize);
            }

            buffer = new short[bufferSize];
            audioRecord.startRecording();
            isRecording = true;
        }

        @Override
        protected void onPostExecute(String result) {
            //audioRecord = null;
            if (result == null) {
                lastCacheFile = null;
            } else {
                lastCacheFile = new File(result);
            }
            if (audioRecordingCallback != null) {
                audioRecordingCallback.onStopRecord(result);
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String tempFileName = null;
            while (isRecording) {
                int result = audioRecord.read(buffer, 0, buffer.length);
                if (audioRecordingCallback != null) {
                    byte[] audio = new byte[result*2];
                    int index = 0;
                    byte[] shortToByte = null;
                    for (int i = 0; i < result; i++) {
                        shortToByte = getBytes(buffer[i]);
                        System.arraycopy(shortToByte, 0, audio, index, shortToByte.length);
                        index += shortToByte.length;
                    }

                    if (shortToByte != null && shortToByte.length > 0) {
                        audioRecordingCallback.onRecording(audio);
                    }

                }

            }

            if (audioRecord != null) {
                audioRecord.stop();
            }
            return tempFileName;
        }
    }

    public void release() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }

    }

    public static byte[] getBytes(short data) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        return bytes;
    }
}
