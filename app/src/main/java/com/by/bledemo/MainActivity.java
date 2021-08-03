package com.by.bledemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();
    private boolean mScanning;

    /**
     * 连接状态，默认断开连接状态
     */
    public final static UUID SER_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public final static UUID SPP_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");

    public final static UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothGatt mBluetoothGatt;
    private TextView deviceName;

    private TextView textView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
            if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }
        deviceName = (TextView) findViewById(R.id.device_name);
        textView1 = (TextView) findViewById(R.id.recieve_text);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private BluetoothDevice mDevice;
    final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            String name = device.getName();
            if (name != null) {
                deviceName.setText(name);
                if (name.contains("HD") || device.getAddress().contains("HD")) {
                    mDevice = device;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                }
            }
        }

    };

    private void scanLeDevice() {


        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, 30000);
        mScanning = true;
        // 定义一个回调接口供扫描结束处理
        mBluetoothAdapter.startLeScan(mLeScanCallback);

    }


    private String TAG = "haha";
    private boolean isServiceConnected;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                String err = "Cannot connect device with error status: " + status;

                gatt.close();
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                if (mDevice != null) {
                    mBluetoothGatt = mDevice.connectGatt(MainActivity.this, false, mGattCallback);
                }
                Log.e(TAG, err);
                return;
            }


            if (newState == BluetoothProfile.STATE_CONNECTED) {//当蓝牙设备已经连接
                isServiceConnected = true;
//获取ble设备上面的服务
//                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                Log.i("haha", "Attempting to start service discovery:" +

                        mBluetoothGatt.discoverServices());

                Log.d("haha", "onConnectionStateChange: " + "连接成功");

                textView1.setText("连接成功");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//当设备无法连接
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                gatt.close();
                if (mDevice != null) {
                    mBluetoothGatt = mDevice.connectGatt(MainActivity.this, false, mGattCallback);
                }
            }


        }

        //发现服务回调。
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("haha", "onServicesDiscovered: " + "发现服务 : " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (gatt != null) {
                    BluetoothGattService gattService = gatt.getService(SER_UUID);//直接获取服务 省略搜索麻烦
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(SPP_UUID);
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(DESCRIPTOR_UUID);//蓝牙模块会收到开启通知信息
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean iswrite = gatt.writeDescriptor(descriptor);
                    }

//            boolean isMtu=setMTU(53);
                }


            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            byte[] value = characteristic.getValue();
            Log.d(TAG, "read value: " + Arrays.toString(value));
            Log.d(TAG, "callback characteristic read status " + status
                    + " in thread " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "read value: " + characteristic.getValue());
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: " + "设置成功");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + "发送成功");

            boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            mBluetoothGatt.readCharacteristic(characteristic);
        }

        @Override
        public final void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            Log.d(TAG, "read value: " + Arrays.toString(value));
            textView1.setText(Arrays.toString(value));

        }

    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) {
            finish();


            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startConnect(View view) {
        if (mDevice != null) {
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
            mBluetoothGatt = mDevice.connectGatt(MainActivity.this, false, mGattCallback);
        }
    }


    public void startScan(View view) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        scanLeDevice();


    }

    public void startSend(View view) {
        if (mBluetoothGatt != null && isServiceConnected) {
            BluetoothGattService gattService = mBluetoothGatt.getService(SER_UUID);
            if (gattService != null) {
                BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(SPP_UUID);
                if (characteristic != null) {
                    String order = "7B A5 000000000101  C60400000000 0000B75A";
                    byte[] send_data = hex2Bytes(order);
                    send_data[14] = 70;
                    send_data[15] = 34;
                    characteristic.setValue(send_data);
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    boolean b = mBluetoothGatt.writeCharacteristic(characteristic);
                    Log.d(TAG, "startSend: " + b);
                }
            }
        }

    }

    /**
     * hex字符串转byte数组<br/>
     * 2个hex转为一个byte
     *
     * @param src
     * @return
     */
    public static byte[] hex2Bytes(String src) {
        src = src.replaceAll(" ", "");

        if (src.length() % 2 != 0) {
            return null;
        }

        byte[] res = new byte[src.length() / 2];
        char[] chs = src.toCharArray();
        for (int i = 0, c = 0; i < chs.length; i += 2, c++) {
            res[c] = (byte) (Integer.parseInt(new String(chs, i, 2), 16));
        }

        return res;
    }

    public void startRead(View view) {
        if (mBluetoothGatt != null && isServiceConnected) {


            BluetoothGattService gattService = mBluetoothGatt.getService(SER_UUID);
            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(SPP_UUID);
            boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            if (b) {

                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {

                    boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    if (b1) {
                        mBluetoothGatt.writeDescriptor(descriptor);
                        Log.d(TAG, "startRead: " + "监听收数据");
                    }

                }

            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        super.onDestroy();

    }

    public void stopConnect(View view) {
        if (mBluetoothGatt != null) {

            mBluetoothGatt.close();
        }
    }
}
