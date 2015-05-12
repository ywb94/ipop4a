package org.ywb_ipop.util;

/**
 * Created by Administrator on 2015/4/24.
 */
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;

import org.ywb_ipop.util.MyDialogListener;
import org.ywb_ipop.R;


/**
 *
 *
 *
 * @author
 *
 */
public class FileOpenerDialog extends Dialog implements android.view.View.OnClickListener {

    private android.widget.ListView list;

    SimpleAdapter mSimpleAdapter;
    ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();

    Context context;
    private String path;

    private android.widget.TextView curFilePath;
    private android.widget.EditText saveFileName;
    private android.widget.Button home, back, ok, cancel;
    private android.widget.LinearLayout layout;

    private int type = 1;
    private String[] fileType = null;

    public final static int TypeOpen = 1;
    public final static int TypeSave = 2;

    private MyDialogListener listener;

    /**
     * @param context
     * @param 值为1表示OpenFileDialog, 值为2表示SaveFileDialog
     * @param 需要过滤的文件类型，若为空表示只显示文件夹
     * @param 初始路径，这个有问题
     */
    public FileOpenerDialog(Context context, int type, String[] fileType, String resultPath,
                            MyDialogListener listener) {
        super(context);

        this.context = context;
        this.type = type;
        this.fileType = fileType;
        this.path = resultPath;
        this.listener = listener;
    }

    @Override
    public void dismiss() {

        super.dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_filechooser);

        path = getSDPath();

        listItem=getDirsmap(path);
         mSimpleAdapter = new SimpleAdapter(this.context,listItem,//需要绑定的数据
                R.layout.item_file,//每一行的布局//动态数组中的数据源的键对应到定义布局的View中
                new String[] {"ItemImage","ItemTitle"},new int[] {R.id.ItemImage,R.id.ItemTitle}
        );
        list = (android.widget.ListView)findViewById(R.id.FileChooserDirList);
        list.setAdapter(mSimpleAdapter);

        list.setOnItemClickListener(lvLis);

        home = (android.widget.Button)findViewById(R.id.FileChooserHomeBtn);
        home.setOnClickListener(this);

        back = (android.widget.Button)findViewById(R.id.FileChooserBackBtn);
        back.setOnClickListener(this);

        ok = (android.widget.Button)findViewById(R.id.FileChooserOkBtn);
        ok.setOnClickListener(this);

        cancel = (android.widget.Button)findViewById(R.id.FileChooserCancelBtn);
        cancel.setOnClickListener(this);

        layout = (android.widget.LinearLayout)findViewById(R.id.FileChooserDirLayout);

