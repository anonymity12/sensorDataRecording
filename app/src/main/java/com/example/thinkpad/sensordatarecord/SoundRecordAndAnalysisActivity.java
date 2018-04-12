package com.example.thinkpad.sensordatarecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.thinkpad.sensordatarecord.fftpack.RealDoubleFFT;


public class SoundRecordAndAnalysisActivity extends Activity implements OnClickListener{
    private static final String TAG = "OhMyHz";
    private static final double[] CANCELLED = {100};
    int frequency = 8000;/*44100;*/
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    AudioRecord audioRecord;
    private RealDoubleFFT transformer;
    int blockSize = /*2048;// = */256;
    Button startStopButton;
    boolean started = false;
    boolean CANCELLED_FLAG = false;
    double[][] cancelledResult = {{100}};
    int mPeakPos;
    double mHighestFreq;
    RecordAudio recordTask;
    ImageView imageViewDisplaySectrum;
    MyImageView imageViewScale;
    Bitmap bitmapDisplaySpectrum;

    Canvas canvasDisplaySpectrum;

    Paint paintSpectrumDisplay;
    Paint paintScaleDisplay;
    static SoundRecordAndAnalysisActivity mainActivity;
    LinearLayout main;
    int width;
    int height;
    int left_Of_BimapScale;
    int left_Of_DisplaySpectrum;
    private final static int ID_BITMAPDISPLAYSPECTRUM = 1;
    private final static int ID_IMAGEVIEWSCALE = 2;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display = getWindowManager().getDefaultDisplay();
        //Point size = new Point();
        //display.get(size);
        width = display.getWidth();
        height = display.getHeight();

