package com.example;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.recognizer.DataFiles;
import com.example.recognizer.Grammar;
import com.example.recognizer.PhonMapper;
import com.example.vera.Controller;
import com.example.vera.Device;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;


public class MainActivity extends Activity implements RecognitionListener, SensorEventListener {

    private static final String TAG = "Recognizer";

    private static final String COMMAND_SEARCH = "command";
    private static final String KWS_SEARCH = "hotword";

    private final Handler mHandler = new Handler();
    private final Queue<String> mSpeechQueue = new LinkedList<String>();

    private SensorManager mSensorManager;
    private float mSensorMaximum;
    private float mSensorValue;

    private SpeechRecognizer mRecognizer;
    private Controller mController;
    private TextToSpeech mTextToSpeech;
    private View mMicView;

    private final Runnable mStopRecognitionCallback = new Runnable() {
        @Override
        public void run() {
            stopRecognition();
        }
    };

    private final TextToSpeech.OnUtteranceCompletedListener mUtteranceCompletedListener = new TextToSpeech.OnUtteranceCompletedListener() {
        @Override
        public void onUtteranceCompleted(String utteranceId) {
            synchronized (mSpeechQueue) {
                mSpeechQueue.poll();
                if (mSpeechQueue.isEmpty()) {
                    mRecognizer.startListening(KWS_SEARCH);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupController();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (sensor != null) {
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
            mSensorMaximum = sensor.getMaximumRange();
        }
        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) return;
                if (mTextToSpeech.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE) {
                    mTextToSpeech.setLanguage(Locale.getDefault());
                }
                mTextToSpeech.setOnUtteranceCompletedListener(mUtteranceCompletedListener);
            }
        });
        mMicView = findViewById(R.id.mic);
        mMicView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mMicView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        startStopRecognition();
                        break;
                    case MotionEvent.ACTION_UP:
                        stopRecognition();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mRecognizer != null) mRecognizer.cancel();
        mSensorManager.unregisterListener(this);
        mTextToSpeech.shutdown();
        super.onDestroy();
    }

    private void setupController() {
        new AsyncTask<Void, Void, Controller>() {
            @Override
            protected Controller doInBackground(Void... params) {
                Controller controller = new Controller();
                return controller.initialize() ? controller : null;
            }

            @Override
            protected void onPostExecute(Controller controller) {
                mController = controller;
                if (controller == null) {
                    Toast.makeText(MainActivity.this, "Controller is not found", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Controller is found! Please wait...", Toast.LENGTH_SHORT).show();
                    setupRecognizer();
                }
            }
        }.execute();
    }

    private void setupRecognizer() {
        if (mController == null) return;
        final String hotword = getString(R.string.hotword);
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    List<Device> devices = mController.getDevices();
                    final String[] names = new String[devices.size()];
                    for (int i = 0; i < names.length; i++) {
                        names[i] = devices.get(i).name;
                    }
                    PhonMapper phonMapper = new PhonMapper(getAssets().open("dict/ru/hotwords"));
                    Grammar grammar = new Grammar(names, phonMapper);
                    grammar.addWords(hotword);
                    DataFiles dataFiles = new DataFiles(getPackageName(), "ru");
                    File hmmDir = new File(dataFiles.getHmm());
                    File dict = new File(dataFiles.getDict());
                    File jsgf = new File(dataFiles.getJsgf());
                    copyAssets(hmmDir);
                    saveFile(jsgf, grammar.getJsgf());
                    saveFile(dict, grammar.getDict());
                    mRecognizer = SpeechRecognizerSetup.defaultSetup()
                            .setAcousticModel(hmmDir)
                            .setDictionary(dict)
                            .setBoolean("-remove_noise", false)
                            .setKeywordThreshold(1e-7f)
                            .getRecognizer();
                    mRecognizer.addKeyphraseSearch(KWS_SEARCH, hotword);
                    mRecognizer.addGrammarSearch(COMMAND_SEARCH, jsgf);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception ex) {
                if (ex != null) {
                    onRecognizerSetupError(ex);
                } else {
                    onRecognizerSetupComplete();
                }
            }
        }.execute();
    }

    private void onRecognizerSetupComplete() {
        Toast.makeText(this, "Ready", Toast.LENGTH_SHORT).show();
        mRecognizer.addListener(this);
        mRecognizer.startListening(KWS_SEARCH);
    }

    private void onRecognizerSetupError(Exception ex) {
        Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void copyAssets(File baseDir) throws IOException {
        String[] files = getAssets().list("hmm/ru");

        for (String fromFile : files) {
            File toFile = new File(baseDir.getAbsolutePath() + "/" + fromFile);
            InputStream in = getAssets().open("hmm/ru/" + fromFile);
            FileUtils.copyInputStreamToFile(in, toFile);
        }
    }

    private void saveFile(File f, String content) throws IOException {
        File dir = f.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create directory: " + dir);
        }
        FileUtils.writeStringToFile(f, content, "UTF8");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
        if (mRecognizer.getSearchName().equals(COMMAND_SEARCH)) {
            mRecognizer.stop();
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) return;
        String text = hypothesis.getHypstr();
        if (KWS_SEARCH.equals(mRecognizer.getSearchName())) {
            startRecognition();
        } else {
            Log.d(TAG, text);
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        mMicView.setBackgroundResource(R.drawable.background_big_mic);
        mHandler.removeCallbacks(mStopRecognitionCallback);
        String text = hypothesis != null ? hypothesis.getHypstr() : null;
        Log.d(TAG, "onResult " + text);
        if (text != null) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            process(text);
        }
        if (COMMAND_SEARCH.equals(mRecognizer.getSearchName())) {
            mRecognizer.startListening(KWS_SEARCH);
        }
    }

    private void startStopRecognition() {
        if (mRecognizer == null) return;
        if (KWS_SEARCH.equals(mRecognizer.getSearchName())) {
            startRecognition();
        } else {
            stopRecognition();
        }
    }

    private synchronized void startRecognition() {
        if (mRecognizer == null || COMMAND_SEARCH.equals(mRecognizer.getSearchName())) return;
        mRecognizer.cancel();
        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME).startTone(ToneGenerator.TONE_CDMA_PIP, 200);
        post(400, new Runnable() {
            @Override
            public void run() {
                mMicView.setBackgroundResource(R.drawable.background_big_mic_green);
                mRecognizer.startListening(COMMAND_SEARCH, 3000);
                Log.d(TAG, "Listen commands");
                post(4000, mStopRecognitionCallback);
            }
        });
    }

    private synchronized void stopRecognition() {
        if (mRecognizer == null || KWS_SEARCH.equals(mRecognizer.getSearchName())) return;
        mRecognizer.stop();
        mMicView.setBackgroundResource(R.drawable.background_big_mic);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mRecognizer == null) return;
        mSensorValue = event.values[0];
        if (mSensorValue < mSensorMaximum) {
            post(500, new Runnable() {
                @Override
                public void run() {
                    if (mSensorValue < mSensorMaximum) {
                        startRecognition();
                    }
                }
            });
        } else if (COMMAND_SEARCH.equals(mRecognizer.getSearchName())) {
            stopRecognition();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void post(long delay, Runnable task) {
        mHandler.postDelayed(task, delay);
    }

    private void process(final String text) {
        new AsyncTask<String, Void, List<Device>>() {
            @Override
            protected List<Device> doInBackground(String... params) {
                return mController.getDevices(params[0]);
            }

            @Override
            protected void onPostExecute(List<Device> devices) {
                for (Device device : devices) {
                    String result = mController.process(device);
                    if (result != null) {
                        Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                        speak(result);
                    }
                }
            }
        }.execute(text);
    }

    private void speak(String text) {
        synchronized (mSpeechQueue) {
            mRecognizer.stop();
            mSpeechQueue.add(text);
            HashMap<String, String> params = new HashMap<String, String>(2);
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString());
            params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            params.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true");
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params);
        }

    }
}
