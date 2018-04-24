package com.example.rem.ecgauth;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

public class MainActivity extends Activity implements View.OnClickListener{

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        if (Bluetooth.connectedThread != null) {
            Bluetooth.connectedThread.write("Q");}//Stop streaming
        super.onBackPressed();
    }

    //toggle Button
    static boolean Lock;//whether lock the x-axis to 0-5
    static boolean AutoScrollX;//auto scroll to the last x value
    static boolean Stream;//Start or stop streaming
    int old_interval=0;
    int new_interval=0;
    int mean_interval=20;
    //Button init
    Button bXminus;
    Button bXplus;
    ToggleButton tbLock;
    ToggleButton tbScroll;
    ToggleButton tbStream;
    //GraphView init
    static LinearLayout GraphView;
    static GraphView graphView;
    static GraphViewSeries Series;
    //graph value
    private static double graph2LastXValue = 0;
    private static int Xview=10;
    Button bConnect, bDisconnect;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch(msg.what){
                case Bluetooth.SUCCESS_CONNECT:
                    Bluetooth.connectedThread = new Bluetooth.ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(getApplicationContext(), "Connected!", 0).show();
                    String s = "successfully connected";
                    Bluetooth.connectedThread.start();
                    break;
                case Bluetooth.MESSAGE_READ:
                    if(tbStream.isChecked()){
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, 5);                 // create string from bytes array
                        String bpm=new String(readBuf, 0, 80);
                        String ibi=new String(readBuf, 0, 80);

                        Log.d("strIncom", strIncom);

                        if (strIncom.indexOf('.')==2 && strIncom.indexOf('s')==0){
                            strIncom = strIncom.replace("s", "");
                            if (isFloatNumber(strIncom)){
                                Series.appendData(new GraphViewData(graph2LastXValue,Double.parseDouble(strIncom)),AutoScrollX);
                                //X-axis control
                                if (graph2LastXValue >= Xview && Lock == true){
                                    Series.resetData(new GraphViewData[] {});
                                    graph2LastXValue = 0;
                                }else graph2LastXValue += 0.1;

                                if(Lock == true)
                                    graphView.setViewPort(0, Xview);
                                else
                                    graphView.setViewPort(graph2LastXValue-Xview, Xview);

                                //refresh
                                GraphView.removeView(graphView);
                                GraphView.addView(graphView);

                            }
                        }

                        int temp1,temp2;
                        temp1=bpm.indexOf('b');
                        temp2=bpm.indexOf(',');
                        if(temp1>=0 && temp2>=temp1){
                            bpm = bpm.substring(temp1+1,temp2);
                            TextView BPM = (TextView) findViewById (R.id.BPM);
                            BPM.setText(bpm);
                            BPM.setBackgroundResource(R.drawable.heartbeat);
                        }else{
                            TextView BPM = (TextView) findViewById (R.id.BPM);
                            BPM.setBackgroundResource(R.drawable.heartbeat2);
                        }
                        temp1=ibi.indexOf('i');
                        temp2=ibi.indexOf('e');

                        if(temp1>=0 && temp2>=temp1){
                            ibi = ibi.substring(temp1+1,temp2);
                            old_interval=new_interval;
                            new_interval=Integer.parseInt(ibi);
                            TextView IBI = (TextView) findViewById (R.id.IBI);
                            IBI.setText(ibi);
                            int timeDifference= java.lang.Math.abs(old_interval-new_interval);
                            mean_interval=(mean_interval+timeDifference)/2;
                            if(timeDifference>(mean_interval+200)){
                                TextView arrythmia = (TextView) findViewById (R.id.arrythmiaStatus);
                                arrythmia.setText("TRUE");
                                arrythmia.setBackgroundResource(R.color.white);

                            }else{
                                TextView arrythmia = (TextView) findViewById (R.id.arrythmiaStatus);
                                arrythmia.setText("FALSE"); //String.valueOf(mean_interval)
                                arrythmia.setBackgroundResource(R.color.black);
                            }
                            mean_interval=(mean_interval+timeDifference)/2;
                        }
                    }


                    break;
            }
        }

        public boolean isFloatNumber(String num){
            //Log.d("checkfloatNum", num);
            try{
                Double.parseDouble(num);
            } catch(NumberFormatException nfe) {
                return false;
            }
            return true;
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//Hide title
        this.getWindow().setFlags(WindowManager.LayoutParams.
                FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);//Hide Status bar
        setContentView(R.layout.activity_main);
        //set background color
        LinearLayout background = (LinearLayout)findViewById(R.id.bg);
        background.setBackgroundColor(Color.BLACK);
        init();
        ButtonInit();
    }

    void init(){
        Bluetooth.gethandler(mHandler);

        //init graphview
        GraphView = (LinearLayout) findViewById(R.id.Graph);
        // init example series data-------------------
        Series = new GraphViewSeries("Signal",
                new GraphViewStyle(Color.YELLOW, 2),//color and thickness of the line
                new GraphViewData[] {new GraphViewData(0, 0)});
        graphView = new LineGraphView(
                this // context
                , "Electrocardiogram AUTH" // heading
        );
        graphView.setViewPort(0, Xview);
        graphView.setScrollable(true);
        graphView.setScalable(true);
        graphView.setShowLegend(true);
        graphView.setLegendAlign(LegendAlign.BOTTOM);
        graphView.setManualYAxis(true);
        graphView.setManualYAxisBounds(5, 0);
        graphView.addSeries(Series); // data
        GraphView.addView(graphView);
    }

    void ButtonInit(){
        bConnect = (Button)findViewById(R.id.bConnect);
        bConnect.setOnClickListener(this);
        bDisconnect = (Button)findViewById(R.id.bDisconnect);
        bDisconnect.setOnClickListener(this);
        //X-axis control button
        bXminus = (Button)findViewById(R.id.bXminus);
        bXminus.setOnClickListener(this);
        bXplus = (Button)findViewById(R.id.bXplus);
        bXplus.setOnClickListener(this);
        //
        tbLock = (ToggleButton)findViewById(R.id.tbLock);
        tbLock.setOnClickListener(this);
        tbScroll = (ToggleButton)findViewById(R.id.tbScroll);
        tbScroll.setOnClickListener(this);
        tbStream = (ToggleButton)findViewById(R.id.tbStream);
        tbStream.setOnClickListener(this);
        //init toggleButton
        Lock=true;
        AutoScrollX=true;
        Stream=true;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
            case R.id.bConnect:
                startActivity(new Intent("android.intent.action.BT1"));
                break;
            case R.id.bDisconnect:
                Bluetooth.disconnect();
                break;
            case R.id.bXminus:
                if (Xview>1) Xview--;
                break;
            case R.id.bXplus:
                if (Xview<30) Xview++;
                break;
            case R.id.tbLock:
                if (tbLock.isChecked()){
                    Lock = true;
                }else{
                    Lock = false;
                }
                break;
            case R.id.tbScroll:
                if (tbScroll.isChecked()){
                    AutoScrollX = true;
                }else{
                    AutoScrollX = false;
                }
                break;
            //case R.id.tbStream:
//                if (tbStream.isChecked()){
//                    if (Bluetooth.connectedThread != null)
//                        Bluetooth.connectedThread.write("E");
//                }else{
//                    if (Bluetooth.connectedThread != null)
//                        Bluetooth.connectedThread.write("Q");
//                }
                //break;
        }
    }


}