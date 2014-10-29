package com.kankan.player.api.tddownload;

import com.kankan.player.app.AppConfig;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by zhangdi on 14-4-2.
 */
public abstract class TDBaseAPI<T> {

    protected abstract String getUrl();

    protected abstract T request(String response);

    public T request() throws IOException {
        String response = execute();
        return request(response);
    }

    protected String execute() throws IOException {
        String ret = null;
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
        HttpConnectionParams.setSoTimeout(httpParameters, 3000);

        HttpClient httpClient = new DefaultHttpClient(httpParameters);
        HttpGet request = new HttpGet(getUrl());

        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            ret = sb.toString();

            br.close();
        }
        return ret;
    }


    protected void LOGD(String msg) {
        AppConfig.logRemote(msg);
    }

}
