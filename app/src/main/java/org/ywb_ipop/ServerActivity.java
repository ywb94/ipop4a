package org.ywb_ipop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TabHost;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2015/5/12.
 */
public class ServerActivity  extends Activity{
    private  boolean isConnected=false,thread_flag=false,thread_read_flag=false,isCliConnected=false,cli_thread_read_flag=false,isUDPStart=false;
    StringBuilder tcp_s_sb=new StringBuilder();
    final StringBuilder tcp_c_sb = new StringBuilder();
    StringBuilder udp_s_sb=new StringBuilder();
    StringBuilder udp_c_sb=new StringBuilder();
    private final String TAG="ipop_ser:";
    //服务器最大连接数
    private final int MAXCONNECT=10;
    private static Socket[] client=null;
    private  int client_index=0,tcp_connect_num=0;
    private ServerSocket serverSocket=null;
    private Socket cliSocket=null;
    private EditText edit_ser_port,tcp_ser_info,tcp_ser_Send_txt,tcp_cli_ip,tcp_cli_port,tcp_cli_info,tcp_cli_Send_txt,udp_ser_info;
    private EditText udp_ser_port,udp_cli_info,udp_cli_Send_txt,udp_cli_ip,udp_cli_port;
    private CheckBox udp_ser_echo,tcp_cli_Hex,tcp_ser_Hex,udp_cli_Hex;
    private Button tcp_ser_start,tcp_cli_conn,udp_ser_start;

