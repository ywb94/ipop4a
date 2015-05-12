/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2007 Kenny Root, Jeffrey Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ywb_ipop;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.ywb_ipop.bean.HostBean;
import org.ywb_ipop.service.TerminalBridge;
import org.ywb_ipop.service.TerminalManager;
import org.ywb_ipop.transport.TransportFactory;
import org.ywb_ipop.util.HostDatabase;
import org.ywb_ipop.util.PreferenceConstants;
import org.ywb_ipop.util.UpdataInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class HostListActivity extends ListActivity {
	public final static int REQUEST_EDIT = 1;

	public final static int REQUEST_EULA = 2;
   //蓝牙
	public final static  int	REQUEST_DISCOVERY=0x3;


	/* 取得默认的蓝牙适配器 */
	final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	private  boolean bBluestatu=false;
	protected TerminalManager bound = null;

	private final static String TAG = "host:";

	private final static  int	UPDATA_CLIENT=0;
	private final static  int	GET_UNDATAINFO_ERROR=1;
	private final static  int	DOWN_ERROR=2;
	private UpdataInfo info;

	//protected HostDatabase hostdb;
    public HostDatabase hostdb;
	private List<HostBean> hosts;
	protected LayoutInflater inflater = null;

	protected boolean sortedByColor = false;

	private MenuItem sortcolor,ipinfo,servermenu;

	private MenuItem sortlast;

	private Spinner transportSpinner;
	private TextView quickconnect;
    private Button connectbutton;

	private SharedPreferences prefs = null;

	protected boolean makingShortcut = false;

	protected Handler updateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			HostListActivity.this.updateList();
		}
	};
	Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
				case UPDATA_CLIENT:
					//对话框通知用户升级程序
					showUpdataDialog();
					break;
				case GET_UNDATAINFO_ERROR:
					//服务器超时
					//Toast.makeText(getApplicationContext(), "获取服务器更新信息失败", 1).show();

					break;
				case DOWN_ERROR:
					//下载apk失败
					//Toast.makeText(getApplicationContext(), "下载新版本失败", 1).show();

					break;
			}
		}
	};

	/*
     *
     * 弹出对话框通知用户更新程序
     *
     * 弹出对话框的步骤：
     *  1.创建alertDialog的builder.
     *  2.要给builder设置属性, 对话框的内容,样式,按钮
     *  3.通过builder 创建一个对话框
     *  4.对话框show()出来
     */
	protected void showUpdataDialog() {
		AlertDialog.Builder builer = new AlertDialog.Builder(HostListActivity.this) ;
		builer.setTitle(getString(R.string.ywb_updatetitle));
		builer.setMessage(info.getDescription());
		//当点确定按钮时从服务器上下载 新的apk 然后安装
		builer.setPositiveButton(getString(R.string.ywb_filedlg_ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Log.i(TAG, "下载apk,更新");
				downLoadApk();
			}
		});
		//当点取消按钮时进行登录
		builer.setNegativeButton(getString(R.string.ywb_filedlg_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {


			}
		});
		AlertDialog dialog = builer.create();
		dialog.show();
	}

	/*
     * 从服务器中下载APK
     */
	protected void downLoadApk() {
		final ProgressDialog pd;    //进度条对话框
		pd = new  ProgressDialog(this);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setMessage(getString(R.string.ywb_updating));
		pd.show();
		new Thread(){
			@Override
			public void run() {
				try {
					File file = getFileFromServer(info.getUrl(), pd);
					sleep(3000);
					installApk(file);
					pd.dismiss(); //结束掉进度条对话框
				} catch (Exception e) {
					Message msg = new Message();
					msg.what = DOWN_ERROR;
					handler.sendMessage(msg);
					e.printStackTrace();
				}
			}}.start();
	}

	//安装apk
	protected void installApk(File file) {
		Intent intent = new Intent();
		//执行动作
		intent.setAction(Intent.ACTION_VIEW);
		//执行的数据类型
		intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
		startActivity(intent);
	}

	/*
	 * 获取当前程序的版本号
	 */
	private String getVersionName() throws Exception{
		//获取packagemanager的实例
		PackageManager packageManager = getPackageManager();
		//getPackageName()是你当前类的包名，0代表是获取版本信息
		PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
		return packInfo.versionName;
	}
	/**
	 * 把输入流转换成字符数组
	 * @param inputStream   输入流
	 * @return  字符数组
	 * @throws Exception
	 */
	public static byte[] readStream(InputStream inputStream) throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			bout.write(buffer, 0, len);
		}
		bout.close();
		inputStream.close();

		return bout.toByteArray();

	}


	/*
 * 用pull解析器解析服务器返回的xml文件 (xml封装了版本号)
 */
	public static UpdataInfo getUpdataInfo(InputStream is) throws Exception{
		UpdataInfo info = new UpdataInfo();//实体
		byte[] data = readStream(is);   // 把输入流转换成字符数组
		String json = new String(data,"UTF-8");        // 把字符数组转换成字符串
		JSONObject jsonObject=new JSONObject(json);     //返回的数据形式是一个Object类型，所以可以直接转换成一个Object
		String version=jsonObject.getString("ver");
		String url=jsonObject.getString("address");
		String description=jsonObject.getString("demo");
		info.setVersion(version);	//获取版本号
		info.setUrl(url);    //获取要升级的APK文件
		info.setDescription(description);    //获取该文件的信息
		return info;
	}
	public static File getFileFromServer(String path, ProgressDialog pd) throws Exception{
		//如果相等的话表示当前的sdcard挂载在手机上并且是可用的
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			URL url = new URL(path);
			HttpURLConnection conn =  (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			//获取到文件的大小
			pd.setMax(conn.getContentLength());
			InputStream is = conn.getInputStream();
			File file = new File(Environment.getExternalStorageDirectory(), "updata.apk");
			FileOutputStream fos = new FileOutputStream(file);
			BufferedInputStream bis = new BufferedInputStream(is);
			byte[] buffer = new byte[1024];
			int len ;
			int total=0;
			while((len =bis.read(buffer))!=-1){
				fos.write(buffer, 0, len);
				total+= len;
				//获取当前下载量
				pd.setProgress(total);
			}
			fos.close();
			bis.close();
			is.close();
			return file;
		}
		else{
			return null;
		}
	}
	private void  CheckVersionTask()
	{
		new Thread(new Runnable() {
			public void run() {
				try {
					//从资源文件获取服务器 地址
					String path = "http://www.iytc.net/ver.php?softname=ipop&ver="+getVersionName();
					//String path = "http://www.iytc.net/ipop.xml";
					//包装成url的对象
					URL url = new URL(path);
					HttpURLConnection conn =  (HttpURLConnection) url.openConnection();
					conn.setConnectTimeout(5000);

					InputStream is =conn.getInputStream();
					info =  getUpdataInfo(is);

					if(info.getVersion().equals(getVersionName())){
						Log.i(TAG,"版本号相同无需升级");
						return;
					}else{
						Log.i(TAG,"版本号不同 ,提示用户升级 ");
						Message msg = new Message();
						msg.what = UPDATA_CLIENT;
						handler.sendMessage(msg);
					}
				} catch (Exception e) {
					// 待处理
					Message msg = new Message();
					msg.what = GET_UNDATAINFO_ERROR;
					handler.sendMessage(msg);
					e.printStackTrace();
				}
			}
		}).start();

	}


	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			bound = ((TerminalManager.TerminalBinder) service).getService();

			// update our listview binder to find the service
			HostListActivity.this.updateList();
		}

		public void onServiceDisconnected(ComponentName className) {
			bound = null;
			HostListActivity.this.updateList();
		}
	};

	@Override
	public void onStart() {
		super.onStart();

		// start the terminal manager service
		this.bindService(new Intent(this, TerminalManager.class), connection, Context.BIND_AUTO_CREATE);

		if(this.hostdb == null)
			this.hostdb = new HostDatabase(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		this.unbindService(connection);

		if(this.hostdb != null) {
			this.hostdb.close();
			this.hostdb = null;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_EULA) {
			if(resultCode == Activity.RESULT_OK) {
				// yay they agreed, so store that info
				Editor edit = prefs.edit();
				edit.putBoolean(PreferenceConstants.EULA, true);
				edit.commit();
			} else {
				// user didnt agree, so close
				this.finish();
			}
		} else if (requestCode == REQUEST_EDIT) {
			this.updateList();
		}
		else if (requestCode == REQUEST_DISCOVERY) {
			if (resultCode == Activity.RESULT_OK) {
				final BluetoothDevice device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String BlueMAC="",BlueName="";
				BlueMAC=device.getAddress().replace(":",".");
				BlueName=device.getName();
				Uri uri = TransportFactory.getUri((String) transportSpinner
						.getSelectedItem(), BlueName+"@"+BlueMAC+":"+quickconnect.getText().toString());
				if(this.hostdb == null)
					this.hostdb = new HostDatabase(this);
				startConsoleActivityUri(uri);
			}
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.act_hostlist);

		if(mBluetoothAdapter!=null)
		bBluestatu=mBluetoothAdapter.isEnabled();

		this.setTitle(String.format("%s: %s",
				getResources().getText(R.string.app_name),
				getResources().getText(R.string.title_hosts_list)));

		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// detect HTC Dream and apply special preferences
		if (Build.MANUFACTURER.equals("HTC") && Build.DEVICE.equals("dream")) {
			SharedPreferences.Editor editor = prefs.edit();
			boolean doCommit = false;
			if (!prefs.contains(PreferenceConstants.SHIFT_FKEYS) &&
			    !prefs.contains(PreferenceConstants.CTRL_FKEYS)) {
				editor.putBoolean(PreferenceConstants.SHIFT_FKEYS, true);
				editor.putBoolean(PreferenceConstants.CTRL_FKEYS, true);
				doCommit = true;
			}
			if (!prefs.contains(PreferenceConstants.STICKY_MODIFIERS)) {
				editor.putString(PreferenceConstants.STICKY_MODIFIERS, PreferenceConstants.YES);
				doCommit = true;
			}
			if (!prefs.contains(PreferenceConstants.KEYMODE)) {
				editor.putString(PreferenceConstants.KEYMODE, PreferenceConstants.KEYMODE_RIGHT);
				doCommit = true;
			}
			if (doCommit) {
				editor.commit();
			}
		}

		// check for eula agreement
		boolean agreed = prefs.getBoolean(PreferenceConstants.EULA, false);
		if(!agreed) {
			//this.startActivityForResult(new Intent(this, WizardActivity.class), REQUEST_EULA);
            // yay they agreed, so store that info
            Editor edit = prefs.edit();
            edit.putBoolean(PreferenceConstants.EULA, true);
            edit.commit();
		}

		this.makingShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())
								|| Intent.ACTION_PICK.equals(getIntent().getAction());

		// connect with hosts database and populate list
		this.hostdb = new HostDatabase(this);
		ListView list = this.getListView();

		this.sortedByColor = prefs.getBoolean(PreferenceConstants.SORT_BY_COLOR, false);

		//this.list.setSelector(R.drawable.highlight_disabled_pressed);

		list.setOnItemClickListener(new OnItemClickListener() {

			public synchronized void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				// launch off to console details
				HostBean host = (HostBean) parent.getAdapter().getItem(position);
				Uri uri = host.getUri();

				Intent contents = new Intent(Intent.ACTION_VIEW, uri);
				contents.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

				if (makingShortcut) {
					// create shortcut if requested
                    //在系统小工具中创建终端连接的快捷方式
					ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(HostListActivity.this, R.mipmap.ic_launcher);

					Intent intent = new Intent();
					intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, contents);
					intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, host.getNickname());
					intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

					setResult(RESULT_OK, intent);
					finish();

				} else {
					// otherwise just launch activity to show this host
					HostListActivity.this.startActivity(contents);
				}
			}
		});

		this.registerForContextMenu(list);

        connectbutton = (Button) this.findViewById(R.id.button);
        connectbutton.setOnClickListener(new View.OnClickListener() {
                         public void onClick(View v) {
                                startConsoleActivity();
                               }
                         });
		quickconnect = (TextView) this.findViewById(R.id.front_quickconnect);
		quickconnect.setVisibility(makingShortcut ? View.GONE : View.VISIBLE);
		quickconnect.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {

				if(event.getAction() == KeyEvent.ACTION_UP) return false;
				if(keyCode != KeyEvent.KEYCODE_ENTER) return false;

				return startConsoleActivity();
			}
		});

		transportSpinner = (Spinner)findViewById(R.id.transport_selection);
		transportSpinner.setVisibility(makingShortcut ? View.GONE : View.VISIBLE);
		ArrayAdapter<String> transportSelection = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, TransportFactory.getTransportNames());
		transportSelection.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		transportSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
				String formatHint = TransportFactory.getFormatHint(
						(String) transportSpinner.getSelectedItem(),
						HostListActivity.this);

				quickconnect.setHint(formatHint);
				quickconnect.setError(null);
				quickconnect.requestFocus();
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		transportSpinner.setAdapter(transportSelection);

		this.inflater = LayoutInflater.from(this);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
			super.onPrepareOptionsMenu(menu);

		// don't offer menus when creating shortcut
		if (makingShortcut) return true;

		sortcolor.setVisible(!sortedByColor);
			sortlast.setVisible(sortedByColor);

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// don't offer menus when creating shortcut
		if(makingShortcut) return true;
		CheckVersionTask();

		servermenu=menu.add("Server");
		servermenu.setIcon(R.drawable.server);
		if (android.os.Build.VERSION.SDK_INT >=  14)//Android 4.0以上才支持
			servermenu.setShowAsAction(1);//SHOW_AS_ACTION_IF_ROOM,SHOW_AS_ACTION_ALWAYS:2
		servermenu.setIntent(new Intent(HostListActivity.this,ServerActivity.class));

        ipinfo= menu.add("IP");
        ipinfo.setIcon(R.drawable.ipinfo);
        if (android.os.Build.VERSION.SDK_INT >=  14)//Android 4.0以上才支持
            ipinfo.setShowAsAction(1);//SHOW_AS_ACTION_IF_ROOM,SHOW_AS_ACTION_ALWAYS:2
        ipinfo.setIntent(new Intent(HostListActivity.this, IPinfoActivity.class));


		// add host, ssh keys, about
		sortcolor = menu.add(R.string.list_menu_sortcolor);
		sortcolor.setIcon(android.R.drawable.ic_menu_share);
		sortcolor.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				sortedByColor = true;
				updateList();
				return true;
			}
		});

		sortlast = menu.add(R.string.list_menu_sortname);
		sortlast.setIcon(android.R.drawable.ic_menu_share);
		sortlast.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				sortedByColor = false;
				updateList();
				return true;
			}
		});

		MenuItem keys = menu.add(R.string.list_menu_pubkeys);
		keys.setIcon(android.R.drawable.ic_lock_lock);
		keys.setIntent(new Intent(HostListActivity.this, PubkeyListActivity.class));

		MenuItem colors = menu.add(R.string.title_colors);
		colors.setIcon(android.R.drawable.ic_menu_slideshow);
		colors.setIntent(new Intent(HostListActivity.this, ColorsActivity.class));

		MenuItem settings = menu.add(R.string.list_menu_settings);
		settings.setIcon(android.R.drawable.ic_menu_preferences);
		settings.setIntent(new Intent(HostListActivity.this, SettingsActivity.class));

		MenuItem help = menu.add(R.string.title_help);
		help.setIcon(android.R.drawable.ic_menu_help);
		help.setIntent(new Intent(HostListActivity.this, HelpActivity.class));

		return true;

	}

    @Override
    public boolean onMenuOpened(int featureId, Menu menu)
    {
        //if(featureId ==Window.FEATURE_ACTION_BAR && menu !=null){
        if(featureId == Window.FEATURE_OPTIONS_PANEL && menu != null){
            if(menu.getClass().getSimpleName().equals("MenuBuilder")){
                try{
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                }
                catch(NoSuchMethodException e){
                    //Log.e(TAG, "onMenuOpened", e);
                }
                catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

		// create menu to handle hosts

		// create menu to handle deleting and sharing lists
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final HostBean host = (HostBean) this.getListView().getItemAtPosition(info.position);

		menu.setHeaderTitle(host.getNickname());

		// edit, disconnect, delete
		MenuItem connect = menu.add(R.string.list_host_disconnect);
		final TerminalBridge bridge = (bound == null) ? null : bound.getConnectedBridge(host);
		connect.setEnabled(bridge != null);
		connect.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				bridge.dispatchDisconnect(true);
				updateHandler.sendEmptyMessage(-1);
				return true;
			}
		});

		MenuItem edit = menu.add(R.string.list_host_edit);
		edit.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(HostListActivity.this, HostEditorActivity.class);
				intent.putExtra(Intent.EXTRA_TITLE, host.getId());
				HostListActivity.this.startActivityForResult(intent, REQUEST_EDIT);
				return true;
			}
		});

		MenuItem portForwards = menu.add(R.string.list_host_portforwards);
		portForwards.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(HostListActivity.this, PortForwardListActivity.class);
				intent.putExtra(Intent.EXTRA_TITLE, host.getId());
				HostListActivity.this.startActivityForResult(intent, REQUEST_EDIT);
				return true;
			}
		});
		if (!TransportFactory.canForwardPorts(host.getProtocol()))
			portForwards.setEnabled(false);

		MenuItem delete = menu.add(R.string.list_host_delete);
		delete.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				// prompt user to make sure they really want this
				new AlertDialog.Builder(HostListActivity.this)
						.setMessage(getString(R.string.delete_message, host.getNickname()))
						.setPositiveButton(R.string.delete_pos, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								// make sure we disconnect
								if (bridge != null)
									bridge.dispatchDisconnect(true);

								hostdb.deleteHost(host);
								updateHandler.sendEmptyMessage(-1);
							}
						})
						.setNegativeButton(R.string.delete_neg, null).create().show();

				return true;
			}
		});
	}

	 private void scanbluetooth()
	 {
		 int i=0;

		 if(mBluetoothAdapter==null) {
			 Toast.makeText(this, getString(R.string.ywb_nobluetooth), Toast.LENGTH_SHORT).show();
			 return;
		 }
		 if(!mBluetoothAdapter.isEnabled()){
			 mBluetoothAdapter.enable();

			 while (!mBluetoothAdapter.isEnabled())
			 {
				 try
				 {
					 Thread.sleep(100L);
					 i++;
					 if(i>100)
						 return;
				 }
				 catch (InterruptedException ie)
				 {
					 // unexpected interruption while enabling bluetooth
					 Thread.currentThread().interrupt(); // restore interrupted flag
					 return;
				 }
			 }



			/* while(!mBluetoothAdapter.isEnabled()) {
				 try {
					 wait(1000);
				 }catch (Exception e){}
				 i++;
				// if(i>5)
				//	 return;
			 }*/
		 }
		// else {
			 Intent enabler = new Intent(this, DiscoveryActivity.class);
			 startActivityForResult(enabler, REQUEST_DISCOVERY);
		// }
	 }
	/**
	 * @param text
	 * @return
	 */
	private boolean startConsoleActivity() {
		if(transportSpinner.getSelectedItemPosition()==3) {
			scanbluetooth();
			return true;
		}

		Uri uri = TransportFactory.getUri((String) transportSpinner
				.getSelectedItem(), quickconnect.getText().toString());
		return startConsoleActivityUri(uri);

	}
	private boolean startConsoleActivityUri(Uri uri)
	{
		if (uri == null) {
			quickconnect.setError(getString(R.string.list_format_error,
					TransportFactory.getFormatHint(
							(String) transportSpinner.getSelectedItem(),
							HostListActivity.this)));
			return false;
		}

		HostBean host = TransportFactory.findHost(hostdb, uri);
		if (host == null) {
			host = TransportFactory.getTransport(uri.getScheme()).createHost(uri);
			host.setColor(HostDatabase.COLOR_GRAY);
			host.setPubkeyId(HostDatabase.PUBKEYID_ANY);
			hostdb.saveHost(host);
		}

		Intent intent = new Intent(HostListActivity.this, ConsoleActivity.class);
		intent.setData(uri);
		startActivity(intent);

		return true;
	}

	protected void updateList() {
		if (prefs.getBoolean(PreferenceConstants.SORT_BY_COLOR, false) != sortedByColor) {
			Editor edit = prefs.edit();
			edit.putBoolean(PreferenceConstants.SORT_BY_COLOR, sortedByColor);
			edit.commit();
		}

		if (hostdb == null)
			hostdb = new HostDatabase(this);

		hosts = hostdb.getHosts(sortedByColor);

		// Don't lose hosts that are connected via shortcuts but not in the database.
		if (bound != null) {
			for (TerminalBridge bridge : bound.bridges) {
				if (!hosts.contains(bridge.host))
					hosts.add(0, bridge.host);
			}
		}

		HostAdapter adapter = new HostAdapter(this, hosts, bound);

		this.setListAdapter(adapter);
	}

	class HostAdapter extends ArrayAdapter<HostBean> {
		private List<HostBean> hosts;
		private final TerminalManager manager;
		private final ColorStateList red, green, blue;

		public final static int STATE_UNKNOWN = 1, STATE_CONNECTED = 2, STATE_DISCONNECTED = 3;

		class ViewHolder {
			public TextView nickname;
			public TextView caption;
			public ImageView icon;
		}

		public HostAdapter(Context context, List<HostBean> hosts, TerminalManager manager) {
			super(context, R.layout.item_host, hosts);

			this.hosts = hosts;
			this.manager = manager;

			red = context.getResources().getColorStateList(R.color.red);
			green = context.getResources().getColorStateList(R.color.green);
			blue = context.getResources().getColorStateList(R.color.blue);
		}

		/**
		 * Check if we're connected to a terminal with the given host.
		 */
		private int getConnectedState(HostBean host) {
			// always disconnected if we dont have backend service
			if (this.manager == null)
				return STATE_UNKNOWN;

			if (manager.getConnectedBridge(host) != null)
				return STATE_CONNECTED;

			if (manager.disconnected.contains(host))
				return STATE_DISCONNECTED;

			return STATE_UNKNOWN;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_host, null, false);

				holder = new ViewHolder();

				holder.nickname = (TextView)convertView.findViewById(android.R.id.text1);
				holder.caption = (TextView)convertView.findViewById(android.R.id.text2);
				holder.icon = (ImageView)convertView.findViewById(android.R.id.icon);

				convertView.setTag(holder);
			} else
				holder = (ViewHolder) convertView.getTag();

			HostBean host = hosts.get(position);
			if (host == null) {
				// Well, something bad happened. We can't continue.
				Log.e("HostAdapter", "Host bean is null!");

				holder.nickname.setText("Error during lookup");
				holder.caption.setText("see 'adb logcat' for more");
				return convertView;
			}

			holder.nickname.setText(host.getNickname());

			switch (this.getConnectedState(host)) {
			case STATE_UNKNOWN:
				holder.icon.setImageState(new int[] { }, true);
				break;
			case STATE_CONNECTED:
				holder.icon.setImageState(new int[] { android.R.attr.state_checked }, true);
				break;
			case STATE_DISCONNECTED:
				holder.icon.setImageState(new int[] { android.R.attr.state_expanded }, true);
				break;
			}

			ColorStateList chosen = null;
			if (HostDatabase.COLOR_RED.equals(host.getColor()))
				chosen = this.red;
			else if (HostDatabase.COLOR_GREEN.equals(host.getColor()))
				chosen = this.green;
			else if (HostDatabase.COLOR_BLUE.equals(host.getColor()))
				chosen = this.blue;

			Context context = convertView.getContext();

			if (chosen != null) {
				// set color normally if not selected
				holder.nickname.setTextColor(chosen);
				holder.caption.setTextColor(chosen);
			} else {
				// selected, so revert back to default black text
				holder.nickname.setTextAppearance(context, android.R.attr.textAppearanceLarge);
				holder.caption.setTextAppearance(context, android.R.attr.textAppearanceSmall);
			}

			CharSequence nice = context.getString(R.string.bind_never);
			if (host.getLastConnect() > 0) {
				nice = DateUtils.getRelativeTimeSpanString(host.getLastConnect() * 1000);
			}

			holder.caption.setText(nice);

			return convertView;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		boolean bcloseblue=true;

		if (bound != null) {
			for (TerminalBridge bridge : bound.bridges) {
				if (bridge.host.getProtocol().equals("bluetooth"))
					bcloseblue=false;
			}
		}
		if(bBluestatu==false&&mBluetoothAdapter!=null&&bcloseblue)
			if(mBluetoothAdapter.isEnabled())
				mBluetoothAdapter.disable();
	}
}
