package com.kankan.player.api.tddownload;

import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by zhangdi on 14-4-3.
 */
public class GetUsedPartitionInfoAPI extends TDBaseAPI<UsedPartitionInfo> {

    private static final String API_NAME = "getusedpartitioninfo";
    private String apiUrl ;

    public GetUsedPartitionInfoAPI(String baseUrl) {
        apiUrl = baseUrl + API_NAME;
    }


    @Override
    protected String getUrl() {
        return apiUrl;
    }

    @Override
    protected UsedPartitionInfo request(String response) {
        LOGD(response);

        if (TextUtils.isEmpty(response)) {
            LOGD("response is null");
            return null;
        }


        try {
            UsedPartitionInfo partitionInfo = new UsedPartitionInfo();
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() >= 1) {
                partitionInfo.result = jsonArray.getInt(0);
            }

            partitionInfo.partitions = new ArrayList<UsedPartitionInfo.Partition>();
            for (int i = 1; i < jsonArray.length(); i++) {
                try {
                    JSONArray array = jsonArray.getJSONArray(i);
                    UsedPartitionInfo.Partition partition = new UsedPartitionInfo.Partition();
                    partition.drive = array.getString(0);
                    partition.mountPath = array.getString(1);
                    partitionInfo.partitions.add(partition);
                } catch (JSONException e) {
                    LOGD("jsonexception " + e.getMessage());
                }
            }
            return partitionInfo;
        } catch (JSONException e) {
            LOGD("json array parse exception");
            return null;
        }
    }
}
