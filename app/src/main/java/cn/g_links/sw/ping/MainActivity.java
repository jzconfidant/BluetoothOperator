package cn.g_links.sw.ping;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import net.frakbot.jumpingbeans.JumpingBeans;

import java.util.UUID;

import cn.glinks.lib.bt.BluetoothOperationCallback;
import cn.glinks.lib.bt.BluetoothOperator;

public class MainActivity extends AppCompatActivity
            implements View.OnClickListener {

    private static final String TAG = "G-Links_" + MainActivity.class.getSimpleName();
    private static final boolean DBG = BuildConfig.DEBUG;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private TextView mTextView;
    JumpingBeans mJumpingBeans;

    // Bluetooth
    private boolean hasEnabled = false;
    private BluetoothOperator mBluetoothOp;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothOp = new BluetoothOperator(this);
        mBluetoothOp.registerCallback(callback);
        if (mBluetoothOp.isEnabled())
            hasEnabled = true;

        mTextView = (TextView) findViewById(R.id.textView);
        mJumpingBeans = JumpingBeans.with(mTextView).appendJumpingDots().build();
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();

        mBluetoothOp.enable();
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();

        mJumpingBeans.stopJumping();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothOp.unregisterCallback(callback);
        if (!hasEnabled)
            mBluetoothOp.disable();
        mBluetoothOp.destroy();
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_scan_btn:
                mBluetoothOp.connect("34:87:3D:4A:3F:94");
                break;
            case R.id.bt_send_btn:
                mBluetoothOp.write("hello".getBytes());
                break;
        }
    }
}
