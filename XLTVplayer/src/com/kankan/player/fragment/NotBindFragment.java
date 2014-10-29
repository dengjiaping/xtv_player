package com.kankan.player.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.kankan.player.activity.RemoteBindTdActivity;
import com.kankan.player.app.Constants;
import com.xunlei.tv.player.R;

/**
 * Created by wangyong on 14-5-20.
 */
public class NotBindFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_nobind, null);
        TextView secondTv = (TextView) view.findViewById(R.id.tv_2);

        if(this.isAdded()){
            if(getActivity() instanceof RemoteBindTdActivity){
                RemoteBindTdActivity activity = (RemoteBindTdActivity) getActivity();
                if(activity != null){
                    if(activity.getRmoteDeviceType() != Constants.KEY_REMOTE_ROUTER){
                        secondTv.setVisibility(View.GONE);
                    }
                }
            }
        }


        return view;
    }
}
