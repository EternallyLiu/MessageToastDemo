package com.meitu.MessageToastDemo;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

/**
 * Liu Pei  2018/4/26.
 * Mail ：Lp960822@outlook.com
 */

public class ChangeUI {
    /**
     * 动态设置动画背景
     */
    public static void setToastBg(TextView tv,int res,Context context) {
        tv.setPadding(DensityUtil.dip2px(context, 35), DensityUtil.dip2px(context, 10)
                , DensityUtil.dip2px(context, 10), DensityUtil.dip2px(context, 10));
        tv.setTextColor(ContextCompat.getColor(context, R.color.colorTextNormal));
        System.out.println("嘿嘿嘿");
        tv.setBackground(ContextCompat.getDrawable(context, res));
    }

    /**
     * 多人同时播出动画的设置
     */
    public static void changeTvToNormal(TextView tv, Context context) {
        tv.setPadding(0, 0, 0, 0);
        tv.setBackgroundColor(ContextCompat.getColor(context, R.color.colorTextNormal));
        tv.setTextColor(ContextCompat.getColor(context, R.color.colorTextBg));
    }
}
