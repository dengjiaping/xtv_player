package com.kankan.player.api.tddownload;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

/**
 * Created by zhangdi on 14-4-2.
 */
public class GetTaskListAPI extends TDBaseAPI<TaskList> {

    public static final int TYPE_TASK_COMPLETED = 1;
    public static final int TYPE_TASK_UNCOMPLETED = 0;
    private static final String API_NAME = "gettasklist";
    private String apiUrl;

    private int complete;
    private int pageCapacity;
    private int pageIndex;

    private int mVerison;
    private boolean mIsLocal;

    public GetTaskListAPI(String baseUrl, int complete, int pageIndex, int pageCapacity, int version, boolean isLocal) {
        apiUrl = baseUrl + API_NAME;
        this.complete = complete;
        this.pageIndex = pageIndex;
        this.pageCapacity = pageCapacity;
        this.mVerison = version;
        this.mIsLocal = isLocal;
    }

    @Override
    protected String getUrl() {
        if (mIsLocal) {
            if (mVerison < Constants.KEY_REMOTE_VERSION) {
                return apiUrl + String.format("?complete=%d&page_capacity=%d&page_index=%d",
                        complete, pageCapacity, pageIndex);
            } else {
                return apiUrl + String.format("?complete=%d&page_capacity=%d&page_index=%d&abs_path=1",
                        complete, pageCapacity, pageIndex);
            }
        } else {
            return apiUrl + String.format("?complete=%d&page_capacity=%d&page_index=%d",
                    complete, pageCapacity, pageIndex);
        }

    }

    @Override
    protected TaskList request(String response) {
        LOGD(response);

        if (TextUtils.isEmpty(response)) {
            LOGD("response is null");
            return null;
        }

        try {
            TaskList taskList = new TaskList();
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() >= 3) {
                taskList.result = jsonArray.getInt(0);
                taskList.pageCount = jsonArray.getInt(1);

                taskList.tasks = new ArrayList<TaskList.Task>();

                for (int i = 2; i < jsonArray.length(); i++) {

                    JSONArray array = jsonArray.getJSONArray(i);

                    try {
                        TaskList.Task task = new TaskList.Task();
                        if (array.length() >= 13) {
                            task.taskId = array.getInt(0);
                            task.stat = array.getInt(1);
                            task.type = array.getInt(2);
                            try {
                                task.fileName = URLDecoder.decode(array.getString(3), "UTF-8").trim();
                                task.filePath = URLDecoder.decode(array.getString(4), "UTF-8") + task.fileName;

                            } catch (UnsupportedEncodingException e) {
                                AppConfig.LOGD("[[GetTaskListApi]] " + e.getMessage());
                            }
                            task.fileSize = array.getString(5);
                            task.downloadDataSize = array.getString(6);
                            task.startTime = array.getInt(7);
                            task.finishedTime = array.getInt(8);
                            task.failCode = array.getInt(9);
                            task.dlSpeed = array.getInt(10);
                            task.ulSpeed = array.getInt(11);

                            try {
                                task.url = URLDecoder.decode(array.getString(12), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }

                        taskList.tasks.add(task);
                    } catch (JSONException e) {
                        AppConfig.LOGD("[[GetTaskListApi]] " + e.getMessage());
                    }
                }


            }
            return taskList;
        } catch (JSONException e) {
            LOGD("json array parse exception");
            return null;
        }


    }

}
