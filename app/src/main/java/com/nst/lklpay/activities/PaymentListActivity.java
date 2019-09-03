package com.nst.lklpay.activities;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.google.gson.Gson;
import com.nst.lklpay.BaseAppActivity;
import com.nst.lklpay.Layout;
import com.nst.lklpay.R;
import com.nst.lklpay.bean.LoginReceive;
import com.nst.lklpay.bean.PayDetail;
import com.nst.lklpay.bean.PayStatusSend;
import com.nst.lklpay.bean.PaymentLIstReceive;
import com.nst.lklpay.bean.PaymentReceive;
import com.nst.lklpay.util.Cons;
import com.nst.lklpay.util.SpUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;

@Layout(layoutId = R.layout.activity_paymentlist)
public class PaymentListActivity extends BaseAppActivity {

    @BindView(R.id.refresh)
    ImageView refresh;
    @BindView(R.id.merchantName)
    TextView merchantName;
    @BindView(R.id.terminalName)
    TextView terminalName;
    @BindView(R.id.orgCode)
    TextView orgCode;
    @BindView(R.id.posNumber)
    TextView posNumber;
    @BindView(R.id.paymentList)
    RecyclerView paymentList;
    private PaymentListAdapter mPaymentListAdapter;
    private PaymentReceive.Datacontent.PaymentItem mCurrItem;

    @Override
    protected boolean getHomeButtonEnable() {
        return false;
    }

