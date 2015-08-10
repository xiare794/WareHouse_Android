package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;

/**
 * Created by Administrator on 2015/7/18.
 */
public class TruckHomeActivity extends Activity{

    private Context mContext;
    private boolean stopFlag = false; //线程停止器
    private boolean connectionLost = true; //断线值

    private int getDataInterval = 0;// 3000; //获取数据间隔
    private int connectionLostPause =0;// 10000; //10s后连接

    private static int GETTING_MESSAGE = 100;
    private static int GETTING_MESSAGE_FALSE = 101;
    private JSONArray truckRemindersIn;
    private JSONArray truckRemindersOut;
    private JSONArray traysStack;
    private JSONArray traysOutStack;
    private JSONArray slotStack;
    private String recomSlot = "";
    private ListViewAdapter mListViewAdapter = new ListViewAdapter();
    private TraysAdapter tListViewAdaptier = new TraysAdapter();
    private ListView mListView;
    private ListView tListView;
    private SoundPool soundPool;

    private String trayQueryIn = "";
    private String trayQueryOut = "";
    //private boolean loadSafely = false;

    static final int LOAD_SAFELY = 100;

    private AlertDialog UpShelfAD=null;
    private AlertDialog SlotSelectAD=null;
    String[] slot_opt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_truck_home);
        Intent i = getIntent();

        TextView userNameLabel = (TextView)findViewById(R.id.truckPageUserName);
        userNameLabel.setText(MyPreference.getInstance(mContext).getLoginName()+":"+MyPreference.getInstance(mContext).getUserID());

        mListView = (ListView) findViewById(R.id.TruckRemind);
        mListView.setAdapter(mListViewAdapter);

        tListView = (ListView)findViewById(R.id.truckPage_TrayBox);
        tListView.setAdapter(tListViewAdaptier);

        getDataInterval = Integer.parseInt(getString(R.string.RefreshInterval));
        connectionLostPause = Integer.parseInt(getString(R.string.ConnectionLostInterval));

        tListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {
                Log.d("点击详细",trayMsgBox.get(position).status);
                if(trayMsgBox.get(position).status.equals("仓库内") && trayMsgBox.get(position).type.equals("进仓")) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(TruckHomeActivity.this);
                    builder.setTitle("上架托盘");
                    builder.setIcon(android.R.drawable.ic_dialog_info);
                    recomSlot = getProperSlots();
                    builder.setMessage("建议将托盘放入"+recomSlot.split("-")[1]+"位置货架,\n从建议位置上架直接点选上架");
                    builder.setPositiveButton("上架", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d("上架信息", recomSlot + "-" + MyPreference.getInstance(mContext).getLoginName() + trayMsgBox.get(position).wtID);

                            new Thread(){
                                @Override
                                public void run() {
                                    if(LoadTray(trayMsgBox.get(position).wtID,recomSlot.split("-")[0]))
                                    {
                                        Message msg = new Message();
                                        msg.what = LOAD_SAFELY;
                                        msg.obj = "把货物上架到"+recomSlot.split("-")[1]+"位置";
                                        uiHandler.sendMessage(msg);
                                    }
                                }
                            }.start();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("手动选择",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                            final AlertDialog.Builder builder = new AlertDialog.Builder(TruckHomeActivity.this);
                            builder.setItems(slot_opt,new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick (DialogInterface dialog,int which){

                                    Log.d("上架信息", slot_opt[which]+"-"+ trayMsgBox.get(position).wtID);
                                    recomSlot = slot_opt[which];


                                    new Thread(){
                                        @Override
                                        public void run() {
                                            if(LoadTray(trayMsgBox.get(position).wtID,recomSlot.split("-")[0]))
                                            {
                                                Message msg = new Message();
                                                msg.what = LOAD_SAFELY;
                                                msg.obj = "把货物上架到"+recomSlot.split("-")[1]+"位置";
                                                uiHandler.sendMessage(msg);
                                            }
                                        }
                                    }.start();

                                }
                            });
                            builder.setPositiveButton("返回",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();
                                        }
                                    });

                            builder.show();



                        }
                    });
                    UpShelfAD = builder.create();
                    UpShelfAD.show();
                }
            }
        });

        soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        soundPool.load(this, R.raw.remind, 1);
    }

    class AppMsg{
        //左侧出入库单  出入库、入库编号|箱号  xx托/xx托 id
        String appID;
        String appType;
        int count;                  //实际待入数量
        String appMaitou;
        String appName;
        String agentID;
        String OpCounterID;
        String OpCName;          //理货员姓名
        String InStockID;            //入库单存在的入库编号
        String CSeries;                //出库单存在箱号
        String appSeries;               //出入库单都有的出入库编号;

        //入库单信息 构造
        public AppMsg(String _appID,String _appType,String _appMaitou,String _agentID,String _InStockID, String _appSeries, String _OpCounter){
            appID = _appID;
            appType = _appType;
            count = 0;
            appMaitou = _appMaitou;
            agentID = _agentID;
            InStockID = _InStockID;
            appSeries = _appSeries;
            OpCounterID = _OpCounter;

        }
        //出库单信息 构造函数
        public AppMsg(int _out,String _appID,String _agentID, String _appSeries, String _OpCounter, String _CSeries){
            appID = _appID;
            appType = "装箱";
            count = 0;
            agentID = _agentID;
            appSeries = _appSeries;
            OpCounterID = _OpCounter;
            CSeries = _CSeries;
        }
    }
    ArrayList<AppMsg> appMsgBox = new ArrayList<AppMsg>();

    class trayMsg{
        int idx;
        String wtID;
        String appID;
        int itemCount;
        String status;
        String slot;
        String type; //进仓、出仓
        String OpCounterID; //理货员ID
        String OpCounterName; //理货员名
        String outPos;   //舱外 卸装货位置
        String Maitou ; //唛头
        String appName;  //货物名称



        public trayMsg(){
        }
    }
    ArrayList<trayMsg> trayMsgBox = new ArrayList<trayMsg>();


    public void getSlotData(){
        //slotStack
        String address = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wSlots` WHERE 1";
        String comRequest = "http://" + address + "/ws/warhouse/WebServer/phpSearch.php?";
        slotStack = Search(comRequest,query);
    }

    //上货架 Str1 托盘号  Str2 货架号
    public boolean LoadTray(String st1, String st2){

        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("table","wTrays"));
        params.add(new BasicNameValuePair("tKey","wSlotID"));
        params.add(new BasicNameValuePair("tVal",st2));
        params.add(new BasicNameValuePair("idKey","wtID"));
        params.add(new BasicNameValuePair("idVal",st1 ));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        ArrayList<NameValuePair> params2 = new ArrayList<>();
        params2.add(new BasicNameValuePair("table","wTrays"));
        params2.add(new BasicNameValuePair("tKey","twStatus"));
        params2.add(new BasicNameValuePair("tVal","货架"));
        params2.add(new BasicNameValuePair("idKey","wtID"));
        params2.add(new BasicNameValuePair("idVal",st1 ));
        String req2 = comRequest+ URLEncodedUtils.format(params2, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();

        try{
            HttpGet get = new HttpGet(req);
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改货架号结果",EntityUtils.toString(entity));
            }
            HttpGet get1 = new HttpGet(req2);
            HttpResponse res1 = client.execute(get1);
            if(res1.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res1.getEntity();
                Log.d("修改托盘状态结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            connectionLost = true;
            Log.e("执行connectionLost","connectionLost="+connectionLost);
            e.printStackTrace();


        }finally {
            client.getConnectionManager().shutdown();
        }

        //添加消息
        MessageUtils mu = new MessageUtils(getString(R.string.dbAddress));
        String para =MyPreference.getInstance(mContext).getUserID()+"||"+st1+"|"+st2+"||"+MyPreference.getInstance(mContext).getLoginName()+"将托盘"+st1+"放上货架"+st2;
        //InstockID|trayID|slotID|appID|actContent
        mu.AddMessage(6,para);

        return true;
    }

    public String getProperSlots(){
        if(slotStack != null){
            //for(int i=0; i<slotStack.length(); i++){
            int num = (int) (Math.random()/slotStack.length())+1;
            try {
                String idx = slotStack.getJSONObject(num).getString("wSlotID");
                String pos = slotStack.getJSONObject(num).getString("tsPos");
                return idx+"-"+pos;
            }
            catch (JSONException e){
                connectionLost = true;
                e.printStackTrace();

            }
            //}
        }
        return "";
    }

    class ThreadShow extends Thread {
        @Override
        public void run() {
            Log.d("进入线程", driverThread.getName()+connectionLost);
            while (!stopFlag) {
                try{
                    if(!connectionLost) {
                        getSlotData();
                        if (slotStack != null) {
                            try {
                                slot_opt = new String[slotStack.length()];
                                for (int i = 0; i < slotStack.length(); i++) {
                                    slot_opt[i] = slotStack.getJSONObject(i).getString("wSlotID") + "-编号" + slotStack.getJSONObject(i).getString("tsPos");
                                    //Log.d("slot",slot_opt[i]);
                                }
                            } catch (JSONException e) {
                                connectionLost = true;
                                e.printStackTrace();

                            }
                        }
                    }


                    String address = getString(R.string.dbAddress);
                    String query = "SELECT * FROM `wappin` WHERE `appStatus`= \"1\" AND `OpFork`=\""+MyPreference.getInstance(mContext).getUserID()+"\"";
                    String comRequest = "http://" + address + "/ws/warhouse/WebServer/phpSearch.php?";

                    String outQuery = "SELECT * FROM `wAppOut` aOut, `wContainers` wc WHERE aOut.appStatus= \"1\" AND aOut.OpFork=\""+MyPreference.getInstance(mContext).getUserID()+"\" AND aOut.containerID = wc.wCID";
                    String outRequest = "http://" + address + "/ws/warhouse/WebServer/phpSearch.php?";


                    if(!connectionLost) {
                        truckRemindersIn = Search(comRequest, query);
                        truckRemindersOut = Search(outRequest,outQuery);
                    }

                    //控制是否清理appMsgBox的逻辑
                    //当In有东西，Out有东西 clear 按顺序添加
                    //当In有东西，Out没东西 clear 添加In
                    //当In没东西，Out有东西 clear 添加Out
                    //当In，out都没有东西 不清理，不添加


                    if(truckRemindersIn != null && truckRemindersOut != null) {
                        try {
                            if (truckRemindersIn.length() > 0 || truckRemindersOut.length() >0) {
                                appMsgBox.clear();
                                trayQueryIn = "";
                                trayQueryOut = "";
                                //获取入库单
                                for (int i = 0; i < truckRemindersIn.length(); i++) {
                                    JSONObject obj = truckRemindersIn.getJSONObject(i);
                                    AppMsg t = new AppMsg(obj.getString("appID"), "进仓", obj.getString("appMaitou"), obj.getString("agentID"), obj.getString("InStockID"), obj.getString("appSeries"), obj.getString("OpCounter"));
                                    t.appName = obj.getString("appName");
                                    appMsgBox.add(t);
                                    trayQueryIn += " wtAppID=" + obj.getString("appID");
                                    if (i < truckRemindersIn.length() - 1) {
                                        trayQueryIn += " OR";
                                    }
                                }
                                //获取出库单
                                for (int i = 0; i < truckRemindersOut.length(); i++) {
                                    JSONObject obj = truckRemindersOut.getJSONObject(i);
                                    //AppMsg(int _out,String _appID,String _agentID, String _appSeries, String _OpCounter, String _CSeries)
                                    AppMsg t = new AppMsg(0,obj.getString("wAppID"),obj.getString("agentID"),obj.getString("series"),obj.getString("OpCounter"),obj.getString("wCSeries") );
                                    appMsgBox.add(t);
                                    trayQueryOut += " wtAppOutID=" + obj.getString("wAppID");
                                    if (i < truckRemindersOut.length() - 1) {
                                        trayQueryOut += " OR";
                                    }
                                }
                            }
                            connectionLost = false;
                        } catch (JSONException e) {
                            connectionLost = true;
                            e.printStackTrace();
                        }
                    }



                    //获取托盘
                    query = "SELECT * FROM `wTrays` WHERE "+trayQueryIn;
                    comRequest = "http://" + address + "/ws/warhouse/WebServer/phpSearch.php?";

                    outQuery = "SELECT t.*, win.appMaitou, win.appName FROM `wTrays` t,`wAppIn` win WHERE ( "+trayQueryOut +") AND t.wtAppID = win.appID" ;
                    outRequest = "http://" + address + "/ws/warhouse/WebServer/phpSearch.php?";
                    if(!connectionLost) {
                        if(trayQueryIn.length()>0)
                            traysStack = Search(comRequest, query);
                        if(trayQueryOut.length()>0)
                            traysOutStack = Search(outRequest,outQuery);
                    }
                    if(traysStack != null && traysOutStack != null && !connectionLost) {
                        if (traysStack.length() > 0 || traysOutStack.length()>0) {
                            trayMsgBox.clear();

                            for (int i = 0; i < traysStack.length(); i++) {
                                for (int j = 0; j < appMsgBox.size(); j++) {
                                    if (appMsgBox.get(j).appID.equals(traysStack.getJSONObject(i).getString("wtAppID")) && appMsgBox.get(j).appType.equals("进仓")) {
                                        trayMsg t = new trayMsg();
                                        t.idx = j;
                                        t.appID = traysStack.getJSONObject(i).getString("wtAppID");
                                        t.itemCount = traysStack.getJSONObject(i).getInt("twWareCount");
                                        t.status = traysStack.getJSONObject(i).getString("twStatus");
                                        t.slot = "推荐仓位:暂未分配";//计算预入仓位
                                        t.type = "进仓";
                                        t.wtID = traysStack.getJSONObject(i).getString("wtID");
                                        t.Maitou = appMsgBox.get(j).appMaitou;
                                        t.OpCounterID = appMsgBox.get(j).OpCounterID;
                                        t.outPos = "G1门外卸货中";
                                        t.appName = appMsgBox.get(j).appName;
                                        if (t.status.equals("货架")) {
                                            //不加了
                                        } else {
                                            trayMsgBox.add(t);
                                            appMsgBox.get(j).count++;
                                        }

                                        break;
                                    }
                                }
                            }

                            for (int i = 0; i < traysOutStack.length(); i++) {
                                for (int j = 0; j < appMsgBox.size(); j++) {
                                    if (appMsgBox.get(j).appID.equals(traysOutStack.getJSONObject(i).getString("wtAppOutID")) && appMsgBox.get(j).appType.equals("装箱")) {
                                        trayMsg t = new trayMsg();
                                        t.idx = j;
                                        t.appID = traysOutStack.getJSONObject(i).getString("wtAppOutID");
                                        t.itemCount = traysOutStack.getJSONObject(i).getInt("twWareCount");
                                        t.status = traysOutStack.getJSONObject(i).getString("twStatus");
                                        t.slot = traysOutStack.getJSONObject(i).getString("wSlotID");
                                        t.type = appMsgBox.get(j).appType;
                                        t.wtID = traysOutStack.getJSONObject(i).getString("wtID");
                                        t.Maitou = traysOutStack.getJSONObject(i).getString("appMaitou");
                                        t.appName = traysOutStack.getJSONObject(i).getString("appName");
                                        t.OpCounterID = appMsgBox.get(j).OpCounterID;
                                        t.outPos = "箱位C1";

                                        if (t.status.equals("仓库外")) {
                                            //不加了
                                        } else {
                                            trayMsgBox.add(t);
                                            appMsgBox.get(j).count++;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    //获取告警
                    /*
                    query = "SELECT * FROM `wActions` WHERE (`actType`= \"出入库出错\" AND  `waRep`=1) ORDER BY `actID` DESC LIMIT 0,1 ";
                    comRequest = "http://" + address + "/ws/warhouse/WebServer/phpSearch.php?";
                    if(!connectionLost) {
                        if(trayQueryIn.length()>0)
                            traysStack = Search(comRequest, query);
                        if(trayQueryOut.length()>0)
                            traysOutStack = Search(outRequest,outQuery);
                    }*/

                }
                catch ( JSONException e){
                    connectionLost = true;
                    e.printStackTrace();

                }





                if(!connectionLost){
                    Log.d("线程", driverThread.getName() + "正常循环");
                    Message linkMsg = new Message();
                    linkMsg.what = GETTING_MESSAGE;
                    handler.sendMessage(linkMsg);

                    try {
                        Log.d("线程", driverThread.getName() + "睡眠"+getDataInterval);
                        driverThread.sleep(getDataInterval);
                    }
                    catch (Exception e) {
                        connectionLost = true;
                        e.printStackTrace();

                        System.out.println("thread error...connect");
                    }
                }
                else{
                    Log.d("线程", driverThread.getName() + "有问题");
                    Message linkMsg = new Message();
                    linkMsg.what = GETTING_MESSAGE_FALSE;
                    handler.sendMessage(linkMsg);

                    try {
                        Log.d("线程", driverThread.getName() + "连接有问题,休息"+connectionLost+"ms");
                        driverThread.sleep(connectionLostPause);
                    }
                    catch (Exception e) {
                        connectionLost = true;
                        e.printStackTrace();

                        System.out.println("thread error...connect");
                    }
                }
                System.out.println("线程" + Thread.currentThread().getName() + "执行完毕");

                connectionLost = false;
            }
        }
        public void StopFlag(){
            stopFlag = true;
            connectionLost= true;
        }
    };

    private JSONArray Search(String comRequest, String query){
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest + URLEncodedUtils.format(params, HTTP.UTF_8);
        Log.d("访问地址", req);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        //JSONArray json = null;
        try {
            HttpResponse res = client.execute(get);
            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = res.getEntity();
                return new JSONArray(EntityUtils.toString(entity));
                //比较获取新的msg Arraylist

            }
        } catch (Exception e) {
            connectionLost = true;
            e.printStackTrace();

        } finally {
            client.getConnectionManager().shutdown();
        }
        return null;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == GETTING_MESSAGE) {
                //接受事件中 每次接受完事件在这里更新
                //Log.d("连接中", "30s回访一次");
                TextView status = (TextView) findViewById(R.id.truck_home_Status);
                status.setText(Html.fromHtml("<font color='#00CC33'>" + "已连接</font>"));
                mListView.setAdapter(mListViewAdapter);
                tListView.setAdapter(tListViewAdaptier);
                //InvalidateMonitor();
                //getDataStatus = true;

            }

            else if (msg.what == GETTING_MESSAGE_FALSE) {
                //无法连接服务器
                Log.d("失去连接", connectionLostPause+"ms后,尝试连接");
                TextView status = (TextView) findViewById(R.id.truck_home_Status);
                status.setText(Html.fromHtml("<font color='#FFCC33'>" + "STATUS:服务器失去连接(" + getString(R.string.dbAddress) + ") 连接中..." + "</font>"));
                //getDataStatus = false;

            }
        }
    };

    public ThreadShow driverThread;

    class ListViewAdapter extends BaseAdapter {
        int count = 1;

        public int getCount() {
            if (appMsgBox != null)
                return appMsgBox.size();
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
                mTextView = new TextView(TruckHomeActivity.this);
            } else {
                mTextView = (TextView) view;
            }
            String itemStr = "无任务";

            if(appMsgBox != null) {

                if(appMsgBox.get(position).appType.equals("进仓")){
                    itemStr = "进仓编号:"+ appMsgBox.get(position).InStockID+"<br>序号:" +appMsgBox.get(position).appSeries+"<br>剩余"+appMsgBox.get(position).count+"托("+appMsgBox.get(position).appMaitou+")等待入库";
                    mTextView.setBackgroundResource(R.color.mistyrose);
                }

                if(appMsgBox.get(position).appType.equals("装箱")){
                    itemStr = "出仓|箱号:"+ appMsgBox.get(position).CSeries+"<br>序号:" +appMsgBox.get(position).appSeries+"<br>剩余"+appMsgBox.get(position).count+"托()等待装箱";
                    mTextView.setBackgroundResource(R.color.mintcream);
                }

            }
            mTextView.setPadding(2, 5, 2, 5);
            mTextView.setText(Html.fromHtml(itemStr));
            mTextView.setTextSize(20f);
            //mTextView.setHeight(200);
            return mTextView;
        }
    }

    class TraysAdapter extends BaseAdapter {
        int count = 1;

        public int getCount() {
            if (trayMsgBox != null)
                return trayMsgBox.size();
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
                mTextView = new TextView(TruckHomeActivity.this);
            } else {
                mTextView = (TextView) view;
            }
            String itemStr = "未知";

            if(trayMsgBox.get(position).type.equals("进仓")){
                //卸货区域/理货员/ 货物(唛头)件数
                //分配货位
                itemStr =trayMsgBox.get(position).outPos+"-"+trayMsgBox.get(position).OpCounterID+"-"+trayMsgBox.get(position).appName+"("+trayMsgBox.get(position).Maitou+")"+trayMsgBox.get(position).itemCount+"箱";
                itemStr += "<br>托盘目前位置:"+trayMsgBox.get(position).status+"-"+trayMsgBox.get(position).slot+"-----"+trayMsgBox.get(position).wtID;
                mTextView.setBackgroundResource(R.color.mistyrose);
            }
            if(trayMsgBox.get(position).type.equals("装箱")){
                //取货位置   货物名称(唛头) 件数
                //出厂箱位 理货员
                itemStr ="托盘目前位置:"+trayMsgBox.get(position).slot+"-"+trayMsgBox.get(position).appName+"("+trayMsgBox.get(position).Maitou+")"+trayMsgBox.get(position).itemCount+"箱";
                itemStr += "<br>"+trayMsgBox.get(position).outPos+"-"+"任意理货员";
                mTextView.setBackgroundResource(R.color.mintcream);
            }



            mTextView.setPadding(2, 5, 2, 5);
            mTextView.setText(Html.fromHtml(itemStr));
            mTextView.setTextSize(20f);
            return mTextView;
        }
    }


    //添加点击
    /*
    tListView.setOnItemClickListener(new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView <?> arg0, View arg1, int arg2,
        long arg3) {
            setTitle("点击第"+arg2+"个项目");
        }
    });*/

    @Override
    public void onResume() {
        super.onResume();
        //driverThread.start();
    }
    @Override
    public void onPause() {
        super.onPause();
        //driverThread.StopFlag();
    }
    @Override
    protected  void onStart(){
        super.onStart();
        connectionLost = false;
        driverThread = new ThreadShow();
        driverThread.start();
        Log.d("司机页面", Thread.currentThread().getName() + "线程开始");
    }
    @Override
    protected void onStop(){
        super.onStop();
        driverThread.StopFlag();
        Log.d("司机页面", Thread.currentThread().getName() + "线程停止");
    }


    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case LOAD_SAFELY:
                    Toast.makeText(getBaseContext(), (String)msg.obj, Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }

        }
    };
}
