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

package org.ywb_ipop.util;

import org.ywb_ipop.HelpActivity;
import org.ywb_ipop.R;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * @author Kenny Root
 *
 */
public class HelpTopicView extends WebView {
	public HelpTopicView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	public HelpTopicView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public HelpTopicView(Context context) {
		super(context);
		initialize();
	}

	private void initialize() {
		WebSettings wSet = getSettings();
		wSet.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
		wSet.setUseWideViewPort(false);
	}

	public HelpTopicView setTopic(String topic) {
        String temps=getResources().getString(R.string.ywb_help);
        if(temps.equals("help_cn"))
        {
            if(topic.equals("小提示"))
                topic="Hints";
            else if(topic.equals("物理键盘"))
                topic="PhysicalKeyboard";
            else if(topic.equals("屏幕手势"))
                topic="ScreenGestures";
            else if(topic.equals("虚拟键盘"))
                topic="VirtualKeyboard";

        }
		String path;
		if(topic.equals("在线帮助")||topic.equals("Online Help"))
			 path ="http://iytc.net/soft/ipop.html" ;
		else
		 path = String.format("file:///android_asset/%s/%s%s",
                temps, topic, HelpActivity.SUFFIX);
				//HelpActivity.HELPDIR, topic, HelpActivity.SUFFIX);
		loadUrl(path);

		computeScroll();

		return this;
	}
}
