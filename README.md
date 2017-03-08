BluetoothLibrary
================

## 使用步骤
### 初始化
要使用BluetoothLibrary，首先要构造一个BluetoothOperator对象，用于操作蓝牙设备。建议的
做法是在Activity的onCreate()函数中完成初始化，如下：

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothOp = new BluetoothOperator(this);
        mBluetoothOp.registerCallback(callback);
        if (mBluetoothOp.isEnabled())
            hasEnabled = true;
    }

### 使能蓝牙功能
mBluetoothOp.enable();

    protected void onResume() {
        super.onResume();

        mBluetoothOp.enable();
    }

### 禁用蓝牙功能
mBluetoothOp.disable();

    protected void onDestroy() {
        super.onDestroy();

        mBluetoothOp.unregisterCallback(callback);
        if (!hasEnabled)
            mBluetoothOp.disable();
        mBluetoothOp.destroy();
    }

### 注册回调
BluetoothOperationCallback用于接收反馈信息，如连接成功、收到数据。

    private BluetoothOperationCallback callback = new BluetoothOperationCallback() {

        @Override
        public void onConnect(int err, String desc) {
            if (err == 0) {
                Log.d(TAG, "Bluetooth connect success");
                mBluetoothOp.write("hello".getBytes());
            }
        }

        @Override
        public void onDataRecv(int err, String desc, byte[] data, int len) {
            if (err == 0) {
                Log.d(TAG, "Bluetooth receive data:" + new String(data, 0, len));
            }
        }
    };

    mBluetoothOp.registerCallback(callback);

### 取消注册
调用BluetoothOperator.unregisterCallback(callback)。

### 建立连接
调用mBluetoothOp.connect()，成功建立连接之后会通过onConnect()通知用户。

### 发送数据
调用mBluetoothOp.write();

### 接收数据
当接收到数据之后，会通过onDataRecv()通知用户（前提是用户注册过callback）。

### 释放资源
mBluetoothOp.destrory()

注意事项
=======
在用于不在使用BluetoothOperator的时候，必须调用BluetoothOperator.destroy()来释放所有
的资源。

***代码可参考BluetoothLibrary库里面的测试代码。***