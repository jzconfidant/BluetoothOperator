package cn.glinks.lib.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Aamir on 2017/3/7.
 */

public class BluetoothOperator {
    private static final String TAG = "G-Links_" + BluetoothOperator.class.getSimpleName();
    private static final boolean DBG = BuildConfig.DEBUG;

    private static final int CMD_CONNECT = 1;
    private static final int CMD_DATA_RECEIVE = 2;
    private static final int CMD_DEVICE_FOUND = 3;

    // Bluetooth Serial
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String FILTER_NAME = "TEST";

    private Context mContext;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothReceiver mBluetoothReceiver;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayList<BluetoothDevice> mFoundDevices = new ArrayList<>();
    private ArrayList<BluetoothOperationCallback> mCallbacks = new ArrayList<>();
    private ConnectedThread mConnectedThread;
    private String mConnectDeviceMAC = null;

    private Handler mRecvHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CMD_DEVICE_FOUND:
                    BluetoothDevice device = null;
                    String mac = setConnectDeviceMAC(null);
                    if (mac != null) {
                        device = searchFoundDevices(mac);
                        if (device != null) {
                            new ConnectThread(device).start();
                        }
                    }
                    break;
                case CMD_CONNECT:
                    if (mCallbacks.size() > 0) {
                        for (BluetoothOperationCallback callback : mCallbacks) {
                            if (msg.arg1 == -1)
                                callback.onConnect(-1, "connect fail");
                            else
                                callback.onConnect(0, "connect success");
                        }
                    }
                    break;
                case CMD_DATA_RECEIVE:
                    if (mCallbacks.size() > 0) {
                        for (BluetoothOperationCallback callback : mCallbacks) {
                            callback.onDataRecv(0, "data receive", (byte[]) msg.obj, msg.arg1);
                        }
                    }
                    break;
            }
        }
    };

    public BluetoothOperator(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mBluetoothReceiver = new BluetoothReceiver();
            context.registerReceiver(mBluetoothReceiver, filter);
        }
    }

    /**
     * 判断手机是否支持蓝牙功能。
     *
     * @return
     */
    public boolean isBluetoothSupported() {
        if (mBluetoothAdapter == null)
            return false;
        else
            return true;
    }

    /**
     * 使能蓝牙功能。
     * @return
     */
    public boolean enable() {
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled())
                return mBluetoothAdapter.enable();
            else
                return true;
        }
        return false;
    }

    /**
     * 禁用蓝牙功能。
     * @return
     */
    public boolean disable() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled())
                return mBluetoothAdapter.disable();
            else
                return true;
        }
        return false;
    }

    /**
     * 判断蓝牙是否开启。
     * @return
     */
    public boolean isEnabled() {
        if (mBluetoothAdapter != null)
            return mBluetoothAdapter.isEnabled();

        return false;
    }

    /**
     * 用户最后必须调用此函数来销毁不用的资源。
     */
    public void destroy() {
        if (mConnectedThread != null)
            mConnectedThread.quit();

        if (mBluetoothReceiver != null)
            mContext.unregisterReceiver(mBluetoothReceiver);
    }

    /**
     * 连接指定地址的蓝牙设备。
     * @param mac
     * @return
     */
    public boolean connect(String mac) {
        if (TextUtils.isEmpty(mac))
            return false;

        setConnectDeviceMAC(null);

        BluetoothDevice device = searchPairedDevices(mac);
        if (device == null) {
            device = searchFoundDevices(mac);
            if (device == null) {
                mBluetoothAdapter.startDiscovery();
                setConnectDeviceMAC(mac);
                return false;
            }
        }

        new ConnectThread(device).start();

        return true;
    }

    /**
     * 向已建立连接的蓝牙设备发送数据。
     * @param buffer
     */
    public void write(byte[] buffer) {
        if (mConnectedThread != null)
            mConnectedThread.write(buffer);
    }

    /**
     * 注册回调函数，用于接收反馈，比如连接成功、接收到数据。
     * @param callback
     */
    public void registerCallback(BluetoothOperationCallback callback) {
        if (callback != null && !mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    /**
     * 取消注册。
     * @param callback
     */
    public void unregisterCallback(BluetoothOperationCallback callback) {
        if (callback != null && mCallbacks.contains(callback)) {
            mCallbacks.remove(callback);
        }
    }

    private synchronized String setConnectDeviceMAC(String mac) {
        String tmp = mConnectDeviceMAC;
        mConnectDeviceMAC = mac;
        return tmp;
    }

    /**
     * 在已配对的设备列表中查找指定MAC地址的BluetoothDevice。
     *
     * @param mac
     * @return
     */
    private BluetoothDevice searchPairedDevices(String mac) {
        if (TextUtils.isEmpty(mac))
            return null;

        if (mPairedDevices.size() > 0) {
            for (BluetoothDevice device : mPairedDevices) {
                if (device.getAddress().equals(mac))
                    return device;
            }
        }

        return null;
    }

    /**
     * 在搜索到的设备列表中查找指定MAC地址的蓝牙设备。
     * @param mac
     * @return
     */
    private BluetoothDevice searchFoundDevices(String mac) {
        if (TextUtils.isEmpty(mac))
            return null;

        if (mFoundDevices.size() > 0) {
            for (BluetoothDevice device : mFoundDevices) {
                if (device.getAddress().equals(mac))
                    return device;
            }
        }

        return null;
    }

    /**
     * 广播接收器。
     */
    private class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getName().equals(FILTER_NAME)) {
                    String mac = device.getAddress();
                    int i = 0;
                    for (; i < mFoundDevices.size(); i++) {
                        if (mFoundDevices.get(i).getAddress().equals(mac))
                            break;
                    }
                    if (i == mFoundDevices.size()) {
                        mFoundDevices.add(device);

                        Message msg = mRecvHandler.obtainMessage();
                        msg.what = CMD_DEVICE_FOUND;
                        msg.sendToTarget();
                    }
                }
            }
        }
    }

    /**
     * 创建新的线程用于和对端的蓝牙设备建立连接。
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket socket = null;
            mmDevice = device;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {

            }

            mmSocket = socket;
        }

        public void run() {
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                if (DBG) Log.d(TAG, "connected");
            } catch (IOException e) {
                try {
                    mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket",
                            new Class[] {int.class}).invoke(mmDevice, Integer.valueOf(1));
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                } catch (InvocationTargetException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    e1.printStackTrace();
                }
                try {
                    mmSocket.connect();
                    if (DBG) Log.d(TAG, "connected");
                } catch (IOException e1) {
                    Log.e(TAG, "connect to bluetooth device failed");
                    Message msg = mRecvHandler.obtainMessage();
                    msg.what = CMD_CONNECT;
                    msg.arg1 = -1;
                    msg.sendToTarget();
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {

                    }
                    return;
                }
            }

            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();

            Message msg = mRecvHandler.obtainMessage();
            msg.what = CMD_CONNECT;
            msg.sendToTarget();
        }
    }

    /**
     * 创建新的线程用于和蓝牙设备通信。
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private boolean quit = false;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {

            }

            mmInStream = inputStream;
            mmOutStream = outputStream;
        }

        public void run() {
            byte[] buffer;
            int bytes;

            while (!quit) {
                try {
                    buffer = new byte[2048];
                    bytes = mmInStream.read(buffer);

                    Message msg = mRecvHandler.obtainMessage();
                    msg.what = CMD_DATA_RECEIVE;
                    msg.arg1 = bytes;
                    msg.obj = buffer;
                    msg.sendToTarget();
                } catch (IOException e) {

                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {

            }
        }

        public void quit() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "close bluetooth socket failed");
            }
            quit = true;
        }
    }
}
