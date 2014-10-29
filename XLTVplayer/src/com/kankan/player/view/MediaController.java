package com.kankan.player.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.kankan.player.app.AppConfig;
import com.kankan.player.util.AdUtil;
import com.xunlei.tv.player.R;

import java.util.Formatter;
import java.util.Locale;

/**
 * 总结一下目前长按快进策略：向右键第一次按下时，往前快进30秒，如果没松开，系统检测为长按快进，每300毫秒快进视频总时长的1/60，
 * 当900毫秒后如果还在长按快进模式，那么调整每次快进时间为视频总时长的1/55，再过900毫秒调整为1/50，每次快进时间上限为视频总时长的1/30
 */
public class MediaController extends FrameLayout {
    private static enum Operation {
        FORWARD, PREV, PAUSE
    }

    // 单次快进，时间为视频总时长的这么多分之一
    private static final int SINGLE_FORWARD_INTERVAL_BASE = 120;
    // 单次快进，最小间隔10s
    private static final int SINGLE_INTERVAL_LIMIT = 10000;
    private static final int PREV_FORWARD_INTERVAL = 30000; //快进快退基础时间间隔30秒
    //    private static final int PREV_FORWARD_MAX_MULTIPLE = 10; //快进快退最大不超过300秒/次
    // 每隔这么多毫秒发送一次消息，让视频快进一段时间(初始基数为总时长的1/60)
    private static final int FAST_MSG_DELAY = 150;
    // 当连续快进3次后如果还在快进模式，那么增加视频快进的基数
    private static final int FAST_BASE_COUNT = 3;
    // 快进基数为视频总时长除60的时间间隔，根据长按的时间会减小此基数，下限由limit来决定
    private static final int FAST_BASE_TIME = 120;
    private static final int FAST_BASE_TIME_LIMIT = 50;

    // 出现暂停之后3秒不动就出现广告页
    public static final int PAUSE_AD_INTERVAL = 3000;

    private static final int MSG_QUICK_FORWARD = 0x100;
    private static final int MSG_QUICK_PREV = 0x101;
    private static final int MSG_FINISH_QUICK_FORWARD = 0x102;
    private static final int MSG_FINISH_QUICK_PREV = 0x103;
    private static final int MSG_SHOW_PAUSE_AD = 0x104;

    private MediaPlayerControl mPlayer;
    private Context mContext;
    private ViewGroup mAnchor;
    private View mRoot;
    private MediaSeekBar mProgressSb;
    private TextView mEndTime, mCurrentTime, mPredicateTime;
    private boolean mShowing;
    private boolean mPaused;
    private Operation mCurrentOperation;
    private static final int sDefaultTimeout = 2000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int PRE_FADE_OUT = 3;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private ImageView mPauseIv;
    private ImageView mPrevIv;
    private ImageView mForwardIv;
    private TextView mTitleTv;
    private View mBottomControllView;

    private Animation mPushTopInAnim;
    private Animation mPushTopOutAnim;
    private Animation mPushBottomInAnim;
    private Animation mPushBottomOutAnim;

    private boolean mCanRestart = false;
    // 记录是否进入快进或快退模式
    private boolean mIsInFastMode = false;
    // 当进入快进或快退模式的时候，每FAST_MESSAGE_DELAY让视频快进总时长的1/60，
    // 每
    private int mFastCount = 0;

    private float mDensity;
    private String mTitle;

    private OnPauseListener mOnPauseListener;

    public MediaController(Context context) {
        super(context);
        mContext = context;
        mDensity = mContext.getResources().getDisplayMetrics().density;
    }

    @Override
    public void onFinishInflate() {
        if (mRoot != null) {
            initControllerView(mRoot);
        }
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
    }

    public void setOnPauseListener(OnPauseListener listener) {
        mOnPauseListener = listener;
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * When VideoView calls this method, it will use the VideoView's parent
     * as the anchor.
     *
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(ViewGroup view) {
        mAnchor = view;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        makeControllerView();

        addView(mRoot, frameParams);
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     *
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected void makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.view_media_controller, null);

        initControllerView(mRoot);
    }

