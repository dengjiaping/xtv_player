package com.kankan.player.fragment;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.kankan.player.api.tddownload.SysInfo;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.dao.model.video;
import com.kankan.player.event.QrcodeEvent;
import com.kankan.player.item.TdGuideItem;
import com.kankan.player.manager.LocalTDDownloadManager;
import com.plugin.common.utils.CustomThreadPool;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangyong on 14-5-6.
 */
public class TdGuideFragment extends Fragment {


    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private static final int COUNT_WEB_BIND_VIEWS = 3;
    private static final int COUNT_MOBILE_BIND_VIEWS = 4;
    private static final int COUNT_WEB_DOWNLOAD_VIEWS = 4;

    private LinearLayout mContainerView;
    private LinearLayout mTitleView;
    private HorizontalScrollView scrollview;

    private List<TdGuideItem> mList;

    private TdGuideItem.GuideItemType mType;

    private LayoutInflater mInfalter;

    private String mCode;

    private ImageView mDownYunboIv;

    private ImageView mCodeIv;

    private TextView mTitleTv;

    private int mRemoteDeviceType;

    private LocalTDDownloadManager mTdDownloadMgr;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_bind_td, null);

        this.mInfalter = (LayoutInflater) getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.mContainerView = (LinearLayout) view.findViewById(R.id.container_ll);
        this.mContainerView.requestFocus();
        this.mTitleView = (LinearLayout) view.findViewById(R.id.ll);
        this.mTitleTv = (TextView) view.findViewById(R.id.tv);
        this.mTitleView.setVisibility(View.GONE);

        scrollview = (HorizontalScrollView) view.findViewById(R.id.lv);
        scrollview.requestFocus();

        initData();

        return view;
    }

    private void initData() {

        Bundle bundle = getArguments();
        if(bundle != null){
            mRemoteDeviceType = bundle.getInt(Constants.KEY_REMOTE_TYPE, -1);
        }
        int index = bundle.getInt(Constants.KEY_GUIDE_TYPE, -1);
        if (index != -1) {
            TdGuideItem.GuideItemType[] types = TdGuideItem.GuideItemType.values();
            mType = types[index];
        }



        initWebGuideListData(mType);
        initItemView();

        EventBus.getDefault().register(this);

        this.mTdDownloadMgr = LocalTDDownloadManager.getInstance();


        updateCode();

        if (mType == TdGuideItem.GuideItemType.MOBILE_DOWN) {
            this.mTitleTv.setText(getString(R.string.remote_down_method_mobile));
//            this.mTitleView.setVisibility(View.VISIBLE);
        }

        if (mType == TdGuideItem.GuideItemType.WEB_DOWN) {
            this.mTitleTv.setText(getString(R.string.remote_down_method_web));
//            this.mTitleView.setVisibility(View.VISIBLE);
        }

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) scrollview.getLayoutParams();
        if (mType == TdGuideItem.GuideItemType.WEB_BIND) {
            lp.topMargin = getResources().getDimensionPixelSize(R.dimen.remote_scrollview_topmargin_1);
        }

        if (mType == TdGuideItem.GuideItemType.MOBILE_BIND) {
            lp.topMargin = getResources().getDimensionPixelSize(R.dimen.remote_scrollview_topmargin_2);
        }

        if (mType == TdGuideItem.GuideItemType.WEB_DOWN || mType == TdGuideItem.GuideItemType.MOBILE_DOWN) {
            lp.topMargin = getResources().getDimensionPixelSize(R.dimen.remote_scrollview_topmargin_3);

        }

        scrollview.setLayoutParams(lp);

    }

    private void initWebGuideListData(TdGuideItem.GuideItemType type) {

        mList = new ArrayList<TdGuideItem>();

        if (type == TdGuideItem.GuideItemType.WEB_BIND) {
            TdGuideItem first = new TdGuideItem(-1, TdGuideItem.GuideItemType.WEB_BIND);
            first.setFirstLine(getString(R.string.remote_web_bind_step_one_first));
//            first.setSecondLine(getString(R.string.remote_web_bind_step_one_second));
            first.setHasNext(true);

            TdGuideItem second = new TdGuideItem(-1, TdGuideItem.GuideItemType.WEB_BIND);
            second.setFirstLine(getString(R.string.remote_web_bind_step_two_first));
            second.setSecondLine(getString(R.string.remote_web_bind_step_two_second));
            second.setHasNext(true);


            TdGuideItem third = new TdGuideItem(-1, TdGuideItem.GuideItemType.WEB_BIND);
            third.setFirstLine(getString(R.string.remote_web_bind_step_three_first));
            third.setSecondLine(getString(R.string.remote_web_bind_step_three_second));
            third.setHasNext(false);

            mList.add(first);
            mList.add(second);
            mList.add(third);
            return;
        }

        if (type == TdGuideItem.GuideItemType.MOBILE_BIND) {

            if(AppConfig.ADVERTISE_ON){
                TdGuideItem first = new TdGuideItem(R.drawable.remote_yunbo, TdGuideItem.GuideItemType.MOBILE_BIND);
                first.setFirstLine(getString(R.string.remote_mobile_bind_step_one_first));
                first.setSecondLine(getString(R.string.remote_mobile_bind_step_one_second));
                first.setHasNext(true);

                TdGuideItem second = new TdGuideItem(R.drawable.td_item_mobilebind_2, TdGuideItem.GuideItemType.MOBILE_BIND);
                second.setFirstLine(getString(R.string.remote_mobile_bind_step_two_first));
                second.setHasNext(true);


                TdGuideItem third = new TdGuideItem(R.drawable.td_item_mobilebind_3, TdGuideItem.GuideItemType.MOBILE_BIND);
                third.setFirstLine(getString(R.string.remote_mobile_bind_step_three_first));
                third.setHasNext(true);

                TdGuideItem fourth = new TdGuideItem(-1, TdGuideItem.GuideItemType.MOBILE_BIND);
                fourth.setFirstLine(getString(R.string.remote_mobile_bind_step_four_first));
                fourth.setSecondLine(getString(R.string.remote_mobile_bind_step_four_second));
                fourth.setHasNext(false);

                mList.add(first);
                mList.add(second);
                mList.add(third);
                mList.add(fourth);

                return;
            }else{
                TdGuideItem first = new TdGuideItem(R.drawable.remote_yunbo_pre, TdGuideItem.GuideItemType.MOBILE_BIND);
                first.setFirstLine(getString(R.string.remote_mobile_bind_step_one_first_pre));
                first.setSecondLine(getString(R.string.remote_mobile_bind_step_one_second_pre));
                first.setHasNext(true);

                TdGuideItem second = new TdGuideItem(R.drawable.td_item_mobilebind_2_pre, TdGuideItem.GuideItemType.MOBILE_BIND);
                second.setFirstLine(getString(R.string.remote_mobile_bind_step_two_first_pre));
                second.setHasNext(true);

                TdGuideItem third = new TdGuideItem(R.drawable.td_item_mobilebind_3_pre, TdGuideItem.GuideItemType.MOBILE_BIND);
                third.setFirstLine(getString(R.string.remote_mobile_bind_step_three_first_pre));
                third.setHasNext(true);

                TdGuideItem fourth = new TdGuideItem(-1, TdGuideItem.GuideItemType.MOBILE_BIND);
                fourth.setFirstLine(getString(R.string.remote_mobile_bind_step_four_first_pre));
                fourth.setSecondLine(getString(R.string.remote_mobile_bind_step_four_second_pre));
                fourth.setHasNext(false);

                mList.add(first);
                mList.add(second);
                mList.add(third);
                mList.add(fourth);

                return;
            }


        }

        if(mRemoteDeviceType != -1){

            if(mRemoteDeviceType == Constants.KEY_REMOTE_LOCAL){
                if (type == TdGuideItem.GuideItemType.WEB_DOWN) {

                    TdGuideItem first = new TdGuideItem(R.drawable.item_td_downguide_1, TdGuideItem.GuideItemType.WEB_DOWN);
                    first.setFirstLine(getString(R.string.remote_web_down_step_one_first));
//            first.setSecondLine(getString(R.string.remote_web_down_step_one_second));
                    first.setHasNext(true);

                    TdGuideItem second = new TdGuideItem(R.drawable.item_td_downguide_2, TdGuideItem.GuideItemType.WEB_DOWN);
                    second.setFirstLine(getString(R.string.remote_web_down_step_two_first));
                    second.setSecondLine(getString(R.string.remote_web_down_step_two_second));
                    second.setHasNext(true);


                    TdGuideItem third = new TdGuideItem(R.drawable.item_td_downguide_3, TdGuideItem.GuideItemType.WEB_DOWN);
                    third.setFirstLine(getString(R.string.remote_web_down_step_three_first));
                    third.setSecondLine(getString(R.string.remote_web_down_step_three_second));
                    third.setHasNext(false);

                    mList.add(first);
                    mList.add(second);
                    mList.add(third);
                    return;
                }

                if (type == TdGuideItem.GuideItemType.MOBILE_DOWN) {

                    if(AppConfig.ADVERTISE_ON){

                        TdGuideItem first = new TdGuideItem(R.drawable.remote_yunbo, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        first.setFirstLine(getString(R.string.remote_mobile_down_step_one_first));
                        first.setSecondLine(getString(R.string.remote_mobile_down_step_one_second));
                        first.setHasNext(true);

                        TdGuideItem second = new TdGuideItem(R.drawable.td_item_downguide_mobile_2, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        second.setFirstLine(getString(R.string.remote_mobile_down_step_two_first));
                        second.setHasNext(true);


                        TdGuideItem third = new TdGuideItem(R.drawable.td_item_downguide_mobile3, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        third.setFirstLine(getString(R.string.remote_mobile_down_step_three_first));
                        third.setHasNext(true);

                        TdGuideItem fourth = new TdGuideItem(R.drawable.td_item_downguide_mobile_4, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        fourth.setFirstLine(getString(R.string.remote_mobile_down_step_four_first));
                        fourth.setSecondLine(getString(R.string.remote_mobile_down_step_four_second));
                        fourth.setHasNext(true);

                        TdGuideItem five = new TdGuideItem(R.drawable.td_item_downguide_mobile_5, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        five.setFirstLine(getString(R.string.remote_mobile_down_step_five_first));
                        five.setSecondLine(getString(R.string.remote_mobile_down_step_five_second));
                        five.setHasNext(false);

                        mList.add(first);
                        mList.add(second);
                        mList.add(third);
                        mList.add(fourth);
                        mList.add(five);
                        return;
                    }else{
                        TdGuideItem first = new TdGuideItem(R.drawable.remote_yunbo_pre, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        first.setFirstLine(getString(R.string.remote_mobile_down_step_one_first_pre));
                        first.setSecondLine(getString(R.string.remote_mobile_down_step_one_second_pre));
                        first.setHasNext(true);

                        TdGuideItem second = new TdGuideItem(R.drawable.td_item_downguide_mobile_2_pre, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        second.setFirstLine(getString(R.string.remote_mobile_down_step_two_first_pre));
                        second.setHasNext(true);


                        TdGuideItem third = new TdGuideItem(R.drawable.td_item_downguide_mobile3_pre, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        third.setFirstLine(getString(R.string.remote_mobile_down_step_three_first_pre));
                        third.setHasNext(true);

                        TdGuideItem fourth = new TdGuideItem(R.drawable.td_item_downguide_mobile_4_pre, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        fourth.setFirstLine(getString(R.string.remote_mobile_down_step_four_first_pre));
                        fourth.setSecondLine(getString(R.string.remote_mobile_down_step_four_second_pre));
                        fourth.setHasNext(false);


                        mList.add(first);
                        mList.add(second);
                        mList.add(third);
                        mList.add(fourth);
                    }


                }
            }

            if(mRemoteDeviceType == Constants.KEY_REMOTE_ROUTER){
                if (type == TdGuideItem.GuideItemType.WEB_DOWN) {

                    TdGuideItem first = new TdGuideItem(R.drawable.remote_router_web_guide1, TdGuideItem.GuideItemType.WEB_DOWN);
                    first.setFirstLine(getString(R.string.remote_router_web_down_step_one_first));
                    first.setSecondLine(getString(R.string.remote_router_web_down_step_one_second));
                    first.setHasNext(true);

                    TdGuideItem second = new TdGuideItem(R.drawable.remote_router_web_guide2, TdGuideItem.GuideItemType.WEB_DOWN);
                    second.setFirstLine(getString(R.string.remote_router_web_down_step_two_first));
                    second.setSecondLine(getString(R.string.remote_router_web_down_step_two_second));
                    second.setHasNext(true);


                    TdGuideItem third = new TdGuideItem(R.drawable.remote_router_web_guide3, TdGuideItem.GuideItemType.WEB_DOWN);
                    third.setFirstLine(getString(R.string.remote_router_web_down_step_three_first));
                    third.setSecondLine(getString(R.string.remote_router_web_down_step_three_second));
                    third.setHasNext(true);

                    TdGuideItem fourth = new TdGuideItem(R.drawable.item_td_downguide_3, TdGuideItem.GuideItemType.WEB_DOWN);
                    fourth.setFirstLine(getString(R.string.remote_router_web_down_step_fourth_first));
//                    third.setSecondLine(getString(R.string.remote_router_web_down_step_three_second));
                    fourth.setHasNext(false);

                    mList.add(first);
                    mList.add(second);
                    mList.add(third);
                    mList.add(fourth);
                    return;
                }

                if (type == TdGuideItem.GuideItemType.MOBILE_DOWN) {

                        TdGuideItem first = new TdGuideItem(R.drawable.remote_router_mobile_guide1, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        first.setFirstLine(getString(R.string.remote_router_mobile_down_step_one_first));
                        first.setSecondLine(getString(R.string.remote_router_mobile_down_step_one_second));
                        first.setHasNext(true);

                        TdGuideItem second = new TdGuideItem(R.drawable.remote_router_mobile_guide2, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        second.setFirstLine(getString(R.string.remote_router_mobile_down_step_two_first));
                        second.setHasNext(true);


                        TdGuideItem third = new TdGuideItem(R.drawable.remote_router_mobile_guide3, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        third.setFirstLine(getString(R.string.remote_router_mobile_down_step_three_first));
                        third.setHasNext(true);

                        TdGuideItem fourth = new TdGuideItem(R.drawable.remote_router_mobile_guide4, TdGuideItem.GuideItemType.MOBILE_DOWN);
                        fourth.setFirstLine(getString(R.string.remote_router_mobile_down_step_four_first));
                        fourth.setSecondLine(getString(R.string.remote_router_mobile_down_step_four_second));
                        fourth.setHasNext(false);

                        mList.add(first);
                        mList.add(second);
                        mList.add(third);
                        mList.add(fourth);
                        return;



                }
            }


        }



    }

    private void initItemView() {
        if (mList != null) {

            mContainerView.removeAllViews();
            for (int i = 0; i < mList.size(); i++) {

                TdGuideItem item = mList.get(i);

                View convertView = this.mInfalter.inflate(R.layout.item_guide_list, null);

                View containerView = convertView.findViewById(R.id.container_ll);
                View innerView = convertView.findViewById(R.id.inner_rl);
                LinearLayout upperll = (LinearLayout) convertView.findViewById(R.id.upper_tv_ll);
                TextView upperfirstTv = (TextView) convertView.findViewById(R.id.upper_tv_firstline);
                TextView upperSecondTv = (TextView) convertView.findViewById(R.id.upper_tv_secondline);

                ImageView imageView = (ImageView) convertView.findViewById(R.id.iv);
                ImageView imageNext = (ImageView) convertView.findViewById(R.id.iv_next);

                LinearLayout insidell = (LinearLayout) convertView.findViewById(R.id.tv_ll);
                TextView insidefirstTv = (TextView) convertView.findViewById(R.id.tv_firstline);
                TextView insideSecondTv = (TextView) convertView.findViewById(R.id.tv_secondline);

                ImageView shadowIv = (ImageView) convertView.findViewById(R.id.shadow_bg_iv);

                if (item.getResourceId() != -1) {
                    imageView.setImageResource(item.getResourceId());
                }


                if (item.isHasNext()) {
                    imageNext.setVisibility(View.VISIBLE);
                } else {
                    imageNext.setVisibility(View.INVISIBLE);
                }


                if (item.getType() == TdGuideItem.GuideItemType.WEB_BIND) {
                    upperll.setVisibility(View.GONE);
                    insidell.setVisibility(View.VISIBLE);
                    insidefirstTv.setText(item.getFirstLine());
                    insideSecondTv.setText(item.getSecondLine());

                }

                if (item.getType() == TdGuideItem.GuideItemType.MOBILE_BIND) {
                    upperll.setVisibility(View.VISIBLE);
                    insidell.setVisibility(View.INVISIBLE);
                    upperfirstTv.setText(item.getFirstLine());
                    upperSecondTv.setText(item.getSecondLine());

                    if (i == 0) {
                        mDownYunboIv = imageView;
                    }

                    if (i == mList.size() - 1) {
                        mCodeIv = imageView;
                    }

                }

                if (item.getType() == TdGuideItem.GuideItemType.WEB_DOWN) {
                    upperll.setVisibility(View.VISIBLE);
                    insidell.setVisibility(View.INVISIBLE);
                    upperfirstTv.setText(item.getFirstLine());
                    upperSecondTv.setText(item.getSecondLine());

                }

                if (item.getType() == TdGuideItem.GuideItemType.MOBILE_DOWN) {
                    upperll.setVisibility(View.VISIBLE);
                    insidell.setVisibility(View.INVISIBLE);
                    upperfirstTv.setText(item.getFirstLine());
                    upperSecondTv.setText(item.getSecondLine());

                    if (i == 0) {
                        mDownYunboIv = imageView;
                    }

                }

                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) innerView.getLayoutParams();
                RelativeLayout.LayoutParams shadowLp = (RelativeLayout.LayoutParams) shadowIv.getLayoutParams();

                if (item.getType() == TdGuideItem.GuideItemType.WEB_BIND) {
                    lp.width = getResources().getDimensionPixelSize(R.dimen.remote_guide_item_width_1);
                    lp.height = getResources().getDimensionPixelSize(R.dimen.remote_guide_item_height_1);
                    shadowLp.width = lp.width;
                } else {
                    lp.width = getResources().getDimensionPixelSize(R.dimen.remote_guide_item_width_2);
                    lp.height = getResources().getDimensionPixelSize(R.dimen.remote_guide_item_height_2);
                    shadowLp.width = lp.width;

                }

                innerView.setLayoutParams(lp);
                shadowIv.setLayoutParams(shadowLp);

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) containerView.getLayoutParams();

                if(i ==0 ){
                    layoutParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.remote_guide_item_firstmargin);
                    layoutParams.rightMargin = 0;
                }else{
                    layoutParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.remote_item_margin);
                    layoutParams.rightMargin = 0;
                }
                containerView.setLayoutParams(layoutParams);

                mContainerView.addView(convertView);

            }
        }
    }

    public void setCode(String code) {
        if (!TextUtils.isEmpty(this.mCode)) {
            if (!this.mCode.equals(code)) {
                if (mCodeIv != null) {
                    getBitmap(mCode, true);
                }
            }
        }
        this.mCode = code;
    }


    public void getBitmap(final String content, final boolean bind) {
        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = toBitmap(content);

                QrcodeEvent event = new QrcodeEvent();
                if (bind) {
                    event.bindBitmap = bmp;
                } else {
                    event.downAppBitmap = bmp;
                }
                EventBus.getDefault().post(event);

            }
        }));
    }


    private Bitmap toBitmap(String content) {

        if (TextUtils.isEmpty(content)) {
            return null;
        }

        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try {
            BitMatrix lBitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 400, 400, hints);

            int width = lBitMatrix.getWidth();
            int height = lBitMatrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = lBitMatrix.get(x, y) ? BLACK : WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            AppConfig.LOGD("[[TdGuideFragment]] toBitmap " + e.getMessage());
        }


        return null;

    }

    public void onEventMainThread(QrcodeEvent event) {
        if (event != null) {

            Bitmap bindBitmap = event.bindBitmap;
            Bitmap downloadBitmap = event.downAppBitmap;
            if (bindBitmap != null && mCodeIv != null) {
                mCodeIv.setImageBitmap(bindBitmap);
            }else{
                mCodeIv.setImageResource(R.drawable.remote_loading);
            }

            if (downloadBitmap != null && mDownYunboIv != null) {
                mDownYunboIv.setImageBitmap(downloadBitmap);
            }
        }

    }

    private void upateBindCodeTextView() {

//        if (TextUtils.isEmpty(mCode)) {
//            return;
//        }

        if (mType == TdGuideItem.GuideItemType.WEB_BIND) {

            if (mContainerView != null) {

                int count = mContainerView.getChildCount();

                if (count == COUNT_WEB_BIND_VIEWS) {

                    View childView = mContainerView.getChildAt(count - 1);

                    TextView tv = (TextView) childView.findViewById(R.id.tv_firstline);

                    if (tv != null) {
                        SpannableString spannableString = null;
                        if(TextUtils.isEmpty(mCode)){
                            spannableString = new SpannableString(getString(R.string.remote_code_getting));
                        }else{
                            spannableString = new SpannableString(mCode);
                        }
                        spannableString.setSpan(new ForegroundColorSpan(Color.rgb(253, 216, 00)), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        tv.setText(getString(R.string.remote_web_bind_step_three_first));
                        tv.append("\"");
                        tv.append(spannableString);
                        tv.append("\"");
                    }

                    View firstItemView = mContainerView.getChildAt(0);
                    TextView secondLineTv = (TextView) firstItemView.findViewById(R.id.tv_secondline);
                    if (secondLineTv != null) {
                        secondLineTv.setText(getString(R.string.remote_web_bind_step_one_second_1));
                        SpannableString spannableString = new SpannableString(getString(R.string.remote_web_bind_step_one_second_2));
                        spannableString.setSpan(new ForegroundColorSpan(Color.rgb(253, 216, 00)), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        secondLineTv.append("\"");
                        secondLineTv.append(spannableString);
                        secondLineTv.append("\"");
                    }

                }
            }

        }

        if (mType == TdGuideItem.GuideItemType.MOBILE_BIND) {

            int count = mContainerView.getChildCount();

            if (count == COUNT_MOBILE_BIND_VIEWS) {

                View childView = mContainerView.getChildAt(count - 1);

                TextView tv = (TextView) childView.findViewById(R.id.upper_tv_secondline);

                if (tv != null) {
                    SpannableString spannableString = null;
                    if(TextUtils.isEmpty(mCode)){
                        spannableString = new SpannableString(getString(R.string.remote_code_getting));
                    }else{
                        spannableString = new SpannableString(mCode);
                    }
                    spannableString.setSpan(new ForegroundColorSpan(Color.rgb(253, 216, 00)), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    tv.setText(getString(R.string.remote_mobile_bind_step_four_second));
                    tv.append("\"");
                    tv.append(spannableString);
                    tv.append("\"");
                }
            }
        }

        if (mType == TdGuideItem.GuideItemType.WEB_DOWN) {

            int count = mContainerView.getChildCount();

            if (count == COUNT_WEB_DOWNLOAD_VIEWS) {

                View childView = mContainerView.getChildAt(0);

                TextView tv = (TextView) childView.findViewById(R.id.upper_tv_secondline);

                if (tv != null) {
                    tv.setText(getString(R.string.remote_web_down_step_one_second_1));
                    SpannableString spannableString = new SpannableString(getString(R.string.remote_web_down_step_one_second_2));
                    spannableString.setSpan(new ForegroundColorSpan(Color.rgb(253, 216, 00)), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    tv.append("\"");
                    tv.append(spannableString);
                    tv.append("\"");
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void updateCode(){
        if (mType == TdGuideItem.GuideItemType.WEB_BIND) {
            SysInfo infos = this.mTdDownloadMgr.getSysInfo();
            if (infos != null) {
                mCode = infos.bindAcktiveKey;
            }

            upateBindCodeTextView();
        }

        if (mType == TdGuideItem.GuideItemType.MOBILE_BIND) {

            SysInfo infos = this.mTdDownloadMgr.getSysInfo();
            if (infos != null) {
                mCode = infos.bindAcktiveKey;
            }

            upateBindCodeTextView();

            getBitmap(mCode, true);
        }
    }

}