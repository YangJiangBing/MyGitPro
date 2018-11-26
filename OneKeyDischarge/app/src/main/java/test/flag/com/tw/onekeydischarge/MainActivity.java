package test.flag.com.tw.onekeydischarge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Camera;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import java.lang.reflect.Field;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //phone battery info variable
    private TextView testViewLevel = null;
    private int batteryLevel;
    private int batteryScale;
    private Button buttonBatteryShow;
    private Button buttonStop;
    private Button buttonExitApp;

    //phone temperature info variable
    private SensorManager sensorManager=null;
    private Sensor sensor=null;
    private TextView textViewSensor = null;

    //phone bluetooth info variable
    private BluetoothAdapter bluetoothAdapter;

    //phone Lighting info variable
    private CameraManager manager;
    private Camera camera = null;
    private static boolean LightSwitch = true;

    //checkBox info variable
    private int[] checkId = {R.id.chk1,R.id.chk2,R.id.chk3,R.id.chk4,R.id.chk5,R.id.chk6};
    private CheckBox chk = null;

    //phone vibrate variable
    Vibrator vb = null;

    //toast variable
    Toast toastHint = null;

    //gps variable
    public LocationManager mlocationManager ;
    public double latitude = 0.0;
    public double longitude = 0.0;
    private static final String TAG = MainActivity.class.getSimpleName();

    //seekbar variable
    private SeekBar seekBar;
    private TextView textViewTarBat;
    public static int LeftBattery = 0;

    //mode chance variable
    Spinner spinnerMode;

    //wifi variable;
    public WifiManager wifiManager;

    //create main task
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //set main activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get system sensor service object
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //get temperature sensor object
        sensor = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        textViewSensor = (TextView) findViewById(R.id.textViewSensor);

        //get battery object
        testViewLevel = (TextView) findViewById(R.id.textViewBattery);
        buttonBatteryShow = (Button) findViewById(R.id.button_show_battery);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonExitApp = (Button) findViewById(R.id.buttonExitApp);

        //get System camera service object
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        //get vibrate service object
        vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //get toast object
        toastHint = Toast.makeText(this,"",Toast.LENGTH_SHORT);

        //set seekbar object
        seekBar = (SeekBar) findViewById(R.id.seekBarLowBat);
        textViewTarBat = (TextView) findViewById(R.id.textViewTarBat);
        textViewTarBat.setText("目标电量："+"0%");
        seekBar.setMax(100);
        seekBar.setProgress(1);
        seekBar.setOnSeekBarChangeListener(sbl);

        //get mode object
        spinnerMode = (Spinner) findViewById(R.id.spinnerMode);

        //get wifi object
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        //get gps object
        mlocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

