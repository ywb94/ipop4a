package org.ywb_ipop.transport;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.ywb_ipop.R;
import org.ywb_ipop.bean.HostBean;
import org.ywb_ipop.util.HostDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2015/5/5.
 */
public class Bluetooth  extends AbsTransport {
    private static final String TAG = "Blue:";
    private static final String PROTOCOL = "bluetooth";
    private static final String DEFAULT_URI = "bluetooth:#bluetooth";
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private boolean restoreoff=false;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    static final Pattern hostmask;
    static {
        hostmask = Pattern.compile("^(.+)@([0-9a-z.-]+)(:([0-9a-z.-]+))?$", Pattern.CASE_INSENSITIVE);
    }
    public Bluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (inStream == null) {
            bridge.dispatchDisconnect(false);
            throw new IOException("session closed");
        }
        return inStream.read(buffer, offset, length);

    }

    @Override
    public void write(int c) throws IOException {
        try {
            if (outStream != null)
                outStream.write(c);
        } catch (SocketException e) {
            bridge.dispatchDisconnect(false);
        }
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        if (outStream != null)
            outStream.write(buffer);
    }

    @Override
    public void setDimensions(int columns, int rows, int width, int height) {

    }

    @Override
    public void close() {
        if (btSocket != null) {
            try {
                btSocket.close();
                btSocket = null;


            } catch (IOException e) {
                Log.d(TAG, "Error closing blue socket.", e);
            }
        }
        if (restoreoff && mBluetoothAdapter != null)
            mBluetoothAdapter.disable();
    }
    @Override
    public int getDefaultPort() {
        return 0;
    }

    @Override
    public void flush() throws IOException {
        outStream.flush();
    }

    @Override
    public boolean isConnected() {
        return inStream != null && outStream != null;
    }

    @Override
    public boolean usesNetwork() {
        return false;
    }

    @Override
    public void getSelectionArgs(Uri uri, Map<String, String> selection) {
        selection.put(HostDatabase.FIELD_HOST_PROTOCOL, PROTOCOL);
        selection.put(HostDatabase.FIELD_HOST_NICKNAME, uri.getFragment());
    }

    @Override
    public boolean isSessionOpen() {
        return inStream != null && outStream != null;
    }

    @Override
    public void connect() {
        int i=0;
        String Macaddress=host.getHostname().replace(".",":");
        if (mBluetoothAdapter == null) return;
        if(!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            restoreoff=true;

            while (!mBluetoothAdapter.isEnabled()) {
                try {
                    Thread.sleep(100L);
                    i++;
                    if (i > 100)
                        return;
                } catch (InterruptedException ie) {
                    // unexpected interruption while enabling bluetooth
                    Thread.currentThread().interrupt(); // restore interrupted flag
                    return;
                }
            }
        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(Macaddress);
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {

        }
        mBluetoothAdapter.cancelDiscovery();
        try {
            btSocket.connect();
            Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");
            bridge.outputLine(manager.res.getString(R.string.ywb_connectblueok));
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.e(TAG,
                        "ON RESUME: Unable to close socket during connection failure", e2);

            }
            if (restoreoff && mBluetoothAdapter != null)
                mBluetoothAdapter.disable();
            bridge.outputLine(manager.res.getString(R.string.ywb_connectbluefail));
            return;
        }
        try {
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "ON RESUME: stream creation failed.", e);
        }
        bridge.onConnected();
    }

    @Override
    public HostBean createHost(Uri uri) {
        HostBean host = new HostBean();

        host.setProtocol(PROTOCOL);

        host.setHostname(uri.getHost());


        host.setUsername(uri.getUserInfo());

        String nickname = uri.getFragment();
        if (nickname == null || nickname.length() == 0) {
            host.setNickname(getDefaultNickname(host.getUsername(),
                    host.getHostname(), host.getPort()));
        } else {
            host.setNickname(uri.getFragment());
        }

        return host;
    }

    @Override
    public String getDefaultNickname(String username, String hostname, int port) {
        return DEFAULT_URI;
    }
    public static String getProtocolName() {
        return PROTOCOL;
    }
    public static String getFormatHint(Context context) {
        return context.getString(R.string.hostpref_nickname_title);
    }
    public static Uri getUri(String input) {
        Matcher matcher = hostmask.matcher(input);

        if (!matcher.matches())
            return null;

        StringBuilder sb = new StringBuilder();

        sb.append(PROTOCOL)
                .append("://")
                .append(Uri.encode(matcher.group(1)))
                .append('@')
                .append(matcher.group(2));

        String NikeString = matcher.group(4);
        sb.append("/#")
                .append(NikeString);

        Uri uri = Uri.parse(sb.toString());

        return uri;
    }
}

