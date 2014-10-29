package com.kankan.player.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.kankan.player.adapter.SubtitleAdapter;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.app.TVPlayerApplication;
import com.kankan.player.dao.model.Subtitle;
import com.kankan.player.event.UpdateSubtitleEvent;
import com.kankan.player.model.GetSubtitleModel;
import com.kankan.player.subtitle.SubtitleType;
import com.plugin.common.utils.SingleInstanceBase;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

import java.util.List;

public class MenuActivity extends Activity implements View.OnClickListener, View.OnFocusChangeListener {
    // 这个位置计算可能要适配
    public static final int SUBMENU_LEFT_MARGIN = 70;

    LinearLayout mDisplayMenu;
    LinearLayout mAudioMenu;
    LinearLayout mSubtitleMenu;
    LinearLayout mDisplayFullMenu;
    LinearLayout mDisplayAutoMenu;
    LinearLayout mAudioLeftMenu;
    LinearLayout mAudioRightMenu;

    LinearLayout mDisplayMenuContainer;
    LinearLayout mAudioMenuContainer;
    LinearLayout mSubtitleMenuContainer;

    LinearLayout mTitleDisplay;
    LinearLayout mTitleSubtitle;
    LinearLayout mTitleAudio;

    ImageView mDisplayIcon;
    ImageView mAudioIcon;
    ImageView mSubtitleIcon;

    ImageView mSubtitleSelectedArrow;
    ImageView mScreenSelectedArrow;
    ImageView mAudioSelectedArrow;

    ImageView mFullScreenIcon;
    ImageView mAutoSizeIcon;

    ImageView mLeftAudioIcon;
    ImageView mRightAudioIcon;

    TextView mSubtitleTv;
    TextView mAudioTv;
    TextView mDisplayTv;

    ListView mSubtitleList;
    SubtitleAdapter mAdapter;
    List<Subtitle> mSubtitles;

    private GetSubtitleModel mGetSubtitleModel;

    private float mDensity;

    private int mAudioMode;
    private int mDisplayMode;
    private int mSubtitleType;

