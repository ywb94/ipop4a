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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.apache.http.util.EncodingUtils;
import org.xmlpull.v1.XmlPullParser;
import org.ywb_ipop.bean.SelectionArea;
import org.ywb_ipop.service.PromptHelper;
import org.ywb_ipop.service.TerminalBridge;
import org.ywb_ipop.service.TerminalKeyListener;
import org.ywb_ipop.service.TerminalManager;
import org.ywb_ipop.util.FileOpenerDialog;
import org.ywb_ipop.util.MyDialogListener;
import org.ywb_ipop.util.PreferenceConstants;
import org.ywb_ipop.util.UpdataInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

import de.mud.terminal.vt320;

public class ConsoleActivity extends Activity implements MyDialogListener {
	public final static String TAG = "ConnectBot.ConsoleActivity";

	protected static final int REQUEST_EDIT = 1;
    protected static final int REQUEST_SENDEDIT = 2;
    private static final int CLICK_TIME = 400;
	private static final float MAX_CLICK_DISTANCE = 25f;
	private static final int KEYBOARD_DISPLAY_TIME = 1500;

	// Direction to shift the ViewFlipper
	private static final int SHIFT_LEFT = 0;
	private static final int SHIFT_RIGHT = 1;
    private String mytitle;


    protected ViewFlipper flip = null;
	protected TerminalManager bound = null;
	protected LayoutInflater inflater = null;

	private SharedPreferences prefs = null;

	// determines whether or not menuitem accelerators are bound
	// otherwise they collide with an external keyboard's CTRL-char
	private boolean hardKeyboard = false;

	protected Uri requested;

	protected ClipboardManager clipboard;
	private RelativeLayout stringPromptGroup;
	protected EditText stringPrompt;
    protected Button buttonPrompt;
	private TextView stringPromptInstructions;

	private RelativeLayout booleanPromptGroup;
	private TextView booleanPrompt;
	private Button booleanYes, booleanNo;

	private RelativeLayout keyboardGroup;
	private Runnable keyboardGroupHider;

	private TextView empty;

	private Animation slide_left_in, slide_left_out, slide_right_in, slide_right_out, fade_stay_hidden, fade_out_delayed;

	private Animation keyboard_fade_in, keyboard_fade_out;
	private float lastX, lastY;

	private InputMethodManager inputManager;

	private MenuItem disconnect, copy, paste, portForward, resize, urlscan,sendtext;

	protected TerminalBridge copySource = null;
	private int lastTouchRow, lastTouchCol;

	private boolean forcedOrientation;

	private Handler handler = new Handler();

	private ImageView mKeyboardButton;

