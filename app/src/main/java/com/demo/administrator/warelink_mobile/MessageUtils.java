package com.demo.administrator.warelink_mobile;

import android.util.Log;

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

import java.util.ArrayList;

/**
 * Created by Administrator on 2015/7/14.
 */
public class MessageUtils {
    //消息扩展是用来手机端向数据库同步传递消息的应用

    //messageType仅包含手机端所使用到的对应类型
    //1.指定司机
    //2.通知过门
    //3.添加托盘
    //4.添加货物
    //5.申请完成

    final String[] messageType = new String[] { "指定司机", "通知过门", "添加托盘", "添加货物","申请完成","注册托盘","上架","装箱" };
    private static String dbAddress;
    public MessageUtils(String address){
        dbAddress = address;
    }

    /* AddMessage() */
    public boolean AddMessage(int typeID, String para){
        //para以UserID|分割InstockID|trayID|slotID|appID|actContent  /* AddMessage() */
        //String []paraStr = para.split("|");
        String comRequest = "http://"+dbAddress+"/ws/warhouse/WebServer/phpMessage.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("MessageType",messageType[typeID]));
        params.add(new BasicNameValuePair("mPara",para));
        Log.d("添加消息","mPara"+para);
        String finRequest = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        Log.d("消息URL",finRequest);

        HttpGet get = new HttpGet(finRequest);
        HttpClient client = new DefaultHttpClient();
        try {
            HttpResponse res = client.execute(get);

            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = res.getEntity();
                String entityStr = EntityUtils.toString(entity);
                Log.e("添加事件", entityStr);

            }
        }
        catch (Exception e){
            //Log.e("服务器响应错误最终为成功",EntityUtils.toString(entity));
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }




}
