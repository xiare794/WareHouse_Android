package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/7/15.
 */
public class MonitorActivity extends Activity {

    static int MONITOR_CONNECTED = 100;
    static int MONITOR_LOST = 101;
    static int GETTING_MESSAGE = 102;

    static int pauseTime = 30000; //连接失败的间隔
    static int getDataInterval = 5000; //查询数据间隔



    public boolean connected = false;
    public boolean getDataStatus = false;
    JSONArray msgPool;
    private ListViewAdapter mListViewAdapter = new ListViewAdapter();
    private ListView mListView;
    //public List<String> msgData;

    private SoundPool sp;
    private Map<Integer, Integer> map;
    private boolean stopFlag = false;


    //Map<Integer,String> msgRem=new HashMap<Integer,String>(); //目前显示的信息

    class Msg{
        String id;
        String content;
        int rem;
        public Msg(String _id,String _content){
            id=_id; content=_content; rem = 10;
        }
    }

    ArrayList<Msg> msgRem = new ArrayList<Msg>();
    //Msg[] msgRem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        //获取入库单
        setContentView(R.layout.activity_monitor);
        //ConThread = new Thread();

        //线程启动

        mListView = (ListView) findViewById(R.id.rectNotification);
        mListView.setAdapter(mListViewAdapter);
        //mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,GetMsgData()));

        sp = new SoundPool(5,// 同时播放的音效
                AudioManager.STREAM_MUSIC, 0);
        map = new HashMap<Integer, Integer>();
        map.put(1, sp.load(this, R.raw.remind, 1));
        map.put(2, sp.load(this, R.raw.l_alarm, 1));

