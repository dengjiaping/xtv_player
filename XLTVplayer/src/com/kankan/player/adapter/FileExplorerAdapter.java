package com.kankan.player.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.xunlei.tv.player.R;
import com.kankan.player.explorer.FileCategory;
import com.kankan.player.explorer.FileIconHelper;
import com.kankan.player.explorer.FileItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangdi on 14-3-27.
 */
public class FileExplorerAdapter extends BaseAdapter {

    public Context mContext;

    public List<FileItem> mFileItemList;

    private FileIconHelper mFileIconHelper;

    private int mSelectedId;

    public FileExplorerAdapter(Context context, List<FileItem> fileItemList, FileIconHelper fileIconHelper) {
        this.mContext = context;
        this.mFileItemList = fileItemList;
        this.mFileIconHelper = fileIconHelper;
    }

    public void setData(List<FileItem> list){
        mFileItemList = list;
        notifyDataSetChanged();
    }

    public int getSelectedId() {
        return mSelectedId;
    }

    @Override
    public int getCount() {
        return mFileItemList != null ? mFileItemList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        if(mFileItemList != null && mFileItemList.size() >0){
            return mFileItemList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setSelectedId(int position) {
        mSelectedId = position;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_file_list, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.topLine.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

        FileItem fileItem = mFileItemList.get(position);
        if (fileItem.category == FileCategory.DIR) {
            holder.nextIv.setVisibility(View.VISIBLE);
        } else if (fileItem.category == FileCategory.VIDEO) {
            holder.nextIv.setVisibility(View.INVISIBLE);
        } else if (fileItem.category == FileCategory.APK) {
            holder.nextIv.setVisibility(View.INVISIBLE);
        }

        mFileIconHelper.setIcon(holder.iconIv, fileItem);

        holder.nameTv.setText(fileItem.fileName);
        holder.newIv.setVisibility(fileItem.isNew ? View.VISIBLE : View.INVISIBLE);

        if (mSelectedId == position) {
            holder.nameTv.setSelected(true);
        }

        return convertView;
    }

    private class ViewHolder {
        public ImageView newIv;
        public ImageView iconIv;
        public TextView nameTv;
        public ImageView nextIv;
        public View topLine;
        public View bottomLine;

        public ViewHolder(View convertView) {
            iconIv = (ImageView) convertView.findViewById(R.id.icon);
            nameTv = (TextView) convertView.findViewById(R.id.name);
            nextIv = (ImageView) convertView.findViewById(R.id.next);
            newIv = (ImageView) convertView.findViewById(R.id.new_iv);
            topLine = convertView.findViewById(R.id.top_line);
            bottomLine = convertView.findViewById(R.id.bottom_line);
        }
    }
}
