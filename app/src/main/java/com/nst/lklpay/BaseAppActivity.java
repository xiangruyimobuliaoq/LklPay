package com.nst.lklpay;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.lkl.cloudpos.mdx.aidl.AidlDeviceService;
import com.lkl.cloudpos.mdx.aidl.system.AidlMerListener;
import com.lkl.cloudpos.mdx.aidl.system.AidlSystem;
import com.nst.lklpay.activities.PaymentListActivity;
import com.nst.lklpay.util.Cons;
import com.nst.lklpay.util.SpUtil;

import java.util.ArrayList;
import java.util.List;



public abstract class BaseAppActivity extends BaseActivity {

    public Toolbar toolbar;
    public TextView actTitle;
    public final static int HANlDER_ACTIVITY_TIMEOUT = 10001;
    private int activity_timeout_millis = 60000;

    private static ArrayList<String> nameList;

    protected ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AidlDeviceService serviceManager = AidlDeviceService.Stub.asInterface(iBinder);
            onDeviceConnected(serviceManager);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    protected ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mServer = ((SocketServer.SocketBinder) iBinder).getServer();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
    protected SocketServer mServer;

    private AidlSystem systemInf;

    public void onDeviceConnected(AidlDeviceService serviceManager) {
        try {
            systemInf = AidlSystem.Stub.asInterface(serviceManager
                    .getSystemService());
            systemInf.getMerMsg(new AidlMerListener.Stub() {
                @Override
                public void onSuccess(String s, String s1) throws RemoteException {
                    Log.e("123", "商户号" + s + "->" + "商户终端号" + s1);
                    SpUtil.putString(Cons.SysInfo, Cons.orgCode, s);
                    SpUtil.putString(Cons.SysInfo, Cons.posNumber, s1);
                    bindService(new Intent(BaseAppActivity.this, SocketServer.class), mConn, BIND_AUTO_CREATE);
                }

                @Override
                public void onFail(int i) throws RemoteException {
                    showSnackBarForever("无法识别设备终端号");
                }
            });
            Log.e("123", "绑定系统服务接口正常");
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e("123", "绑定系统服务接口异常");
        }
    }

    //绑定服务
    public void bindService() {
        try {
            Intent intent = new Intent();
            intent.setAction("lkl_cloudpos_mdx_service");
            Intent eintent = new Intent(getExplicitIntent(this, intent));
            Log.d("packageName", eintent.toString());
            boolean flag = false;

            flag = bindService(eintent, conn, Context.BIND_AUTO_CREATE);

            if (flag) {
                Log.d("aaa", "服务绑定成功");
            } else {
                Log.d("aaa", "服务绑定失败");
            }
        } catch (Exception e) {
            return;
        }
    }


    public static Intent getExplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        int j = 0;
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(implicitIntent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            //得到手机上已经安装的应用的名字,即在AndriodMainfest.xml中的app_name。
            String appName = resolveInfo.loadLabel(pm).toString();
            //得到应用所在包的名字,即在AndriodMainfest.xml中的package的值。
            String packageName = resolveInfo.serviceInfo.packageName;
            Log.i("123", "应用的名字:" + appName);
            Log.i("123", "应用的包名字:" + packageName);
            if ("com.lkl.cloudpos.payment".equals(packageName)) {
                nameList.add(appName + ":" + packageName);
                j++;
            }
        }
        // Make sure only one match was found
        if (resolveInfos == null || resolveInfos.size() != 1) {
            return null;
        }
        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfos.get(0);
        Log.d("PackageName", resolveInfos.size() + "");
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;

        ComponentName component = new ComponentName(packageName, className);
        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);
        // Set the component to be explicit
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

    protected void setActivityTimeOutMillis(int millis) {
        this.activity_timeout_millis = millis;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        actTitle = (TextView) findViewById(R.id.tv_toolbar_title);
        initToolbar();
        if (null == SpUtil.getString(Cons.SysInfo, Cons.orgCode, null) || null == SpUtil.getString(Cons.SysInfo, Cons.posNumber, null)) {
            bindService();
        } else {
            bindService(new Intent(BaseAppActivity.this, SocketServer.class), mConn, BIND_AUTO_CREATE);
        }
    }

    protected void sendAcivityTimeOutMessage(int millis) {
        setActivityTimeOutMillis(millis);
        sendAcivityTimeOutMessage();
    }

    protected void sendAcivityTimeOutMessage() {
        mHandler.sendEmptyMessageDelayed(HANlDER_ACTIVITY_TIMEOUT, activity_timeout_millis);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void initToolbar() {
        super.initToolbar(toolbar);
        setActTitle(getTitle());
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(getHomeButtonEnable());
        actionBar.setDisplayHomeAsUpEnabled(getHomeButtonEnable());

    }

    public void setActTitle(CharSequence title) {
        if (TextUtils.isEmpty(title)) return;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
            if (actTitle != null)
                actTitle.setText(title);
        }
    }

    protected Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case HANlDER_ACTIVITY_TIMEOUT:
                    finish();
                    break;
                default:
                    dispatchOtherMessage(msg);
                    break;
            }


        }
    };

    protected void dispatchOtherMessage(Message msg) {

    }

    protected boolean getHomeButtonEnable() {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("123", "关闭了");
        unbindService(conn);
        unbindService(mConn);
    }
}
