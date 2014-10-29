package com.plugin.common.view;

import android.net.Uri;

/**
 * Created with IntelliJ IDEA.
 * User: michael
 * Date: 13-8-10
 * Time: PM5:05
 * To change this template use File | Settings | File Templates.
 */
public interface WebImageViewStatusInterface {

    boolean imageShowSuccess(Uri uri);

    Uri getCurrentShouldShowImageUri();
}
