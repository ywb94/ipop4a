package org.ywb_ipop;

import android.content.Context;
import android.widget.Button;

/**
 * Created by Administrator on 2015/5/10.
 */
public class MyButton extends Button {
    private String content;
    private int type;
    public MyButton(Context context) {
        super(context,null);
    }
    public String getButtonContent() {
        return content;
    }
    public void setButtonContent(String content) {
        this.content = content;
    }
    public int getButtonType() {
        return type;
    }
    public void setButtonType(int type) {
        this.type = type;
    }
}
