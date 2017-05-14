package com.tayjiaxi.bletrial1.bletrial1;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.GridLabelRenderer;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Locale;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.io.FileWriter;

import com.opencsv.CSVWriter;

import static com.opencsv.CSVWriter.DEFAULT_SEPARATOR;
import static com.opencsv.CSVWriter.NO_QUOTE_CHARACTER;

public class SimpleControls extends Activity {
    private final static String TAG = SimpleControls.class.getSimpleName();

    private long starttime;
    private long endtime;
    private Button connectBtn = null;
    private Button graphBtn = null;
    private Button resetBtn = null;
    private Button exportBtn = null;
    private TextView rssiValue = null;
    private TextView connTime = null;
    private TextView AnalogInValue1 = null;
    private TextView AnalogInValue2 = null;
    private TextView AnalogInValue3 = null;
    private TextView AnalogInValue4 = null;
    private TextView AnalogInValue5 = null;
    private TextView AnalogInValue6 = null;
    private TextView AnalogInValue7 = null;
    private int ime = 0;
    public double pin4 = 0;
    public double pin5 = 0;
    public int grapher = 0;
    public int graphcounter = 0;

    private BluetoothGattCharacteristic characteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean connState = false;
    private boolean scanFlag = false;

    private byte[] data = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;
    private static double GAUGE_FACTOR = 2.13;

    private BluetoothAdapter btAdapter = null;
    public static String anaddress = "C7:29:BB:BF:E3:24";//another address of ble

    final private static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    //add PointsGraphSeries of DataPoint type
    LineGraphSeries<DataPoint> xySeriesL;
    GraphView mScatterPlot;
    //make xyValueArray global
    public ArrayList<XYValue> xyValueArray;
    public List<String[]> database = new ArrayList<String[]>();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
                setButtonDisable();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();

                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

                readAnalogInValue(data);
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

        rssiValue = (TextView) findViewById(R.id.rssiValue);
        connTime = (TextView) findViewById(R.id.connTime);

        AnalogInValue1 = (TextView) findViewById(R.id.AIText1);
        AnalogInValue2 = (TextView) findViewById(R.id.AIText2);
        AnalogInValue3 = (TextView) findViewById(R.id.AIText3);
        AnalogInValue4 = (TextView) findViewById(R.id.AIText4);
        AnalogInValue5 = (TextView) findViewById(R.id.AIText5);
        AnalogInValue6 = (TextView) findViewById(R.id.AIText6);
        AnalogInValue7 = (TextView) findViewById(R.id.AIText7);

        //declare variables in oncreate
        mScatterPlot = (GraphView) findViewById(R.id.scatterPlot);
        xyValueArray = new ArrayList<>();
        database.add(new String[] {"No.", "Strain"});
        connectBtn = (Button) findViewById(R.id.connect);
        connectBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                starttime = android.os.SystemClock.uptimeMillis();
                mBluetoothLeService.connect(anaddress);
                /*if (scanFlag == false) {
					scanLeDevice();

					Timer mTimer = new Timer();
					mTimer.schedule(new TimerTask() {

						@Override
						public void run() {
							if (mDevice != null) {
								mBluetoothLeService.connect(mDeviceAddress);
								scanFlag = true;
							} else {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast toast = Toast
												.makeText(
														SimpleControls.this,
														"Couldn't search Ble Shield device!",
														Toast.LENGTH_SHORT);
										toast.setGravity(0, 0, Gravity.CENTER);
										toast.show();
									}
								});
							}
						}
					}, SCAN_PERIOD);
				}*/