        //soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 5);
        //soundPool.load(this, R.raw.remind, 1);
        //soundPool.load(this, R.raw.l_alarm,2);

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == GETTING_MESSAGE) {
                //接受事件中 每次接受完事件在这里更新
                Log.d("连接中", "30s回访一次");


                InvalidateMonitor();
                //getDataStatus = true;

            }
            if (msg.what == MONITOR_CONNECTED) {
                //连接服务器成功
                //Log.d("连接中","不在回访");
                TextView status = (TextView) findViewById(R.id.monitor_status);
                status.setText(Html.fromHtml("<font color='#00CC33'>" + "已连接主机</font>"));
                //Message linkMsg = new Message();
                //linkMsg.what = GETTING_MESSAGE;
                //handler.sendMessage(linkMsg);
            }
            if (msg.what == MONITOR_LOST) {
                //无法连接服务器
                Log.d("失去连接", "30s尝试连接一次");
                TextView status = (TextView) findViewById(R.id.monitor_status);
                status.setText(Html.fromHtml("<font color='#FFCC33'>" + "失去连接," + getString(R.string.dbAddress) + "尝试连接中，下次连接一分钟" + "</font>"));
                //getDataStatus = false;

            }
        }
    };
    private void playSound(int sound, int number) {
        AudioManager am = (AudioManager) getSystemService(this.AUDIO_SERVICE);// 实例化
        float audioMaxVolum = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);// 音效最大值
        float audioCurrentVolum = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float audioRatio = audioCurrentVolum / audioMaxVolum;
        sp.play(map.get(sound),
                audioRatio,// 左声道音量
                audioRatio,// 右声道音量
                1, // 优先级
                number,// 循环播放次数
                1);// 回放速度，该值在0.5-2.0之间 1为正常速度
    }

    public void putIntoMsgBox(String _id, String _msg,int msgType){
        boolean me = false;
        //试图添加，如果没见就加
        boolean _exsit = false;
        Iterator<Msg> it=msgRem.iterator();
        while(it.hasNext()){
            if(it.next().id.equals(_id)){
                _exsit = true;
                break;
            }
        }
        if(!_exsit) {
            msgRem.add(new Msg(_id, _msg));
            if(msgType == 0) {
                playSound(1, 10);
                //soundPool.play(0, 1, 1, 0, 0, 1);
            }
            if(msgType == 1){
                playSound(2,10);
            }
        }

    }


    public void markReaded(String id){
        String address = getString(R.string.dbAddress);
        String comRequest = "http://" + address + "/ws/warhouse/WebServer/phpMessage.php?";
        //参数<
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("Read", id));
        String req = comRequest + URLEncodedUtils.format(params, HTTP.UTF_8);
        Log.d("访问地址", comRequest);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try {
            HttpResponse res = client.execute(get);
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = res.getEntity();
                Log.d("markRead结果", EntityUtils.toString(entity));
            }
        }
        catch (Exception e) {
            Log.d("连接失败", "Exception");
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }
    class ThreadShow extends Thread {
        //private boolean isSleep = true;

        @Override
        public void run() {
            if (!stopFlag) {
                // TODO Auto-generated method stub
                //int cTime = 0;
                Log.d("进入线程", ConThread.getName());
                while (!connected) {
                    Log.d("线程", ConThread.getName() + "循环");

                    ConnectDB();
                    try {
                        if (!connected) {
                            ConThread.sleep(pauseTime);
                            System.out.println("连接又失败，暂停30s");
                        } else {
                            ConThread.sleep(3000);
                            getDataStatus = true;
                            System.out.println("连接成功，暂停3s,开始接收message");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("thread error...connect");
                    }


                    while (getDataStatus) {
                        try {
                            ConThread.sleep(getDataInterval);
                            //cTime++;
                            Log.d("获取数据",  "次,间隔" + getDataInterval + "ms"+",msg"+msgRem.size());
                            GettingActions();

                            Iterator<Msg> it = msgRem.iterator();
                            while(it.hasNext()){
                                Msg m = it.next();
                                if(m.rem == 0){
                                    Log.d("markread","mark"+m.id);
                                    markReaded(m.id);
                                    it.remove();
                                    playSound(1,10);
                                }
                            }

                            Message linkMsg = new Message();
                            linkMsg.what = GETTING_MESSAGE;
                            handler.sendMessage(linkMsg);


                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("thread error... getting data");
                        }
                        /*
                        if (cTime > 3) {
                            break;
                        }*/
                    }

                    connected = false; //失去连接置假，永远不停止大while循环
                    if(stopFlag)
                        connected = true;
                }
                System.out.println("线程" + Thread.currentThread().getName() + "执行完毕");

            }
        }
        public void StopFlag(){
            stopFlag = true;
            getDataStatus= false;

        }
    };

    public ThreadShow ConThread = new ThreadShow();
    @Override
    public void onResume() {
        super.onResume();
        Log.d("123", "BadCodeActivity instance is called onResume :" + this.hashCode());
        ConThread.start();

    }

    @Override
    public void onPause() {
        super.onPause();

        ConThread.StopFlag();
        Log.e("程序暂停","线程停止");
    }

    public void ConnectDB() {
        String address = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wappin` WHERE `appStatus`=1";
        String comRequest = "http://" + address + "/ws/warhouse/WebServer/phpSearch.php?";
        //参数<
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query", query));
        String req = comRequest + URLEncodedUtils.format(params, HTTP.UTF_8);
        Log.d("访问地址", comRequest);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        JSONArray json = null;
        try {
            HttpResponse res = client.execute(get);
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = res.getEntity();
                json = new JSONArray(EntityUtils.toString(entity));

                //TextView status = (TextView) findViewById(R.id.monitor_status);
                //status.setText(Html.fromHtml("<font color='#00CC33'>" + "已连接" + "</font>"));
                connected = true;
            } else {
                //TextView status = (TextView) findViewById(R.id.monitor_status);
                //status.setText(Html.fromHtml("<font color='#FFCC33'>" + "失去连接" + "</font>"));
                connected = false;
            }
        } catch (Exception e) {
            Log.d("连接失败", "Exception");
            e.printStackTrace();
            connected = false;
            //throw new RuntimeException(e);
        } finally {
            Log.d("连接失败", "finally");
            client.getConnectionManager().shutdown();
        }

        if (!connected) {
            Message linkMsg = new Message();
            linkMsg.what = MONITOR_LOST;
            handler.sendMessage(linkMsg);
        } else {
            Message linkMsg = new Message();
            linkMsg.what = MONITOR_CONNECTED;
            handler.sendMessage(linkMsg);
            //getDataStatus = true;
        }
    }


    public void GettingActions() {
        String address = getString(R.string.dbAddress);
        //http://localhost/ws/warhouse/WebServer/phpMessage.php?GetMessage=true
        String query = "SELECT * FROM `wappin` WHERE `appStatus`=1";
        String comRequest = "http://" + address + "/ws/warhouse/WebServer/phpMessage.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("GetMessage", "true"));
        //默认参数为获取最近10个，月内
        //params.add(new BasicNameValuePair("msgCount","50"));
        //params.add(new BasicNameValuePair("interval", "YEAR"));
        String req = comRequest + URLEncodedUtils.format(params, HTTP.UTF_8);
        Log.d("访问地址", comRequest);

        msgPool = get(req);
    }


    public void InvalidateMonitor() {
        mListView.setAdapter(mListViewAdapter);

        //循环rem-1，减到0就删除
        Iterator<Msg> it=msgRem.iterator();
        while(it.hasNext()){
            Msg m = it.next();
            if(m.rem>0)
                m.rem--;
            if(m.rem == 0){
                Log.d("消息可以退出了",m.content);
                //it.remove();
            }
        }
        //Log.d("信息监控",msgRem.toString());


    }

    private List<String> GetMsgData() {
        Log.d("GetMsgData", "listview数据");
        List<String> ret = new ArrayList<String>();
        if (msgPool != null) {
            if (msgPool.length() > 0) {
                try {
                    for (int i = 0; i < msgPool.length(); i++) {
                        ret.add(msgPool.getJSONObject(i).getString("actContent"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                ret.add("数据等待中");
            }
        } else {
            ret.add("数据等待中");
        }
        return ret;

    }


    class ListViewAdapter extends BaseAdapter {
        int count = 1;

        public int getCount() {
            if (msgPool != null)
                return msgPool.length();
            return count;
            //return count;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View view, ViewGroup parent) {
            TextView mTextView;
            int lineCount = 1;
            if (view == null) {
                mTextView = new TextView(MonitorActivity.this);
            } else {
                mTextView = (TextView) view;
            }
            String itemStr = "123";

            if(msgPool != null) {
                try {
                    itemStr = msgPool.getJSONObject(position).getString("actContent");
                    String type =  msgPool.getJSONObject(position).getString("actType");
                    if(type.equals("出入库出错")){
                        itemStr = "<font color='#FF1111'>"+itemStr+"</font>";
                        //Html.fromHtml("<font color='#FF7F27'>扫描到</font>")
                        putIntoMsgBox(msgPool.getJSONObject(position).getString("actID"), msgPool.getJSONObject(position).getString("actContent"),1);
                    }
                    else{
                        putIntoMsgBox(msgPool.getJSONObject(position).getString("actID"), msgPool.getJSONObject(position).getString("actContent"),0);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                itemStr = "等待消息";
            }
            mTextView.setPadding(3,8,3,8);
            mTextView.setText(Html.fromHtml(itemStr));
            mTextView.setTextColor(getResources().getColor(R.color.antiquewhite));
            //mTextView.setTextSize(20f);
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,70);
            return mTextView;
        }
    }


    //通过地址返回JSONArray
    public static JSONArray get(String url) {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        JSONArray json = null;
        try {
            HttpResponse res = client.execute(get);
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = res.getEntity();
                //Log.d("JSON",EntityUtils.toString(entity));
                json = new JSONArray(EntityUtils.toString(entity));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return json;
    }



}