package org.ywb_ipop;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Administrator on 2015/4/30.
 */
public class IPinfoActivity extends Activity {
    public static int mtype = 0;
    private final String TAG="ipop_ip:";
    private EditText PingHost, PingInfo;
    private Button Ping;
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.act_ipinfo);
        this.setTitle(String.format("%s: %s",
                getResources().getText(R.string.app_name),
                getResources().getText(R.string.title_ipinfo)));


        EditText et = (EditText)findViewById(R.id.editText);
        EditText et2 = (EditText)findViewById(R.id.editText2);
        et.setText(getCurrentNetType(this));
        et2.setText(getLocalIpAddress());
        PingInfo=(EditText)findViewById(R.id.PingInfo);
        PingHost=(EditText)findViewById(R.id.PingHost);
        Ping=(Button)findViewById(R.id.Ping);
        //读取保存的配置
        //sendtxt表示存放时所用的xml文件名称
        SharedPreferences mSharedPreferences=getSharedPreferences("sendtxt", MODE_PRIVATE);
        PingHost.setText(mSharedPreferences.getString("PingHost", ""));

        Ping.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //String ip =PingHost.getText().toString();

                Ping.setVisibility(View.INVISIBLE);
                new Thread(new Ping_Thread()).start();
               /* try {
                    Process p = Runtime.getRuntime().exec("/system/bin/ping -c 3 -w 100 " + ip);//ping3次
                    InputStream input = p.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(input));
                    StringBuffer stringBuffer = new StringBuffer();

                    String content = "";

                    while ((content = in.readLine()) != null) {

                        stringBuffer.append(content+"\r\n");
                        PingInfo.setText(stringBuffer.toString());

                    }
                    PingInfo.setText(stringBuffer.toString());
                    // PING的状态

                    int status = p.waitFor();

                    if (status != 0) {

                        stringBuffer.append("ping fail!\r\n");
                        PingInfo.setText(stringBuffer.toString());

                    }
                }catch (Exception e)
                {
                    Log.d(TAG, e.getMessage());
                }*/


            }
        });
        ActionBarWrapper actionBar = ActionBarWrapper.getActionBar(this);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
    class Ping_Thread implements  Runnable {

        public void run() {
            try {
                String ip =PingHost.getText().toString();
                Process p = Runtime.getRuntime().exec("/system/bin/ping -c 3 -w 100 " + ip);//ping3次
                InputStream input = p.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(input));
                final StringBuffer stringBuffer = new StringBuffer();

                String content = "";

                while ((content = in.readLine()) != null) {

                    stringBuffer.append(content+"\r\n");
                    runOnUiThread(new Runnable() {
                        public void run() {
                    PingInfo.setText(stringBuffer.toString());
                        }
                    });

                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        PingInfo.setText(stringBuffer.toString());
                    }
                    });
                // PING的状态

                int status = p.waitFor();

                if (status != 0) {

                    stringBuffer.append("ping fail!\r\n");
                    runOnUiThread(new Runnable() {
                        public void run() {
                    PingInfo.setText(stringBuffer.toString());
                        }
                    });
                }
            }catch (Exception e)
            {
                Log.d(TAG, e.getMessage());
            }
            runOnUiThread(new Runnable() {
                public void run() {
            Ping.setVisibility(View.VISIBLE);
                }
            });
        }
    }
    //退出当前Activity时被调用,调用之后Activity就结束了
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //保存配置
        SharedPreferences mSharedPreferences = getSharedPreferences("sendtxt", MODE_PRIVATE);
        mSharedPreferences.edit().putString("PingHost", PingHost.getText().toString()).commit();
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    /**
     * 得到当前的手机网络类型
     *
     * @param context
     * @return
     */
    public static String  getCurrentNetType(Context context) {
        String type = "";
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) {
            type = "NULL";
            mtype = -1;
            return type;
        }
        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            type = "Type:WIFI" ;
            mtype = 0 ;
        }
        else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) {
            type = "Type:ETHERNET" ;
            mtype = 1 ;
        }
        else{
            type ="Type:"+info.getTypeName()+"["+info.getSubtypeName()+"]\r\n";
            mtype = 2 ;
            type =type+"State:"+info.getState().toString()+"\r\n";
            type =type+"Reason:"+info.getReason()+"\r\n";
            type =type+"Roaming:"+String.valueOf(info.isRoaming());
        }

        return type;
    }
    public String getLocalIpAddress()
    {
        String temps="",temps2="",temps3="",outstr="";
        int num=0;
        //获取wifi服务
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (wifiManager.isWifiEnabled()&&mtype==0) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String ip = "IP:" + intToIp(ipAddress) + "\r\n";


            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

            ip = ip + "Mask:" + intToIp(dhcpInfo.netmask) + "\r\n";
            ip = ip + "Gateway:" + intToIp(dhcpInfo.gateway) + "\r\n";
            ip = ip + "DHCP:" + intToIp(dhcpInfo.serverAddress) + "\r\n";
            ip = ip + "DNS1:" + intToIp(dhcpInfo.dns1) + "\r\n";
            ip = ip + "DNS2:" + intToIp(dhcpInfo.dns2) + "\r\n";

            ip = ip + "MAC :" + wifiInfo.getMacAddress().toUpperCase();
            return ip;
        }
        else {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    num = 0;
                    temps2 = "";
                    temps = "";
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        // if (!inetAddress.isLoopbackAddress())
                        {
                            num++;
                            temps2 = temps2 + "IP" + String.valueOf(num) + ":" + inetAddress.getHostAddress().toString() + "\r\n";


                        }
                    }

                    temps = temps + "Name:" + intf.getDisplayName() + "\r\n";
                    if (android.os.Build.VERSION.SDK_INT >= 9) {
                        try {
                            byte[] mac = intf.getHardwareAddress();
                            temps = temps + "MAC:" + bytesToHexString(mac) + "\r\n";
                            temps = temps + "MTU:" + String.valueOf(intf.getMTU()) + "\r\n";
                        } catch (Exception e) {
                        }
                    }

                    if (num > 0)
                        outstr = outstr + temps + temps2 + "\r\n";
                    else
                        temps3 = temps3 + temps + "\r\n";

                }
           /* String dns="";
            try {
                Process localProcess = Runtime.getRuntime().exec("getprop net.dns1");
                dns=localProcess.toString();

            }catch (IOException e){}

            outstr=outstr+temps3;

            outstr=outstr+"DNS1:"+dns;*/


                outstr = outstr + temps3;
                return outstr;
            } catch (SocketException ex) {
                Log.e("WifiPreference IpAddress", ex.toString());
            }
            return null;
        }
    }
    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString().toUpperCase();
    }
    private String intToIp(int i) {

        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

}

