package com.nst.lklpay.bean;

/**
 * 创建者     彭龙
 * 创建时间   2019/4/19 11:06 AM
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class PaymentReceive extends DataReceive {
    public Datacontent datacontent;

    public static class Datacontent {
        public String datasecret;
        public String requesttime;
        public PaymentItem data;

        public static class PaymentItem {
            public int transactionType;
            public String orgCode;
            public String posNumber;
            public String billPaymentId;
            //充值单号
            public String billPaymentCode;
            public String paymentRecordId;
            //交费单号
            public String paymentRecordCode;
            public String amount;
            public String originalBelum;
            public String datatype;
            public String datasecret;
            public String requesttime;
            public String pay_tp;
            public String refernumber;
            public String card_no;
            public String time_stamp;
            public String adddataword;
            public PayDetail txndetail;
            public String card_org;
            public String remarkinfo;
            public String batchno;
            public String systraceno;
        }
    }
}