                System.out.println(connState);
                if (connState == false) {
                    //mBluetoothLeService.connect(mDeviceAddress);
                    mBluetoothLeService.connect(anaddress); //testcode
                } else {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                    setButtonDisable();
                }

            }
        });
        graphBtn = (Button) findViewById(R.id.graph);
        graphBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (grapher == 1) {
                    grapher = 0;
                } else {
                    grapher = 1;
                }
            }
        });
        resetBtn = (Button) findViewById(R.id.reset);
        resetBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v2) {

                rssiValue.setText(null);
                connTime.setText(null);
                AnalogInValue1.setText(null);
                AnalogInValue2.setText(null);
                AnalogInValue3.setText(null);
                AnalogInValue4.setText(null);
                AnalogInValue5.setText(null);
                AnalogInValue6.setText(null);
                AnalogInValue7.setText(null);
                grapher = 0;
                graphcounter = 0;
                ime = 0;
                mScatterPlot.removeAllSeries();
                xyValueArray.clear();
                database.clear();
                Log.d(TAG, "Reset Button Pressed");

            }
        });

        exportBtn = (Button) findViewById(R.id.export);
        exportBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v3) {
                CSVWriter writer = null;
                try {
                    writer = new CSVWriter(new FileWriter("/sdcard/RBL/data.csv"), DEFAULT_SEPARATOR, NO_QUOTE_CHARACTER);
                    writer.writeAll(database);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //bluetooth part
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(SimpleControls.this,
                RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private void displayData(String data) {
        if (data != null) {
            rssiValue.setText(data);
            connTime.setText(String.format("%d ms", endtime - starttime));
        }
    }

    private void readAnalogInValue(byte[] data) {

        for (int i = 0; i < data.length; i += 3) {
            ime++;
            //for Pin 4
            if (data[i] == 0x0B) {
                int Value;
                Value = ((data[i + 1] << 8) & 0x0000ff00)
                        | (data[i + 2] & 0x000000ff);
                pin4 = (Value - 4) * 3.3 / 1024;
                AnalogInValue1.setText(String.valueOf(Value));
                AnalogInValue3.setText(String.format("%.4f", pin4));

            }

            //for Pin 5
            if (data[i] == 0x0C) {
                int Value;
                Value = ((data[i + 1] << 8) & 0x0000ff00)
                        | (data[i + 2] & 0x000000ff);
                pin5 = (Value - 3) * 3.3 / 1024;
                AnalogInValue2.setText(String.valueOf(Value));
                AnalogInValue4.setText(String.format("%.4f", pin5));
            }
            double PD = (pin5 - pin4);
            AnalogInValue5.setText(String.format("%.4f", PD));

            //calculation of strain for the device is = -Vr/GF
            //Vr = (Vout/Vin)strained - (Vout/Vin)unstrained
            //Define unstrained potential difference for Vr as 0 for now. Subject to callibration
            //Define Vin as 3.3v since that is the Vdd voltage
            double strain = -((PD / 3.3) / GAUGE_FACTOR);
            AnalogInValue6.setText(String.format("%.4f", strain));
            AnalogInValue7.setText(String.valueOf(ime));
            database.add(new String[] {String.valueOf(ime), String.valueOf(strain)});

            if (grapher == 1) {
                //declare the xySeries Object
                xySeriesL = new LineGraphSeries<>();
                double x = graphcounter;
                double y = strain;
                Log.d(TAG, "onClick: Adding a new point. (x,y): (" + x + "," + y + ")");
                xyValueArray.add(new XYValue(x, y));

                //little bit of exception handling for if there is no data.
                if (xyValueArray.size() != 0) {
                    createScatterPlot();
                    graphcounter++;
                } else {
                    Log.d(TAG, "onCreate: No data to plot.");

                }
            }
        }
    }


    private void setButtonEnable() {
        flag = true;
        connState = true;
        connectBtn.setText("Disconnect");
    }

    private void setButtonDisable() {
        flag = false;
        connState = false;
        connectBtn.setText("Connect");
    }

    private void startReadRssi() {
        new Thread() {
            public void run() {
                endtime = android.os.SystemClock.uptimeMillis();
                Log.d("TAG", "Execution time: " + (endtime - starttime) + " ms");

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }.start();
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        setButtonEnable();
        startReadRssi();

        characteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device != null) {
                        mDevice = device;
                    } else {
                        mDevice = btAdapter.getRemoteDevice(anaddress);
                    }
                }
            });
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }

    @Override
    protected void onStop() {
        super.onStop();

        flag = false;

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null)
            unbindService(mServiceConnection);
    }


    private void createScatterPlot() {

        //set some properties
        xySeriesL.setThickness(5);
        xySeriesL.setColor(Color.RED);
        xySeriesL.setDrawDataPoints(true);
        xySeriesL.setDataPointsRadius(5);
        xySeriesL.setTitle("Line Chart of Strain");

        //set Scrollable and Scaleable
        mScatterPlot.getViewport().setScalable(true);
        mScatterPlot.getViewport().setScalableY(true);
        mScatterPlot.getViewport().setScrollable(true);
        mScatterPlot.getViewport().setScrollableY(true);

        //set manual y bounds
        mScatterPlot.getViewport().setYAxisBoundsManual(true);
        mScatterPlot.getViewport().setMaxY(0.005);
        mScatterPlot.getViewport().setMinY(-0.005);

        //set manual x bounds
        mScatterPlot.getViewport().setXAxisBoundsManual(true);
        mScatterPlot.getViewport().setMaxX(20);
        mScatterPlot.getViewport().setMinX(0);

        mScatterPlot.getGridLabelRenderer().setLabelVerticalWidth(100);
        mScatterPlot.addSeries(xySeriesL);

        Log.d(TAG, "createScatterPlot: Creating scatter plot.");

        //add the data to the series
        for (int i = 0; i < xyValueArray.size(); i++) {
            try {
                double x = xyValueArray.get(i).getX();
                double y = xyValueArray.get(i).getY();
                xySeriesL.appendData(new DataPoint(x, y), true, 20);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "createScatterPlot: IllegalArgumentException: " + e.getMessage());
            }
        }

    }
}


