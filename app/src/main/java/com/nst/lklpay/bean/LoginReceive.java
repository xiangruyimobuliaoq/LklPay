package com.nst.lklpay.bean;

import com.google.gson.annotations.SerializedName;

/**
 * 创建者     彭龙
 * 创建时间   2019-04-24 11:47
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class LoginReceive extends DataReceive {


    /**
     * datacontent : {"terminalID":"34500509","merchantID":"6101","terminalName":"测试机1","remark":"是备注","state":"0","merchantName":"成都米兰柏羽"}
     */

    public LoginInfo datacontent;

    public static class LoginInfo {
        /**
         * terminalID : 34500509
         * merchantID : 6101
         * terminalName : 测试机1
         * remark : 是备注
         * state : 0
         * merchantName : 成都米兰柏羽
         */

        public String terminalID;
        public String merchantID;
        public String terminalName;
        public String remark;
        @SerializedName("state")
        public String stateX;
        public String merchantName;
    }
}