    @Override
    protected void init() {
//        showDialog("haha",true);
//        dismissDialog();
        orgCode.setText(SpUtil.getString(Cons.SysInfo, Cons.orgCode, null));
        posNumber.setText(SpUtil.getString(Cons.SysInfo, Cons.posNumber, null));
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        paymentList.setLayoutManager(manager);
        mPaymentListAdapter = new PaymentListAdapter(R.layout.item_paymentlist, null);
        paymentList.setAdapter(mPaymentListAdapter);
        mPaymentListAdapter.setEmptyView(R.layout.layout_empty, paymentList);
        refresh.setOnClickListener(v -> {
            getPaymentList();
        });
        mPaymentListAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {

                mCurrItem = (PaymentReceive.Datacontent.PaymentItem) adapter.getItem(position);
                if (view.getId() == R.id.card) {
//                    toast("刷卡");
                    pay((PaymentReceive.Datacontent.PaymentItem) adapter.getData().get(position), "card");
                } else if (view.getId() == R.id.code) {
                    pay((PaymentReceive.Datacontent.PaymentItem) adapter.getData().get(position), "0".equals(mCurrItem.pay_tp) || TextUtils.isEmpty(mCurrItem.pay_tp) ? "card" : "code");
//                    toast("扫码");
                } else if (view.getId() == R.id.cancel) {
                    showCancelDialog(mCurrItem);
                }
            }
        });
    }

    private void showCancelDialog(PaymentReceive.Datacontent.PaymentItem currItem) {
        new AlertDialog.Builder(this).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDialog("处理中,请稍后", false);
                PayStatusSend send = new PayStatusSend();
                send.funcName = "setPaymentStatus";
                if (mCurrItem.transactionType == 1) {
                    send.state = "1";
                } else {
                    send.state = "2";
                }
                send.datacontent = currItem;
                mServer.send(new Gson().toJson(send));
            }
        }).setNegativeButton("取消", null).setTitle("温馨提示").setMessage("您确定要取消此交易订单吗?").show();
    }

    void pay(PaymentReceive.Datacontent.PaymentItem item, String card) {
        Log.e("123", card);
        showDialog("处理中,请稍后", false);
        Intent pay = new Intent();
        pay.setComponent(new ComponentName("com.lkl.cloudpos.payment", "com.lkl.cloudpos.payment.activity.MainMenuActivity"));
        Bundle bundle = new Bundle();
        bundle.putString("msg_tp", "0020");
        bundle.putString("pay_tp", card.equals("card") ? "0" : "1");
        bundle.putString("proc_tp", "00");
        if (item.transactionType == 1) {
            //支付
            if ("card".equals(card)) {
                bundle.putString("proc_cd", "000000");
            } else {
                bundle.putString("proc_cd", "660000");
            }
            bundle.putString("amt", "0.01");
        } else if (item.transactionType == 2) {
            //退款
            if ("card".equals(card)) {
                bundle.putString("proc_cd", "200000");
                bundle.putString("batchbillno", item.batchno + item.systraceno);
                Log.e("123", item.batchno + item.systraceno);
            } else {
                bundle.putString("proc_cd", "680000");
                bundle.putString("batchbillno", item.batchno + item.systraceno);
            }
        }
        bundle.putString("order_no", item.paymentRecordCode);
        bundle.putString("appid", "com.nst.lklpay");
        bundle.putString("time_stamp", System.currentTimeMillis() + "");
        bundle.putString("order_info", "测试订单信息");
        bundle.putString("print_info", "打印测试订单信息");
        pay.putExtras(bundle);
        startActivityForResult(pay, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                mCurrItem.paymentRecordCode = data.getStringExtra("order_no");
                mCurrItem.pay_tp = data.getStringExtra("pay_tp");
                mCurrItem.refernumber = data.getStringExtra("refernumber");
                mCurrItem.card_no = data.getStringExtra("card_no");
                mCurrItem.time_stamp = data.getStringExtra("time_stamp");
                mCurrItem.adddataword = data.getStringExtra("adddataword");
                mCurrItem.txndetail = new Gson().fromJson(data.getStringExtra("txndetail"), PayDetail.class);
                mCurrItem.card_org = data.getStringExtra("card_org");
                mCurrItem.remarkinfo = data.getStringExtra("remarkinfo");
                PayStatusSend send = new PayStatusSend();
                if (mCurrItem.transactionType == 1) {
                    send.state = "2";
                } else {
                    send.state = "3";
                }
                send.funcName = "setPaymentStatus";
                send.datacontent = mCurrItem;
                String text = new Gson().toJson(send);
                Log.e("123", text);
                mServer.send(text);
            } else if (resultCode == RESULT_CANCELED) {
                dismissDialog();
                toast(data.getStringExtra("reason"));
                Log.e("123", data.getStringExtra("reason") + resultCode);
            } else {
                dismissDialog();
                toast(data.getStringExtra("reason"));
                Log.e("123", data.getStringExtra("reason") + resultCode);
            }
        }
    }

    private void getPaymentList() {
        try {
            mServer.send(new JSONObject().put("funcName", "paymentList").toString());
            Log.e("123", "11111111111");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    class PaymentListAdapter extends BaseQuickAdapter<PaymentReceive.Datacontent.PaymentItem, BaseViewHolder> {

        public PaymentListAdapter(int layoutResId, @Nullable List<PaymentReceive.Datacontent.PaymentItem> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, PaymentReceive.Datacontent.PaymentItem item) {
            helper.setText(R.id.requesttime, item.requesttime)
                    .setText(R.id.paymentRecordCode, item.paymentRecordCode)
                    .setText(R.id.amount, item.amount)
                    .setText(R.id.transactionType, item.transactionType == 1 ? "交费" : "冲销")
                    .addOnClickListener(R.id.card)
                    .addOnClickListener(R.id.cancel)
                    .addOnClickListener(R.id.code)
                    .setVisible(R.id.card, item.transactionType == 1)
                    .setText(R.id.code, item.transactionType == 1 ? "刷卡" : "冲销");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginReceive(LoginReceive.LoginInfo ev) {
        merchantName.setText(ev.merchantName);
        terminalName.setText(ev.terminalName);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPaymentListReceive(PaymentLIstReceive ev) {
        mPaymentListAdapter.replaceData(ev.datacontent);
        dismissDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPaymentPush(PaymentReceive.Datacontent ev) {
        boolean needAdd = true;
        for (PaymentReceive.Datacontent.PaymentItem item :
                mPaymentListAdapter.getData()) {
            if (item.paymentRecordCode.equals(ev.data.paymentRecordCode)) {
                needAdd = false;
            }
        }
        if (needAdd) {
            ev.data.requesttime = ev.requesttime;
            mPaymentListAdapter.addData(ev.data);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPayStatusReceive(PaymentReceive.Datacontent.PaymentItem ev) {
        getPaymentList();
    }
}
