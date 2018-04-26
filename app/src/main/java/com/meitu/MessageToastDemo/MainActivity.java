package com.meitu.MessageToastDemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.LruCache;
import android.view.Display;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.edit_room)
    EditText mEditRoom;
    @BindView(R.id.edit_like)
    EditText mEditLike;
    @BindView(R.id.edit_gift)
    EditText mEditGift;
    @BindView(R.id.edit_share)
    EditText mEditShare;
    @BindView(R.id.tv_toast)
    TextView tv_toast;

    private ConcurrentLinkedQueue<MessageBean> mRoomQueue;
    private ConcurrentLinkedQueue<MessageBean> mLikeQueue;
    private ConcurrentLinkedQueue<MessageBean> mGiftQueue;
    private ConcurrentLinkedQueue<MessageBean> mShareQueue;

    private MainActivity.MyHandler mHandler = new MainActivity.MyHandler(this);
    private MessageBean mBean;//消息提示实体对象
    private AnimatorSet mAnimatorSet;
    private LooperThread mLooperThread;

    private int mScreenHeight;
    private int mScreenWidth;
    private StringBuffer mMessage;//文案内容
    private int mKeyIndex = 0;
    private int mPlayIndex = 0;
    private volatile boolean isPlaying = false;//动画是否在播放的标志
    private volatile boolean isStopLooper = false;//是否退出消息轮询线程
    private LruCache<Integer, AnimatorBean> mAnimatorList;

    private static final int COME_TIME = 800;
    private static final int SHOW_TIME = 500;
    private static final int OUT_TIME = 200;
    private static final int MAX_LRUCACHE = 500;
    private static final int SHOW_NUMBER = 3;
    private static final int MAX_VALUE = 6;

    private Object mRoomLock = new Object();
    private Object mLikeLock = new Object();
    private Object mGiftLock = new Object();
    private Object mShareLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initStructure();//初始化存储数据结构
        initAnimation();//初始化动画集合
        mLooperThread = new LooperThread();
        mLooperThread.start();
    }

    /**
     * 初始化点击事件
     * @param view
     */
    @OnClick({R.id.btn_room, R.id.btn_like, R.id.btn_gift, R.id.btn_share, R.id.btn_radom})
    public void onclickBtn(View view) {
        int count;
        switch (view.getId()) {
            case R.id.btn_room:
                count = Integer.parseInt(mEditRoom.getText().toString());
                for (int i = 0; i < count; i++) {
                    sendMessage(1);
                }
                break;
            case R.id.btn_like:
                count = Integer.parseInt(mEditLike.getText().toString());
                for (int i = 0; i < count; i++) {
                    sendMessage(2);
                }
                break;
            case R.id.btn_gift:
                count = Integer.parseInt(mEditGift.getText().toString());
                for (int i = 0; i < count; i++) {
                    sendMessage(3);
                }
                break;
            case R.id.btn_share:
                count = Integer.parseInt(mEditShare.getText().toString());
                for (int i = 0; i < count; i++) {
                    sendMessage(4);
                }
                break;
            case R.id.btn_radom:
                sendMessage(new Random().nextInt(4) + 1);
                break;
        }
    }

    /**
     * 发送消息
     * @param opType
     */
    private void sendMessage(final int opType) {
        Thread message = new Thread(new Runnable() {
            @Override
            public void run() {
                MessageBean.User userBean = getUser();
                MessageBean.Gift giftBean = null;
                switch (opType) {
                    case 3:
                        giftBean = getGiftBean();
                        break;
                }
                MessageBean message = new MessageBean(opType, userBean, giftBean);
                switch (message.getOpType()) {
                    case 1:
                        synchronized (mRoomLock) {
                            mRoomQueue.offer(message);
                        }
                        break;
                    case 2:
                        synchronized (mLikeLock) {
                            mLikeQueue.offer(message);
                        }
                        break;
                    case 3:
                        //送礼物队列不限制大小
                        synchronized (mGiftLock) {
                            mGiftQueue.offer(message);
                        }
                        break;
                    case 4:
                        synchronized (mShareLock) {
                            mShareQueue.offer(message);
                        }
                        break;
                }
            }
        });
        message.start();
    }

    private MessageBean.User getUser() {
        int id = new Random().nextInt(10);
        int level = new Random().nextInt(60);
        return new MessageBean.User("userId" + id, "userName" + id, level);
    }

    private MessageBean.Gift getGiftBean() {
        int id = new Random().nextInt(10);
        return new MessageBean.Gift("giftId" + id, "giftName" + id);
    }

    /**
     * 初始化存储数据结构
     */
    private void initStructure() {
        mRoomQueue = new ConcurrentLinkedQueue<>();
        mLikeQueue = new ConcurrentLinkedQueue<>();
        mShareQueue = new ConcurrentLinkedQueue<>();
        mGiftQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Handler静态内部类
     */
    private static class MyHandler extends Handler {
        //持有activity的弱引用，防止无法及时释放
        private WeakReference<Context> reference;

        private MyHandler(Context context) {
            reference = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = (MainActivity) reference.get();
            if (activity != null) {
                activity.startAnimation((AnimatorBean) msg.obj);
            }
        }
    }

    /**
     * 初始化动画集合
     */
    public void initAnimation() {
        initDisPlay();
        mAnimatorList = new LruCache<>(MAX_LRUCACHE);
        mAnimatorSet = new AnimatorSet();
        //按次序播放动画
        mAnimatorSet.playSequentially(createAnimation("TranslationX", COME_TIME, mScreenWidth, tv_toast.getX()),
                createAnimation("alpha", SHOW_TIME, 1.0f, 1.0f),
                createAnimation("TranslationX", OUT_TIME, -mScreenWidth));
        mAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mPlayIndex += 1;
                changeTvToShow(tv_toast,mAnimatorList.get(mPlayIndex));
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mPlayIndex + 1 > mKeyIndex) {
                    isPlaying = false;
                    synchronized (mLooperThread) {
                        mLooperThread.notifyAll();
                        return;
                    }
                }
                mAnimatorSet.start();
            }
        });
    }

    /**
     * 创建动画
     */
    public ObjectAnimator createAnimation(String propertyName, long time, float... values) {
        return ObjectAnimator.ofFloat(tv_toast, propertyName, values).setDuration(time);
    }

    /**
     * 开启动画
     */
    public void startAnimation(AnimatorBean message) {
        mKeyIndex += 1;
        mAnimatorList.put(mKeyIndex, message);
        if (!isPlaying) {
            isPlaying = true;
            tv_toast.setVisibility(View.VISIBLE);
            mAnimatorSet.start();
        }
    }

    /**
     * 获取屏幕宽高，便于执行动画
     */
    private void initDisPlay() {
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        final Point realSize = new Point();
        defaultDisplay.getRealSize(realSize);
        mScreenHeight = realSize.x;
        mScreenWidth = realSize.y;
    }

    /**
     * 设置播放单人提示时的配置
     *
     * @param animatorBean
     */
    private void changeTvToShow(TextView tv,AnimatorBean animatorBean) {
        if (animatorBean == null) {
            return;
        }
        //根据用户级别设置动画背景
        if (animatorBean.isShowBg()) {
            int level = animatorBean.getUserLevel();
            if (level == 1) {
                ChangeUI.setToastBg(tv,R.drawable.level_badge_1,getApplicationContext());
            } else if (level >= 2 && level <= 15) {
                ChangeUI.setToastBg(tv,R.drawable.level_badge_2,getApplicationContext());
            } else if (level >= 16 && level <= 25) {
                ChangeUI.setToastBg(tv,R.drawable.level_badge_16,getApplicationContext());
            } else if (level >= 26 && level <= 40) {
                ChangeUI.setToastBg(tv,R.drawable.level_badge_26,getApplicationContext());
            } else if (level >= 41) {
                ChangeUI.setToastBg(tv,R.drawable.level_badge_41,getApplicationContext());
            }
        } else {
            ChangeUI.changeTvToNormal(tv,getApplicationContext());
        }
        tv.setText(animatorBean.getMessage());
    }

    /**
     * 轮训器线程,定义为内部类而非匿名内部类，防止内存泄漏
     */
    private class LooperThread extends Thread {
        @Override
        public void run() {
            super.run();
            //判断Activity是否已经被销毁
            while (!isStopLooper) {

                        //官网推荐使用isEmpty替换size方法，size方法需要遍历整个链表
                        if (!mRoomQueue.isEmpty()) {
                            synchronized (mRoomLock) {
                                sendMessageToHandler(getTypeStr(mRoomQueue));//处理进入房间
                            }
                        }
                        if (!mLikeQueue.isEmpty()) {
                            synchronized (mLikeLock) {
                                sendMessageToHandler(getTypeStr(mLikeQueue));//处理点赞
                            }
                        }
                        //处理送礼物,只取截止到获取礼物池中的数量时的队列长度
                        synchronized (mGiftLock) {
                            while (true) {
                                if (mGiftQueue.isEmpty()) {
                                    break;
                                }
                                sendMessageToHandler(getTypeStr(mGiftQueue));
                            }
                        }

                        if (!mShareQueue.isEmpty()) {
                            synchronized (mShareLock) {
                                sendMessageToHandler(getTypeStr(mShareQueue));//处理分享
                            }
                        }
            }
        }
    }

    /**
     * 发送消息至Handler消息队列
     */
    private void sendMessageToHandler(AnimatorBean obj) {
        if (obj != null) {
            Message toastMessage = Message.obtain();
            toastMessage.obj = obj;
            mHandler.sendMessage(toastMessage);
            synchronized (mLooperThread) {
                try {
                    mLooperThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取到对应类型的文案输出
     */
    public AnimatorBean getTypeStr(ConcurrentLinkedQueue<MessageBean> queue) {
        if (mMessage == null) {
            mMessage = new StringBuffer();
        }
        //清空StringBuffer中的内容
        mMessage.setLength(0);
        //记录本次播放次数
        int index = 0;
        //单人进入房间时记录级别
        int userLevel = 1;

        if (queue != mGiftQueue) {
            //以此时开始取的数据个数为size,在此期间加入队列的数据不计入其中
            int reallySize = queue.size();
            int size = reallySize > SHOW_NUMBER ? SHOW_NUMBER : reallySize;
            //非送礼部分
            for (int i = 0; i < size; i++) {
                mBean = queue.poll();
                if (mBean == null) {
                    return null;
                }
                if (size == 1) {
                    userLevel = mBean.getUser().getLevel();
                }
                index += 1;
                if (i == size - 1) {
                    //最后一个
                    mMessage.append(mBean.getUser().getName());
                    switch (mBean.getOpType()) {
                        case 1:
                            if (i == SHOW_NUMBER - 1) {
                                //超过最多显示人数
                                mMessage.append(getResources().getString(R.string.comes));
                            } else {
                                mMessage.append(getResources().getString(R.string.come));
                            }
                            break;
                        case 2:
                            if (i == SHOW_NUMBER - 1) {
                                //超过最多显示人数
                                mMessage.append(getResources().getString(R.string.likes));
                            } else {
                                mMessage.append(getResources().getString(R.string.like));
                            }
                            break;
                        case 4:
                            if (i == SHOW_NUMBER - 1) {
                                //超过最多显示人数
                                mMessage.append(getResources().getString(R.string.shares));
                            } else {
                                mMessage.append(getResources().getString(R.string.share));
                            }
                            break;
                    }
                    continue;
                }
                mMessage.append(mBean.getUser().getName()).append(",");
            }
            if (reallySize > SHOW_NUMBER) {
                //此时应该清除掉剩下的数据（在刚刚取操作的时候新添入的数据不可被移除）
                queue.clear();
            }
        } else {
            //送礼部分
            mBean = queue.poll();
            if (mBean == null) {
                return null;
            }
            index += 1;
            userLevel = mBean.getUser().getLevel();
            mMessage.append(mBean.getUser().getName()).append(getResources().getString(R.string.gift)).append(mBean.getGift().getName());
        }

        if (index != 1) {
            return new AnimatorBean(mMessage.toString(), userLevel, false);
        }
        return new AnimatorBean(mMessage.toString(), userLevel, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //移除所有消息释放资源
        mMessage = null;
        isStopLooper = true;//释放消息轮询线程资源
        mHandler.removeCallbacksAndMessages(null);
    }
}
