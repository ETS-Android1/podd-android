/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cm.podd.report.util;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

public class RequestDataUtil {

    private static final String TAG = "RequestDataUtil";
    private static Charset utf8Charset = Charset.forName("UTF-8");

    public static ResponseObject post(String path, String query, String json, String token) {
        JSONObject jsonObj = null;
        int statusCode = 0;
        String reqUrl = String.format("%s%s%s", SharedPrefUtil.getServerAddress(), path,
                query == null ? "" : "?"+query);
        Log.i(TAG, "submit url=" + reqUrl);
        Log.i(TAG, "post data=" + json);

        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpClient client = new DefaultHttpClient(params);

        try {
            HttpPost post = new HttpPost(reqUrl);
            post.setHeader("Content-type", "application/json");
            if (token != null) {
                post.setHeader("Authorization", "Token " + token);
            }
            post.setEntity(new StringEntity(json, HTTP.UTF_8));

            HttpResponse response;
            response = client.execute(post);
            HttpEntity entity = response.getEntity();

            // Detect server complaints
            statusCode = response.getStatusLine().getStatusCode();
            Log.e(TAG, "status code=" + statusCode);

            if (statusCode < HttpURLConnection.HTTP_INTERNAL_ERROR) {
                InputStream in = entity.getContent();
                String resp = FileUtil.convertInputStreamToString(in);

                jsonObj = new JSONObject(resp);
                entity.consumeContent();
            }

        } catch (ClientProtocolException e) {
            Log.e(TAG, "error post data", e);
        } catch (IOException e) {
            Log.e(TAG, "error post data", e);

        } catch (JSONException e) {
            Log.e(TAG, "error convert json", e);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return new ResponseObject(statusCode, jsonObj);
    }


    public static class ResponseObject implements Serializable {
        private JSONObject jsonObject;
        private int statusCode;

        public ResponseObject(int statusCode, JSONObject jsonObject) {
            this.jsonObject = jsonObject;
            this.statusCode = statusCode;
        }

        public JSONObject getJsonObject() {
            return jsonObject;
        }

        public void setJsonObject(JSONObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }
    }
}
