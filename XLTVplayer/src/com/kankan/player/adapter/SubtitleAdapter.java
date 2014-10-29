package com.kankan.player.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.kankan.player.dao.model.Subtitle;
import com.kankan.player.model.GetSubtitleModel;
import com.kankan.player.subtitle.SubtitleType;
import com.plugin.common.utils.SingleInstanceBase;
import com.xunlei.tv.player.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangyong on 14-3-31.
 */
public class SubtitleAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private List<Subtitle> mlist = new ArrayList<Subtitle>();
    private GetSubtitleModel mGetSubtitleModel;

    public SubtitleAdapter(Context context) {
        mContext = context.getApplicationContext();

        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mGetSubtitleModel = SingleInstanceBase.getInstance(GetSubtitleModel.class);
    }

    public void setData(List<Subtitle> list) {
        if (list != null) {
            this.mlist.clear();
            this.mlist.addAll(list);
            this.notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mlist.size();
    }

    @Override
    public Object getItem(int i) {
        return mlist.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_subtitle_list, null);

            viewHolder = new ViewHolder();
            viewHolder.iconIv = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.nameTv = (TextView) convertView.findViewById(R.id.title);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Subtitle subtitle = this.mlist.get(position);
        String name = subtitle.getName();
        if (subtitle.getType() != SubtitleType.ONLINE.ordinal() && !TextUtils.isEmpty(name)) {
            viewHolder.nameTv.setText(name);
        } else if (subtitle.getType() == SubtitleType.ONLINE.ordinal()) {
            viewHolder.nameTv.setText(mGetSubtitleModel.getLanguageDescription(subtitle.getLanguage()));
        }

        Boolean selected = subtitle.getSelected();
        if (selected != null && selected.booleanValue()) {
            viewHolder.iconIv.setVisibility(View.VISIBLE);
        } else {
            viewHolder.iconIv.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }


    class ViewHolder {
        ImageView iconIv;
        TextView nameTv;
    }

    private String formatSubtitleName(String name) {
        if (!TextUtils.isEmpty(name)) {
            char character;
            int index = 0;
            for (int i = name.length() - 1; i >= 0; i--) {
                character = name.charAt(i);
                if (character == '/') {
                    index = i;
                }
            }
            return name.substring(index);
        }
        return null;
    }

}
