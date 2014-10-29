package com.kankan.player.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.kankan.player.activity.MainActivity;
import com.kankan.player.view.RemoteDialog;
import com.xunlei.tv.player.R;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UIHelper {

    public static int getResourceIdForName(String res) {
        int resId = -1;

        Pattern pattern = Pattern.compile("^([\\w.]+R)\\.(\\w+)\\.(\\w+)$");
        Matcher matcher = pattern.matcher(res);
        if (matcher.find()) {
            String className = String.format(Locale.US, "%s$%s",
                    matcher.group(1), matcher.group(2));
            String fieldName = matcher.group(3);
            try {
                Class<?> klass = Class.forName(className);
                Field field = klass.getField(fieldName);
                resId = field.getInt(klass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return resId;
    }

    public static Bitmap getDrawingCache(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return bitmap;
    }

    public static void showAlertTips(Activity activity, int iconId, int title, int subtitle, boolean shouldFinish) {
        if (activity == null) {
            return;
        }

        Resources resources = activity.getResources();
        showAlertTips(activity, iconId, resources.getString(title), subtitle == -1 ? "" : resources.getString(subtitle), shouldFinish);
    }

    public static void showAlertTips(final Activity activity, int iconId, String title, String subtitle, final boolean shouldFinish) {
        if (activity == null) {
            return;
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        final AlertDialog dialog = new AlertDialog.Builder(activity).create();
        View view = inflater.inflate(R.layout.layout_alert_tips, null);
        dialog.setView(view);

        if (iconId != -1) {
            ((ImageView) view.findViewById(R.id.tips_icon)).setImageResource(iconId);
        }
        if (title != null) {
            ((TextView) view.findViewById(R.id.title)).setText(title);
        }
        if (subtitle != null) {
            //((TextView) view.findViewById(R.id.subtitle)).setText(subtitle);
        }
        view.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shouldFinish) {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.startActivity(intent);
                } else {
                    dialog.dismiss();
                }
            }
        });
        dialog.getWindow().setLayout(1280, 900);
        dialog.show();
    }

    public static boolean isRomoteAlertTipsShow = false;

    public static void showRemoteAlertTips(final Activity activity, String subtitle, final RemoteOnclickListener listener) {
        if (activity == null || isRomoteAlertTipsShow) {
            return;
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        final Dialog dialog = new Dialog(activity, R.style.toast);
        View view = inflater.inflate(R.layout.layout_remote_tips, null);
        dialog.setContentView(view);

        if (subtitle != null) {
            ((TextView) view.findViewById(R.id.subtitle)).setText(String.format(activity.getString(R.string.remote_tips_tv), subtitle));
        }
        view.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onclick();
                }
                dialog.dismiss();
                isRomoteAlertTipsShow = false;
            }

        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (listener != null) {
                    listener.onclick();
                }
                isRomoteAlertTipsShow = false;
            }
        });
        isRomoteAlertTipsShow = true;

        WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
        p.width = (int) activity.getResources().getDimension(R.dimen.bindsucess_width);
        p.height = (int) activity.getResources().getDimension(R.dimen.bindsucess_height);
        p.gravity = Gravity.CENTER_VERTICAL;
        dialog.getWindow().setAttributes(p);
        dialog.show();
    }

    public static void showRemoteAlertDialog(final Activity activity, int leftInconId, int rightIconId,
                                             final String left, final String right,
                                             final RemoteOnclickListener leftOnclickListener, final RemoteOnclickListener rightOnclickListener) {
        if (activity == null) {
            return;
        }

        final RemoteDialog dialog = new RemoteDialog(activity, R.style.RemoteDialog);

        final View leftll = dialog.findViewById(R.id.left_ll);
        final View rightll = dialog.findViewById(R.id.right_ll);
        final ImageView leftIv = (ImageView) dialog.findViewById(R.id.iv_left);
        final ImageView rightIv = (ImageView) dialog.findViewById(R.id.iv_right);
        TextView leftTv = (TextView) dialog.findViewById(R.id.left_tv);
        TextView rightTv = (TextView) dialog.findViewById(R.id.right_tv);

        if (leftInconId != -1) {
            leftIv.setImageResource(leftInconId);
        }

        if (rightIconId != -1) {
            rightIv.setImageResource(rightIconId);
        }

        if (!TextUtils.isEmpty(left)) {
            leftTv.setText(left);
        }

        if (!TextUtils.isEmpty(right)) {
            rightTv.setText(right);
        }

        leftll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (leftOnclickListener != null) {
                    leftOnclickListener.onclick();
                }
                dialog.dismiss();
            }
        });


        rightll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rightOnclickListener != null) {
                    rightOnclickListener.onclick();
                }
                dialog.dismiss();
            }
        });

        dialog.show();

    }


    public static void showRemoteSupportAlertTips(final Activity activity, final RemoteOnclickListener left, final RemoteOnclickListener right) {
        if (activity == null || isRomoteAlertTipsShow) {
            return;
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        final AlertDialog dialog = new AlertDialog.Builder(activity).create();
        View view = inflater.inflate(R.layout.layout_remote_nosupport, null);
        dialog.setView(view);


        view.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                left.onclick();
                dialog.dismiss();
            }

        });
        view.findViewById(R.id.know).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                right.onclick();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public interface RemoteOnclickListener {
        public void onclick();
    }
}