	private ActionBarWrapper actionBar;
	private boolean inActionBarMenu = false;
	private boolean titleBarHide;
    private int period=1000;
    private boolean bFilesave;
    private int iSendtype=0,bCrlf=1,iLoop;
	private TableRow tr=null;
	private static final int ITEM1 = Menu.FIRST;
	private static final int ITEM2 = Menu.FIRST + 1;
	MyButton editButton=null;
	int iButtoncount=0;
	MyButton aSendButton[];
	final int MAXBUTTON=20;



	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		// 向上下文菜单中添加菜单项
		// 注意此处的menu是ContextMenu
		editButton=(MyButton)v;
		menu.setHeaderTitle(getString(R.string.ywb_buttontitle));
		menu.add(0, ITEM1, 0, getString(R.string.ywb_buttonedit));
		menu.add(0, ITEM2, 0, getString(R.string.ywb_buttondel));
	}
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case ITEM1:
				if(editButton==null) return false;
				LayoutInflater inflater = getLayoutInflater();
				final  View layout = inflater.inflate(R.layout.add_button,
						(ViewGroup) findViewById(R.id.dialog));
				RadioGroup rg=(RadioGroup)layout.findViewById(R.id.sendType);
				rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {


					public void onCheckedChanged(RadioGroup arg0, int arg1) {
						TextView tv = (TextView) layout.findViewById(R.id.textView5);
						if (arg1 == R.id.buttontype3) {

							tv.setText(getString(R.string.ywb_buttontype3title));
							EditText ed = (EditText) layout.findViewById(R.id.editText5);
							ed.setVisibility(View.INVISIBLE);
							Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
							sp.setVisibility(View.VISIBLE);
						} else if (arg1 == R.id.buttontype2) {

							tv.setText(getString(R.string.ywb_buttontype2title));
							EditText ed = (EditText) layout.findViewById(R.id.editText5);
							ed.setVisibility(View.VISIBLE);
							Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
							sp.setVisibility(View.INVISIBLE);
						} else {
							tv.setText(getString(R.string.ywb_buttontxt));
							EditText ed = (EditText) layout.findViewById(R.id.editText5);
							ed.setVisibility(View.VISIBLE);
							Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
							sp.setVisibility(View.INVISIBLE);
						}


					}
				});
				int type=editButton.getButtonType();

				RadioButton rb;
				if(type==0)
					rb=(RadioButton)layout.findViewById(R.id.buttontype1);
				else if(type==1)
					rb=(RadioButton)layout.findViewById(R.id.buttontype2);
				else
					rb=(RadioButton)layout.findViewById(R.id.buttontype3);
				rb.setChecked(true);
				EditText ed = (EditText) layout.findViewById(R.id.editText4);
				ed.setText(editButton.getText());
				if(type!=2) {
					ed = (EditText) layout.findViewById(R.id.editText5);
					ed.setText(editButton.getButtonContent());
				}else{
					int mycode=1;
					Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
					try
					{
						mycode=Integer.parseInt(editButton.getButtonContent());
					}
					catch(Exception e)
					{
						e.printStackTrace();

					}
					if(mycode<=sp.getCount())
					sp.setSelection(mycode-1);
				}
					//rg.getChildAt(type).setSelected(true);

				AlertDialog.Builder builder =new AlertDialog.Builder(ConsoleActivity.this).setTitle(getString(R.string.ywb_buttontitle)).setView(layout)
						.setPositiveButton(getString(R.string.ywb_filedlg_ok), new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {
								int type;
								EditText ed = (EditText) layout.findViewById(R.id.editText4);
								editButton.setText(ed.getText().toString());

								RadioGroup rg = (RadioGroup) layout.findViewById(R.id.sendType);
								if (rg.getCheckedRadioButtonId() == R.id.buttontype1)
									type = 0;
								else if (rg.getCheckedRadioButtonId() == R.id.buttontype2)
									type = 1;
								else
									type = 2;

								editButton.setButtonType(type);
								if (type == 2) {
									Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
									editButton.setButtonContent(String.valueOf(sp.getSelectedItemPosition()+1));
								} else {
									ed = (EditText) layout.findViewById(R.id.editText5);
									editButton.setButtonContent(ed.getText().toString());
								}

							}
						})
						.setNegativeButton(getString(R.string.ywb_filedlg_cancel), null);
				builder.show();
				break;
			case ITEM2:
				if(editButton!=null)
					editButton.setVisibility(View.GONE);
				break;
			default:

				break;
		}
		return true;
	}
	private String HexStrToTxt(String hexstr)
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
		try
		{
			temps = new String(baKeyword, "utf-8");//UTF-16le:Not
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}

		return temps;

	}
	private MyButton AddMyButton(String title,String sendcontent,int type)
	{
		MyButton button = new MyButton(ConsoleActivity.this);
		button.setText(title);
		button.setButtonContent(sendcontent);
		button.setButtonType(type);

		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
				TerminalBridge bridge = terminalView.bridge;
				String temps;

				temps = ((MyButton) v).getButtonContent();
				int type=((MyButton) v).getButtonType();
				if(type==2)
				{
					int mycode=0;
					try
					{
						mycode=Integer.parseInt(temps);
					}
					catch(Exception e)
					{
						e.printStackTrace();
						return;
					}
					//if(mycode>=131&&mycode<142)
					//{

						View flip = findCurrentView(R.id.console_flip);
						if(flip != null) {
							TerminalView terminal = (TerminalView) flip;
							((vt320) terminal.bridge.buffer).keyPressed(mycode, ' ', 0);
						}
					//}
					//else
					//sendKeyCode(mycode);
				}
				else {
					if (type == 0) {
						if (bCrlf == 1)
							temps = temps + "\r\n";
						else
							temps = temps + "\n";
					}
					else
						temps=HexStrToTxt(temps);
					bridge.injectString(temps);
					bridge.resetScrollPosition();
				}
		    }

		});
		TableLayout tbLayout = (TableLayout) ConsoleActivity.this.findViewById(R.id.tbLayout);
		if (tr == null) {
			tr = new TableRow(ConsoleActivity.this);
			tbLayout.addView(tr);
		} else if (tr.getChildCount() >= 5) {
			tr = new TableRow(ConsoleActivity.this);
			tbLayout.addView(tr);
		}
		ConsoleActivity.this.registerForContextMenu(button);


		tr.addView(button);

		return button;

	}

	/**
	 * 传入需要的键值即可
	 * @param keyCode
	 */
	private void sendKeyCode(final int keyCode){
		new Thread () {
			public void run() {
				try {
					Instrumentation inst = new Instrumentation();
					inst.sendKeyDownUpSync(keyCode);
					//inst.sendCharacterSync(keyCode);
				} catch (Exception e) {
					Log.e("Exception when sendPointerSync", e.toString());
				}
			}
		}.start();
	}

	//写数据到SD中的文件
    public void writeFileSdcardFile(String fileName,String write_str) throws IOException {
        try{

            FileOutputStream fout = new FileOutputStream(fileName);
            byte [] bytes = write_str.getBytes();

            fout.write(bytes);
            fout.close();
        }

        catch(Exception e){
            e.printStackTrace();
        }
    }


    //读SD中的文件
    public String readFileSdcardFile(String fileName) throws IOException{
        String res="";
        try{
            FileInputStream fin = new FileInputStream(fileName);

            int length = fin.available();

            byte [] buffer = new byte[length];
            fin.read(buffer);

            res = EncodingUtils.getString(buffer, "UTF-8");

            fin.close();
        }

        catch(Exception e){
            e.printStackTrace();
        }
        return res;
    }
    private void refreshSendButton()
    {
        try{
            ImageButton pausebutton = (ImageButton) findViewById(R.id.pauseButton);
            ImageButton sendbutton = (ImageButton) findViewById(R.id.sendButton);
            TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
            TerminalBridge bridge = terminalView.bridge;

            if(bridge.isendFlag==2)
            {
                sendbutton.setImageDrawable(getResources().getDrawable(R.drawable.stop));
                pausebutton.setImageDrawable(getResources().getDrawable(R.drawable.resume));
                pausebutton.setEnabled(true);
            }
            else if(bridge.isendFlag==1)
            {
                sendbutton.setImageDrawable(getResources().getDrawable(R.drawable.stop));
                pausebutton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                pausebutton.setEnabled(true);
            }
            else
            {
                sendbutton.setImageDrawable(getResources().getDrawable(R.drawable.play));
                pausebutton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                pausebutton.setEnabled(false);
            }
        }catch(Exception e){}
    }
    private void setSendWin(){
            SharedPreferences prefs =PreferenceManager.getDefaultSharedPreferences(this);
            //设置发送框高度
            String temps= prefs.getString("send_height","200");
            int height= Integer.parseInt(temps);
            RelativeLayout sendLayout = (RelativeLayout) findViewById(R.id.SendLayout);
            sendLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height));
            RelativeLayout.LayoutParams linearParams=( RelativeLayout.LayoutParams)sendLayout.getLayoutParams();
            // LinearLayout.LayoutParams linearParams =(LinearLayout.LayoutParams) sendLayout.getLayoutParams();
            linearParams.height =height;
            linearParams.alignWithParent=true;
            linearParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            sendLayout.setLayoutParams(linearParams);
            //设置发送周期
            temps= prefs.getString("send_period","1000");
            period=Integer.parseInt(temps);
            //设置字体大小
            temps= prefs.getString("send_fontsize","20");
            TextView sendtext = (TextView)this.findViewById(R.id.sendText);
            sendtext.setTextSize(TypedValue.COMPLEX_UNIT_PX,Integer.parseInt(temps));
            //设置发送类型
            temps= prefs.getString("send_type","0");
            iSendtype=Integer.parseInt(temps);
		    if(iSendtype==1)
				sendtext.setHint(getString(R.string.ywb_sendhexhint));
		     else
				sendtext.setHint(getString(R.string.ywb_sendhint));
           //设置回车符类型
           temps= prefs.getString("send_enter", "1");
           bCrlf=Integer.parseInt(temps);
        //循环模式
        temps= prefs.getString("send_loop","1");
        iLoop=Integer.parseInt(temps);



    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setSendWin();
    }
    @Override
    public void OnOkClick(String path,int openORsave) {
        // 将对话框返回值显示在TextView中
        if(path.equals(""))
            Toast.makeText(this, getString(R.string.ywb_opensavetip), Toast.LENGTH_SHORT).show();

        TextView sendtext = (TextView)this.findViewById(R.id.sendText);
        try {
            if (openORsave == 0) {
                sendtext.setText(readFileSdcardFile(path));
                Toast.makeText(this, getString(R.string.ywb_openfileok)+path, Toast.LENGTH_SHORT).show();
            }
            else {
                writeFileSdcardFile(path, sendtext.getText().toString());
                Toast.makeText(this,  getString(R.string.ywb_opensaveok)+path, Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){}
       // mypath=path;
    }

    private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			bound = ((TerminalManager.TerminalBinder) service).getService();

			// let manager know about our event handling services
			bound.disconnectHandler = disconnectHandler;
            bound.sendoverHandler=sendoverHandler;

			Log.d(TAG, String.format("Connected to TerminalManager and found bridges.size=%d", bound.bridges.size()));

			bound.setResizeAllowed(true);

			// clear out any existing bridges and record requested index
			flip.removeAllViews();

			final String requestedNickname = (requested != null) ? requested.getFragment() : null;
			int requestedIndex = 0;

			TerminalBridge requestedBridge = bound.getConnectedBridge(requestedNickname);

			// If we didn't find the requested connection, try opening it
			if (requestedNickname != null && requestedBridge == null) {
				try {
					Log.d(TAG, String.format("We couldnt find an existing bridge with URI=%s (nickname=%s), so creating one now", requested.toString(), requestedNickname));
                    //新建一个bridge
					requestedBridge = bound.openConnection(requested);
                    //设置自动存盘
                    requestedBridge.bFilesave=bFilesave;


				} catch(Exception e) {
					Log.e(TAG, "Problem while trying to create new requested bridge from URI", e);
				}
			}
			//设置为缺省bridge
			bound.defaultBridge = requestedBridge;

			// create views for all bridges on this service
			for (TerminalBridge bridge : bound.bridges) {

				final int currentIndex = addNewTerminalView(bridge);

				// check to see if this bridge was requested
				if (bridge == requestedBridge)
					requestedIndex = currentIndex;
			}

			setDisplayedTerminal(requestedIndex);
		}

		public void onServiceDisconnected(ComponentName className) {
			// tell each bridge to forget about our prompt handler
			synchronized (bound.bridges) {
				for(TerminalBridge bridge : bound.bridges)
					bridge.promptHelper.setHandler(null);
			}

			flip.removeAllViews();
			updateEmptyVisible();
			bound = null;
		}
	};

	protected Handler promptHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// someone below us requested to display a prompt
			updatePromptVisible();
		}
	};

	protected Handler disconnectHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "Someone sending HANDLE_DISCONNECT to parentHandler");

			// someone below us requested to display a password dialog
			// they are sending nickname and requested
			TerminalBridge bridge = (TerminalBridge)msg.obj;

			if (bridge.isAwaitingClose())
				closeBridge(bridge);
		}
	};

    protected Handler sendoverHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ///Log.d(TAG, "Someone sending SEND_OVER to parentHandler");

            // someone below us requested to display a password dialog
            // they are sending nickname and requested
            TerminalBridge bridge = (TerminalBridge)msg.obj;
           if((bridge==bound.defaultBridge)||(null==bound.defaultBridge))
           {
               refreshSendButton();
           }

        }
    };
	/**
	 * @param bridge
	 */
	private void closeBridge(final TerminalBridge bridge) {
		synchronized (flip) {
			final int flipIndex = getFlipIndex(bridge);

			if (flipIndex >= 0) {
				if (flip.getDisplayedChild() == flipIndex) {
					shiftCurrentTerminal(SHIFT_LEFT);
				}
				flip.removeViewAt(flipIndex);

				/* TODO Remove this workaround when ViewFlipper is fixed to listen
				 * to view removals. Android Issue 1784
				 */
				final int numChildren = flip.getChildCount();
				if (flip.getDisplayedChild() >= numChildren &&
						numChildren > 0) {
					flip.setDisplayedChild(numChildren - 1);
				}

				updateEmptyVisible();
			}

			// If we just closed the last bridge, go back to the previous activity.
			if (flip.getChildCount() == 0) {
				finish();
			}
		}
	}

	protected View findCurrentView(int id) {
		View view = flip.getCurrentView();
		if(view == null) return null;
		return view.findViewById(id);
	}

	protected PromptHelper getCurrentPromptHelper() {
		View view = findCurrentView(R.id.console_flip);
		if(!(view instanceof TerminalView)) return null;
		return ((TerminalView)view).bridge.promptHelper;
	}

	protected void hideAllPrompts() {
		stringPromptGroup.setVisibility(View.GONE);
		booleanPromptGroup.setVisibility(View.GONE);
	}

	private void showEmulatedKeys() {
		keyboardGroup.startAnimation(keyboard_fade_in);
		keyboardGroup.setVisibility(View.VISIBLE);
		actionBar.show();

		if (keyboardGroupHider != null)
			handler.removeCallbacks(keyboardGroupHider);
		keyboardGroupHider = new Runnable() {
			public void run() {
				if (keyboardGroup.getVisibility() == View.GONE || inActionBarMenu)
					return;

				keyboardGroup.startAnimation(keyboard_fade_out);
				keyboardGroup.setVisibility(View.GONE);
				if (titleBarHide) {
					actionBar.hide();
				}
				keyboardGroupHider = null;
			}
		};
		handler.postDelayed(keyboardGroupHider, KEYBOARD_DISPLAY_TIME);
	}

	private void hideEmulatedKeys() {
		if (keyboardGroupHider != null)
			handler.removeCallbacks(keyboardGroupHider);
		keyboardGroup.setVisibility(View.GONE);
		if (titleBarHide) {
			actionBar.hide();
		}
	}

	// more like configureLaxMode -- enable network IO on UI thread
	private void configureStrictMode() {
		try {
			Class.forName("android.os.StrictMode");
			StrictModeSetup.run();
		} catch (ClassNotFoundException e) {
		}
	}
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		aSendButton=new MyButton[MAXBUTTON];


		configureStrictMode();

		hardKeyboard = getResources().getConfiguration().keyboard ==
				Configuration.KEYBOARD_QWERTY;

		clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		titleBarHide = prefs.getBoolean(PreferenceConstants.TITLEBARHIDE, false);
		if (titleBarHide) {
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		}

		this.setContentView(R.layout.act_console);
        SharedPreferences prefs =PreferenceManager.getDefaultSharedPreferences(this);
        bFilesave=prefs.getBoolean("send_autosave", false);

        RelativeLayout sendLayout = (RelativeLayout) findViewById(R.id.SendLayout);
        sendLayout.setVisibility(View.INVISIBLE);
		sendLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 1));
		RelativeLayout.LayoutParams linearParams=( RelativeLayout.LayoutParams)sendLayout.getLayoutParams();
		linearParams.height =1;
		linearParams.alignWithParent=true;
    	linearParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		sendLayout.setLayoutParams(linearParams);

		RelativeLayout buttonLayout = (RelativeLayout) findViewById(R.id.buttonLayout);
		buttonLayout.setVisibility(View.INVISIBLE);

        ImageButton pausebutton = (ImageButton) findViewById(R.id.pauseButton);
        pausebutton.setEnabled(false);
        pausebutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
				TerminalBridge bridge = terminalView.bridge;
				if (bridge.isendFlag == 1) {
					bridge.stopSend(false);
					ImageButton pausebutton = (ImageButton) findViewById(R.id.pauseButton);
					pausebutton.setImageDrawable(getResources().getDrawable(R.drawable.resume));
				} else if (bridge.isendFlag == 2) {
					String temps = ((TextView) findViewById(R.id.sendText)).getText().toString();
					bridge.startSend(temps, period, iSendtype, bCrlf, iLoop);
					ImageButton pausebutton = (ImageButton) findViewById(R.id.pauseButton);
					pausebutton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
				}
			}
		});
        ImageButton sendbutton = (ImageButton) findViewById(R.id.sendButton);
        sendbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
                TerminalBridge bridge = terminalView.bridge;
				CheckBox sendLine=(CheckBox)findViewById(R.id.sendLine);

                if(bridge.isendFlag>=1)
                {
                    bridge.stopSend(true);
                    ImageButton sendbutton = (ImageButton) findViewById(R.id.sendButton);
                    sendbutton.setImageDrawable(getResources().getDrawable(R.drawable.play));
                    ImageButton pausebutton = (ImageButton) findViewById(R.id.pauseButton);
                    pausebutton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                    pausebutton.setEnabled(false);
                }
                else
                {
					if(sendLine.isChecked()) {
						String sendStr[];
						String temps = ((TextView) findViewById(R.id.sendText)).getText().toString();
						String pattern = "\n";
						Pattern pat = Pattern.compile(pattern);
						sendStr = pat.split(temps);
						temps="";
						if(sendStr.length!=0)
						{
							int cloc =((TextView) findViewById(R.id.sendText)).getSelectionStart();
							int totallen=0;
							for(int i=0;i<sendStr.length;i++) {
								totallen += sendStr[i].length()+1;
								if(cloc<totallen) {
									temps = sendStr[i];
									break;
								}
							}

						}
						if(bCrlf==1)
							temps=temps+"\r\n";
						else
							temps=temps+"\n";
						bridge.injectString(temps);
						bridge.resetScrollPosition();
						return;
					}
					else {
						String temps = ((TextView) findViewById(R.id.sendText)).getText().toString();
						bridge.startSend(temps, period, iSendtype, bCrlf, iLoop);
						bridge.resetScrollPosition();
						ImageButton sendbutton = (ImageButton) findViewById(R.id.sendButton);
						sendbutton.setImageDrawable(getResources().getDrawable(R.drawable.stop));
						ImageButton pausebutton = (ImageButton) findViewById(R.id.pauseButton);
						pausebutton.setEnabled(true);
					}
                }
            }
        });
        ImageButton openbutton = (ImageButton) findViewById(R.id.openButton);
        openbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String path = null;
                String filetype[]={"txt","tcl"};
                FileOpenerDialog dlg = new FileOpenerDialog(ConsoleActivity.this, 1,filetype, path, ConsoleActivity.this);
                dlg.setTitle(getString(R.string.ywb_filedlg_opentitle));
                dlg.show();
            }
        });
        ImageButton savebutton = (ImageButton) findViewById(R.id.saveButton);
        savebutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String path = null;
                String filetype[]={"txt","tcl"};
                FileOpenerDialog dlg = new FileOpenerDialog(ConsoleActivity.this, 2, filetype, path, ConsoleActivity.this);
                dlg.setTitle(getString(R.string.ywb_filedlg_savetitle));
                dlg.show();
            }
        });
        ImageButton setbutton = (ImageButton) findViewById(R.id.setButton);
        setbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ConsoleActivity.this, SendSettingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, REQUEST_SENDEDIT);
            }
        });
		ImageButton morebutton = (ImageButton) findViewById(R.id.moreButton);
		morebutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				RelativeLayout sendLayout = (RelativeLayout)findViewById(R.id.SendLayout);
				sendLayout.setVisibility(View.INVISIBLE);
				RelativeLayout buttonLayout = (RelativeLayout) findViewById(R.id.buttonLayout);


				SharedPreferences prefs =PreferenceManager.getDefaultSharedPreferences(ConsoleActivity.this);
				//设置发送框高度
				String temps= prefs.getString("send_height", "200");
				int height= Integer.parseInt(temps);

				buttonLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height));
				RelativeLayout.LayoutParams linearParams=( RelativeLayout.LayoutParams)buttonLayout.getLayoutParams();
				linearParams.height =height;
				linearParams.alignWithParent=true;
				linearParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				buttonLayout.setLayoutParams(linearParams);

				buttonLayout.setVisibility(View.VISIBLE);
			}
	    });
		ImageButton returnButton = (ImageButton) findViewById(R.id.returnButton);
		returnButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				RelativeLayout sendLayout = (RelativeLayout)findViewById(R.id.SendLayout);
				refreshSendButton();
				sendLayout.setVisibility(View.VISIBLE);
				RelativeLayout buttonLayout = (RelativeLayout) findViewById(R.id.buttonLayout);
				buttonLayout.setVisibility(View.INVISIBLE);
			}
		});
		ImageButton addButton = (ImageButton) findViewById(R.id.addButton);
		addButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(iButtoncount>=MAXBUTTON)
				{
					Toast.makeText(ConsoleActivity.this, getString(R.string.ywb_noaddbutton), Toast.LENGTH_SHORT).show();
					return;
				}
				LayoutInflater inflater = getLayoutInflater();
				final  View layout = inflater.inflate(R.layout.add_button,
						(ViewGroup) findViewById(R.id.dialog));
				RadioGroup rg=(RadioGroup)layout.findViewById(R.id.sendType);
				rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {


					public void onCheckedChanged(RadioGroup arg0, int arg1) {
						TextView tv=(TextView) layout.findViewById(R.id.textView5);
						if(arg1==R.id.buttontype3)
						{

									tv.setText(getString(R.string.ywb_buttontype3title));

							EditText ed = (EditText) layout.findViewById(R.id.editText5);
							ed.setVisibility(View.INVISIBLE);
							Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
							sp.setVisibility(View.VISIBLE);
						}
						else if(arg1==R.id.buttontype2)
						{

							tv.setText(getString(R.string.ywb_buttontype2title));
							EditText ed = (EditText) layout.findViewById(R.id.editText5);
							ed.setVisibility(View.VISIBLE);
							Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
							sp.setVisibility(View.INVISIBLE);
						}
						else
						{
							tv.setText(getString(R.string.ywb_buttontxt));
							EditText ed = (EditText) layout.findViewById(R.id.editText5);
							ed.setVisibility(View.VISIBLE);
							Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
							sp.setVisibility(View.INVISIBLE);
						}



					}
				});
				new AlertDialog.Builder(ConsoleActivity.this).setTitle(getString(R.string.ywb_buttontitle)).setView(layout)
						.setPositiveButton(getString(R.string.ywb_filedlg_ok), new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {
								String title, sendcontent="";
								int type=0;

								RadioGroup rg=(RadioGroup)layout.findViewById(R.id.sendType);



								EditText ed = (EditText) layout.findViewById(R.id.editText4);
								title = ed.getText().toString();
								ed = (EditText) layout.findViewById(R.id.editText5);
								if(rg.getCheckedRadioButtonId()==R.id.buttontype1) {
									sendcontent = ed.getText().toString();
									type=0;
								}
								else if(rg.getCheckedRadioButtonId()==R.id.buttontype2) {
									sendcontent = ed.getText().toString();
									type=1;
								}
								else {

									Spinner sp=(Spinner)layout.findViewById(R.id.spinner);
									sendcontent=String.valueOf(sp.getSelectedItemPosition()+1);
									type=2;
								}
								aSendButton[iButtoncount]=AddMyButton(title,sendcontent,type);
								iButtoncount++;

							}
						})
						.setNegativeButton(getString(R.string.ywb_filedlg_cancel), null).show();
			}
		});
		ImageButton myButton = (ImageButton) findViewById(R.id.tabButton);
		myButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				/*TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
				TerminalBridge bridge = terminalView.bridge;
				bridge.injectString("\t");*/
				sendKeyCode(KeyEvent.KEYCODE_TAB);
			}
		});
		myButton = (ImageButton) findViewById(R.id.upButton);
		myButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sendKeyCode(KeyEvent.KEYCODE_DPAD_UP);

			}
		});
		myButton = (ImageButton) findViewById(R.id.downButton);
		myButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sendKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
			}
		});
		myButton = (ImageButton) findViewById(R.id.leftButton);
		myButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sendKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
			}
		});
		myButton = (ImageButton) findViewById(R.id.rightButton);
		myButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sendKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
			}
		});
		myButton = (ImageButton) findViewById(R.id.enterButton);
		myButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sendKeyCode(KeyEvent.KEYCODE_ENTER);
			}
		});
		// hide status bar if requested by user
		if (prefs.getBoolean(PreferenceConstants.FULLSCREEN, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		// TODO find proper way to disable volume key beep if it exists.
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// handle requested console from incoming intent
		requested = getIntent().getData();
        mytitle=String.format("%s: %s",
                getResources().getText(R.string.app_name),
                (requested != null) ? requested.getFragment() : null);
        this.setTitle(mytitle);
		inflater = LayoutInflater.from(this);

		flip = (ViewFlipper)findViewById(R.id.console_flip);
		empty = (TextView)findViewById(android.R.id.empty);

		stringPromptGroup = (RelativeLayout) findViewById(R.id.console_password_group);
		stringPromptInstructions = (TextView) findViewById(R.id.console_password_instructions);

        buttonPrompt=(Button)findViewById(R.id.button2);
        buttonPrompt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // pass collected password down to current terminal
                String value = stringPrompt.getText().toString();

                PromptHelper helper = getCurrentPromptHelper();
                if(helper == null) return ;
                helper.setResponse(value);

                // finally clear password for next user
                stringPrompt.setText("");
                updatePromptVisible();

                return ;

            }
        });
		stringPrompt = (EditText)findViewById(R.id.console_password);
		stringPrompt.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(event.getAction() == KeyEvent.ACTION_UP) return false;
				if(keyCode != KeyEvent.KEYCODE_ENTER) return false;

				// pass collected password down to current terminal
				String value = stringPrompt.getText().toString();

				PromptHelper helper = getCurrentPromptHelper();
				if(helper == null) return false;
				helper.setResponse(value);

				// finally clear password for next user
				stringPrompt.setText("");
				updatePromptVisible();

				return true;
			}
		});

		booleanPromptGroup = (RelativeLayout) findViewById(R.id.console_boolean_group);
		booleanPrompt = (TextView)findViewById(R.id.console_prompt);

		booleanYes = (Button)findViewById(R.id.console_prompt_yes);
		booleanYes.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PromptHelper helper = getCurrentPromptHelper();
				if(helper == null) return;
				helper.setResponse(Boolean.TRUE);
				updatePromptVisible();
			}
		});

		booleanNo = (Button)findViewById(R.id.console_prompt_no);
		booleanNo.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PromptHelper helper = getCurrentPromptHelper();
				if(helper == null) return;
				helper.setResponse(Boolean.FALSE);
				updatePromptVisible();
			}
		});

		// preload animations for terminal switching
		slide_left_in = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
		slide_left_out = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
		slide_right_in = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
		slide_right_out = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

		fade_out_delayed = AnimationUtils.loadAnimation(this, R.anim.fade_out_delayed);
		fade_stay_hidden = AnimationUtils.loadAnimation(this, R.anim.fade_stay_hidden);

		// Preload animation for keyboard button
		keyboard_fade_in = AnimationUtils.loadAnimation(this, R.anim.keyboard_fade_in);
		keyboard_fade_out = AnimationUtils.loadAnimation(this, R.anim.keyboard_fade_out);

		inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		keyboardGroup = (RelativeLayout) findViewById(R.id.keyboard_group);

		mKeyboardButton = (ImageView) findViewById(R.id.button_keyboard);
		mKeyboardButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				View flip = findCurrentView(R.id.console_flip);
				if (flip == null)
					return;

				inputManager.showSoftInput(flip, InputMethodManager.SHOW_FORCED);
				hideEmulatedKeys();
			}
		});

		final ImageView ctrlButton = (ImageView) findViewById(R.id.button_ctrl);
		ctrlButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				View flip = findCurrentView(R.id.console_flip);
				if (flip == null) return;
				TerminalView terminal = (TerminalView)flip;

				TerminalKeyListener handler = terminal.bridge.getKeyHandler();
				handler.metaPress(TerminalKeyListener.OUR_CTRL_ON, true);
				hideEmulatedKeys();
			}
		});

		final ImageView escButton = (ImageView) findViewById(R.id.button_esc);
		escButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				View flip = findCurrentView(R.id.console_flip);
				if (flip == null) return;
				TerminalView terminal = (TerminalView)flip;

				TerminalKeyListener handler = terminal.bridge.getKeyHandler();
				handler.sendEscape();
				hideEmulatedKeys();
			}
		});

		actionBar = ActionBarWrapper.getActionBar(this);
		actionBar.setDisplayHomeAsUpEnabled(true);
		if (titleBarHide) {
			actionBar.hide();
		}
		actionBar.addOnMenuVisibilityListener(new ActionBarWrapper.OnMenuVisibilityListener() {
			public void onMenuVisibilityChanged(boolean isVisible) {
				inActionBarMenu = isVisible;
				if (isVisible == false) {
					hideEmulatedKeys();
				}
			}
		});

		// detect fling gestures to switch between terminals
		final GestureDetector detect = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
			private float totalY = 0;

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

				final float distx = e2.getRawX() - e1.getRawX();
				final float disty = e2.getRawY() - e1.getRawY();
                //将1/2改为1/3
				final int goalwidth = flip.getWidth() / 3;

				// need to slide across half of display to trigger console change
				// make sure user kept a steady hand horizontally
				if (Math.abs(disty) < (flip.getHeight() / 4)) {
                    //左右滑动切换终端窗口，大于1/2屏幕宽度，小于1/4高度时触发
					if (distx > goalwidth) {
						shiftCurrentTerminal(SHIFT_RIGHT);
						return true;
					}

					if (distx < -goalwidth) {
						shiftCurrentTerminal(SHIFT_LEFT);
						return true;
					}

				}

				return false;
			}


			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

				// if copying, then ignore
				if (copySource != null && copySource.isSelectingForCopy())
					return false;

				if (e1 == null || e2 == null)
					return false;

				// if releasing then reset total scroll
				if (e2.getAction() == MotionEvent.ACTION_UP) {
					totalY = 0;
				}

				// activate consider if within x tolerance
				if (Math.abs(e1.getX() - e2.getX()) < ViewConfiguration.getTouchSlop() * 4) {

					View flip = findCurrentView(R.id.console_flip);
					if(flip == null) return false;
					TerminalView terminal = (TerminalView)flip;

					// estimate how many rows we have scrolled through
					// accumulate distance that doesn't trigger immediate scroll
					totalY += distanceY;
					final int moved = (int)(totalY / terminal.bridge.charHeight);

					// consume as scrollback only if towards right half of screen
					if (e2.getX() > flip.getWidth() / 2) {
						if (moved != 0) {
							int base = terminal.bridge.buffer.getWindowBase();
							terminal.bridge.buffer.setWindowBase(base + moved);
							totalY = 0;
							return true;
						}
					} else {
						// otherwise consume as pgup/pgdown for every 5 lines
						if (moved > 5) {
							((vt320)terminal.bridge.buffer).keyPressed(vt320.KEY_PAGE_DOWN, ' ', 0);
							terminal.bridge.tryKeyVibrate();
							totalY = 0;
							return true;
						} else if (moved < -5) {
							((vt320)terminal.bridge.buffer).keyPressed(vt320.KEY_PAGE_UP, ' ', 0);
							terminal.bridge.tryKeyVibrate();
							totalY = 0;
							return true;
						}

					}

				}

				return false;
			}


		});

		flip.setLongClickable(true);
		flip.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {

				// when copying, highlight the area
				if (copySource != null && copySource.isSelectingForCopy()) {
					int row = (int)Math.floor(event.getY() / copySource.charHeight);
					int col = (int)Math.floor(event.getX() / copySource.charWidth);

					SelectionArea area = copySource.getSelectionArea();

					switch(event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						// recording starting area
						if (area.isSelectingOrigin()) {
							area.setRow(row);
							area.setColumn(col);
							lastTouchRow = row;
							lastTouchCol = col;
							copySource.redraw();
						}
						return true;
					case MotionEvent.ACTION_MOVE:
						/* ignore when user hasn't moved since last time so
						 * we can fine-tune with directional pad
						 */
						if (row == lastTouchRow && col == lastTouchCol)
							return true;

						// if the user moves, start the selection for other corner
						area.finishSelectingOrigin();

						// update selected area
						area.setRow(row);
						area.setColumn(col);
						lastTouchRow = row;
						lastTouchCol = col;
						copySource.redraw();
						return true;
					case MotionEvent.ACTION_UP:
						/* If they didn't move their finger, maybe they meant to
						 * select the rest of the text with the directional pad.
						 */
						if (area.getLeft() == area.getRight() &&
								area.getTop() == area.getBottom()) {
							return true;
						}

						// copy selected area to clipboard
						String copiedText = area.copyFrom(copySource.buffer);

						clipboard.setText(copiedText);
						Toast.makeText(ConsoleActivity.this, getString(R.string.console_copy_done, copiedText.length()), Toast.LENGTH_LONG).show();
						// fall through to clear state

					case MotionEvent.ACTION_CANCEL:
						// make sure we clear any highlighted area
						area.reset();
						copySource.setSelectingForCopy(false);
						copySource.redraw();
						return true;
					}
				}

				Configuration config = getResources().getConfiguration();

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					lastX = event.getX();
					lastY = event.getY();
				} else if (event.getAction() == MotionEvent.ACTION_UP
						&& keyboardGroup.getVisibility() == View.GONE
						&& event.getEventTime() - event.getDownTime() < CLICK_TIME
						&& Math.abs(event.getX() - lastX) < MAX_CLICK_DISTANCE
						&& Math.abs(event.getY() - lastY) < MAX_CLICK_DISTANCE) {
					showEmulatedKeys();
				}

				// pass any touch events back to detector
				return detect.onTouchEvent(event);
			}

		});

	}

	/**
	 *
	 */
	private void configureOrientation() {
		String rotateDefault;
		if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS)
			rotateDefault = PreferenceConstants.ROTATION_PORTRAIT;
		else
			rotateDefault = PreferenceConstants.ROTATION_LANDSCAPE;

		String rotate = prefs.getString(PreferenceConstants.ROTATION, rotateDefault);
		if (PreferenceConstants.ROTATION_DEFAULT.equals(rotate))
			rotate = rotateDefault;

		// request a forced orientation if requested by user
		if (PreferenceConstants.ROTATION_LANDSCAPE.equals(rotate)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			forcedOrientation = true;
		} else if (PreferenceConstants.ROTATION_PORTRAIT.equals(rotate)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			forcedOrientation = true;
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			forcedOrientation = false;
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		//sendtxt表示存放时所用的xml文件名称
		SharedPreferences mSharedPreferences=getSharedPreferences("sendtxt", MODE_PRIVATE);
		//txt为项目名，""为取不到相应项目时的默认值
		String mTempString=mSharedPreferences.getString("txt", "");
		boolean mcb=mSharedPreferences.getBoolean("sendline",false);
		EditText ed1=(EditText)findViewById(R.id.sendText);
		ed1.setText(mTempString);
		CheckBox cb=(CheckBox)findViewById(R.id.sendLine);
		cb.setChecked(mcb);
		iButtoncount=mSharedPreferences.getInt("buttoncount", 0);
		for(int i=0;i<iButtoncount;i++)
		{

			String title=mSharedPreferences.getString("button_name" + String.valueOf(i), "");
			String sendcontent=mSharedPreferences.getString("button_content" + String.valueOf(i), "");
			int type=mSharedPreferences.getInt("button_type" + String.valueOf(i), 0);
			aSendButton[i]=AddMyButton(title,sendcontent,type);
		}

		View view = findCurrentView(R.id.console_flip);
		final boolean activeTerminal = (view instanceof TerminalView);
		boolean sessionOpen = false;
		boolean disconnected = false;
		boolean canForwardPorts = false;

		if (activeTerminal) {
			TerminalBridge bridge = ((TerminalView) view).bridge;
			sessionOpen = bridge.isSessionOpen();
			disconnected = bridge.isDisconnected();
			canForwardPorts = bridge.canFowardPorts();
		}

		menu.setQwertyMode(true);



        sendtext= menu.add(R.string.ywb_sendtext);
        sendtext.setIcon(android.R.drawable.stat_notify_more);
        if (android.os.Build.VERSION.SDK_INT >=  14)//Android 4.0以上才支持
        sendtext.setShowAsAction(1);//SHOW_AS_ACTION_IF_ROOM,SHOW_AS_ACTION_ALWAYS:2
        sendtext.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                RelativeLayout sendLayout = (RelativeLayout) findViewById(R.id.SendLayout);
				RelativeLayout buttonLayout = (RelativeLayout) findViewById(R.id.buttonLayout);
                if(sendLayout.getVisibility()==View.VISIBLE||buttonLayout.getVisibility()==View.VISIBLE) {
					sendLayout.setVisibility(View.INVISIBLE);
					sendLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 1));
					RelativeLayout.LayoutParams linearParams=( RelativeLayout.LayoutParams)sendLayout.getLayoutParams();
					linearParams.height =1;
					linearParams.alignWithParent=true;
					linearParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					sendLayout.setLayoutParams(linearParams);
					buttonLayout.setVisibility(View.INVISIBLE);
				}
                else {
                    setSendWin();
                    refreshSendButton();
                    sendLayout.setVisibility(View.VISIBLE);
                }

                return true;
            }
        });
		disconnect = menu.add(R.string.list_host_disconnect);
		if (hardKeyboard)
			disconnect.setAlphabeticShortcut('w');
		if (!sessionOpen && disconnected)
			disconnect.setTitle(R.string.console_menu_close);
		disconnect.setEnabled(activeTerminal);
		disconnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		if (android.os.Build.VERSION.SDK_INT >=  14)//Android 4.0以上才支持
			disconnect.setShowAsAction(1);
		disconnect.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				// disconnect or close the currently visible session
				TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
				TerminalBridge bridge = terminalView.bridge;

				bridge.dispatchDisconnect(true);
				return true;
			}
		});

		copy = menu.add(R.string.console_menu_copy);
		if (hardKeyboard)
			copy.setAlphabeticShortcut('c');
		copy.setIcon(android.R.drawable.ic_menu_set_as);
		copy.setEnabled(activeTerminal);
		copy.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				// mark as copying and reset any previous bounds
				TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
				copySource = terminalView.bridge;

				SelectionArea area = copySource.getSelectionArea();
				area.reset();
				area.setBounds(copySource.buffer.getColumns(), copySource.buffer.getRows());

				copySource.setSelectingForCopy(true);

				// Make sure we show the initial selection
				copySource.redraw();

				Toast.makeText(ConsoleActivity.this, getString(R.string.console_copy_start), Toast.LENGTH_LONG).show();
				return true;
			}
		});

		paste = menu.add(R.string.console_menu_paste);
		if (hardKeyboard)
			paste.setAlphabeticShortcut('v');
		paste.setIcon(android.R.drawable.ic_menu_edit);
		paste.setEnabled(clipboard.hasText() && sessionOpen);
		paste.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				// force insert of clipboard text into current console
				TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
				TerminalBridge bridge = terminalView.bridge;

				// pull string from clipboard and generate all events to force down
				String clip = clipboard.getText().toString();
				bridge.injectString(clip);

				return true;
			}
		});

		portForward = menu.add(R.string.console_menu_portforwards);
		if (hardKeyboard)
			portForward.setAlphabeticShortcut('f');
		portForward.setIcon(android.R.drawable.ic_menu_manage);
		portForward.setEnabled(sessionOpen && canForwardPorts);
		portForward.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);
				TerminalBridge bridge = terminalView.bridge;

				Intent intent = new Intent(ConsoleActivity.this, PortForwardListActivity.class);
				intent.putExtra(Intent.EXTRA_TITLE, bridge.host.getId());
				ConsoleActivity.this.startActivityForResult(intent, REQUEST_EDIT);
				return true;
			}
		});

		urlscan = menu.add(R.string.console_menu_urlscan);
		if (hardKeyboard)
			urlscan.setAlphabeticShortcut('u');
		urlscan.setIcon(android.R.drawable.ic_menu_search);
		urlscan.setEnabled(activeTerminal);
		urlscan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				final TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);

				List<String> urls = terminalView.bridge.scanForURLs();

				Dialog urlDialog = new Dialog(ConsoleActivity.this);
				urlDialog.setTitle(R.string.console_menu_urlscan);

				ListView urlListView = new ListView(ConsoleActivity.this);
				URLItemListener urlListener = new URLItemListener(ConsoleActivity.this);
				urlListView.setOnItemClickListener(urlListener);

				urlListView.setAdapter(new ArrayAdapter<String>(ConsoleActivity.this, android.R.layout.simple_list_item_1, urls));
				urlDialog.setContentView(urlListView);
				urlDialog.show();

				return true;
			}
		});

		resize = menu.add(R.string.console_menu_resize);
		if (hardKeyboard)
			resize.setAlphabeticShortcut('s');
		resize.setIcon(android.R.drawable.ic_menu_crop);
		resize.setEnabled(sessionOpen);
		resize.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				final TerminalView terminalView = (TerminalView) findCurrentView(R.id.console_flip);

				final View resizeView = inflater.inflate(R.layout.dia_resize, null, false);
				new AlertDialog.Builder(ConsoleActivity.this)
					.setView(resizeView)
					.setPositiveButton(R.string.button_resize, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							int width, height;
							try {
								width = Integer.parseInt(((EditText) resizeView
										.findViewById(R.id.width))
										.getText().toString());
								height = Integer.parseInt(((EditText) resizeView
										.findViewById(R.id.height))
										.getText().toString());
							} catch (NumberFormatException nfe) {
								// TODO change this to a real dialog where we can
								// make the input boxes turn red to indicate an error.
								return;
							}

							terminalView.forceSize(width, height);
						}
					}).setNegativeButton(android.R.string.cancel, null).create().show();

				return true;
			}
		});

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);

		final View view = findCurrentView(R.id.console_flip);
		boolean activeTerminal = (view instanceof TerminalView);
		boolean sessionOpen = false;
		boolean disconnected = false;
		boolean canForwardPorts = false;

		if (activeTerminal) {
			TerminalBridge bridge = ((TerminalView) view).bridge;
			sessionOpen = bridge.isSessionOpen();
			disconnected = bridge.isDisconnected();
			canForwardPorts = bridge.canFowardPorts();
		}

		disconnect.setEnabled(activeTerminal);
		if (sessionOpen || !disconnected)
			disconnect.setTitle(R.string.list_host_disconnect);
		else
			disconnect.setTitle(R.string.console_menu_close);
		copy.setEnabled(activeTerminal);
		paste.setEnabled(clipboard.hasText() && sessionOpen);
		portForward.setEnabled(sessionOpen && canForwardPorts);
		urlscan.setEnabled(activeTerminal);
		resize.setEnabled(sessionOpen);

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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, HostListActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	public void onStart() {
		super.onStart();

		// connect with manager service to find all bridges
		// when connected it will insert all views
		bindService(new Intent(this, TerminalManager.class), connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause called");

		if (forcedOrientation && bound != null)
			bound.setResizeAllowed(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume called");

		// Make sure we don't let the screen fall asleep.
		// This also keeps the Wi-Fi chipset from disconnecting us.
		if (prefs.getBoolean(PreferenceConstants.KEEP_ALIVE, true)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		configureOrientation();

		if (forcedOrientation && bound != null)
			bound.setResizeAllowed(true);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Log.d(TAG, "onNewIntent called");

		requested = intent.getData();

		if (requested == null) {
			Log.e(TAG, "Got null intent data in onNewIntent()");
			return;
		}

		if (bound == null) {
			Log.e(TAG, "We're not bound in onNewIntent()");
			return;
		}

		TerminalBridge requestedBridge = bound.getConnectedBridge(requested.getFragment());
		int requestedIndex = 0;

		synchronized (flip) {
			if (requestedBridge == null) {
				// If we didn't find the requested connection, try opening it

				try {
					Log.d(TAG, String.format("We couldnt find an existing bridge with URI=%s (nickname=%s),"+
							"so creating one now", requested.toString(), requested.getFragment()));
					requestedBridge = bound.openConnection(requested);
				} catch(Exception e) {
					Log.e(TAG, "Problem while trying to create new requested bridge from URI", e);
					// TODO: We should display an error dialog here.
					return;
				}

				requestedIndex = addNewTerminalView(requestedBridge);
			} else {
				final int flipIndex = getFlipIndex(requestedBridge);
				if (flipIndex > requestedIndex) {
					requestedIndex = flipIndex;
				}
			}

			setDisplayedTerminal(requestedIndex);
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		unbindService(connection);
		String temp;
		EditText ed=(EditText)findViewById(R.id.sendText);
		temp=ed.getText().toString();
		CheckBox cb=(CheckBox)findViewById(R.id.sendLine);

		SharedPreferences mSharedPreferences = getSharedPreferences("sendtxt",MODE_PRIVATE);
		mSharedPreferences.edit().putString("txt", temp).commit();
		mSharedPreferences.edit().putBoolean("sendline", cb.isChecked()).commit();

		int ienable=0;
		for(int i=0;i<iButtoncount;i++)
		{
			if(aSendButton[i].getVisibility()==View.GONE)
				continue;
			mSharedPreferences.edit().putString("button_name"+String.valueOf(ienable), aSendButton[i].getText().toString()).commit();
			mSharedPreferences.edit().putString("button_content"+String.valueOf(ienable), aSendButton[i].getButtonContent()).commit();
			mSharedPreferences.edit().putInt("button_type" + String.valueOf(ienable), aSendButton[i].getButtonType()).commit();
			ienable++;
		}
		mSharedPreferences.edit().putInt("buttoncount", ienable).commit();
		//TableLayout tbLayout = (TableLayout) ConsoleActivity.this.findViewById(R.id.tbLayout);

	}
    //终端窗口切换函数
	protected void shiftCurrentTerminal(final int direction) {
		View overlay;
		synchronized (flip) {
			boolean shouldAnimate = flip.getChildCount() > 1;

			// Only show animation if there is something else to go to.
			if (shouldAnimate) {
				// keep current overlay from popping up again
				overlay = findCurrentView(R.id.terminal_overlay);
				if (overlay != null)
					overlay.startAnimation(fade_stay_hidden);

				if (direction == SHIFT_LEFT) {
					flip.setInAnimation(slide_left_in);
					flip.setOutAnimation(slide_left_out);
					flip.showNext();
				} else if (direction == SHIFT_RIGHT) {
					flip.setInAnimation(slide_right_in);
					flip.setOutAnimation(slide_right_out);
					flip.showPrevious();
				}
			}

			ConsoleActivity.this.updateDefault();

			if (shouldAnimate) {
				// show overlay on new slide and start fade
				overlay = findCurrentView(R.id.terminal_overlay);
               //刷新窗口标题
                TextView overlay1 = (TextView)overlay.findViewById(R.id.terminal_overlay);
                String temps="IPOP:"+overlay1.getText().toString();
                this.setTitle(temps);
                //刷新文本发送按钮
                refreshSendButton();
				if (overlay != null)
					overlay.startAnimation(fade_out_delayed);
			}

			updatePromptVisible();

		}
	}

	/**
	 * Save the currently shown {@link TerminalView} as the default. This is
	 * saved back down into {@link TerminalManager} where we can read it again
	 * later.
	 */
	private void updateDefault() {
		// update the current default terminal
		View view = findCurrentView(R.id.console_flip);
		if(!(view instanceof TerminalView)) return;


		TerminalView terminal = (TerminalView)view;
		if(bound == null) return;
		bound.defaultBridge = terminal.bridge;
	}

	protected void updateEmptyVisible() {
		// update visibility of empty status message
		empty.setVisibility((flip.getChildCount() == 0) ? View.VISIBLE : View.GONE);
	}

	/**
	 * Show any prompts requested by the currently visible {@link TerminalView}.
	 */
	protected void updatePromptVisible() {
		// check if our currently-visible terminalbridge is requesting any prompt services
		View view = findCurrentView(R.id.console_flip);


		// Hide all the prompts in case a prompt request was canceled
		hideAllPrompts();

		if(!(view instanceof TerminalView)) {
			// we dont have an active view, so hide any prompts
			return;
		}

		PromptHelper prompt = ((TerminalView)view).bridge.promptHelper;
		if(String.class.equals(prompt.promptRequested)) {
			stringPromptGroup.setVisibility(View.VISIBLE);

			String instructions = prompt.promptInstructions;
			if (instructions != null && instructions.length() > 0) {
				stringPromptInstructions.setVisibility(View.VISIBLE);
				stringPromptInstructions.setText(instructions);
			} else
				stringPromptInstructions.setVisibility(View.GONE);
			stringPrompt.setText("");
			stringPrompt.setHint(prompt.promptHint);
			stringPrompt.requestFocus();

		} else if(Boolean.class.equals(prompt.promptRequested)) {
			booleanPromptGroup.setVisibility(View.VISIBLE);
			booleanPrompt.setText(prompt.promptHint);
			booleanYes.requestFocus();

		} else {
			hideAllPrompts();
			view.requestFocus();
		}
	}

	private class URLItemListener implements OnItemClickListener {
		private WeakReference<Context> contextRef;

		URLItemListener(Context context) {
			this.contextRef = new WeakReference<Context>(context);
		}

		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
			Context context = contextRef.get();

			if (context == null)
				return;

			try {
				TextView urlView = (TextView) view;

				String url = urlView.getText().toString();
				if (url.indexOf("://") < 0)
					url = "http://" + url;

				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				context.startActivity(intent);
			} catch (Exception e) {
				Log.e(TAG, "couldn't open URL", e);
				// We should probably tell the user that we couldn't find a handler...
			}
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		Log.d(TAG, String.format("onConfigurationChanged; requestedOrientation=%d, newConfig.orientation=%d", getRequestedOrientation(), newConfig.orientation));
		if (bound != null) {
			if (forcedOrientation &&
					(newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE &&
					getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ||
					(newConfig.orientation != Configuration.ORIENTATION_PORTRAIT &&
					getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
				bound.setResizeAllowed(false);
			else
				bound.setResizeAllowed(true);

			bound.hardKeyboardHidden = (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES);

			mKeyboardButton.setVisibility(bound.hardKeyboardHidden ? View.VISIBLE : View.GONE);
		}
	}

	/**
	 * Adds a new TerminalBridge to the current set of views in our ViewFlipper.
	 *
	 * @param bridge TerminalBridge to add to our ViewFlipper
	 * @return the child index of the new view in the ViewFlipper
	 */
	private int addNewTerminalView(TerminalBridge bridge) {
		// let them know about our prompt handler services
		bridge.promptHelper.setHandler(promptHandler);

		// inflate each terminal view
		RelativeLayout view = (RelativeLayout)inflater.inflate(R.layout.item_terminal, flip, false);

		// set the terminal overlay text
		TextView overlay = (TextView)view.findViewById(R.id.terminal_overlay);
		overlay.setText(bridge.host.getNickname());

		// and add our terminal view control, using index to place behind overlay
		TerminalView terminal = new TerminalView(ConsoleActivity.this, bridge);
		terminal.setId(R.id.console_flip);
		view.addView(terminal, 0);

		synchronized (flip) {
			// finally attach to the flipper
			flip.addView(view);
			return flip.getChildCount() - 1;
		}
	}

	private int getFlipIndex(TerminalBridge bridge) {
		synchronized (flip) {
			final int children = flip.getChildCount();
			for (int i = 0; i < children; i++) {
				final View view = flip.getChildAt(i).findViewById(R.id.console_flip);

				if (view == null || !(view instanceof TerminalView)) {
					// How did that happen?
					continue;
				}

				final TerminalView tv = (TerminalView) view;

				if (tv.bridge == bridge) {
					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * Displays the child in the ViewFlipper at the requestedIndex and updates the prompts.
	 *
	 * @param requestedIndex the index of the terminal view to display
	 */
	private void setDisplayedTerminal(int requestedIndex) {
		synchronized (flip) {
			try {
				// show the requested bridge if found, also fade out overlay
				flip.setDisplayedChild(requestedIndex);
				flip.getCurrentView().findViewById(R.id.terminal_overlay)
						.startAnimation(fade_out_delayed);
			} catch (NullPointerException npe) {
				Log.d(TAG, "View went away when we were about to display it", npe);
			}

			updatePromptVisible();
			updateEmptyVisible();
		}
	}
}
