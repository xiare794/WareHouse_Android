package com.demo.administrator.warelink_mobile;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Administrator on 2015/5/27.
 */
public class JsonUtils {
    private static String dbAddress;


    public JsonUtils(String address){
        dbAddress = address;
    }

    public JSONObject getTray(String rfidID){

        String comRequest = "http://"+dbAddress+"/ws/warhouse/WebServer/phpSearch.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("rfid",rfidID));
        String finRequest = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        JSONArray jsonA = getJson(finRequest);
        //如果返回为空 rfid没有符合项
        if(jsonA.length() == 0) {
            //自动绑定新托盘 并且初始化
//          //稍后添加
        }
        return getFirst(jsonA);
    }

    public JSONObject getFirst(JSONArray array){
        if(array.length()>0){
            //Log.d("JsonUtil", "getFirst");

            try {
                return array.getJSONObject(0);
            }
            catch (JSONException e){
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    //直接通过query来访问数据库，返回对应jsonArray
    public JSONArray getRequestJson(String req){
        //String query = "SELECT * FROM `wappin` WHERE `appStatus`<3";
        String comRequest = "http://"+dbAddress+"/ws/warhouse/WebServer/phpSearch.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",req));
        String finRequest = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        JSONArray jsonA = getJson(finRequest);
        return jsonA;
    }
    //对托盘进行修改，插入Log和Unit
    public String InsertUintForTray(Map<String,String> map){
        //uintData含有 userName userID appID trayID length width height count
        //3个任务 1.增加uint 2.增加Log 3.修改tray
        String baseRequest = "http://"+dbAddress+"/ws/warhouse/WebServer/phpSearch.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("mobile","InsertUnitForTray_addUint"));
        for (String key : map.keySet()) {
            params.add(new BasicNameValuePair(key,map.get(key)));
        }
        String finRequest = baseRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        Log.d("InsertUintForTray",finRequest);
        JSONArray jsonA = getJson(finRequest);
        Log.d("InsertUintForTray",jsonA.toString());
        return jsonA.toString();
        //params.add(new BasicNameValuePair("query",req));
        /*
        String cR1 =baseRequest+ "&&appID="+map.get("appID");
        cR1 += "&&trayID="+map.get("trayID");
        cR1 += "&&itemType="+map.get("itemType");
        cR1 += "&&length="+map.get("length");
        cR1 += "&&width="+map.get("width");
        cR1 += "&&height="+map.get("height");
        cR1 += "&&count="+map.get("count");*/

    }

    private static String getString(String url){
        HttpClient client = new DefaultHttpClient();
        Log.d("查询全地址",url);
        HttpGet get = new HttpGet(url);
        String result="";
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                result = res.getEntity().toString();
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return result;
    }

    //通过地址返回JSONArray
    private static JSONArray getJson(String url){
        HttpClient client = new DefaultHttpClient();
        Log.d("查询全地址",url);

        HttpGet get = new HttpGet(url);
        JSONArray json = null;
        try{
            HttpResponse res = client.execute(get);

            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                String entityStr = EntityUtils.toString(entity);
                Log.e("服务器响应",entityStr);
                json = new JSONArray(entityStr);
            }
        }catch (Exception e){
            //Log.e("服务器响应错误最终为成功",EntityUtils.toString(entity));
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return json;
    }
}