        //blockSize = 256;



    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        //left_Of_BimapScale = main.getC.getLeft();
        MyImageView  scale = (MyImageView)main.findViewById(R.id.image_view_scale/*ID_IMAGEVIEWSCALE*/);
        ImageView bitmap = (ImageView)main.findViewById(R.id.spectrum_display);
        left_Of_BimapScale = scale.getLeft();
        left_Of_DisplaySpectrum = bitmap.getLeft();
    }
    private class RecordAudio extends AsyncTask<Void, double[], Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            int bufferSize = AudioRecord.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, frequency,/*sample rate in Hz, here 8000*/
                    channelConfiguration, audioEncoding, bufferSize);
            int bufferReadResult;
            short[] buffer = new short[blockSize];//tt: 256
            double[] toTransform = new double[blockSize];
            try {
                audioRecord.startRecording();
            } catch (IllegalStateException e) {
                Log.e("Recording failed", e.toString());

            }
            while (started) {

                if (isCancelled() || (CANCELLED_FLAG == true)) {
                    //stop this background task
                    started = false;
                    publishProgress(cancelledResult);
                    Log.d("doInBackground", "Cancelling the RecordTask");
                    break;
                } else {
                    //goon process the audioRecord object
                    bufferReadResult = audioRecord.read(buffer, 0, blockSize);//tt: 256
                    //tt: will show u in file end [2] this output
                    for (int c = 45; c < 200; c ++) {
                        Log.e(TAG, "doInBackground: buffer = " + buffer[c]);
                    }
                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit ；32768 = 1后15个0
                        Log.d(TAG, "before transformation>>> toTransform[i] : " + toTransform[i]);//tt: give u the whole three data sample output at file end[3]
                    }

                    transformer.ft(toTransform);
                    for (int c = 45; c < 200; c ++) {
                        Log.d(TAG, "after transformation>>> toTransform[i] : " + toTransform[c]);//tt: give u the sample output at file end[1]
                    }

                    publishProgress(toTransform);

                }

            }
            //tt: this return called when use click stop, the detail is :
            //tt: when click happen, variable:started = false, then the above while loop dead,
            //tt: so here return
            return true;
        }
        @Override
        protected void onProgressUpdate(double[]...progress) {
            Log.e("RecordingProgress", "Displaying in progress");
            double mMaxFFTSample = 150.0;

            Log.d("Test:", Integer.toString(progress[0].length));
            //tt: pass the cancelResult
            if(progress[0].length == 1 ){

                Log.d("FFTSpectrumAnalyzer", "onProgressUpdate: Blackening the screen");
                canvasDisplaySpectrum.drawColor(Color.BLACK);
                imageViewDisplaySectrum.invalidate();

            }
            //tt: pass the useful sound result
            else {
                //tt: when you're using newest good devices.
                if (width > 512) {
                    for (int i = 0; i < progress[0].length; i++) {/*length always is 256*/
                        int x = 2 * i;
                        int downy = (int) (150 - (progress[0][i] * 10));//tt: value around +-3.xxx, 0.xxx,+-7.cxx
                        int upy = 150;
                        if(downy < mMaxFFTSample)
                        {
                            mMaxFFTSample = downy;//tt: wtf? you are choosing the min, not max; tt: well you are right, downy min means this data is large
                            //mMag = mMaxFFTSample;
                            mPeakPos = i;
                        }

                        canvasDisplaySpectrum.drawLine(x, downy, x, upy, paintSpectrumDisplay);//tt: guess you are drawing on the bitmapDisplaySpectrum which is inside of canvasDisplaySpectrum
                    }

                    imageViewDisplaySectrum.invalidate();
                } else {
                //tt: when you use old small screen devices, guess my App on Samsung never enter this logic
                    for (int i = 0; i < progress[0].length; i++) {
                        int x = i;
                        int downy = (int) (150 - (progress[0][i] * 10));
                        int upy = 150;
                        if(downy < mMaxFFTSample)
                        {
                            mMaxFFTSample = downy;
                            //mMag = mMaxFFTSample;
                            mPeakPos = i;
                        }
                        canvasDisplaySpectrum.drawLine(x, downy, x, upy, paintSpectrumDisplay);
                    }


                    imageViewDisplaySectrum.invalidate();
                }
            }


        }
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            try{
                audioRecord.stop();
            }
            catch(IllegalStateException e){
                Log.e("Stop failed", e.toString());
            }

            canvasDisplaySpectrum.drawColor(Color.BLACK);
            imageViewDisplaySectrum.invalidate();
               /* mHighestFreq = (((1.0 * frequency) / (1.0 * blockSize)) * mPeakPos)/2;
                String str = "Frequency for Highest amplitude: " + mHighestFreq;
                Toast.makeText(getApplicationContext(), str , Toast.LENGTH_LONG).show();*/

        }
    }

    protected void onCancelled(Boolean result){

        try{
            audioRecord.stop();
        }
        catch(IllegalStateException e){
            Log.e("Stop failed", e.toString());

        }
        //recordTask.cancel(true);

        Log.d("FFTSpectrumAnalyzer","onCancelled: New Screen");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    public void onClick(View v) {
        if (started == true) {
            //tt: to stop the process
            //started = false;
            CANCELLED_FLAG = true;
            //recordTask.cancel(true);
            try{
                audioRecord.stop();
            }
            catch(IllegalStateException e){
                Log.e("Stop failed", e.toString());

            }
            startStopButton.setText("Start");
            //show the frequency that has the highest amplitude...
            mHighestFreq = (((1.0 * frequency) / (1.0 * blockSize)) * mPeakPos)/2;
            String str = "Frequency for Highest amplitude: " + mHighestFreq;
            Toast.makeText(getApplicationContext(), str , Toast.LENGTH_LONG).show();

            canvasDisplaySpectrum.drawColor(Color.BLACK);

        }

        else {
            //tt: to start the process
            started = true;
            CANCELLED_FLAG = false;
            startStopButton.setText("Stop");
            recordTask = new RecordAudio();
            recordTask.execute();
        }

    }
    static SoundRecordAndAnalysisActivity getMainActivity(){

        return mainActivity;
    }

    public void onStop(){
        super.onStop();
        	/*started = false;
            startStopButton.setText("Start");*/
        //if(recordTask != null){
        recordTask.cancel(true);
        //}
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void onStart(){

        super.onStart();
        main = new LinearLayout(this);
        main.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        main.setOrientation(LinearLayout.VERTICAL);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        transformer = new RealDoubleFFT(blockSize);

        imageViewDisplaySectrum = new ImageView(this);
        if(width > 512){
            bitmapDisplaySpectrum = Bitmap.createBitmap((int)512,(int)300,Bitmap.Config.ARGB_8888);
        }
        else{
            bitmapDisplaySpectrum = Bitmap.createBitmap((int)256,(int)150,Bitmap.Config.ARGB_8888);
        }
        LinearLayout.LayoutParams layoutParams_imageViewScale = null;
        //Bitmap scaled = Bitmap.createScaledBitmap(bitmapDisplaySpectrum, 320, 480, true);
        canvasDisplaySpectrum = new Canvas(bitmapDisplaySpectrum);
        //canvasDisplaySpectrum = new Canvas(scaled);
        paintSpectrumDisplay = new Paint();
        paintSpectrumDisplay.setColor(Color.GREEN);
        imageViewDisplaySectrum.setImageBitmap(bitmapDisplaySpectrum);
        if(width >512){
            //imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ((MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(100, 600, 0, 0);
            imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);
            layoutParams_imageViewScale= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            //layoutParams_imageViewScale.gravity = Gravity.CENTER_HORIZONTAL;
            ((MarginLayoutParams) layoutParams_imageViewScale).setMargins(100, 20, 0, 0);

        }

        else if ((width >320) && (width<512)){
            LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ((MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(60, 250, 0, 0);
            //layoutParams_imageViewDisplaySpectrum.gravity = Gravity.CENTER_HORIZONTAL;
            imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);

            //imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            layoutParams_imageViewScale=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ((MarginLayoutParams) layoutParams_imageViewScale).setMargins(60, 20, 0, 100);
            //layoutParams_imageViewScale.gravity = Gravity.CENTER_HORIZONTAL;
        }

        else if (width < 320){
            	/*LinearLayout.LayoutParams layoutParams_imageViewDisplaySpectrum=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ((MarginLayoutParams) layoutParams_imageViewDisplaySpectrum).setMargins(30, 100, 0, 100);
                imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);*/
            imageViewDisplaySectrum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            layoutParams_imageViewScale=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            //layoutParams_imageViewScale.gravity = Gravity.CENTER;
        }
        imageViewDisplaySectrum.setId(R.id.spectrum_display);
        main.addView(imageViewDisplaySectrum);

        imageViewScale = new MyImageView(this);
        imageViewScale.setLayoutParams(layoutParams_imageViewScale);
        imageViewScale.setId(R.id.image_view_scale);

        //imageViewScale.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        main.addView(imageViewScale);

        startStopButton = new Button(this);
        startStopButton.setText("Start");
        startStopButton.setOnClickListener(this);
        startStopButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        main.addView(startStopButton);

        setContentView(main);

        mainActivity = this;

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //if(recordTask != null){
        recordTask.cancel(true);
        //}
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        recordTask.cancel(true);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    //Custom Imageview Class
    public class MyImageView extends android.support.v7.widget.AppCompatImageView {
        Paint paintScaleDisplay;
        Bitmap bitmapScale;
        //Canvas canvasScale;
        public MyImageView(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
            if(width >512){
                bitmapScale = Bitmap.createBitmap((int)512,(int)50,Bitmap.Config.ARGB_8888);
            }
            else{
                bitmapScale =  Bitmap.createBitmap((int)256,(int)50,Bitmap.Config.ARGB_8888);
            }

            paintScaleDisplay = new Paint();
            paintScaleDisplay.setColor(Color.WHITE);
            paintScaleDisplay.setStyle(Paint.Style.FILL);

            //canvasScale = new Canvas(bitmapScale);

            setImageBitmap(bitmapScale);
            invalidate();
        }
        @Override
        protected void onDraw(Canvas canvas)
        {
            // TODO Auto-generated method stub
            super.onDraw(canvas);

            if(width > 512){
                //canvasScale.drawLine(0, 30,  512, 30, paintScaleDisplay);
                canvas.drawLine(0, 30,  512, 30, paintScaleDisplay);
                for(int i = 0,j = 0; i< 512; i=i+128, j++){
                    for (int k = i; k<(i+128); k=k+16){
                        //canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                        canvas.drawLine(k, 30, k, 25, paintScaleDisplay);
                    }
                    //canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                    canvas.drawLine(i, 40, i, 25, paintScaleDisplay);
                    String text = Integer.toString(j) + " KHz";
                    //canvasScale.drawText(text, i, 45, paintScaleDisplay);
                    canvas.drawText(text, i, 45, paintScaleDisplay);
                }
                canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
            }
            else if ((width >320) && (width<512)){
                //canvasScale.drawLine(0, 30, 0 + 256, 30, paintScaleDisplay);
                canvas.drawLine(0, 30, 0 + 256, 30, paintScaleDisplay);
                for(int i = 0,j = 0; i<256; i=i+64, j++){
                    for (int k = i; k<(i+64); k=k+8){
                        //canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                        canvas.drawLine(k, 30, k, 25, paintScaleDisplay);
                    }
                    //canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                    canvas.drawLine(i, 40, i, 25, paintScaleDisplay);
                    String text = Integer.toString(j) + " KHz";
                    //canvasScale.drawText(text, i, 45, paintScaleDisplay);
                    canvas.drawText(text, i, 45, paintScaleDisplay);
                }
                canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
            }

            else if (width <320){
                //canvasScale.drawLine(0, 30,  256, 30, paintScaleDisplay);
                canvas.drawLine(0, 30,  256, 30, paintScaleDisplay);
                for(int i = 0,j = 0; i<256; i=i+64, j++){
                    for (int k = i; k<(i+64); k=k+8){
                        //canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
                        canvas.drawLine(k, 30, k, 25, paintScaleDisplay);
                    }
                    //canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
                    canvas.drawLine(i, 40, i, 25, paintScaleDisplay);
                    String text = Integer.toString(j) + " KHz";
                    //canvasScale.drawText(text, i, 45, paintScaleDisplay);
                    canvas.drawText(text, i, 45, paintScaleDisplay);
                }
                canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
            }
        }
    }
}


/*1. the sample toTransform[] output:
* toTransform[i] : 0.001800537109375
toTransform[i] : 0.0047607421875
toTransform[i] : 0.002899169921875
toTransform[i] : 0.003662109375
toTransform[i] : 0.006622314453125
toTransform[i] : 0.0
toTransform[i] : -0.00445556640625
toTransform[i] : -0.00335693359375
toTransform[i] : -0.00408935546875
toTransform[i] : -0.00372314453125
toTransform[i] : -0.00482177734375
toTransform[i] : -0.00778198242187
toTransform[i] : -0.01260375976562
toTransform[i] : -0.01446533203125
toTransform[i] : -0.0096435546875
toTransform[i] : -0.00741577148437
toTransform[i] : -0.00299072265625
toTransform[i] : -0.00149536132812
toTransform[i] : 0.00439453125
toTransform[i] : 0.0040283203125
toTransform[i] : 0.001434326171875
toTransform[i] : 0.005126953125
toTransform[i] : 0.007720947265625
toTransform[i] : 0.006988525390625
toTransform[i] : 0.006256103515625
toTransform[i] : 0.005523681640625
toTransform[i] : 0.005889892578125
toTransform[i] : 0.007720947265625
toTransform[i] : 0.006988525390625
toTransform[i] : 0.006988525390625
toTransform[i] : 0.00811767578125
toTransform[i] : 0.006988525390625
toTransform[i] : 0.0040283203125
toTransform[i] : 0.001068115234375
toTransform[i] : -0.00299072265625
toTransform[i] : -0.00186157226562 equals 60.99999 in buffer[i]
toTransform[i] : -7.62939453125E-4 equals -250000 in buffer[]
toTransform[i] : -0.00521850585937
toTransform[i] : -0.0096435546875
toTransform[i] : -0.01077270507812
toTransform[i] : -0.01187133789062
toTransform[i] : -0.008544921875
toTransform[i] : -0.00558471679687
toTransform[i] : -3.96728515625E-4
toTransform[i] : -0.00149536132812
toTransform[i] : -3.96728515625E-4
toTransform[i] : -0.00372314453125
toTransform[i] : -0.00521850585937
toTransform[i] : -0.00668334960937
toTransform[i] : -0.00704956054687
toTransform[i] : -0.00299072265625
toTransform[i] : -0.00335693359375
toTransform[i] : -0.0074462890625
toTransform[i] : -0.0074462890625
toTransform[i] : -0.0074462890625
toTransform[i] : -0.008544921875
toTransform[i] : -0.007080078125
toTransform[i] : -0.01077270507812
toTransform[i] : -0.01004028320312
toTransform[i] : -0.00631713867187
toTransform[i] : -0.00149536132812
toTransform[i] : -7.62939453125E-4
toTransform[i] : -0.00372314453125
toTransform[i] : -3.96728515625E-4
toTransform[i] : 0.009246826171875
* */

/*[2] the log output buffer array elements
*
* doInBackground: buffer = 197
doInBackground: buffer = 154
doInBackground: buffer = 129
doInBackground: buffer = 0
doInBackground: buffer = -137
doInBackground: buffer = -261
doInBackground: buffer = -292
doInBackground: buffer = -230
doInBackground: buffer = -174
doInBackground: buffer = -81
doInBackground: buffer = 5
doInBackground: buffer = 73
doInBackground: buffer = 104
doInBackground: buffer = 178
doInBackground: buffer = 178
doInBackground: buffer = 85
doInBackground: buffer = 85
doInBackground: buffer = 92
doInBackground: buffer = 42
doInBackground: buffer = 48
doInBackground: buffer = 17
doInBackground: buffer = 30
doInBackground: buffer = 0
doInBackground: buffer = 11
doInBackground: buffer = 23
doInBackground: buffer = 11
doInBackground: buffer = 42
doInBackground: buffer = 104
doInBackground: buffer = 104
doInBackground: buffer = 48
* */


/*[3] output of buffer[], toTransform[] array before transform and after transform
  buffer = 424
 buffer = 448
 buffer = 99
 buffer = -325
 buffer = -587
 buffer = 149
 buffer = 174
 buffer = 149
 buffer = -88
 buffer = 187
 buffer = -63
 buffer = -948
 buffer = 124
 buffer = 261
 buffer = 137
 buffer = -100
 buffer = -362
 buffer = 62
 buffer = -50
 buffer = 548
 buffer = 523
 buffer = 511
 buffer = 411
 buffer = -187
 buffer = 112
 buffer = 99
 buffer = 423
 buffer = 286

 before transformation>>> toTransform[i] : 0.007232666015625
before transformation>>> toTransform[i] : 0.008392333984375
before transformation>>> toTransform[i] : -0.002685546875
before transformation>>> toTransform[i] : -0.0172119140625
before transformation>>> toTransform[i] : -0.00192260742187
before transformation>>> toTransform[i] : -0.00726318359375
before transformation>>> toTransform[i] : 0.02099609375
before transformation>>> toTransform[i] : 0.018707275390625
before transformation>>> toTransform[i] : 0.014129638671875
before transformation>>> toTransform[i] : -0.0084228515625
before transformation>>> toTransform[i] : -0.0267333984375
before transformation>>> toTransform[i] : -0.01300048828125
before transformation>>> toTransform[i] : 0.001129150390625
before transformation>>> toTransform[i] : 0.0125732421875
before transformation>>> toTransform[i] : 0.005340576171875
before transformation>>> toTransform[i] : -0.01376342773437
before transformation>>> toTransform[i] : -0.00955200195312
before transformation>>> toTransform[i] : -0.00537109375
before transformation>>> toTransform[i] : 0.005706787109375
before transformation>>> toTransform[i] : 0.009521484375
before transformation>>> toTransform[i] : 0.005340576171875
before transformation>>> toTransform[i] : -0.012939453125
before transformation>>> toTransform[i] : 0.009490966796875
before transformation>>> toTransform[i] : 0.007598876953125
before transformation>>> toTransform[i] : 0.009490966796875
before transformation>>> toTransform[i] : 0.007598876953125
before transformation>>> toTransform[i] : -0.012542724609375
after transformation>>> toTransform[i] : 0.016386890561793946
after transformation>>> toTransform[i] : 0.1607625504974015
after transformation>>> toTransform[i] : 0.08626160437487893
after transformation>>> toTransform[i] : 0.14437323117620687
after transformation>>> toTransform[i] : -0.031739745315146256
after transformation>>> toTransform[i] : 0.15486572525587958
after transformation>>> toTransform[i] : -0.07813975632883836
after transformation>>> toTransform[i] : 0.14394773134503866
after transformation>>> toTransform[i] : -0.08071344454804791
after transformation>>> toTransform[i] : 0.04882324437129382
after transformation>>> toTransform[i] : 0.0503348380063424
after transformation>>> toTransform[i] : 0.04506187935715268
after transformation>>> toTransform[i] : -0.07841651273061731
after transformation>>> toTransform[i] : 0.13125279444369248
after transformation>>> toTransform[i] : -0.017625561761860767
after transformation>>> toTransform[i] : -0.003563848409531379
after transformation>>> toTransform[i] : -0.025323431958499686
after transformation>>> toTransform[i] : 0.13167737431919949
after transformation>>> toTransform[i] : -0.08378717769444652
after transformation>>> toTransform[i] : 0.08303657076984344
after transformation>>> toTransform[i] : -0.08545103376156024
after transformation>>> toTransform[i] : 0.07368822339365252
after transformation>>> toTransform[i] : -0.0052192942199127314
after transformation>>> toTransform[i] : 0.1126984748667631
after transformation>>> toTransform[i] : -0.21438374714644456
after transformation>>> toTransform[i] : 0.3774270334452147
after transformation>>> toTransform[i] : 0.465166019567587
after transformation>>> toTransform[i] : -0.07745855043443067
after transformation>>> toTransform[i] : 0.03845399087974697
*
* */