    private boolean mHasLeftAudio;
    private boolean mHasRightAudio;

    Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_menu);
        TVPlayerApplication.sMenuActivity = this;
        mDensity = getResources().getDisplayMetrics().density;

        getWindow().getAttributes().gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mDisplayMenu = (LinearLayout) findViewById(R.id.display_menu);
        mAudioMenu = (LinearLayout) findViewById(R.id.audio_menu);
        mSubtitleMenu = (LinearLayout) findViewById(R.id.subtitle_menu);
        mDisplayFullMenu = (LinearLayout) findViewById(R.id.display_fullscreen_menu);
        mDisplayAutoMenu = (LinearLayout) findViewById(R.id.display_autoajust_menu);
        mAudioLeftMenu = (LinearLayout) findViewById(R.id.audio_left_menu);
        mAudioRightMenu = (LinearLayout) findViewById(R.id.audio_right_menu);

        mDisplayMenuContainer = (LinearLayout) findViewById(R.id.display_menu_container);
        mAudioMenuContainer = (LinearLayout) findViewById(R.id.audio_menu_container);
        mSubtitleMenuContainer = (LinearLayout) findViewById(R.id.subtitle_menu_container);

        mSubtitleSelectedArrow = (ImageView) findViewById(R.id.arrow_subtitle);
        mScreenSelectedArrow = (ImageView) findViewById(R.id.arrow_display);
        mAudioSelectedArrow = (ImageView) findViewById(R.id.arrow_audio);

        mFullScreenIcon = (ImageView) findViewById(R.id.fullscreen_iv);
        mAutoSizeIcon = (ImageView) findViewById(R.id.autojust_iv);

        mLeftAudioIcon = (ImageView) findViewById(R.id.leftaudio_iv);
        mRightAudioIcon = (ImageView) findViewById(R.id.rightaudio_iv);

        mDisplayIcon = (ImageView) findViewById(R.id.display_icon);
        mSubtitleIcon = (ImageView) findViewById(R.id.subtitle_icon);
        mAudioIcon = (ImageView) findViewById(R.id.audio_icon);

        mTitleAudio = (LinearLayout) findViewById(R.id.audio_title);
        mTitleDisplay = (LinearLayout) findViewById(R.id.display_title);
        mTitleSubtitle = (LinearLayout) findViewById(R.id.subtitle_title);

        mSubtitleTv = (TextView) findViewById(R.id.subtitle_text);
        mAudioTv = (TextView) findViewById(R.id.audio_text);
        mDisplayTv = (TextView) findViewById(R.id.display_text);

        mSubtitleList = (ListView) findViewById(R.id.subtitle_list);
        mAdapter = new SubtitleAdapter(this);
        mSubtitleList.setAdapter(mAdapter);

        mDisplayMenu.setOnClickListener(this);
        mAudioMenu.setOnClickListener(this);
        mSubtitleMenu.setOnClickListener(this);
        mDisplayFullMenu.setOnClickListener(this);
        mDisplayAutoMenu.setOnClickListener(this);
        mAudioLeftMenu.setOnClickListener(this);
        mAudioRightMenu.setOnClickListener(this);

        mDisplayMenu.setOnFocusChangeListener(this);
        mAudioMenu.setOnFocusChangeListener(this);
        mSubtitleMenu.setOnFocusChangeListener(this);

        mSubtitleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppConfig.LOGD("[[MenuActivity]] onItemClick position=" + position);
                Subtitle subtitle = (Subtitle) mAdapter.getItem(position);
                AppConfig.LOGD("[[MenuActivity]] subtitle is null: " + (subtitle == null));
                if (subtitle != null) {
                    mSubtitleType = position;
                    changeSelectedStatus();
                    EventBus.getDefault().post(new UpdateSubtitleEvent(mSubtitleType));
                }
            }
        });

        initData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        TVPlayerApplication.sMenuActivity = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.display_menu:
                openDisplayMenu();
                break;
            case R.id.audio_menu:
                openAudioMenu();
                break;
            case R.id.subtitle_menu:
                openSubtitleMenu();
                break;
            case R.id.display_autoajust_menu:
                changeSelectedStatus();
                sendResult(PlayVideoActivity.MSG_AUTO_SIZE);
                break;
            case R.id.display_fullscreen_menu:
                changeSelectedStatus();
                sendResult(PlayVideoActivity.MSG_FULL_SIZE);
                break;
            case R.id.audio_left_menu:
                changeSelectedStatus();
                sendResult(PlayVideoActivity.MSG_LEFT_AUDIO);
                break;
            case R.id.audio_right_menu:
                changeSelectedStatus();
                sendResult(PlayVideoActivity.MSG_RIGHT_AUDIO);
                break;
            default:
                break;
        }
    }

    private void sendResult(final int what) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.putExtra(Constants.KEY_WHAT, what);
                setResult(RESULT_OK, intent);

                finish();
            }
        }, 500L);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (mAudioMenuContainer.getVisibility() == View.VISIBLE) {
                closeAudioMenu();
            } else if (mDisplayMenuContainer.getVisibility() == View.VISIBLE) {
                closeDisplayMenu();
            } else if (mSubtitleMenuContainer.getVisibility() == View.VISIBLE) {
                closeSubtitleMenu();
            } else {
                finish();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            View view = getCurrentFocus();
            if (view == mDisplayMenu) {
                openDisplayMenu();
            } else if (view == mAudioMenu) {
                openAudioMenu();
            } else if (view == mSubtitleMenu) {
                openSubtitleMenu();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            View currentFocus = getCurrentFocus();
            if (mDisplayMenu.isSelected()) {
                if (currentFocus == mDisplayFullMenu) {
                    return true;
                }
            } else if (mAudioMenu.isSelected()) {
                if (currentFocus == mAudioLeftMenu) {
                    if (mAudioRightMenu.getVisibility() == View.GONE) {
                        return true;
                    }
                } else if (currentFocus == mAudioRightMenu) {
                    return true;
                }
            } else if (mSubtitleMenu.isSelected()) {
                if (mSubtitleList.getChildAt(mSubtitleList.getChildCount() - 1).isSelected()) {
                    return true;
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mAudioMenuContainer.getVisibility() == View.VISIBLE) {
                closeAudioMenu();
                return true;
            } else if (mDisplayMenuContainer.getVisibility() == View.VISIBLE) {
                closeDisplayMenu();
                return true;
            } else if (mSubtitleMenuContainer.getVisibility() == View.VISIBLE) {
                closeSubtitleMenu();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void initData() {
        mGetSubtitleModel = SingleInstanceBase.getInstance(GetSubtitleModel.class);

        Intent intent = getIntent();
        mAudioMode = intent.getIntExtra(Constants.KEY_AUDIO_MODE, Constants.VIDEO_AUDIO_LEFT_MODE);
        mDisplayMode = intent.getIntExtra(Constants.KEY_DISPLAY_MODE, Constants.VIDEO_DISPLAY_AUTO_MODE);
        mSubtitleType = intent.getIntExtra(Constants.KEY_SUBTITLE_TYPE, SubtitleType.NONE.ordinal());
        mHasLeftAudio = intent.getBooleanExtra(Constants.KEY_AUDIO_LEFT, true);
        mHasRightAudio = intent.getBooleanExtra(Constants.KEY_AUDIO_RIGHT, false);
    }

    private void openDisplayMenu() {
        mDisplayMenu.setSelected(true);
        mDisplayMenu.getChildAt(mDisplayMenu.getChildCount() - 1).setVisibility(View.VISIBLE);
        hideAllMenuTitle();
        mAudioMenu.setVisibility(View.GONE);
        mSubtitleMenu.setVisibility(View.GONE);

        mDisplayMenuContainer.setX(mAudioMenu.getX() + (int) (SUBMENU_LEFT_MARGIN * mDensity));
        mDisplayMenuContainer.setY(mAudioMenu.getY());
        mDisplayMenuContainer.setVisibility(View.VISIBLE);
        mSubtitleMenuContainer.setVisibility(View.GONE);
        mAudioMenuContainer.setVisibility(View.GONE);

        if (mDisplayMode == Constants.VIDEO_DISPLAY_AUTO_MODE) {
            mDisplayAutoMenu.getChildAt(0).setVisibility(View.VISIBLE);
            mDisplayAutoMenu.requestFocus();
        } else if (mDisplayMode == Constants.VIDEO_DISPLAY_FULL_MODE) {
            mDisplayFullMenu.getChildAt(0).setVisibility(View.VISIBLE);
            mDisplayFullMenu.requestFocus();
        }
    }

    private void closeDisplayMenu() {
        mDisplayMenu.setSelected(false);
        mDisplayMenu.getChildAt(mDisplayMenu.getChildCount() - 1).setVisibility(View.GONE);
        showAllMenuTitle();
        mAudioMenu.setVisibility(View.VISIBLE);
        mSubtitleMenu.setVisibility(View.VISIBLE);

        mDisplayMenuContainer.setVisibility(View.GONE);
        mDisplayMenu.requestFocus();
    }

    private void openAudioMenu() {
        mAudioMenu.setSelected(true);
        mAudioMenu.getChildAt(mAudioMenu.getChildCount() - 1).setVisibility(View.VISIBLE);
        hideAllMenuTitle();
        mDisplayMenu.setVisibility(View.GONE);
        mSubtitleMenu.setVisibility(View.GONE);

        mAudioMenuContainer.setX(mAudioMenu.getX() + (int) (SUBMENU_LEFT_MARGIN * mDensity));
        mAudioMenuContainer.setY(mAudioMenu.getY());
        mAudioMenuContainer.setVisibility(View.VISIBLE);
        mDisplayMenuContainer.setVisibility(View.GONE);
        mSubtitleMenuContainer.setVisibility(View.GONE);

        mAudioRightMenu.setVisibility(mHasRightAudio ? View.VISIBLE : View.GONE);

        if (mAudioMode == Constants.VIDEO_AUDIO_LEFT_MODE) {
            mAudioLeftMenu.getChildAt(0).setVisibility(View.VISIBLE);
            mAudioLeftMenu.requestFocus();
        } else {
            mAudioRightMenu.getChildAt(0).setVisibility(View.VISIBLE);
            mAudioRightMenu.requestFocus();
        }
    }

    private void closeAudioMenu() {
        mAudioMenu.setSelected(false);
        mAudioMenu.getChildAt(mAudioMenu.getChildCount() - 1).setVisibility(View.GONE);
        showAllMenuTitle();
        mSubtitleMenu.setVisibility(View.VISIBLE);
        mDisplayMenu.setVisibility(View.VISIBLE);

        mAudioMenuContainer.setVisibility(View.GONE);
        mAudioMenu.requestFocus();
    }

    private void openSubtitleMenu() {
        mSubtitleMenu.setSelected(true);
        mSubtitleMenu.getChildAt(mSubtitleMenu.getChildCount() - 1).setVisibility(View.VISIBLE);
        hideAllMenuTitle();
        mAudioMenu.setVisibility(View.GONE);
        mDisplayMenu.setVisibility(View.GONE);

        mSubtitleMenuContainer.setX(mAudioMenu.getX() + (int) (SUBMENU_LEFT_MARGIN * mDensity));
        mSubtitleMenuContainer.setY(mAudioMenu.getY());
        mSubtitleMenuContainer.setVisibility(View.VISIBLE);
        mDisplayMenuContainer.setVisibility(View.GONE);
        mAudioMenuContainer.setVisibility(View.GONE);

        setSubtitles();
    }

    private void closeSubtitleMenu() {
        mSubtitleMenu.setSelected(false);
        mSubtitleMenu.getChildAt(mSubtitleMenu.getChildCount() - 1).setVisibility(View.GONE);
        showAllMenuTitle();
        mAudioMenu.setVisibility(View.VISIBLE);
        mDisplayMenu.setVisibility(View.VISIBLE);

        mSubtitleMenuContainer.setVisibility(View.GONE);
        mSubtitleMenu.requestFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
    }

    private void changeSelectedStatus() {
        View currentFocus = getCurrentFocus();

        if (mDisplayMenu.isSelected()) {
            if (currentFocus == mDisplayAutoMenu) {
                mDisplayAutoMenu.getChildAt(0).setVisibility(View.VISIBLE);
                mDisplayFullMenu.getChildAt(0).setVisibility(View.INVISIBLE);
            } else if (currentFocus == mDisplayFullMenu) {
                mDisplayAutoMenu.getChildAt(0).setVisibility(View.INVISIBLE);
                mDisplayFullMenu.getChildAt(0).setVisibility(View.VISIBLE);
            }
        } else if (mAudioMenu.isSelected()) {
            if (currentFocus == mAudioLeftMenu) {
                mAudioLeftMenu.getChildAt(0).setVisibility(View.VISIBLE);
                mAudioRightMenu.getChildAt(0).setVisibility(View.INVISIBLE);
            } else if (currentFocus == mAudioRightMenu) {
                mAudioLeftMenu.getChildAt(0).setVisibility(View.INVISIBLE);
                mAudioRightMenu.getChildAt(0).setVisibility(View.VISIBLE);
            }
        } else if (mSubtitleMenu.isSelected()) {
            for (int i = 0; i < mSubtitles.size(); i++) {
                mSubtitles.get(i).setSelected(false);
            }

            mSubtitles.get(mSubtitleType).setSelected(true);
            mAdapter.setData(mSubtitles);
        }
    }

    private void hideAllMenuTitle() {
        mTitleAudio.setVisibility(View.GONE);
        mTitleSubtitle.setVisibility(View.GONE);
        mTitleDisplay.setVisibility(View.GONE);
    }

    private void showAllMenuTitle() {
        mTitleAudio.setVisibility(View.VISIBLE);
        mTitleSubtitle.setVisibility(View.VISIBLE);
        mTitleDisplay.setVisibility(View.VISIBLE);

        mDisplayTv.setVisibility(View.VISIBLE);
        mAudioTv.setVisibility(View.VISIBLE);
        mSubtitleTv.setVisibility(View.VISIBLE);
        mScreenSelectedArrow.setImageResource(R.drawable.menu_arrow_bg);
        mAudioSelectedArrow.setImageResource(R.drawable.menu_arrow_bg);
        mSubtitleSelectedArrow.setImageResource(R.drawable.menu_arrow_bg);
    }

    private void setSubtitles() {
        mSubtitles = mGetSubtitleModel.getDisplaySubtitleList();
        if (mSubtitleType < mSubtitles.size()) {
            mSubtitles.get(mSubtitleType).setSelected(true);
        }
        mAdapter.setData(mSubtitles);
        mSubtitleList.requestFocus();
        mSubtitleList.setSelection(mSubtitleType);
    }
}
