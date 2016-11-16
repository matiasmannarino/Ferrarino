package com.example.mana.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.hardware.SensorEventListener;

import android.bluetooth.BluetoothSocket;
import android.widget.ImageButton;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Botones extends AppCompatActivity implements SensorEventListener{
    public static final int DEFAULT_BUTTON_COLOR = Color.BLACK;
    public static final int ACTIVE_BUTTON_COLOR = Color.GREEN;
    public static final int DEFAULT_STOP_BUTTON_COLOR = Color.YELLOW;
    private static final int PERMISSION_RECORD_AUDIO = 222;
    private SensorManager mSensorManager;
    Sensor accelerometer;
    Button leftArrow;
    Button rightArrow;
    Button downArrow;
    Button upArrow;
    Button stopSignal;
    Button activarAcelerometro;
    Button activarBluetooth;
    boolean _leftActive;
    boolean _rightActive;
    boolean _upActive;
    boolean _downActive;
    boolean _aceleremotroActivado;

    //BLUETOOTH
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //VOZ
    private ImageButton btnSpeak;
    private SpeechRecognizer sr;
    private boolean _pilotoAutomatico;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_botones);

        rightArrow = (Button) this.findViewById(R.id.rightArrow);
        leftArrow = (Button) this.findViewById(R.id.leftArrow);
        upArrow = (Button) this.findViewById(R.id.upArrow);
        downArrow = (Button) this.findViewById(R.id.downArrow);
        stopSignal = (Button) this.findViewById(R.id.stopSignal);
        btnSpeak = (ImageButton) this.findViewById(R.id.btnSpeak);
        activarAcelerometro = (Button) this.findViewById(R.id.activarAcelerometro);
        activarBluetooth = (Button) this.findViewById(R.id.bluetooth);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        setDefaultColors();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        stopSignal.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onStopTouch(v, event);
            }
        });
        downArrow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onArrowTouch(v, event);
            }
        });
        upArrow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onArrowTouch(v, event);
            }
        });
        leftArrow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onArrowTouch(v, event);
            }
        });
        rightArrow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onArrowTouch(v, event);
            }
        });
        btnSpeak.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                setButtonColor(btnSpeak, ACTIVE_BUTTON_COLOR);
                return false;
            }
        });

        //BLUETOOTH
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device
        if (address != null)
            new ConnectBT().execute(); //Call the class to connect

        //VOZ
        checkRecordAudioPermission();
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new listener());
        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onSpeechInput();
            }
        });
    }

    private boolean onArrowTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            setActiveArrow((Button)v);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            //setButtonColor(v, DEFAULT_BUTTON_COLOR);
        }
        return false;
    }
    private boolean onStopTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            setActiveArrow((Button)v);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            //setButtonColor(v, DEFAULT_STOP_BUTTON_COLOR);
        }
        return false;
    }

    private void setDefaultColors() {
        setButtonColor(leftArrow, DEFAULT_BUTTON_COLOR);
        setButtonColor(rightArrow, DEFAULT_BUTTON_COLOR);
        setButtonColor(upArrow, DEFAULT_BUTTON_COLOR);
        setButtonColor(downArrow, DEFAULT_BUTTON_COLOR);
        setButtonColor(stopSignal, DEFAULT_STOP_BUTTON_COLOR);
        setButtonColor(btnSpeak, DEFAULT_BUTTON_COLOR);
    }
    private void setButtonColor(View v, int color){
        v.setBackgroundTintList(ColorStateList.valueOf(color));
    }
    private void setActiveArrow(Button bt) {
        setDefaultColors();
        setButtonColor(bt, ACTIVE_BUTTON_COLOR);
        sendBT(bt.getId());
    }
    private void toastMsg(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    protected void onDestroy() {
        super.onDestroy();
        sendBT("5");
    }

    //Bluetooth
    private void sendBT(int btId) {
        if (btId == downArrow.getId()){
            sendBT("1");
        }
        else if (btId == upArrow.getId()){
            sendBT("2");
        }
        else if (btId == leftArrow.getId()){
            sendBT("3");
        }
        else if (btId == rightArrow.getId()){
            sendBT("4");
        }
        else if (btId == stopSignal.getId()){
            sendBT("5");
        }
    }
    private void sendBT(String code) {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(code.getBytes());
            }
            catch (IOException e)
            {
                toastMsg("Error al enviar la orden a Arduino.");
            }
        }
    }
    public void activarBluetooth(View view) {
        GradientDrawable myGrad = (GradientDrawable)activarBluetooth.getBackground();
        if (_pilotoAutomatico) {
            _pilotoAutomatico = false;
            setDefaultColors();
            myGrad.setStroke(5, Color.BLACK);
            activarBluetooth.setTextColor(Color.BLACK);
            sendBT("5");
        }
        else {
            _pilotoAutomatico = true;
            myGrad.setStroke(5, Color.GREEN);
            activarBluetooth.setTextColor(Color.GREEN);
            sendBT("6");
        }
    }
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(Botones.this, "Conectando...", "Por favor, espere.");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                toastMsg("Conexión fallida. ¿Estás seguro que el dispositivo es un Arduino?");
                finish();
            }
            else
            {
                toastMsg("Conexión exitosa.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    //Voz
    private void onSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,10);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,10);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,10);
        sr.startListening(intent);
        Log.i("SpeechRecognizer","New Input");
    }
    private class listener implements RecognitionListener {
        private static final String TAG = "MyStt3Activity";
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, DateFormat.getDateTimeInstance().format(new Date()) + "onReadyForSpeech");
        }
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, DateFormat.getDateTimeInstance().format(new Date()) + "onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB)
        {
            //Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, DateFormat.getDateTimeInstance().format(new Date()) + "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            Log.d(TAG, DateFormat.getDateTimeInstance().format(new Date()) + "onEndofSpeech");
        }
        public void onError(int error)
        {
            Log.d(TAG,  "error " +  error);
            toastMsg("Hubo un error. Vuelva a intentarlo.");
        }
        public void onResults(Bundle results)
        {
            Log.d(TAG, "onResults " + results);

            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                Log.d(TAG, "result " + data.get(i));
            }
            if (data.contains("adelante"))
                setActiveArrow(upArrow);
            else if (data.contains("atrás"))
                setActiveArrow(downArrow);
            else if (data.contains("derecha"))
                setActiveArrow(rightArrow);
            else if (data.contains("izquierda"))
                setActiveArrow(leftArrow);
            else if (data.contains("parar") || data.contains("stop"))
                setActiveArrow(stopSignal);
            else
                toastMsg("No se reconoce el comando. Vuelva a intentarlo.");
            Log.d(TAG, DateFormat.getDateTimeInstance().format(new Date()) + "FlechaActiva");
        }
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, DateFormat.getDateTimeInstance().format(new Date()) + "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
        }
    }
    private void checkRecordAudioPermission(){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    //Acelerómetro
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }
    public void onSensorChanged(SensorEvent event) {
        if (_aceleremotroActivado && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            if (event.values[0] > 2)
            {
                setActiveArrow(leftArrow);
                _leftActive = true;
            }
            else if (event.values[0] < -2)
            {
                setActiveArrow(rightArrow);
                _rightActive = true;

            }
            else if (event.values[1] > 2)
            {
                setActiveArrow(downArrow);
                _downActive = true;
            }
            else if (event.values[1] < -2)
            {
                setActiveArrow(upArrow);
                _upActive = true;
            }
            else
            {
                //setActiveArrow(stopSignal);
                if (_rightActive)
                {
                    setButtonColor(rightArrow, DEFAULT_BUTTON_COLOR);
                    _rightActive = false;
                }
                if (_leftActive)
                {
                    setButtonColor(leftArrow, DEFAULT_BUTTON_COLOR);
                    _leftActive = false;
                }
                if (_upActive)
                {
                    setButtonColor(upArrow, DEFAULT_BUTTON_COLOR);
                    _upActive = false;
                }
                if (_downActive)
                {
                    setButtonColor(downArrow, DEFAULT_BUTTON_COLOR);
                    _downActive = false;
                }
            }
        }
    }
    public void activarAcelerometro(View view) {
        GradientDrawable myGrad = (GradientDrawable)activarAcelerometro.getBackground();
        if (_aceleremotroActivado) {
            _aceleremotroActivado = false;
            setDefaultColors();
            myGrad.setStroke(5, Color.BLACK);
            activarAcelerometro.setTextColor(Color.BLACK);
        }
        else {
            _aceleremotroActivado = true;
            myGrad.setStroke(5, Color.GREEN);
            activarAcelerometro.setTextColor(Color.GREEN);
        }
    }
}


