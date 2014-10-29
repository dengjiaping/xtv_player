package com.kankan.player.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.kankan.player.api.tddownload.SysInfo;
import com.kankan.player.app.Constants;
import com.kankan.player.event.UnbindResultEvent;
import com.kankan.player.event.VideoHistoryEvent;
import com.kankan.player.manager.LocalTDDownloadManager;
import com.kankan.player.util.UIHelper;
import com.kankan.player.view.ToastHelper;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

/**
 * Created by wangyong on 14-4-17.
 */
public class TDRemoteMenuActivity extends Activity implements View.OnClickListener, View.OnFocusChangeListener {

    LinearLayout mUnbindMenu;
    LinearLayout mGuideMenu;

    private LocalTDDownloadManager mTDownloadMgr;

    private TextView mTextView;

    private ImageView mIconIv;

    private int mRemoteDeviceType;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tdremote_menu);

        initUI();

        initData();
    }

    private void initUI() {

        getWindow().getAttributes().gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mUnbindMenu = (LinearLayout) findViewById(R.id.unbind_menu);
        mGuideMenu = (LinearLayout) findViewById(R.id.guide_menu);

        mTextView = (TextView) findViewById(R.id.title);


        mIconIv = (ImageView) findViewById(R.id.bind_icon_iv);

        if (LocalTDDownloadManager.getInstance().isSupportTD()) {

            SysInfo infos = LocalTDDownloadManager.getInstance().getSysInfo();

            if (infos != null) {

                if (infos.isBindOk == Constants.KEY_REMOTE_BIND_FAILED) {
                    mIconIv.setImageResource(R.drawable.remote_menu_bind_bg);
                    mTextView.setText(getString(R.string.menu_bind));
                }

                if (infos.isBindOk == Constants.KEY_REMOTE_BIND_SUCESS) {
                    mIconIv.setImageResource(R.drawable.remote_menu_unbind_bg);
                    mTextView.setText(getString(R.string.menu_unbind));
                }

            }
        }

        Intent intent = getIntent();
        boolean show = intent.getBooleanExtra(Constants.KEY_SHOW_GUIDE_DOWNLOAD, false);
        boolean showBind = intent.getBooleanExtra(Constants.KEY_SHOW_BIND,false);
        if (show) {
            mGuideMenu.setVisibility(View.VISIBLE);
        } else {
            mGuideMenu.setVisibility(View.GONE);
        }

        mRemoteDeviceType = intent.getIntExtra(Constants.KEY_REMOTE_TYPE, -1);
        if(showBind){
            mUnbindMenu.setVisibility(View.VISIBLE);
        }else{
            mUnbindMenu.setVisibility(View.GONE);
        }

        mUnbindMenu.setOnClickListener(this);
        mGuideMenu.setOnClickListener(this);

    }

    private void initData() {
        mTDownloadMgr = LocalTDDownloadManager.getInstance();
//         mTDownloadMgr.init();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.unbind_menu:
                performOnclick();
                break;
            case R.id.guide_menu:
                showAlertDialog();
                break;
            default:
                break;
        }
    }

    @Override
    public void onFocusChange(View view, boolean b) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            View view = getCurrentFocus();
            if (view == mUnbindMenu) {
                performOnclick();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void sendResult(int what) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY_WHAT, what);
        setResult(RESULT_OK, intent);

        finish();
    }

    public void onEventMainThread(UnbindResultEvent event) {
        if (event != null) {
            if (event.result == 0) {
                sendResult(BindTdActivity.EXTRA_UNBIND_SUCESS);
            }

            if (event.result == Constants.KEY_REMOTE_NETWORK_ERR) {
                ToastHelper.showMessage(getApplicationContext(), R.string.remote_tips_unbind_network_error);
            }
        }
    }

    private void performOnclick() {
        SysInfo infos = LocalTDDownloadManager.getInstance().getSysInfo();
        if (infos != null) {
            if (infos.isBindOk == Constants.KEY_REMOTE_BIND_SUCESS) {
//                mTDownloadMgr.unbindThunderAccount(getApplicationContext());
                showUnbindDialog();
            } else if (infos.isBindOk == Constants.KEY_REMOTE_BIND_FAILED) {
                sendResult(RemoteBindTdActivity.EXTRA_GO_TO_BIND);
            }
        }
    }

    private void turn2GuideDownload(int key) {
        Intent intent = new Intent(TDRemoteMenuActivity.this, GuideDownloadActivity.class);
        intent.putExtra(Constants.KEY_SHOW_GUIDE, key);
        intent.putExtra(Constants.KEY_REMOTE_TYPE,mRemoteDeviceType);
        startActivity(intent);
    }

    private void showAlertDialog() {
        UIHelper.showRemoteAlertDialog(this, R.drawable.remote_dialog_webbind, R.drawable.remote_dialog_mobilebind,
                getString(R.string.remote_dialog_title_webbind), getString(R.string.remote_dialog_title_mobilebind),
                new UIHelper.RemoteOnclickListener() {
                    @Override
                    public void onclick() {
                        turn2GuideDownload(Constants.KEY_SHOW_WEB_GUIDE);
                        TDRemoteMenuActivity.this.finish();

                    }
                }, new UIHelper.RemoteOnclickListener() {
                    @Override
                    public void onclick() {
                        turn2GuideDownload(Constants.KEY_SHOW_MOBILE_GUIDE);
                        TDRemoteMenuActivity.this.finish();
                    }
                }
        );
    }

    private void showUnbindDialog(){
        final Dialog dialog = new Dialog(this, R.style.toast);
        View view = getLayoutInflater().inflate(R.layout.alert_unbind, null);
        view.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTDownloadMgr.unbindThunderAccount(getApplicationContext());
                dialog.dismiss();
            }
        });
        TextView titleTv = (TextView) view.findViewById(R.id.unbind_title_tv);
        SysInfo infos = LocalTDDownloadManager.getInstance().getSysInfo();
        if(infos != null){
            String title = infos.userName;
            if(!TextUtils.isEmpty(title)){
                titleTv.setText(String.format(getString(R.string.remote_alert_unbind_title),title));
            }else{
                titleTv.setText(String.format(getString(R.string.remote_alert_unbind_title),""));
            }
        }
        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);

        WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
        p.width = (int) getResources().getDimension(R.dimen.remote_unbind_alert_width);
        p.height = (int) getResources().getDimension(R.dimen.remote_unbind_alert_height);
        p.gravity = Gravity.CENTER_VERTICAL;
        dialog.getWindow().setAttributes(p);

        dialog.show();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}

