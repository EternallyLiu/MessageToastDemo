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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.LruCache;
import android.view.Display;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private TextView tv_toast;
    private BlockingQueue<MessageBean> mRoomQueue;
    private BlockingQueue<MessageBean> mLikeQueue;
    private BlockingQueue<MessageBean> mGiftQueue;
    private BlockingQueue<MessageBean> mShareQueue;

    private static final int MAX_VALUE = 6;
    private static final int SHOW_NUMBER = 3;

    private StringBuffer mMessage;//文案内容
    private MainActivity.MyHandler mHandler = new MainActivity.MyHandler(this);
    private MessageBean mBean;//消息提示实体对象
    private int mScreenHeight;
    private int mScreenWidth;
    private AnimatorSet mAnimatorSet;

    private EditText op1, op2, op3, op4;
    private LooperThread mLooperThread;
    private LruCache<Integer, AnimatorBean> mAnimatorList;
    private static final int MAX_LRUCACHE = 500;
    private int mKeyIndex = 0;
    private int mPlayIndex = 0;
    private boolean isPlaying = false;//动画是否在播放的标志

    private static final int COME_TIME = 800;
    private static final int SHOW_TIME = 500;
    private static final int OUT_TIME = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();//初始化发送按钮
        initAnimation();//初始化动画集合
        mLooperThread = new LooperThread();
        mLooperThread.start();
    }

    /**
     * 初始化View
     */
    private void initView() {
        initStructure();//初始化存储数据结构
        tv_toast = findViewById(R.id.tv_toast);
        op1 = findViewById(R.id.op1);
        op2 = findViewById(R.id.op2);
        op3 = findViewById(R.id.op3);
        op4 = findViewById(R.id.op4);
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = Integer.parseInt(op1.getText().toString());
                for (int i = 0; i < count; i++) {
                    sendMessage(1);
                }
            }
        });
        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = Integer.parseInt(op2.getText().toString());
                for (int i = 0; i < count; i++) {
                    sendMessage(2);
                }
            }
        });
        findViewById(R.id.btn3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = Integer.parseInt(op3.getText().toString());
                for (int i = 0; i < count; i++) {
                    sendMessage(3);
                }
            }
        });
        findViewById(R.id.btn4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = Integer.parseInt(op4.getText().toString());
                for (int i = 0; i < count; i++) {
                    sendMessage(4);
                }
            }
        });
        findViewById(R.id.btnR).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new Random().nextInt(4) + 1);
            }
        });
    }

    /**
     * 发送消息
     * @param opType
     */
    private void sendMessage(final int opType) {
        new Thread(new Runnable() {
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
                try {
                    switch (message.getOpType()) {
                        case 1:
                            if (mRoomQueue.size() >= MAX_VALUE) {
                                return;
                            }
                            mRoomQueue.put(message);
                            break;
                        case 2:
                            if (mLikeQueue.size() >= MAX_VALUE) {
                                return;
                            }
                            mLikeQueue.put(message);
                            break;
                        case 3:
                            mGiftQueue.put(message);
                            break;
                        case 4:
                            if (mShareQueue.size() >= MAX_VALUE) {
                                return;
                            }
                            mShareQueue.put(message);
                            break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
     * 对于进入房间点赞分享等操作大量数据时，大量创建Node对象并插入和删除，并且虽然可能会存在有并发操作但是因为只存在一次取操作所以阻塞时间很短。
     * 所以可以直接使用数组队列，而不必考虑锁分离，故综合来讲选择ArrayBlockingQueue。
     *
     * 送礼物的话数据量没有那么庞大（数据量庞大会造成系统不断GC，影响性能），但是每条都需要去读取回调UI，所以耗时长.
     * 同时在此期间可能会有写入的并发操作，故使用带有锁分离的LinkedBlockingQueue降低锁的粒度,
     * 从而不需要去锁住整个队列来提高性能，以确保高并发时减少阻塞，同时虽付出一些空间成本但数据量不是特别大仍是最优。
     */
    private void initStructure() {
        mRoomQueue = new ArrayBlockingQueue<>(MAX_VALUE);//设置为6个是如果当前已经存在三个消息对象在排队，因为只最多展示三个，故之后的消息舍弃掉且不再进入队列。
        mLikeQueue = new ArrayBlockingQueue<>(MAX_VALUE);
        mShareQueue = new ArrayBlockingQueue<>(MAX_VALUE);

        mGiftQueue = new LinkedBlockingQueue<>();
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
     * 初始化动画集合
     */
    public void initAnimation() {
        mAnimatorList = new LruCache<>(MAX_LRUCACHE);
        initDisPlay();
        ObjectAnimator moveAnimator = ObjectAnimator.ofFloat(tv_toast, "TranslationX", mScreenWidth, tv_toast.getX());
        moveAnimator.setDuration(COME_TIME);
        ObjectAnimator showAnimator = ObjectAnimator.ofFloat(tv_toast, "alpha", 1.0f, 1.0f);
        showAnimator.setDuration(SHOW_TIME);
        ObjectAnimator hideAnimator = ObjectAnimator.ofFloat(tv_toast, "TranslationX", -mScreenWidth);
        hideAnimator.setDuration(OUT_TIME);
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playSequentially(moveAnimator, showAnimator, hideAnimator);
        mAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mPlayIndex += 1;
                changeTvToShow(mAnimatorList.get(mPlayIndex));
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!(mPlayIndex + 1 > mKeyIndex)) {
                    mAnimatorSet.start();
                    return;
                }

                synchronized (mLooperThread) {
                    mLooperThread.notifyAll();
                    isPlaying = false;
                }
            }
        });
    }

    /**
     * 设置播放单人提示时的配置
     *
     * @param animatorBean
     */
    private void changeTvToShow(AnimatorBean animatorBean) {
        if (animatorBean == null) {
            return;
        }
        //根据用户级别设置动画背景
        if (animatorBean.isShowBg()) {
            int level = animatorBean.getUserLevel();
            if (level == 1) {
                setToastBg(R.drawable.level_badge_1);
            } else if (level >= 2 && level <= 15) {
                setToastBg(R.drawable.level_badge_2);
            } else if (level >= 16 && level <= 25) {
                setToastBg(R.drawable.level_badge_16);
            } else if (level >= 26 && level <= 40) {
                setToastBg(R.drawable.level_badge_26);
            } else if (level >= 41) {
                setToastBg(R.drawable.level_badge_41);
            }
        } else {
            changeTvToNormal();
        }
        tv_toast.setText(animatorBean.getMessage());
    }

    /**
     * 动态设置背景
     */
    public void setToastBg(int res) {
        tv_toast.setPadding(DensityUtil.dip2px(getApplicationContext(), 35), DensityUtil.dip2px(getApplicationContext(), 10)
                , DensityUtil.dip2px(getApplicationContext(), 10), DensityUtil.dip2px(getApplicationContext(), 10));
        tv_toast.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorTextNormal));
        tv_toast.setBackground(ContextCompat.getDrawable(MainActivity.this, res));
    }

    /**
     * 多人弹窗时的设置
     */
    private void changeTvToNormal() {
        tv_toast.setPadding(0, 0, 0, 0);
        tv_toast.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorTextNormal));
        tv_toast.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorTextBg));
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
     * 轮训器线程,定义为内部类而非匿名内部类，防止内存泄漏
     */
    private class LooperThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                synchronized (mLooperThread) {
                    try {
                        if (mRoomQueue.size() != 0) {
                            sendMessageToHandler(getTypeStr(mRoomQueue));//处理进入房间
                            mLooperThread.wait();
                        }
                        if (mLikeQueue.size() != 0) {
                            sendMessageToHandler(getTypeStr(mLikeQueue));//处理点赞
                            mLooperThread.wait();
                        }
                        //处理送礼物,只取截止到获取礼物池中的数量时的队列长度
                        int giftNumber = mGiftQueue.size();
                        if (giftNumber != 0) {
                            for (int i = 0; i < giftNumber; i++) {
                                sendMessageToHandler(getTypeStr(mGiftQueue));
                            }
                        }

                        if (mShareQueue.size() != 0) {
                            sendMessageToHandler(getTypeStr(mShareQueue));//处理分享
                            mLooperThread.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 发送消息至Handler消息队列
     */
    private void sendMessageToHandler(AnimatorBean obj) {
        Message toastMessage = Message.obtain();
        toastMessage.obj = obj;
        mHandler.sendMessage(toastMessage);
    }

    /**
     * 获取到对应类型的文案输出
     */
    public AnimatorBean getTypeStr(BlockingQueue<MessageBean> queue) {
        if (mMessage == null) {
            mMessage = new StringBuffer();
        }
        //清空StringBuffer中的内容
        mMessage.setLength(0);
        //记录本次播放次数
        int index = 0;
        //如果只有一个人用来记录级别
        int userLevel = 1;

        if (queue instanceof ArrayBlockingQueue) {
            //以此时开始取的数据个数为size,在此期间加入队列的数据不计入其中
            int reallySize = queue.size();
            int size = reallySize > SHOW_NUMBER ? SHOW_NUMBER : reallySize;
            //非送礼部分
            for (int i = 0; i < size; i++) {
                mBean = queue.poll();
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
                for (int i = 0; i < reallySize - SHOW_NUMBER; i++) {
                    queue.poll();
                }
            }
        } else {
            //送礼部分
            mBean = queue.poll();
            index += 1;
            userLevel = mBean.getUser().getLevel();
            mMessage.append(mBean.getUser().getName()).append(getResources().getString(R.string.gift)).append(mBean.getGift().getName());
        }

        if (index != 1) {
            return new AnimatorBean(mMessage.toString(), userLevel, false);
        } else {
            return new AnimatorBean(mMessage.toString(), userLevel, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //移除所有消息释放资源
        mMessage = null;
        mHandler.removeCallbacksAndMessages(null);
    }
}
