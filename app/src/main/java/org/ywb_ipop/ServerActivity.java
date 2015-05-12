package org.ywb_ipop;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

/**
 * Created by Administrator on 2015/5/12.
 */
public class ServerActivity  extends Activity{
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.act_server);
        this.setTitle(String.format("%s: %s",
                getResources().getText(R.string.app_name),
                getResources().getText(R.string.title_server)));
    }

}

