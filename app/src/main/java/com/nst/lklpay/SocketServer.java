package com.nst.lklpay;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.nst.lklpay.bean.DataReceive;
import com.nst.lklpay.bean.LoginReceive;
import com.nst.lklpay.bean.PayStatusReceive;
import com.nst.lklpay.bean.PaymentLIstReceive;
import com.nst.lklpay.bean.PaymentReceive;
import com.nst.lklpay.bean.SocketState;
import com.nst.lklpay.util.Cons;
import com.nst.lklpay.util.SpUtil;

import org.greenrobot.eventbus.EventBus;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 创建者     彭龙
 * 创建时间   2019/4/20 10:22 AM
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class SocketServer extends Service {
    private Gson mGson = new Gson();
    private Handler mHandler = new Handler();
    private Client mClient;
    private Timer mTimer;
    String orgCode = SpUtil.getString(Cons.SysInfo, Cons.orgCode, null);
    String posNumber = SpUtil.getString(Cons.SysInfo, Cons.posNumber, null);
        String url = "wss://106.39.8.73:8443/websocket/" + orgCode + "/" + posNumber;
//    String url = "ws://192.168.2.105:80/websocket/" + orgCode + "/" + posNumber;
    private SocketBinder mSocketBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        connect();
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (null != mClient && mClient.getConnection().isOpen())
                        mClient.sendPing();
                }
            }, 30000, 30000);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (null == mSocketBinder) {
            mSocketBinder = new SocketBinder();
        }
        return mSocketBinder;
    }

    public void connect() {

        try {
            mClient = new Client(new URI(url));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                    new X509TrustManager() {


                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            }, new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SSLSocketFactory factory = sslContext.getSocketFactory();
        mClient.setSocketFactory(factory);
        mClient.connect();
    }


    public void send(String text) {
        if (null == mClient) {
            Log.e("123", "client为空");
        }
        if (!mClient.getConnection().isOpen()) {
            Log.e("123", "client未连接");
        }
        if (null != mClient && mClient.getConnection().isOpen()) {
            mClient.send(text);
        } else {
            mHandler.post(() -> {
                try {
                    mClient = new Client(new URI(url));
                    mClient.connect();
                    Log.e("123", "重新连接");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    class Client extends WebSocketClient {
        public Client(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onWebsocketPong(WebSocket conn, Framedata f) {
            Log.e("onWebsocketPong", f.toString());
            super.onWebsocketPong(conn, f);
        }

        @Override
        public void onWebsocketPing(WebSocket conn, Framedata f) {
            Log.e("onWebsocketPong", f.toString());
            super.onWebsocketPing(conn, f);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.e("onOpen", handshakedata.toString());
            EventBus.getDefault().post(new SocketState(1));
        }

        @Override
        public void onMessage(String message) {
            Log.e("onMessage", message);
            DataReceive dataReceive = mGson.fromJson(message, DataReceive.class);
            if (dataReceive.state.equals("success")) {
                switch (dataReceive.funcName) {
                    case "paymentPush":
                        paymentPush(message);
                        break;
                    case "login":
                        loginPush(message);
                        break;
                    case "paymentList":
                        paymentList(message);
                        break;
                    case "setPaymentStatus":
                        setPaymentStatus(message);
                        break;
                }
            } else if (dataReceive.state.equals("error")) {
                switch (dataReceive.funcName) {
                    case "login":
                        EventBus.getDefault().post(new SocketState(2));
                        break;
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.e("onClose", reason + "   " + remote);
            EventBus.getDefault().post(new SocketState(0));
            if (null != mClient)
                mClient.close();
            mHandler.postDelayed(() -> {
                SocketServer.this.connect();
                Log.e("123", "重新连接");
            }, 2000);
        }

        @Override
        public void onError(Exception ex) {
            Log.e("onError", ex.getMessage());
        }
    }

    private void setPaymentStatus(String message) {
        PayStatusReceive receive = mGson.fromJson(message, PayStatusReceive.class);
        EventBus.getDefault().post(receive.datacontent);
    }

    private void paymentPush(String message) {
        PaymentReceive receive = mGson.fromJson(message, PaymentReceive.class);
        EventBus.getDefault().post(receive.datacontent);
    }

    private void paymentList(String message) {
        PaymentLIstReceive receive = mGson.fromJson(message, PaymentLIstReceive.class);
        EventBus.getDefault().post(receive);
    }

    private void loginPush(String message) {
        LoginReceive receive = mGson.fromJson(message, LoginReceive.class);
        EventBus.getDefault().post(receive.datacontent);
    }

    public class SocketBinder extends Binder {
        public SocketServer getServer() {
            return SocketServer.this;
        }
    }

}
