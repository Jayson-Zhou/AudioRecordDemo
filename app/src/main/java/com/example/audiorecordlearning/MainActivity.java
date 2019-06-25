package com.example.audiorecordlearning;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "MainActivity:";

    private Button btnRecord;
    private Button btnStop;
    //    private Button btnPlay;
    private TextView tvTest;

    private AudioRecord recorder = null;
    private AudioTrack tracker = null;

    // 采样率
    private int frequency = 44100;
    // 采样通道
    private int channelInConfig = AudioFormat.CHANNEL_IN_MONO;
    // 播放通道
    private int channelOutConfig = AudioFormat.CHANNEL_OUT_MONO;
    // 16位音频编码
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    // 录音缓存区
    private byte[] bufferIn = null;
    // 记录是否正在录音
    private boolean isRecording = false;
    // 记录是否正在播放
    private boolean isPlaying = false;

    // 存放录音缓存的队列
    private Queue<byte[]> bufferQueue = null;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    tvTest.setText("正在播放");
                    break;
                case 2:
                    tvTest.setText("停止播放");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialViews();
    }

    private void initialViews() {
        btnRecord = findViewById(R.id.btn_record);
        btnRecord.setOnClickListener(this);
        btnStop = findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(this);
        /*btnPlay = findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(this);*/
        tvTest = findViewById(R.id.tv_test);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_record:
                if (!isRecording) {
                    new AudioRecordTask().execute();
                    startPlaying();
                }
                break;
            case R.id.btn_stop:
                if (recorder != null) {
                    if (isRecording) {
                        isRecording = false;
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                    }
                }
                if (tracker != null) {
                    if (isPlaying) {
                        isPlaying = false;
                        tracker.stop();
                        tracker.release();
                        tracker = null;
                    }
                }
                break;
            /*case R.id.btn_play:
                break;*/
        }
    }

    private void startPlaying() {
        new Thread(new RecordPlayingRunnable()).start();
    }

    @SuppressLint("StaticFieldLeak")
    public class AudioRecordTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            int bufferSize = AudioRecord.getMinBufferSize(frequency, channelInConfig, audioEncoding);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelInConfig, audioEncoding, bufferSize);
            bufferQueue = new LinkedList<>();
            bufferIn = new byte[bufferSize];
            if (recorder != null) {
                recorder.startRecording();
                isRecording = true;
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (isRecording) {
                recorder.read(bufferIn, 0, bufferIn.length);
                /*if(bufferQueue.size() >= 20) {
                    bufferQueue.remove();
                }*/
                bufferQueue.add(bufferIn);
                //Log.d(TAG, String.valueOf(bufferQueue.size()));
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class RecordPlayingRunnable implements Runnable {
        @Override
        public void run() {
            int bufferSize = AudioTrack.getMinBufferSize(frequency, channelOutConfig, audioEncoding);
            tracker = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, channelOutConfig, audioEncoding, bufferSize, AudioTrack.MODE_STREAM);
            tracker.play();
            isPlaying = true;
            while (true) {
                byte[] bufferOut = bufferQueue.poll();
                if (bufferOut != null) {
                    tracker.write(bufferOut, 0, bufferOut.length);
//                    Log.d(TAG, String.valueOf(bufferOut.length));
                }
                handler.sendEmptyMessage(1);
                if (!isPlaying) {
                    handler.sendEmptyMessage(2);
                    return;
                }
            }
        }
    }
}
