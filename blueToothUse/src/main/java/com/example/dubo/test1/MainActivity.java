package com.example.dubo.test1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private BluetoothAdapter bluetoothAdapter;
    //声明一个list集合,,,泛型是蓝牙设备
    private List<BluetoothDevice> list = new ArrayList<>();
    private MyReceiver myReceiver;
    private ListViewAdapter adapter;
    private BluetoothGatt mBluetoothGattOne;
    public final static UUID UUID_SERVICE = UUID.fromString("0003cdd0-0000-1000-8000-00805f9b0131");//蓝牙设备的Service的UUID
    public final static UUID UUID_NOTIFY = UUID.fromString("0003cdd1-0000-1000-8000-00805f9b0131");  //蓝牙设备的notify的UUID
    private TextView test_show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button search = (Button) findViewById(R.id.search);
        test_show = (TextView) findViewById(R.id.test_show);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchDevice();
            }
        });

        listView = (ListView) findViewById(R.id.list_view);

        //获取蓝牙适配器对象
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //首先,如果要操作蓝牙,先判断当前的手机手否存在蓝牙
        if (bluetoothAdapter != null) {
            //判断蓝牙是否可用
            if (!bluetoothAdapter.isEnabled()) {
                //让蓝牙处于可用状态
                bluetoothAdapter.enable();
                //bluetoothAdapter.disable();//不可用,关闭
            }
        }


        myReceiver = new MyReceiver();
        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        //指定广播接收的那个动作...BluetoothDevice.ACTION_FOUND发现了一个蓝牙设备
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //再添加一个动作,,,配对状态改变的时候的动作
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(myReceiver, filter);
        //点击条目的时候跟当前未配对的设配进行配对
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice bluetoothDevice = list.get(i);
                //没有配对的时候,去配对....使用反射去实现
                if (BluetoothDevice.BOND_NONE == bluetoothDevice.getBondState()) {
                    //首先通过MAC地址去获取要配对的设备
                    BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress());
                    //使用反射去配对
                    try {
                        Method method = BluetoothDevice.class.getMethod("createBond", null);//(Class<?>[]) new Object[]{}
                        method.invoke(remoteDevice, null);//new Object[]{}
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    //搜索设备的点击事件
    public void searchDevice() {
        //1.搜索配对过的设备
        searchBoundDevice();
        //2.扫描周边的蓝牙设备
        searchUnboundDevice();
        //3.设置适配器
        setAdapter();
    }


    /**
     * 给listView设置适配器
     */

    private void setAdapter() {
        if (adapter == null) {
            adapter = new ListViewAdapter(MainActivity.this, list);
            listView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }


    /**
     * 扫描没有配对的设备
     */

    private void searchUnboundDevice() {
        //如果现在蓝牙正在扫描
        if (bluetoothAdapter.isDiscovering()) {//directory目录,文件夹
            bluetoothAdapter.cancelDiscovery();//结束当前正在执行的扫描
        }
        //开始本次的扫描
        bluetoothAdapter.startDiscovery();//返回值是boolean类型,代表本次扫描是否已经开始
        //开始扫描之后,,,,一旦扫描到设备之后,,,手机会发送广播,,,所以我们要获取发送过来有关蓝牙设备的信息,需要写一个广播接收者
    }

    /**
     * 该方法是:搜索已经配对的设备
     */

    private void searchBoundDevice() {
        //获取已经配对的设备,,,返回值是set集合,,泛型就是蓝牙设备
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            //添加到list集合
            if (!list.contains(device)) {
                list.add(device);
            }
        }
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //获取传递过来的数据....实际上就是扫描到的蓝牙设备
            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //先获取一下当前广播接收的动作
            String action = intent.getAction();
            if (action.equals(bluetoothDevice.ACTION_FOUND)) {//当前接收者接收的是扫描的广播发出来的信息
                if (!list.contains(bluetoothDevice)) {
                    list.add(bluetoothDevice);
                }
                //设置适配器
                setAdapter();
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                //首先获取到状态....对比状态
                int bondState = bluetoothDevice.getBondState();
                switch (bondState) {
                    case BluetoothDevice.BOND_NONE:
                        Toast.makeText(MainActivity.this, "配对失败", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Toast.makeText(MainActivity.this, "正在配对", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Toast.makeText(MainActivity.this, "配对成功", Toast.LENGTH_SHORT).show();
                        list.remove(bluetoothDevice);
                        list.add(0, bluetoothDevice);
                        setAdapter();
                        break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        //在activity销毁的时候注销广播
        if (myReceiver != null) {
            unregisterReceiver(myReceiver);
            myReceiver = null;
        }
        super.onDestroy();
    }
}

/**
 * @author Dash
 * @date 2017/9/28
 * @description:
 */

class ListViewAdapter extends BaseAdapter {
    Context context;
    List<BluetoothDevice> list;

    public ListViewAdapter(Context context, List<BluetoothDevice> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }


    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            view = View.inflate(context, R.layout.item_layout, null);
            holder = new ViewHolder();
            holder.text_name = view.findViewById(R.id.text_name);
            holder.text_address = view.findViewById(R.id.text_address);
            holder.text_state = view.findViewById(R.id.text_state);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        BluetoothDevice bluetoothDevice = list.get(i);
        holder.text_name.setText(bluetoothDevice.getName());
        holder.text_address.setText(bluetoothDevice.getAddress());

        //设置显示是否配对的状态
        int bondState = bluetoothDevice.getBondState();
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                holder.text_state.setText("未配对");
                break;
            case BluetoothDevice.BOND_BONDING:
                holder.text_state.setText("正在配对");
                break;
            case BluetoothDevice.BOND_BONDED:
                holder.text_state.setText("已经配对");
                break;
        }
        return view;
    }

    //一个蓝牙设备主要的信息:设备的名称,,,设备的地址,,,是否配对一个状态
    private class ViewHolder {
        TextView text_name;
        TextView text_address;
        TextView text_state;
    }
}



