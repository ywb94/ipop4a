package org.ywb_ipop;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

/**
 * Created by Administrator on 2015/5/12.
 */
public class ServerActivity  extends Activity{
    private  boolean isConnected=false,thread_flag=false,thread_read_flag=false,isCliConnected=false,cli_thread_read_flag=false,isUDPStart=false;
    StringBuilder rec_str=null;
    private final String TAG="ipop_ser:";
    //服务器最大连接数
    private final int MAXCONNECT=10;
    private static Socket[] client=null;
    private  int client_index=0,tcp_connect_num=0;
    private ServerSocket serverSocket=null;
    private Socket cliSocket=null;
    private EditText edit_ser_port,tcp_ser_info,tcp_ser_Send_txt,tcp_cli_ip,tcp_cli_port,tcp_cli_info,tcp_cli_Send_txt,udp_ser_info;
    private EditText udp_ser_port,udp_cli_info,udp_cli_Send_txt,udp_cli_ip,udp_cli_port;
    private Button tcp_ser_start,tcp_cli_conn,udp_ser_start;
    private OutputStream outputStream=null,cliOs=null;
    private InputStream inputStream=null,cliIs=null;
    //创建一个DatagramSocket对象，并指定监听端口。（UDP使用DatagramSocket）
    private DatagramSocket udp_ser_socket=null;
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
        tcp_cli_info=(EditText)findViewById(R.id.tcp_cli_info);
        tcp_cli_ip=(EditText)findViewById(R.id.tcp_cli_ip);
        tcp_cli_port=(EditText)findViewById(R.id.tcp_cli_port);
        tcp_cli_Send_txt=(EditText)findViewById(R.id.tcp_cli_Send_txt);
        udp_ser_info=(EditText)findViewById(R.id.udp_ser_info);
        udp_cli_info=(EditText)findViewById(R.id.udp_cli_info);
        udp_ser_port=(EditText)findViewById(R.id.udp_ser_port);
        udp_cli_Send_txt=(EditText)findViewById(R.id.udp_cli_Send_txt);
        udp_cli_ip=(EditText)findViewById(R.id.udp_cli_ip);
        udp_cli_port=(EditText)findViewById(R.id.udp_cli_port);

        tcp_ser_start=(Button)findViewById(R.id.tcp_ser_start);
        tcp_ser_start.setOnClickListener(new tcp_ser_start_onClickListener());
        Button tcp_ser_Send=(Button)findViewById(R.id.tcp_ser_Send);
        tcp_ser_Send.setOnClickListener(new tcp_ser_send_onClickListener());
        Button tcp_ser_clr=(Button)findViewById(R.id.tcp_ser_clr);
        tcp_ser_clr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tcp_ser_info.setText("");
            }
        });
        Button tcp_cli_clr=(Button)findViewById(R.id.tcp_ser_clr);
        tcp_cli_clr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tcp_cli_info.setText("");
            }
        });

        Button udp_ser_clr=(Button)findViewById(R.id.udp_ser_clr);
        udp_ser_clr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                udp_ser_info.setText("");
            }
        });
        Button udp_cli_clr=(Button)findViewById(R.id.udp_cli_clr);
        udp_cli_clr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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
                        rec_str.append(client[index].getInetAddress().toString() + " Disconnect!["+String.valueOf(tcp_connect_num)+"]client have connected!\r\n");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tcp_ser_info.setText(rec_str.toString());
                            }
                        });
                        inputStream.close();
                        client[index].close();
                        client[index]=null;
                        break;
                    }

                    if(readbytes==0) continue;
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
                    tcp_connect_num++;
                    rec_str.append(client[client_index].getInetAddress().toString() + " Connect!["+String.valueOf(tcp_connect_num)+"]client have connected!\r\n");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tcp_ser_info.setText(rec_str.toString());
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
                    rec_str=new StringBuilder();
                    tcp_connect_num=0;
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
                runOnUiThread(new Runnable() {
                    public void run() {
                        tcp_cli_conn.setText(getString(R.string.ywb_cli_connect));
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
                cliOs.write(tcp_cli_Send_txt.getText().toString().getBytes());
                cliOs.flush();

            }catch (Exception e){}


        }
    }
    class udp_ser_start_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            if(isUDPStart==false) {
                udp_ser_start.setText(getString(R.string.ywb_tcp_stop));
                try {
                    udp_ser_socket = new DatagramSocket(Integer.parseInt(udp_ser_port.getText().toString()));
                    new Thread(new UDP_Ser_ReadThread()).start();

                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isUDPStart=true;
            }
            else
            {
                udp_ser_start.setText(getString(R.string.ywb_tcp_start));
                isUDPStart=false;

            }
        }
    }
    class UDP_Ser_ReadThread implements  Runnable {

        public void run() {
            //创建一个byte类型的数组，用于存放接收到得数据
            byte data[] = new byte[4 * 1024];



            final StringBuilder sb = new StringBuilder();
            try{


                while (isUDPStart){

                    //创建一个DatagramPacket对象，并指定DatagramPacket对象的大小
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    //读取接收到得数据
                    udp_ser_socket.receive(packet);
                    //把客户端发送的数据转换为字符串。
                    //使用三个参数的String方法。参数一：数据包 参数二：起始位置 参数三：数据包长
                    sb.append(new String(packet.getData(), packet.getOffset(), packet.getLength()));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            udp_ser_info.setText(sb.toString());
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
    class udp_cli_send_onClickListener implements View.OnClickListener {
        public void onClick(View arg0) {
            new Thread(new UDP_SendThread()).start();


        }
    }

    class UDP_SendThread implements  Runnable {

        public void run() {
            DatagramSocket socket;
            try {
                //创建DatagramSocket对象并指定一个端口号，注意，如果客户端需要接收服务器的返回数据,
                //还需要使用这个端口号来receive，所以一定要记住
                socket = new DatagramSocket();
                //使用InetAddress(Inet4Address).getByName把IP地址转换为网络地址
                InetAddress serverAddress = InetAddress.getByName(udp_cli_ip.getText().toString());
                //Inet4Address serverAddress = (Inet4Address) Inet4Address.getByName("192.168.1.32");

                byte data[] = udp_cli_Send_txt.getText().toString().getBytes();//把字符串str字符串转换为字节数组
                //创建一个DatagramPacket对象，用于发送数据。
                //参数一：要发送的数据  参数二：数据的长度  参数三：服务端的网络地址  参数四：服务器端端口号
                DatagramPacket packet = new DatagramPacket(data, data.length ,serverAddress ,Integer.parseInt(udp_cli_port.getText().toString()));
                socket.send(packet);//把数据发送到服务端。
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

