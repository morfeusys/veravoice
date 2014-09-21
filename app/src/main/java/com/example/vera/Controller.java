package com.example.vera;

import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by morfeusys on 19.09.14.
 */
public class Controller {

    private static final String TAG = "Controller";

    private final Uri mDetectUri = Uri.parse("http://cp.mios.com/detect_unit.php");
    private final DefaultHttpClient httpClient = new DefaultHttpClient();
    private final Executor executor = Executors.newSingleThreadExecutor();

    private Uri mBaseUri;

    public boolean initialize() {
        JSONArray array = getJsonArray(mDetectUri);
        if (array == null || array.length() == 0) return false;
        try {
            String address = array.getJSONObject(0).getString("InternalIP");
            mBaseUri = Uri.parse("http://" + address + ":3480/data_request");
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public List<Device> getDevices(String str) {
        List<Device> devices = getDevices();
        List<Device> result = new ArrayList<Device>();
        Collections.sort(devices);
        for (Device device : devices) {
            String s = str.replace(device.name, "");
            if (!s.equals(str)) {
                str = s;
                result.add(device);
            }
        }
        return result;
    }

    public List<Device> getDevices() {
        if (mBaseUri == null) return Collections.emptyList();
        JSONObject json = getJsonObject(mBaseUri.buildUpon().appendQueryParameter("id", "sdata").appendQueryParameter("output_format", "json").build());
        if (json == null) return Collections.emptyList();
        try {
            JSONArray categories = json.getJSONArray("categories");
            JSONArray array = json.getJSONArray("devices");
            if (array.length() == 0 || categories.length() == 0) return Collections.emptyList();
            SparseArray<String> cats = new SparseArray<String>(categories.length());
            for (int i = 0; i < categories.length(); i++) {
                JSONObject cat = categories.getJSONObject(i);
                cats.append(cat.getInt("id"), cat.getString("name"));
            }
            List<Device> devices = new ArrayList<Device>(array.length());
            for (int i = 0; i < array.length(); i++) {
                Device device = Device.fromJSON(array.getJSONObject(i), cats);
                if (device != null) {
                    devices.add(device);
                }
            }
            return devices;
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }

    public String process(final Device device) {
        if (device.category.field != null) {
            Object obj = device.data.get(device.category.field);
            return obj == null ? null : obj.toString();
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                toggle(device);
            }
        });
        return null;
    }

    private void toggle(Device device) {
        String status = (String) device.data.get("status");
        status = "1".equals(status) ? "0" : "1";
        get(mBaseUri.buildUpon().appendQueryParameter("id", "action")
                .appendQueryParameter("DeviceNum", device.id+"")
                .appendQueryParameter("serviceId", "urn:upnp-org:serviceId:SwitchPower1")
                .appendQueryParameter("action", "SetTarget")
                .appendQueryParameter("newTargetValue", status).build());
    }

    private String get(Uri uri) {
        final String url = uri.toString();
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() != 200) {
                response.getEntity().consumeContent();
                return null;
            }
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "Can't execute [" + url + "]", e);
            return null;
        }
    }

    private JSONObject getJsonObject(Uri uri) {
        String json = get(uri);
        try {
            return json != null ? new JSONObject(json) : null;
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONArray getJsonArray(Uri uri) {
        String json = get(uri);
        try {
            return json != null ? new JSONArray(json) : null;
        } catch (JSONException e) {
            return null;
        }
    }
}