        if(type == TypeOpen)
        {
            // 若为OpenFileDialog，在预留的位置添加TextView，显示当前路径
            curFilePath = new android.widget.TextView(context);
            layout.addView(curFilePath);
            curFilePath.setText(path);
        }
        else if(type == TypeSave)
        {
            // 若为SaveFileDialog，在预留的位置添加EditText，输入要保存的文件名
            saveFileName = new android.widget.EditText(context);
            saveFileName.setWidth(240);
            saveFileName.setHeight(70);
            saveFileName.setGravity(Gravity.CENTER);
            saveFileName.setPadding(0, 2, 0, 0);
            layout.addView(saveFileName);
            saveFileName.setText("send.txt");
        }
    }

    // 自动更新ListView内容
    Runnable add = new Runnable() {
        @Override
        public void run() {


            listItem.clear();

            ArrayList<HashMap<String, Object>> temp = new ArrayList<HashMap<String, Object>>();
            temp = getDirsmap(path);
            for(int i = 0; i < temp.size(); i++)
            {
                listItem.add(temp.get(i));
            }

            mSimpleAdapter.notifyDataSetChanged();
        }
        };

 private OnItemClickListener lvLis = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

            HashMap<String, Object> map=(HashMap<String, Object>)arg0.getItemAtPosition(arg2);
            String temp = (String)map.get("ItemTitle");
            File f=new File(path);
            if(f.isFile()) path = getSubDir(path);
            if(temp.equals(".."))   path = getSubDir(path);   // 如果点击的项目是".."，表示没有子目录，返回上一级目录
            else if(path.equals("/"))   path = path + temp;   // 如果当前目录为根目录，直接添加子目录，例 "/" -> "/sdcard"
            else {
                path = path + "/" + temp;   // 将子目录追加到当前目录后，例 "/sdcard" -> "/sdcard/xml"
            }
            if(type == TypeOpen)    curFilePath.setText(path);
             f=new File(path);
            if(f.isDirectory()) {
                Handler handler = new Handler();
                handler.post(add);// 因为当前目录改变，更新子文件夹
            }
        }
        };

    static Comparator<File> comparator = new Comparator<File>() {
        public int compare(File f1, File f2) {
            if (f1 == null || f2 == null) {// 先比较null
                if (f1 == null) {
                    {
                        return -1;
                    }
                } else {
                    return 1;
                }
            } else {
                if (f1.isDirectory() == true && f2.isDirectory() == true) { // 再比较文件夹
                    return f1.getName().compareToIgnoreCase(f2.getName());
                } else {
                    if ((f1.isDirectory() && !f2.isDirectory()) == true) {
                        return -1;
                    } else if ((f2.isDirectory() && !f1.isDirectory()) == true) {
                        return 1;
                    } else {
                        return f1.getName().compareToIgnoreCase(f2.getName());// 最后比较文件
                    }
                }
            }
        }
    };
    private ArrayList<HashMap<String, Object>> getDirsmap(String ipath) {
        ArrayList<HashMap<String, Object>> dirs = new ArrayList<HashMap<String, Object>>();
        File[] files = new File(ipath).listFiles();



        if(files != null)
        {
            Arrays.sort(files, comparator);
            for(File f: files)
            {
                if(f.isDirectory())
                {
                    String tmp = f.toString();
                    if(tmp.endsWith("/"))   tmp = tmp.substring(0, tmp.length() - 1);
                    int pos = tmp.lastIndexOf("/");
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    map.put("ItemImage", R.drawable.folder_yellow_full);
                    map.put("ItemTitle",tmp.substring(pos + 1, tmp.length()));
                    dirs.add(map);
                }
                else if(f.isFile() && fileType != null)
                {
                    for(int i = 0; i< fileType.length; i++)
                    {
                        int typeStrLen = fileType[i].length();
                        String fileName = f.getPath().substring(f.getPath().length() - typeStrLen);
                        if (fileName.equalsIgnoreCase(fileType[i])) {
                            HashMap<String, Object> map = new HashMap<String, Object>();
                            map.put("ItemImage", R.drawable.format_text);
                            map.put("ItemTitle",f.toString().substring(path.length() + 1, f.toString().length()));
                            dirs.add(map);
                            break;
                        }
                    }
                }
            }
        }
        if(dirs.size() == 0) {
            HashMap<String, Object> map = new HashMap<String, Object>();

            map.put("ItemTitle","..");
            dirs.add(map);
        }

        return dirs;
    }
    @Override
    public void onClick(View args0) {
        // TODO Auto-generated method stub
        if(args0.getId() == home.getId())
        {
            // 点击"Home"按钮，回到根目录
            path = getRootDir();
            if(type == TypeOpen)    curFilePath.setText(path);
            Handler handler = new Handler();
            handler.post(add);   // 更新子文件夹
        }
        else if(args0.getId() == back.getId())
        {
            // 点击"Back"按钮，返回上一级文件夹
            path = getSubDir(path);
            if(type == TypeOpen)    curFilePath.setText(path);
            Handler handler = new Handler();
            handler.post(add);   // 更新子文件夹
        }
        else if(args0.getId() == ok.getId())
        {
            // 点击"OK"按钮，关闭对话框，调用自定义监视器的OnOKClick方法，将当前目录返回主Activity
            dismiss();
            File f=new File(path);
            if(type == TypeOpen) {
                if(f.isFile())
                listener.OnOkClick(path,0);
                else
                listener.OnOkClick("",0);
            }
            else {

                if(f.isFile()) path = getSubDir(path);
                listener.OnOkClick(path +"/"+ saveFileName.getText().toString(),1);
            }
        }
        else if(args0.getId() == cancel.getId())
        {
            // 点击"Cancel”按钮
            this.cancel();
        }
    }

    /**
     * Get SD card directory, if SD card not exist, return '/'
     * @return
     */
    private String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);   // 判断是否存在SD卡
        if(sdCardExist)
        {
            sdDir = Environment.getExternalStorageDirectory();   // 如果SD卡存在，返回SD卡的目录
        }
        if(sdDir == null)
        {
            return "/";   // 如果SD卡不存在，返回根目录
        }
        return sdDir.toString();
    }

    private String getRootDir() {
        return "/";
    }

    /**
     * Get upper directory
     * @param path
     * @return
     */
    private String getSubDir(String path) {
        String subpath = "/";
        if(path.endsWith("/"))
        {
            path = path.substring(0, path.length() - 1);
        }
        int pos = path.lastIndexOf("/");
        if(pos > 0)
        {
            subpath = path.substring(0, pos);
        }
        return subpath;
    }
}
