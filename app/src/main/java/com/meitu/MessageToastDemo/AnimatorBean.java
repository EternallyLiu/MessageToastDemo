package com.meitu.MessageToastDemo;

/**
 * Liu Pei  2018/4/12.
 * Mail ：Lp960822@outlook.com
 */

//动画执行时的配置对象
public class AnimatorBean {
    private String message;
    private int userLevel;
    private boolean isShowBg;

    public AnimatorBean(String message, int userLevel, boolean isShowBg) {
        this.message = message;
        this.userLevel = userLevel;
        this.isShowBg = isShowBg;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(int userLevel) {
        this.userLevel = userLevel;
    }

    public boolean isShowBg() {
        return isShowBg;
    }

    public void setShowBg(boolean showBg) {
        isShowBg = showBg;
    }
}
