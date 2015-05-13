package org.ywb_ipop;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Administrator on 2015/5/12.
 */
public class ServerActivity  extends Activity{
    private  boolean isConnected=false,thread_flag=false,thread_read_flag=false,isCliConnected=false,cli_thread_read_flag=false;
    StringBuilder rec_str=null;
    private final String TAG="ipop_ser:";
    //服务器最大连接数
    private final int MAXCONNECT=10;
    private static Socket[] client=null;
    private  int client_index=0;
    private ServerSocket serverSocket=null;
    private Socket cliSocket=null;
    private EditText edit_ser_port,tcp_ser_info,tcp_ser_Send_txt,tcp_cli_ip,tcp_cli_port,tcp_cli_info,tcp_cli_Send_txt;
    private Button tcp_ser_start,tcp_cli_conn;
    private OutputStream outputStream=null,cliOs=null;
    private InputStream inputStream=null,cliIs=null;
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
        tabhost.addTab(tabhost.newTabSpec("one").setIndicator("TCP").setContent(R.id.tab1));
        tabhost.addTab(tabhost.newTabSpec("two").setIndicator("UDP").setContent(R.id.tab2));
        tabhost.addTab(tabhost.newTabSpec("three").setIndicator("WEB").setContent(R.id.tab3));
        edit_ser_port=(EditText)findViewById(R.id.tcp_ser_port);
        tcp_ser_info=(EditText)findViewById(R.id.tcp_ser_info);
        tcp_ser_Send_txt=(EditText)findViewById(R.id.tcp_ser_Send_txt);
        tcp_cli_info=(EditText)findViewById(R.id.tcp_cli_info);
        tcp_cli_ip=(EditText)findViewById(R.id.tcp_cli_ip);
        tcp_cli_port=(EditText)findViewById(R.id.tcp_cli_port);
        tcp_cli_Send_txt=(EditText)findViewById(R.id.tcp_cli_Send_txt);

        tcp_ser_start=(Button)findViewById(R.id.tcp_ser_start);
        tcp_ser_start.setOnClickListener(new tcp_ser_start_onClickListener());
        Button tcp_ser_Send=(Button)findViewById(R.id.tcp_ser_Send);
        tcp_ser_Send.setOnClickListener(new tcp_ser_send_onClickListener());
        Button tcp_ser_dis=(Button)findViewById(R.id.tcp_ser_dis);
        tcp_ser_dis.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tcp_ser_info.setText("");
            }
        });

        tcp_cli_conn=(Button)findViewById(R.id.tcp_cli_conn);
        tcp_cli_conn.setOnClickListener(new tcp_cli_conn_onClickListener());
        Button tcp_cli_Send=(Button)findViewById(R.id.tcp_cli_Send);
        tcp_cli_Send.setOnClickListener(new tcp_cli_send_onClickListener());
        client=new Socket[MAXCONNECT];
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
                    if(readbytes<=0) continue;
                    Log.d(TAG, "read from"+client[index].getRemoteSocketAddress().toString());
                    rec_str.append(new String(data, 0, readbytes));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tcp_ser_info.setText(rec_str.toString());
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
                    Log.d(TAG, client[client_index].getInetAddress().toString()+"Connect!");
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
                    rec_str=new StringBuilder();
                    serverSocket=new ServerSocket(Integer.parseInt(edit_ser_port.getText().toString()));
                    new Thread(new SocketServerThread()).start();
                    tcp_ser_start.setText(getString(R.string.ywb_tcp_stop));
                    isConnected=true;
                    thread_flag=true;
                    thread_read_flag=true;
                }catch (Exception e){
                    Log.d(TAG, e.getMessage());
                    isConnected=false;
                    thread_flag=false;
                }

            }else {
                isConnected=false;
                thread_flag=false;
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

            try{
                for(int i=0;i<MAXCONNECT;i++)
                {
                    if(client[i]!=null){
                        if(client[i].isConnected()){
                            client[i].getOutputStream().write(tcp_ser_Send_txt.getText().toString().getBytes());
                        }

                    }
                }


            }catch (Exception e){}


        }
    }
    class tcp_cli_conn_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            if(isCliConnected==false){



                            tcp_cli_conn.setText(getString(R.string.ywb_cli_disconnect));
                            isCliConnected = true;
                            cli_thread_read_flag = true;
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
            final StringBuilder sb = new StringBuilder();
            try{
                InetAddress serverAddr = InetAddress.getByName(tcp_cli_ip.getText().toString());
                cliSocket = new Socket(serverAddr, Integer.parseInt(tcp_cli_port.getText().toString()));
                cliOs = cliSocket.getOutputStream();
                cliIs = cliSocket.getInputStream();
                while (cli_thread_read_flag){

                    int readbytes=cliIs.read(data);
                    if(readbytes==-1){
                        cli_thread_read_flag=false;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tcp_cli_conn.setText(getString(R.string.ywb_cli_connect));
                            }
                        });
                        cliIs.close();
                        isCliConnected=false;
                        break;
                    }
                    if(readbytes==0) continue;

                    sb.append(new String(data, 0, readbytes));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tcp_cli_info.setText(sb.toString());
                        }
                    });
                }

            }catch (Exception e){
                Log.d(TAG, e.getMessage());
                tcp_cli_conn.setText(getString(R.string.ywb_cli_connect));
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
                cliOs.write(tcp_cli_Send_txt.getText().toString().getBytes());
                cliOs.flush();

            }catch (Exception e){}


        }
    }
}