    private OutputStream outputStream=null,cliOs=null;
    private InputStream inputStream=null,cliIs=null;
    //创建一个DatagramSocket对象，并指定监听端口。（UDP使用DatagramSocket）
    private DatagramSocket udp_ser_socket=null;DatagramSocket udp_cli_socket=null;
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.act_server);
        this.setTitle(String.format("%s: %s",
                getResources().getText(R.string.app_name),
                getResources().getText(R.string.title_server)));
        //得到TabHost对象实例
        TabHost tabhost =(TabHost) findViewById(R.id.tabHost);
        //调用 TabHost.setup()
        tabhost.setup();
        tabhost.addTab(tabhost.newTabSpec("one").setIndicator("  TCP\nServer").setContent(R.id.tab1));
        tabhost.addTab(tabhost.newTabSpec("two").setIndicator("  TCP\r\nClient").setContent(R.id.tab2));
        tabhost.addTab(tabhost.newTabSpec("three").setIndicator("  UDP\r\nServer").setContent(R.id.tab3));
        tabhost.addTab(tabhost.newTabSpec("four").setIndicator("  UDP\r\nClient").setContent(R.id.tab4));

        edit_ser_port=(EditText)findViewById(R.id.tcp_ser_port);
        tcp_ser_info=(EditText)findViewById(R.id.tcp_ser_info);
        tcp_ser_Send_txt=(EditText)findViewById(R.id.tcp_ser_Send_txt);
        tcp_ser_Hex=(CheckBox)findViewById(R.id.tcp_ser_Hex);
        tcp_cli_info=(EditText)findViewById(R.id.tcp_cli_info);
        tcp_cli_ip=(EditText)findViewById(R.id.tcp_cli_ip);
        tcp_cli_port=(EditText)findViewById(R.id.tcp_cli_port);
        tcp_cli_Send_txt=(EditText)findViewById(R.id.tcp_cli_Send_txt);
        tcp_cli_Hex=(CheckBox)findViewById(R.id.tcp_cli_Hex);
        udp_ser_info=(EditText)findViewById(R.id.udp_ser_info);
        udp_cli_info=(EditText)findViewById(R.id.udp_cli_info);
        udp_ser_port=(EditText)findViewById(R.id.udp_ser_port);
        udp_ser_echo=(CheckBox)findViewById(R.id.udp_ser_echo);
        udp_cli_Send_txt=(EditText)findViewById(R.id.udp_cli_Send_txt);
        udp_cli_ip=(EditText)findViewById(R.id.udp_cli_ip);
        udp_cli_port=(EditText)findViewById(R.id.udp_cli_port);
        udp_cli_Hex=(CheckBox)findViewById(R.id.udp_cli_Hex);
        //读取保存的配置
        //sendtxt表示存放时所用的xml文件名称
        SharedPreferences mSharedPreferences=getSharedPreferences("sendtxt", MODE_PRIVATE);
        edit_ser_port.setText(mSharedPreferences.getString("tcp_s_port", "1122"));
        tcp_ser_Send_txt.setText(mSharedPreferences.getString("tcp_s_send", "server"));
        tcp_cli_ip.setText(mSharedPreferences.getString("tcp_c_ip", "127.0.0.1"));
        tcp_cli_port.setText(mSharedPreferences.getString("tcp_c_port", "1122"));
        tcp_cli_Send_txt.setText(mSharedPreferences.getString("tcp_c_send", "test"));
        udp_ser_port.setText(mSharedPreferences.getString("udp_s_port", "2233"));
        udp_cli_ip.setText(mSharedPreferences.getString("udp_c_ip", "127.0.0.1"));
        udp_cli_port.setText(mSharedPreferences.getString("udp_c_port", "2233"));
        udp_cli_Send_txt.setText(mSharedPreferences.getString("udp_c_send", "test"));
        tcp_ser_Hex.setChecked(mSharedPreferences.getBoolean("tcp_ser_Hex", false));
        tcp_cli_Hex.setChecked(mSharedPreferences.getBoolean("tcp_cli_Hex", false));
        udp_cli_Hex.setChecked(mSharedPreferences.getBoolean("udp_cli_Hex", false));
        udp_ser_echo.setChecked(mSharedPreferences.getBoolean("udpecho", false));

        tcp_cli_Hex.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tcp_cli_Send_txt.setText("");
                    tcp_cli_Send_txt.setHint(getString(R.string.ywb_sendhexhint));
                } else
                    tcp_cli_Send_txt.setHint("");

            }
        });
        tcp_ser_Hex.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    tcp_ser_Send_txt.setText("");
                    tcp_ser_Send_txt.setHint(getString(R.string.ywb_sendhexhint));
                }
                else
                    tcp_ser_Send_txt.setHint("");

            }
        });
        udp_cli_Hex.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    udp_cli_Send_txt.setText("");
                    udp_cli_Send_txt.setHint(getString(R.string.ywb_sendhexhint));
                } else
                    udp_cli_Send_txt.setHint("");

            }
        });
        //tcp_ser_info.setMovementMethod(ScrollingMovementMethod.getInstance());
        tcp_ser_start=(Button)findViewById(R.id.tcp_ser_start);
        tcp_ser_start.setOnClickListener(new tcp_ser_start_onClickListener());
        Button tcp_ser_Send=(Button)findViewById(R.id.tcp_ser_Send);
        tcp_ser_Send.setOnClickListener(new tcp_ser_send_onClickListener());
        Button tcp_ser_clr=(Button)findViewById(R.id.tcp_ser_clr);
        tcp_ser_clr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tcp_s_sb.delete(0,tcp_s_sb.length());
                tcp_ser_info.setText("");
            }
        });
        Button tcp_cli_clr=(Button)findViewById(R.id.tcp_cli_clr);
        tcp_cli_clr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tcp_c_sb.delete(0,tcp_c_sb.length());
                tcp_cli_info.setText("");
            }
        });

        Button udp_ser_clr=(Button)findViewById(R.id.udp_ser_clr);
        udp_ser_clr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                udp_s_sb.delete(0,udp_s_sb.length());
                udp_ser_info.setText("");
            }
        });
        Button udp_cli_clr=(Button)findViewById(R.id.udp_cli_clr);
        udp_cli_clr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                udp_c_sb.delete(0,udp_c_sb.length());
                udp_cli_info.setText("");
            }
        });


        tcp_cli_conn=(Button)findViewById(R.id.tcp_cli_conn);
        tcp_cli_conn.setOnClickListener(new tcp_cli_conn_onClickListener());
        Button tcp_cli_Send=(Button)findViewById(R.id.tcp_cli_Send);
        tcp_cli_Send.setOnClickListener(new tcp_cli_send_onClickListener());
        udp_ser_start=(Button)findViewById(R.id.udp_ser_start);
        udp_ser_start.setOnClickListener(new udp_ser_start_onClickListener());
        Button udp_cli_Send=(Button)findViewById(R.id.udp_cli_Send);
        udp_cli_Send.setOnClickListener(new udp_cli_send_onClickListener());
        client=new Socket[MAXCONNECT];
        ActionBarWrapper actionBar = ActionBarWrapper.getActionBar(this);
        actionBar.setDisplayHomeAsUpEnabled(true);
        Log.i(TAG, "onCreat called.");
    }
    private String TxTotHexStr(String txtstr)
    {
        StringBuilder tempsb=new StringBuilder();
        try {
            byte[] bs = txtstr.getBytes("ISO-8859-1");

        for(int i=0;i<bs.length;i++) {
            tempsb.append(String.format("%x", bs[i]));
            if(i!=bs.length-1)
                tempsb.append(" ");
        }
        }catch(Exception e){}
            return tempsb.toString();
    }
    private byte[] HexStrToTxt(String hexstr)
    {
        hexstr=hexstr.replace("\n"," ");
        String temps="";
        String pattern = " ";
        Pattern pat = Pattern.compile(pattern);
        String[] temps2 = pat.split(hexstr);
        byte[] baKeyword = new byte[temps2.length];
        for(int i = 0; i < baKeyword.length; i++)
        {
            try
            {
                baKeyword[i] = (byte)(0xff & Integer.parseInt(temps2[i],16));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
      /*  try
        {
            temps = new String(baKeyword,"ISO-8859-1");//UTF-16le:Not
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }

        return temps;*/
        return baKeyword;

    }
    //Activity从后台重新回到前台时被调用
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart called.");
    }
    //Activity创建或者从后台重新回到前台时被调用
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart called.");
    }//Activity创建或者从被覆盖、后台重新回到前台时被调用
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume called.");
    } @Override
      protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause called.");
        //有可能在执行完onPause或onStop后,系统资源紧张将Activity杀死,所以有必要在此保存持久数据
    }
    //退出当前Activity或者跳转到新Activity时被调用
    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop called.");
    }

    //退出当前Activity时被调用,调用之后Activity就结束了
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //保存配置
        SharedPreferences mSharedPreferences = getSharedPreferences("sendtxt",MODE_PRIVATE);
        mSharedPreferences.edit().putString("tcp_s_port", edit_ser_port.getText().toString()).commit();
        mSharedPreferences.edit().putString("tcp_s_send", tcp_ser_Send_txt.getText().toString()).commit();
        mSharedPreferences.edit().putString("tcp_c_ip", tcp_cli_ip.getText().toString()).commit();
        mSharedPreferences.edit().putString("tcp_c_port", tcp_cli_port.getText().toString()).commit();
        mSharedPreferences.edit().putString("tcp_c_send", tcp_cli_Send_txt.getText().toString()).commit();
        mSharedPreferences.edit().putString("udp_s_port", udp_ser_port.getText().toString()).commit();
        mSharedPreferences.edit().putString("udp_c_ip", udp_cli_ip.getText().toString()).commit();
        mSharedPreferences.edit().putString("udp_c_port", udp_cli_port.getText().toString()).commit();
        mSharedPreferences.edit().putString("udp_c_send", udp_cli_Send_txt.getText().toString()).commit();

        mSharedPreferences.edit().putBoolean("tcp_ser_Hex", tcp_ser_Hex.isChecked()).commit();
        mSharedPreferences.edit().putBoolean("tcp_cli_Hex", tcp_cli_Hex.isChecked()).commit();
        mSharedPreferences.edit().putBoolean("udp_cli_Hex", udp_cli_Hex.isChecked()).commit();
        mSharedPreferences.edit().putBoolean("udpecho", udp_ser_echo.isChecked()).commit();
        //清除活动连接
        if(isCliConnected)
        {
            try {
                if (cliSocket != null)
                    cliSocket.close();
            }catch (Exception e){}
        }
        if(udp_cli_socket!=null)
           udp_cli_socket.close();
        if(isConnected){
            try{
                for(int i=0;i<MAXCONNECT;i++)
                    if(client[i]!=null)
                        if(client[i].isConnected())
                            client[i].close();
                if(serverSocket!=null)
                serverSocket.close();
            }catch (Exception e){
                Log.d(TAG, e.getMessage());
            }
        }
        if(isUDPStart){
            try{
                if(udp_ser_socket!=null)
                    udp_ser_socket.close();
            }catch (Exception e){
                Log.d(TAG, e.getMessage());
            }
    }
        Log.i(TAG, "onDestory called.");
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
               finish();
                //moveTaskToBack(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class ReadThread implements  Runnable {
        int index;
        public ReadThread(int index){
            this.index=index;
        }
        public void run() {
            byte[] data=new byte[1024];
            try{
                while (true){
                    inputStream=client[index].getInputStream();
                    int readbytes=inputStream.read(data);
                    if(readbytes==-1){
                        thread_read_flag=false;
                        tcp_connect_num--;
                            tcp_s_sb.append(client[index].getInetAddress().getHostAddress() + " Disconnect!\r\n["+String.valueOf(tcp_connect_num)+"]client have connected!\r\n");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tcp_ser_info.setText(tcp_s_sb.toString());
                                tcp_ser_info.setSelection(tcp_ser_info.getText().length(), tcp_ser_info.getText().length());
                            }
                        });
                        inputStream.close();
                        client[index].close();
                        client[index]=null;
                        break;
                    }

                    if(readbytes==0) continue;
                    Log.d(TAG, "read from" + client[index].getRemoteSocketAddress().toString());
                    String temps=new String(data, 0, readbytes);
                    String temps2=new String(data, 0, readbytes,"ISO-8859-1");

                  //  tcp_s_sb.append("["+client[index].getRemoteSocketAddress().toString()+"]"+temps+"\r\n[rec "+Integer.toString(readbytes)+" byte:"+TxTotHexStr(temps2)+"]\r\n");
                    tcp_s_sb.append("["+client[index].getInetAddress().getHostAddress()+":"+Integer.toString(client[index].getPort())+"]"+temps+"\r\n[rec "+Integer.toString(readbytes)+" byte:"+TxTotHexStr(temps2)+"]\r\n");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tcp_ser_info.setText(tcp_s_sb.toString());
                            tcp_ser_info.setSelection(tcp_ser_info.getText().length(), tcp_ser_info.getText().length());
                        }
                    });
                }

            }catch (Exception e){
                Log.d(TAG, e.getMessage());
                thread_read_flag=false;
            }finally {
                try{
                    inputStream.close();

                }catch (Exception e){}
            }

        }
    }
    class SocketServerThread implements  Runnable{
        public void run(){
            try{
                while (thread_flag){

                    client_index=client_index%MAXCONNECT;
                    client[client_index]=serverSocket.accept();
                    tcp_connect_num++;
                    tcp_s_sb.append(client[client_index].getInetAddress().getHostAddress() + " Connect!\r\n["+String.valueOf(tcp_connect_num)+"]client have connected!\r\n");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tcp_ser_info.setText(tcp_s_sb.toString());
                            tcp_ser_info.setSelection(tcp_ser_info.getText().length(), tcp_ser_info.getText().length());
                        }
                    });
                   // Log.d(TAG, client[client_index].getInetAddress().toString()+"Connect!");
                    thread_read_flag=true;
                    new Thread(new ReadThread(client_index)).start();
                    client_index++;
                }

            }catch (Exception e){
                Log.d(TAG, e.getMessage());
                thread_flag=false;
                thread_read_flag=false;
            }

        }
    }
    class tcp_ser_start_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            if(isConnected==false){
                try{

                    tcp_connect_num=0;
                    serverSocket=new ServerSocket(Integer.parseInt(edit_ser_port.getText().toString()));
                    if(serverSocket==null) {
                        tcp_ser_info.setText("Socket creat fail!");
                        tcp_ser_info.setSelection(tcp_ser_info.getText().length(), tcp_ser_info.getText().length());
                        return;
                    }
                    tcp_s_sb.delete(0,tcp_s_sb.length());
                    tcp_s_sb.append("Server Start!\r\n");
                    thread_flag=true;
                    new Thread(new SocketServerThread()).start();

                    tcp_ser_start.setText(getString(R.string.ywb_tcp_stop));
                    tcp_ser_info.setText(tcp_s_sb.toString());
                    tcp_ser_info.setSelection(tcp_ser_info.getText().length(), tcp_ser_info.getText().length());
                    isConnected=true;

                    thread_read_flag=true;
                }catch (Exception e){
                    Log.d(TAG, e.getMessage());
                    tcp_ser_info.setText(e.getMessage());
                    isConnected=false;
                    thread_flag=false;
                }

            }else {
                isConnected=false;
                thread_flag=false;
                tcp_s_sb.append("Server Stop!\r\n");
                tcp_ser_info.setText(tcp_s_sb.toString());
                tcp_ser_info.setSelection(tcp_ser_info.getText().length(), tcp_ser_info.getText().length());
                try{
                    for(int i=0;i<MAXCONNECT;i++)
                        if(client[i]!=null)
                        if(client[i].isConnected())
                            client[i].close();
                    serverSocket.close();
                }catch (Exception e){
                    Log.d(TAG, e.getMessage());
                }
                tcp_ser_start.setText(getString(R.string.ywb_tcp_start));


            }
        }

    }
    class tcp_ser_send_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            if(isConnected==false) return;
            try{
                byte tempb[];
                if(tcp_ser_Hex.isChecked())
                     tempb=HexStrToTxt(tcp_ser_Send_txt.getText().toString());
                else
                     tempb=tcp_ser_Send_txt.getText().toString().getBytes();
                for(int i=0;i<MAXCONNECT;i++)
                {
                    if(client[i]!=null){
                        if(client[i].isConnected()){
                            client[i].getOutputStream().write(tempb);
                        }

                    }
                }
                tcp_s_sb.append("Send " + Integer.toString(tempb.length) + " byte data!\r\n");
                tcp_ser_info.setText(tcp_s_sb.toString());
                tcp_ser_info.setSelection(tcp_ser_info.getText().length(), tcp_ser_info.getText().length());
            }catch (Exception e){
                Log.d(TAG, e.getMessage());
            }


        }
    }
    class tcp_cli_conn_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            if(isCliConnected==false){



                            tcp_cli_conn.setText(getString(R.string.ywb_cli_disconnect));
                            isCliConnected = true;

                            new Thread(new TCP_Cli_ReadThread()).start();



            }else {

                try {
                    if (cliSocket != null)
                        cliSocket.close();
                }catch (Exception e){}
                tcp_cli_conn.setText(getString(R.string.ywb_cli_connect));
                isCliConnected=false;
                cli_thread_read_flag=false;

            }
        }

    }
    class TCP_Cli_ReadThread implements  Runnable {

        public void run() {
            byte[] data=new byte[1024];

            try{
                InetAddress serverAddr = InetAddress.getByName(tcp_cli_ip.getText().toString());
                cliSocket = new Socket(serverAddr, Integer.parseInt(tcp_cli_port.getText().toString()));
                tcp_c_sb.append("Connect success!\r\n");
                runOnUiThread(new Runnable() {
                    public void run() {
                        tcp_cli_info.setText(tcp_c_sb.toString());
                        tcp_cli_info.setSelection(tcp_cli_info.getText().length(), tcp_cli_info.getText().length());
                    }
                });
                cli_thread_read_flag = true;
                cliOs = cliSocket.getOutputStream();
                cliIs = cliSocket.getInputStream();
                while (cli_thread_read_flag){

                    int readbytes=cliIs.read(data);
                    if(readbytes==-1){
                        cli_thread_read_flag=false;
                        tcp_c_sb.append("Connect interrupt!\r\n");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tcp_cli_conn.setText(getString(R.string.ywb_cli_connect));
                                tcp_cli_info.setText(tcp_c_sb.toString());
                                tcp_cli_info.setSelection(tcp_cli_info.getText().length(), tcp_cli_info.getText().length());
                            }
                        });
                        cliIs.close();
                        isCliConnected=false;
                        break;
                    }
                    if(readbytes==0) continue;
                    String temps=new String(data, 0, readbytes);
                    String temps2=new String(data, 0, readbytes,"ISO-8859-1");
                    tcp_c_sb.append(temps+"\r\n[rec "+Integer.toString(readbytes)+" byte:"+TxTotHexStr(temps2)+"]\r\n");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tcp_cli_info.setText(tcp_c_sb.toString());
                            tcp_cli_info.setSelection(tcp_cli_info.getText().length(), tcp_cli_info.getText().length());
                        }
                    });
                }

            }catch (Exception e) {
                Log.d(TAG, e.getMessage());
                if(cli_thread_read_flag)
                    tcp_c_sb.append("Connect disconnect!\r\n");
                else
                     tcp_c_sb.append(e.getMessage()+"\r\n");

                runOnUiThread(new Runnable() {
                    public void run() {
                        tcp_cli_info.setText(tcp_c_sb.toString());
                        tcp_cli_conn.setText(getString(R.string.ywb_cli_connect));
                        tcp_cli_info.setSelection(tcp_cli_info.getText().length(), tcp_cli_info.getText().length());
                    }
                });
                isCliConnected=false;
                cli_thread_read_flag=false;

            }finally {
                try{
                    cliIs.close();
                }catch (Exception e){}
            }

        }
    }
    class tcp_cli_send_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            if(cliOs==null) return;
            try{
                String temps="";
                int sendc=0;
                if(tcp_cli_Hex.isChecked())
                {
                    // temps=HexStrToTxt(tcp_cli_Send_txt.getText().toString());
                   // cliOs.write(temps.getBytes("ISO-8859-1"));
                   // sendc=temps.getBytes("ISO-8859-1").length;
                    byte tempb[]=HexStrToTxt(tcp_cli_Send_txt.getText().toString());
                    cliOs.write(tempb);
                    sendc=tempb.length;
                }
                else
                {
                    temps=tcp_cli_Send_txt.getText().toString();
                    cliOs.write(temps.getBytes());
                    sendc=temps.getBytes().length;
                }

                cliOs.flush();
                tcp_c_sb.append("Send " + Integer.toString(sendc) + " byte data!\r\n");

                tcp_cli_info.setText(tcp_c_sb.toString());
                tcp_cli_info.setSelection(tcp_cli_info.getText().length(), tcp_cli_info.getText().length());

            }catch (Exception e){}


        }
    }
    class udp_ser_start_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            if(isUDPStart==false) {
                udp_ser_start.setText(getString(R.string.ywb_tcp_stop));
                try {
                    udp_ser_socket = new DatagramSocket(Integer.parseInt(udp_ser_port.getText().toString()));
                    udp_s_sb.append("Server Start!\r\n");
                    udp_ser_info.setText(udp_s_sb.toString());
                    udp_ser_info.setSelection(udp_ser_info.getText().length(), udp_ser_info.getText().length());
                    new Thread(new UDP_Ser_ReadThread()).start();

                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isUDPStart=true;
            } else
            {
                udp_ser_start.setText(getString(R.string.ywb_tcp_start));
                udp_s_sb.append("Server Stop!\r\n");
                udp_ser_info.setText(udp_s_sb.toString());
                udp_ser_info.setSelection(udp_ser_info.getText().length(), udp_ser_info.getText().length());
                try{
                    if(udp_ser_socket!=null)
                    udp_ser_socket.close();
                }catch (Exception e){
                    Log.d(TAG, e.getMessage());
                }
                isUDPStart=false;

            }
        }
    }
    class UDP_Ser_ReadThread implements  Runnable {

        public void run() {
            //创建一个byte类型的数组，用于存放接收到得数据
            byte data[] = new byte[4 * 1024];



            try{


                while (isUDPStart){

                    //创建一个DatagramPacket对象，并指定DatagramPacket对象的大小
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    //读取接收到得数据
                    udp_ser_socket.receive(packet);
                    //把客户端发送的数据转换为字符串。
                    //使用三个参数的String方法。参数一：数据包 参数二：起始位置 参数三：数据包长
                    String temps=new String(packet.getData(), packet.getOffset(), packet.getLength());
                    String temps2=new String(packet.getData(), packet.getOffset(), packet.getLength(),"ISO-8859-1");

                    udp_s_sb.append("["+packet.getAddress().getHostAddress()+":"+Integer.toString(packet.getPort())+"]"+temps+"\r\n[rec "+Integer.toString(temps2.length())+" byte:"+TxTotHexStr(temps2)+"]\r\n");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            udp_ser_info.setText(udp_s_sb.toString());
                            udp_ser_info.setSelection(udp_ser_info.getText().length(), udp_ser_info.getText().length());
                        }
                    });

                    boolean bEcho=udp_ser_echo.isChecked();
                    if(bEcho){
                        //回送字符
                        DatagramPacket spacket = new DatagramPacket(packet.getData(), packet.getLength() ,packet.getAddress() ,packet.getPort());
                        udp_ser_socket.send(spacket);
                    }
                }

            }catch (Exception e){
                Log.d(TAG, e.getMessage());

            }finally {
                try{

                }catch (Exception e){}
            }

        }
    }
    class udp_cli_send_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            new Thread(new UDP_SendThread()).start();


        }
    }

    class UDP_Cli_ReadThread implements  Runnable {

        public void run() {
            //创建一个byte类型的数组，用于存放接收到得数据
            byte data[] = new byte[4 * 1024];



            try{


                while (true){

                    //创建一个DatagramPacket对象，并指定DatagramPacket对象的大小
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    //读取接收到得数据
                    udp_cli_socket.receive(packet);
                    //把客户端发送的数据转换为字符串。
                    //使用三个参数的String方法。参数一：数据包 参数二：起始位置 参数三：数据包长
                    String temps=new String(packet.getData(), packet.getOffset(), packet.getLength());
                    String temps2=new String(packet.getData(), packet.getOffset(), packet.getLength(),"ISO-8859-1");
                    udp_c_sb.append(temps+"\r\n[rec "+Integer.toString(temps2.length())+" byte:"+TxTotHexStr(temps2)+"]\r\n");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            udp_cli_info.setText(udp_c_sb.toString());
                            udp_cli_info.setSelection(udp_cli_info.getText().length(), udp_cli_info.getText().length());
                        }
                    });
                }

            }catch (Exception e){
                Log.d(TAG, e.getMessage());

            }finally {
                try{

                }catch (Exception e){}
            }

        }
    }

    class UDP_SendThread implements  Runnable {

        public void run() {

            try {
                //创建DatagramSocket对象并指定一个端口号，注意，如果客户端需要接收服务器的返回数据,
                //还需要使用这个端口号来receive，所以一定要记住
                if(udp_cli_socket==null)
                udp_cli_socket = new DatagramSocket();
                new Thread(new UDP_Cli_ReadThread()).start();
                //使用InetAddress(Inet4Address).getByName把IP地址转换为网络地址
                InetAddress serverAddress = InetAddress.getByName(udp_cli_ip.getText().toString());
                //Inet4Address serverAddress = (Inet4Address) Inet4Address.getByName("192.168.1.32");
                byte data[];
                if(udp_cli_Hex.isChecked())
                    data = HexStrToTxt(udp_cli_Send_txt.getText().toString());//把字符串str字符串转换为字节数组
                else
                 data = udp_cli_Send_txt.getText().toString().getBytes();//把字符串str字符串转换为字节数组
                //创建一个DatagramPacket对象，用于发送数据。
                //参数一：要发送的数据  参数二：数据的长度  参数三：服务端的网络地址  参数四：服务器端端口号
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress ,Integer.parseInt(udp_cli_port.getText().toString()));
                udp_cli_socket.send(packet);//把数据发送到服务端。
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