//        if (mlocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            getLocation();
//            //gps已打开
//        } else {
////            toggleGPS();
////            new Handler() {}.postDelayed(new Runnable() {
////                @Override
////                public void run() {
////                    getLocation();
////                }
////            }, 2000);
//
//        }

        //Exit app key
        buttonExitApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });

        //set stop key listener event
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                closeHardwareDevice();
            }
        });

        //set start key listener event
        buttonBatteryShow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) //single click handle
            {
                int count = 0;
                //Listen alertDialog button is positive or negative
                for (int i:checkId)
                {
                    chk = (CheckBox) findViewById(i); //get checkbox object

                    if (chk.isChecked())
                    {
                        count ++;
                    }
                }

                if (count > 0)
                {
                    count = 0;
                    alertDialog();

                }
                else
                {
                    toastHint.setText("请选择相应的放电设备！！！");
                    toastHint.show();
                }

                //注册温度传感器监听事件
                sensorManager.registerListener(sensorEventListener,sensor,sensorManager.SENSOR_DELAY_NORMAL);


            }
        }); //end OnClickListener

        spinnerMode.setSelection(0,true);
        //注册spinner监听事件
        spinnerMode.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {

                //解决item默认第一次选择的问题
                Class<?> myClass = AdapterView.class;
                try {
                    Field field = myClass.getDeclaredField("mOldSelectedPosition");
                    field.setAccessible(true);
                    field.setInt(spinnerMode,AdapterView.INVALID_POSITION);
                }
                catch (NoSuchFieldException | IllegalAccessException e)
                {
                    e.printStackTrace();
                }

                String str = (String)spinnerMode.getSelectedItem();
                // textViewSensor.setText("放电模式："+str);

                String str1 = "手动选择模式";
                String str2 = "一般放电模式";
                String str3 = "快速放电模式";

                if(str.equals(str1))
                {
                    for (int i:checkId)
                    {
                        chk = (CheckBox) findViewById(i); //get checkbox object

                        chk.setChecked(false);
                    }
                    textViewSensor.setText("放电模式："+str1);
                }

                if(str.equals(str2))
                {
                    for (int i:checkId)
                    {
                        chk = (CheckBox) findViewById(i); //get checkbox object

                        if ((i == R.id.chk1) || (i == R.id.chk2) || (i == R.id.chk5))
                        {
                            chk.setChecked(true);
                        }
                        else
                        {
                            chk.setChecked(false);
                        }
                    }
                    textViewSensor.setText("放电模式："+str2);
                }

                if(str.equals(str3))
                {
                    for (int i:checkId)
                    {
                        chk = (CheckBox) findViewById(i); //get checkbox object

                        chk.setChecked(true);
                    }

                    textViewSensor.setText("放电模式："+str3);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        }); //end setOnItemSelectedListener

        //keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //register battery changed event
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(broadcastReceiver,intentFilter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


    } //end onCreate


    /**toggle GPS
     @return null
     **/
//    private void toggleGPS()
//    {
//        Intent gpsIntent = new Intent();
//        gpsIntent.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
//        gpsIntent.addCategory("android.intent.category.ALTERNATIVE");
//        gpsIntent.setData(Uri.parse("custom:3"));
//        try {
//            PendingIntent.getBroadcast(this, 0, gpsIntent, 0).send();
//        } catch (PendingIntent.CanceledException e) {
//            e.printStackTrace();
//            mlocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mlocationManager);
//            Location location1 = mlocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//            if (location1 != null) {
//                latitude = location1.getLatitude(); // 经度
//                longitude = location1.getLongitude(); // 纬度
//            }
//        }
//    }

    /**getLocation
     @return null
     **/
//    private void getLocation() {
//        Location location = mlocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        if (location != null) {
//            latitude = location.getLatitude();
//            longitude = location.getLongitude();
//        } else {
//
//            mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mlocationManager);
//        }
//        textViewSensor.setText("纬度：" + latitude + "\n" + "经度：" + longitude);
//    }

    /**locationListener
     @return null
     **/
