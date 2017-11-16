package com.example.mybluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.UUID;

// http://blog.csdn.net/geanwen/article/details/73648721
public class MainActivity extends AppCompatActivity {
    private BluetoothGatt mBluetoothGattOne;
    public final static UUID UUID_SERVICE = UUID.fromString("0003cdd0-0000-1000-8000-00805f9b0131");//蓝牙设备的Service的UUID
    public final static UUID UUID_NOTIFY = UUID.fromString("0003cdd1-0000-1000-8000-00805f9b0131");  //蓝牙设备的notify的UUID
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取BluetoothManager
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //获取BluetoothAdapter
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        //如果蓝牙没有打开 打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        BluetoothDevice bluetoothDeviceOne = mBluetoothAdapter.getRemoteDevice("58:44:98:E4:09:A4");//然后就是连接设备，并设置连接的回调：

        /*BluetoothDevice bluetoothDeviceOne = bluetoothAdapter.getRemoteDevice("D8:B0:4C:BC:C0:83"); 两个或多个的方法
        BluetoothDevice bluetoothDeviceTwo = bluetoothAdapter.getRemoteDevice("D8:B0:4C:BA:D5:9D");
        mBluetoothGattOne = bluetoothDeviceOne.connectGatt(MainActivity.this, true, bluetoothGattCallbackOne);*/
        //如果Gatt在运行,将其关闭
        if (mBluetoothGattOne != null) {
            mBluetoothGattOne.close();
            mBluetoothGattOne = null;
        }
        //连接蓝牙设备并获取Gatt对象
        mBluetoothGattOne = bluetoothDeviceOne.connectGatt(MainActivity.this, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i("geanwen", "设备一连接成功");
                        //搜索Service
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i("geanwen", "设备一连接断开");
                    }
                }
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                //根据UUID获取Service中的Characteristic,并传入Gatt中
                BluetoothGattService bluetoothGattService = gatt.getService(UUID_SERVICE);
                BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID_NOTIFY);

                boolean isConnect = gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                if (isConnect) {

                } else {
                    Log.i("geanwen", "onServicesDiscovered: 设备一连接notify失败");
                }
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {//数据改变
                super.onCharacteristicChanged(gatt, characteristic);
                String data = new String(characteristic.getValue());
                Log.i("geanwen", "onCharacteristicChanged: " + data);
            }
        });


    }
}
