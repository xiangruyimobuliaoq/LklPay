package com.nst.lklpay;

import android.app.Application;

import com.nst.lklpay.util.UIUtil;
import com.xiasuhuei321.loadingdialog.manager.StyleManager;
import com.xiasuhuei321.loadingdialog.view.LoadingDialog;

/**
 * 创建者     彭龙
 * 创建时间   2019/4/20 9:58 AM
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        StyleManager s = new StyleManager();
//在这里调用方法设置s的属性
//code here...
        s.Anim(false).repeatTime(0).contentSize(-1).intercept(true);
        LoadingDialog.initStyle(s);
        UIUtil.init(this);
    }
}