//    LocationListener locationListener = new LocationListener() {
//        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
//        @Override
//        public void onStatusChanged(String provider, int status, Bundle extras) {
//        }
//
//        // Provider被enable时触发此函数，比如GPS被打开
//        @Override
//        public void onProviderEnabled(String provider) {
//            Log.e(TAG, provider);
//        }
//
//        // Provider被disable时触发此函数，比如GPS被关闭
//        @Override
//        public void onProviderDisabled(String provider) {
//            Log.e(TAG, provider);
//        }
//
//        // 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
//        @Override
//        public void onLocationChanged(Location location) {
//            if (location != null) {
//                Log.e("Map", "Location changed : Lat: " + location.getLatitude() + " Lng: " + location.getLongitude());
//                latitude = location.getLatitude(); // 经度
//                longitude = location.getLongitude(); // 纬度
//            }
//        }
//    };

    /**打开wifi功能
      @return true or false
    **/
    public boolean OpenWifi() {
        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }
        return bRet;
    }

    /**关闭wifi功能
     @return true or false
     **/
    public boolean closeWifi() {
        if (!wifiManager.isWifiEnabled()) {
            return true;
        } else {
            return wifiManager.setWifiEnabled(false);
        }
    }

    /**listen seekbar change event
     @return null
     **/
    public SeekBar.OnSeekBarChangeListener sbl = new SeekBar.OnSeekBarChangeListener()
    {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            seekBar.setProgress(progress);
            textViewTarBat.setText("目标电量："+progress+"%");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {


        }
    };

    /**weather open gps
     @return null
     **/
    public static final boolean isOPen(Context context)
    {

        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gps || network) {
            return true;
        }
        return false;
    }

    /**open gps
     @return null
     **/
    public static final void openGPS(Context context)
    {
        Intent GPSIntent = new Intent();
        GPSIntent.setClassName("com.android.settings",
                "com.android.settings.widget.SettingsAppWidgetProvider");
        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
        GPSIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(context, 0, GPSIntent, 0).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

    }

    /**jump set gps page
     @return null
     **/
    private void initGPS()
    {

        LocationManager locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // 判断GPS模块是否开启，如果没有则开启
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
        {
            Toast.makeText(this, "请打开GPS",
                    Toast.LENGTH_SHORT).show();

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("请打开GPS");
            dialog.setPositiveButton("确定",
            new android.content.DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {

                    // 转到手机设置界面，用户设置GPS
                    Intent intent = new Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, 0); // 设置完成后返回到原来的界面

                }
            });

            dialog.setNeutralButton("取消", new android.content.DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface arg0, int arg1)
                {
                    arg0.dismiss();
                }
            } );
            dialog.show();
        } else {
                //
        }

    } //end initgps

    /**close hardware device and checkbox
     @return null
     **/
    @SuppressLint("ResourceAsColor")
    private void closeHardwareDevice()
    {
//        int count = 0;
//        //Listen alertDialog button is positive or negative
//        for (int i:checkId)
//        {
//            chk = (CheckBox) findViewById(i); //get checkbox object
//
//            if (chk.isChecked())
//            {
//                count ++;
//            }
//        }
//
//        if (count > 0)
//        {
//            count = 0;
            //close bluetooth
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled())
            {
                bluetoothAdapter.disable();
            }

            //close wifi
            closeWifi();

            //close lighting
            try {
                manager.setTorchMode("0",false);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            //close vibrate
            vb.cancel();

            //cancel checkbox
            for (int i:checkId)
            {
                chk = (CheckBox) findViewById(i); //get checkbox object
                chk.setChecked(false);
            }

            //resume old brightness
            setLight(this,-1); //brigness 0-255  -1:adjust phone brightness
            float brightness = getActivityBrightness(this);
            //textViewSensor.setText("当前自适应手机亮度！");

            //display hint
            textViewSensor.setText("放电模式：关闭");
            toastHint.setText("已关闭放电，恢复初始设置！！！");
            toastHint.show();

//        }
//        else
//        {
//            textViewSensor.setText(" 放电模式：关闭");
//            toastHint.setText("没有需要关闭的设置！！！");
//            toastHint.show();
//        }

    }

    /**open hardware device
     @return null
     **/
    private void openHardwareDevice()
    {
        for (int i:checkId)
        {
            chk = (CheckBox) findViewById(i); //get checkbox object
            if (chk.isChecked())
            {
                if (chk.getText().equals("打开蓝牙"))
                {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (!bluetoothAdapter.isEnabled())
                    {
                        bluetoothAdapter.enable();
                    }

                }

                if (chk.getText().equals("打开WiFi"))
                {
                    OpenWifi();
                }

                if (chk.getText().equals("打开闪光灯"))
                {
                    //open light
                    try {
                        manager.setTorchMode("0",true);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                if (chk.getText().equals("打开振动"))
                {
                    if (vb.hasVibrator())
                    {

                        vb.vibrate(new long[]{0,10000,0,10000},2);
                    }
                }

                if (chk.getText().equals("打开GPS"))
                {
                    //textViewSensor.setText("======");
                    initGPS();

                }

                if (chk.getText().equals("设置屏幕最大亮度"))
                {
                    setLight(this,255); //brigness 0-255
                    float brightness = getActivityBrightness(this);
                    textViewSensor.setText("当前最大亮度:"+(brightness*255));

                }
            }

        }
    }

    /**Listen alertDialog event
     @return null
     **/
    private void alertDialog()
    {
        //get alert object
        AlertDialog.Builder bdr = new AlertDialog.Builder(this);
        //set alertDialog detail
        bdr.setMessage("确认是否开启放电模式？");
        bdr.setTitle("提示！");
        bdr.setCancelable(false);

        //set alertDialog listener event
        bdr.setPositiveButton("开启", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //textViewSensor.setText(" 放电模式：开启");

                String str = (String)spinnerMode.getSelectedItem();
                textViewSensor.setText("放电模式："+str);

                openHardwareDevice();
            }
        });

        bdr.setNegativeButton("不开启", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                closeHardwareDevice();
            }
        });
        //display alertDialog
        bdr.show();
    } //end alertDialog func


    /**Listen battery changed event
     @return null
     **/
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取当前的电量，如未获取到任何值，则默认值为0
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            //获取当前的电量，如未获取到任何值，则默认值为100
            batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
            LeftBattery = batteryLevel * 100/batteryScale;
            //显示电量
            testViewLevel.setText("剩余电量："+LeftBattery+"%");

            //获取seekbar设置的最低亮度
            int seekprocessvalue = seekBar.getProgress();
            //textViewSensor.setText("seekprocessvalue: "+seekprocessvalue);
            //判断此时的电量是否为最低电量，是，则关闭放电模式
            if (LeftBattery == seekprocessvalue)
            {
                closeHardwareDevice();

                toastHint.setText("已放电到目标电量，关闭放电模式！！！");
                toastHint.show();

            }
            else if(LeftBattery < seekprocessvalue)
            {
                closeHardwareDevice();
                toastHint.setText("已低于目标电量，关闭放电模式！！！");
                toastHint.show();
            }


        }
    };

    //set current screen brightness
    //public static void ResumeActivityBrightness(float paramFloat, Activity activity)
    //{
    //        Window localWindow = activity.getWindow();
    //        WindowManager.LayoutParams params = localWindow.getAttributes();
    //        params.screenBrightness = paramFloat;
    //        localWindow.setAttributes(params);
    //}

    /**set current screen brightness
     @return null
     **/
    private void setLight(Activity context, int brightness) {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.screenBrightness = Float.valueOf(brightness) * (1f / 255f);
        context.getWindow().setAttributes(lp);
    }

    /**get current screen brightness
     @return brightness
     **/
    public static float getActivityBrightness(Activity activity)
    {
        Window localWindow = activity.getWindow();
        WindowManager.LayoutParams params = localWindow.getAttributes();
        return params.screenBrightness;
    }

    /**声明温度传感器监听事件
     @return brightness
     **/
    private final SensorEventListener sensorEventListener=new SensorEventListener(){

        @Override
        public void onSensorChanged(SensorEvent event)
        {
            textViewSensor.setText("======");
            if(event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE)
            {
                float temperature = event.values[0];
                textViewSensor.setText(String.valueOf(temperature)+"C");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {

        }

    };

    /**create menu bar
     @return brightness
     **/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**handle menu item
     @return brightness
     **/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.search_setting:
                {
                    Uri uri = Uri.parse("http://www.baidu.com");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
                break;
            case R.id.action_settings:
                {

                }
                break;
            case R.id.about_setting:
                {
                    //get alert object
                    AlertDialog.Builder bdr = new AlertDialog.Builder(this);
                    //set alertDialog detail
                    bdr.setMessage("开发者：Sun\r\n语言：中文(中国)\r\n手机系统：Android\r\n版本号：OneKeyDischarge_V0.0.0.1");
                    bdr.setTitle("关于");
                    bdr.setCancelable(false);

                    //set alertDialog listener event
                    bdr.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {

                        }
                    });

                    //display alertDialog
                    bdr.show();
                }
                break;
            case R.id.sensor_number:
            {
                List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_ALL);

                //get alert object
                AlertDialog.Builder bdr = new AlertDialog.Builder(this);
                //set alertDialog detail
                bdr.setMessage("您的手机有:"+list.size()+"个");
                bdr.setTitle("传感器个数");
                bdr.setCancelable(false);

                //set alertDialog listener event
                bdr.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {

                    }
                });

                //display alertDialog
                bdr.show();
            }
            default:
                break;

        }
        return super.onOptionsItemSelected(item);
    }
}