    private void initControllerView(View v) {
        mForwardIv = (ImageView) v.findViewById(R.id.forward_iv);
        mPrevIv = (ImageView) v.findViewById(R.id.prev_iv);
        mTitleTv = (TextView) v.findViewById(R.id.title);
        mBottomControllView = v.findViewById(R.id.process_controll_rl);
        mPauseIv = (ImageView) v.findViewById(R.id.pause_iv);

        mPushBottomInAnim = AnimationUtils.loadAnimation(mContext, R.anim.push_bottom_in);
        mPushBottomOutAnim = AnimationUtils.loadAnimation(mContext, R.anim.push_bottom_out);
        mPushTopInAnim = AnimationUtils.loadAnimation(mContext, R.anim.push_top_in);
        mPushTopOutAnim = AnimationUtils.loadAnimation(mContext, R.anim.push_top_out);

        if (!TextUtils.isEmpty(mTitle)) {
            mTitleTv.setText(mTitle);
        }

        mPauseIv.setVisibility(GONE);
        mForwardIv.setVisibility(GONE);
        mPrevIv.setVisibility(GONE);

        mProgressSb = (MediaSeekBar) v.findViewById(R.id.progress_sb);
        mProgressSb.setMax(1000);

        // 下面是SeekBar的thum drawable适配的代码
        // 有些盒子对于Drawable的计算跟density有关系，导致Seekbar的thumb大小不一样
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.play_seekbar_thum);
        // nodpi下面的尺寸为1080p的尺寸，720p的要重新计算大小
        if (getResources().getDisplayMetrics().widthPixels == 1280) {
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() / 1.5), (int) (bitmap.getHeight() / 1.5), false);
            // 不是精确的，不能用等于来比较
            if (Math.abs(bitmap.getWidth() - getResources().getDrawable(R.drawable.play_seekbar_thum).getIntrinsicWidth() / 1.5) > 5) {
                mProgressSb.setThumb(new BitmapDrawable(bitmap));
            } else {
                mProgressSb.setThumb(new BitmapDrawable(mContext.getResources(), bitmap));
            }
        } else {
            if (bitmap.getWidth() != mProgressSb.getSeekBarThumb().getIntrinsicWidth()) {
                mProgressSb.setThumb(new BitmapDrawable(bitmap));
            }
        }

        mEndTime = (TextView) v.findViewById(R.id.total_time_tv);
        mCurrentTime = (TextView) v.findViewById(R.id.current_time_tv);
        mPredicateTime = (TextView) v.findViewById(R.id.predicate_time_tv);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    public void show() {
        show(sDefaultTimeout, -1);
    }

    public void show(int timeout) {
        show(timeout, -1);
    }

    public void show(int timeout, int pos) {
        setProgress(pos);

        if (!mShowing && mAnchor != null) {
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mAnchor.addView(this, lp);

            mTitleTv.startAnimation(mPushTopInAnim);
            mBottomControllView.startAnimation(mPushBottomInAnim);

            showIndicator();

            mShowing = true;
        } else if (mShowing && mAnchor != null) {
            showIndicator();
        }

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        // mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(PRE_FADE_OUT);

        mHandler.removeMessages(PRE_FADE_OUT);
        mHandler.removeMessages(FADE_OUT);
        if (timeout != 0) {
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return mShowing;
    }

    public void hide() {
        if (mAnchor == null)
            return;

        if (mShowing) {
            try {
                if (mPredicateTime != null) {
                    mPredicateTime.setVisibility(View.GONE);
                }

                mAnchor.removeView(this);

                mHandler.removeMessages(SHOW_PROGRESS);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            mShowing = false;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long pos;
            long interval;
            switch (msg.what) {
                case PRE_FADE_OUT:
                    mTitleTv.startAnimation(mPushTopOutAnim);
                    mBottomControllView.startAnimation(mPushBottomOutAnim);
                    sendMessageDelayed(Message.obtain(mHandler, FADE_OUT), mPushTopOutAnim.getDuration());
                    break;
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    pos = setProgress(-1);
                    if (mShowing && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case MSG_QUICK_FORWARD:
                    interval = getFastRealInterval(mFastCount);
                    // long防止溢出
                    pos = (long) mProgressSb.getProgress() * mPlayer.getDuration() / mProgressSb.getMax() + interval;
                    AppConfig.LOGD("[[MediaController]] msg_quick_forward pos=" + pos + ", currentPos=" + mPlayer.getCurrentPosition() + ", interval=" + interval);
                    show(sDefaultTimeout, (int) pos);
                    mFastCount++;
                    sendEmptyMessageDelayed(MSG_QUICK_FORWARD, FAST_MSG_DELAY);
                    break;
                case MSG_QUICK_PREV:
                    interval = getFastRealInterval(mFastCount);
                    pos = (long) mProgressSb.getProgress() * mPlayer.getDuration() / mProgressSb.getMax() - interval;
                    if (pos < 0) {
                        pos = 0;
                    }
                    AppConfig.LOGD("[[MediaController]] msg_quick_prev pos=" + pos + ", currentPos=" + mPlayer.getCurrentPosition() + ", interval=" + interval);
                    show(sDefaultTimeout, (int) pos);
                    mFastCount++;
                    sendEmptyMessageDelayed(MSG_QUICK_PREV, FAST_MSG_DELAY);
                    break;
                case MSG_FINISH_QUICK_FORWARD:
                    removeMessages(MSG_QUICK_FORWARD);
                    mIsInFastMode = false;
                    mFastCount = 0;

                    interval = (long) mProgressSb.getProgress() * mPlayer.getDuration() / mProgressSb.getMax() - mPlayer.getCurrentPosition();
                    doForward((int) interval);
                    break;
                case MSG_FINISH_QUICK_PREV:
                    removeMessages(MSG_QUICK_PREV);
                    mIsInFastMode = false;
                    mFastCount = 0;

                    interval = mPlayer.getCurrentPosition() - (long) mProgressSb.getProgress() * mPlayer.getDuration() / mProgressSb.getMax();
                    doPrev((int) interval);
                    break;
                case MSG_SHOW_PAUSE_AD:
                    if (mOnPauseListener != null) {
                        mOnPauseListener.onPause(true);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @TargetApi(16)
    private int setProgress(int position) {
        if (mPlayer == null) {
            return 0;
        }
        if (position < 0) {
            position = mPlayer.getCurrentPosition();
        }
        int duration = mPlayer.getDuration();
        if (mProgressSb != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = (long) mProgressSb.getMax() * position / duration;
                mProgressSb.setProgress((int) pos);
                AppConfig.LOGD("[[MediaController]] setProgress position=" + position + ", finalPos=" + pos);
            }
        }

        if (mEndTime != null) {
            mEndTime.setText(stringForTime(duration));
        }
        if (mCurrentTime != null && !mIsInFastMode) {
            mCurrentTime.setText(stringForTime(position));
        }
        if (mPredicateTime != null && (mCurrentOperation == Operation.FORWARD || mCurrentOperation == Operation.PREV || mCurrentOperation == Operation.PAUSE)) {
            final int p = position;
            mPredicateTime.post(new Runnable() {
                @Override
                public void run() {
                    Rect rect = mProgressSb.getSeekBarThumb().getBounds();
                    mPredicateTime.setText(stringForTime((int) p));
                    mPredicateTime.setX(mProgressSb.getLeft() + rect.left - (mPredicateTime.getWidth() - rect.width()) / 2);
                    mPredicateTime.setVisibility(View.VISIBLE);
                }
            });
        }

        return position;
    }

    public void doPauseResume() {
        mCurrentOperation = Operation.PAUSE;

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            mPaused = true;

            if (AppConfig.ADVERTISE_ON && AdUtil.getInstance(mContext).hasAdImage()) {
                show(PAUSE_AD_INTERVAL);
                if (mOnPauseListener != null) {
                    // 先预加载图片，提高效率
                    mOnPauseListener.onLoadAdImage();
                }
                mHandler.sendEmptyMessageDelayed(MSG_SHOW_PAUSE_AD, PAUSE_AD_INTERVAL);
            } else {
                show(0);
            }
        } else {
            if (mOnPauseListener != null) {
                mOnPauseListener.onPause(false);
            }
            mHandler.removeMessages(MSG_SHOW_PAUSE_AD);

            mPlayer.start();
            mPaused = false;

            Message.obtain(mHandler, PRE_FADE_OUT).sendToTarget();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mProgressSb != null) {
            mProgressSb.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(android.widget.MediaController.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.MediaController.class.getName());
    }

    public void cancelFastForwardOrBackward(boolean isForward) {
        mHandler.sendEmptyMessage(isForward ? MSG_FINISH_QUICK_FORWARD : MSG_FINISH_QUICK_PREV);
    }

    /**
     * 快进快退，当松开的时候才真正seekTo，不松开的时候只更新SeekBar
     */
    public void doFastForward() {
        if (!mPlayer.canSeekForward()) {
            return;
        }

        if (!mIsInFastMode) {
            mPlayer.pause();
            mPaused = true;
            mHandler.sendEmptyMessage(MSG_QUICK_FORWARD);
            mIsInFastMode = true;
        }
    }

    public void doFastBackward() {
        if (!mPlayer.canSeekForward()) {
            return;
        }

        if (!mIsInFastMode) {
            mPlayer.pause();
            mPaused = true;
            mHandler.sendEmptyMessage(MSG_QUICK_PREV);
            mIsInFastMode = true;
        }
    }

    public void doForward(int interval) {
        doForward(interval, true);
    }

    public void doForward(int interval, boolean autoStart) {
        if (!mPlayer.canSeekForward()) {
            return;
        }

        // 不显示广告ImageView
        if (mOnPauseListener != null) {
            mOnPauseListener.onPause(false);
        }
        mHandler.removeMessages(MSG_SHOW_PAUSE_AD);

        if (interval < 0) {
            interval = getSingleForwardInteval();
        }

        int pos = mPlayer.getCurrentPosition();
        pos += interval;
        mPlayer.seekTo(pos);
        AppConfig.LOGD("[[MediaController]] doForward interval=" + interval + ", pos=" + pos + ", currentPos=" + mPlayer.getCurrentPosition());
        if (mPaused && autoStart) {
            mPlayer.start();
            mPaused = false;
        }

        mCurrentOperation = Operation.FORWARD;

        show(sDefaultTimeout);
    }

    public void doPrev(int interval) {
        doPrev(interval, true);
    }

    public void doPrev(int interval, boolean autoStart) {
        if (!mPlayer.canSeekBackward()) {
            return;
        }

        // 不显示广告ImageView
        if (mOnPauseListener != null) {
            mOnPauseListener.onPause(false);
        }
        mHandler.removeMessages(MSG_SHOW_PAUSE_AD);

        if (interval < 0) {
            interval = getSingleForwardInteval();
        }

        int pos = mPlayer.getCurrentPosition();
        pos -= interval;
        mPlayer.seekTo(pos);
        if (mPaused && autoStart) {
            mPlayer.start();
            mPaused = false;
        }

        mCurrentOperation = Operation.PREV;

        show(sDefaultTimeout);
    }

    public void doRestart() {
        if (!mCanRestart) {
            return;
        }

        mPlayer.seekTo(0);

        if (!mPlayer.isPlaying()) {
            mPlayer.start();
        }
        setProgress(-1);

        Message.obtain(mHandler, PRE_FADE_OUT).sendToTarget();
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public boolean canRestart() {
        return mCanRestart;
    }

    /**
     * 可以用上键从头开始播放
     *
     * @param canRestart
     */
    public void setCanRestart(boolean canRestart) {
        mCanRestart = canRestart;
    }

    // 根据操作显示相应的图标
    private void showIndicator() {
        if (mCurrentOperation == Operation.PREV) {
            mForwardIv.setVisibility(GONE);
            mPauseIv.setVisibility(GONE);

            if (mPrevIv.getVisibility() == GONE) {
                mPrevIv.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in));
                mPrevIv.setVisibility(VISIBLE);
            }
        } else if (mCurrentOperation == Operation.FORWARD) {
            mPrevIv.setVisibility(GONE);
            mPauseIv.setVisibility(GONE);

            if (mForwardIv.getVisibility() == GONE) {
                mForwardIv.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in));
                mForwardIv.setVisibility(VISIBLE);
            }
        } else {
            mPrevIv.setVisibility(GONE);
            mForwardIv.setVisibility(GONE);

            if (mPauseIv.getVisibility() == GONE) {
                mPauseIv.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in));
                mPauseIv.setVisibility(VISIBLE);
            }
        }
    }

    private int getFastRealInterval(int mFastCount) {
        int base = mFastCount / FAST_BASE_COUNT;
        int duration = mPlayer.getDuration();
        int divider = FAST_BASE_TIME - base * 10;
        return duration / (divider < FAST_BASE_TIME_LIMIT ? FAST_BASE_TIME_LIMIT : divider);
    }

    private int getSingleForwardInteval() {
        int singleInterval = mPlayer.getDuration() / SINGLE_FORWARD_INTERVAL_BASE;
        return singleInterval < SINGLE_INTERVAL_LIMIT ? SINGLE_INTERVAL_LIMIT : singleInterval;
    }

    public interface MediaPlayerControl {
        void start();

        void pause();

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos);

        boolean isPlaying();

        int getBufferPercentage();

        boolean canPause();

        boolean canSeekBackward();

        boolean canSeekForward();

        /**
         * Get the audio session id for the player used by this VideoView. This can be used to
         * apply audio effects to the audio track of a video.
         *
         * @return The audio session, or 0 if there was an error.
         */
        int getAudioSessionId();
    }

    public interface OnPauseListener {
        public void onPause(boolean isPause);

        public void onLoadAdImage();
    }
}
