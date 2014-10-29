package com.plugin.common.view;

import android.widget.ImageView;

public interface WebImageViewStatusListener {

    void onPreLoadImage(ImageView view, String imageUrl);

    void onLoadImageSuccess(ImageView view, String imageUrl);

    void onLoadImageFailed(ImageView view, String imageUrl);
}
