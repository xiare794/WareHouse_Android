package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gpd.sdk.service.GpdService;

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

import java.util.Date;
//import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by Administrator on 2015/8/5.
 */
public class AppsOutActivity extends Activity {
    private JSONObject json_ScanedTrays;   //扫描出来的托盘
    private JSONObject jsonAppCon;         //本库单的库单和集装箱信息
    private JSONArray jsonTrays;           //该出库的托盘信息
    private JSONArray jsonUsers;           //用户信息
    private JSONArray jsonAppIns;          //对应入库单信息
    private JSONArray jsonDrivers;         //对应司机信息



    private String agentID = "";
    private String agentName = "";
    private int m_Driver;   //叉车司机id
    private String m_Driver_Name;
    //private String scaned_wtID;

    private int appOutID =0; //页面id


    //扫描类型
    private int mScanType;
    private String TrayCodes ="";
    private boolean stopFlag = false;

    //常量
    static final int UPDATE_CREATE =  100; //起始ui刷新
    static final int SCAN_RESPONSE = 101;
    static final int FORK_SUCCESS = 102;
    static final int FORK_FAIL = 103;
    static final  int CONNECTION_LOST = 104;
    static final int UNLOAD_SUCCESS = 105;
    static final int COMPLETE_SUCCUSS = 106;
    static final int COMPLETE_FAIL= 107;


    //对话框
    AlertDialog driverAD;
    AlertDialog trayActAD;
    private AlertDialog appCompleteAD = null;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();

        setContentView(R.layout.activity_appout_page);
        //TextView tv_tmp = (TextView)findViewById(R.id.apo_Title);
        //tv_tmp.setText("装箱清单"+i.getExtras().getString("appID"));

        appOutID = Integer.parseInt(i.getExtras().getString("appOutID"));

        new Thread(){
            @Override
            public void run() {
                if(!stopFlag) {
                    Log.d("线程", Thread.currentThread().getName() + " run()" + ";stopFlag" + stopFlag);
                    getAppsData();
                    Message msg = new Message();
                    msg.what = UPDATE_CREATE;
                    handler.sendMessage(msg);
                    Log.d("Thread线程启动", "AppOut 的创建线程");
                }
            }

        }.start();


        Button trkBtn = (Button)findViewById(R.id.apo_truckBtn);
        trkBtn.setOnClickListener(new Button.OnClickListener(){
            public  void onClick(View v){


                Log.d("按钮点击", "弹出对话框，选择叉车人员司机");
                if (driverAD == null) {
                    truckChosseAD_init();
                }
                driverAD.show();

            }
        });



        Button completeBtn = (Button)findViewById(R.id.apo_completeBtn);
        completeBtn.setOnClickListener(new Button.OnClickListener(){
            public  void onClick(View v){
                Log.d("按钮点击","尝试完成App对话框");
                //Log.d("完成度","预入数量"+m_preCount+";数完的数量"+m_loaded+";托盘数量"+m_traysInApp+";货架托盘数量"+m_traysInShelf);
                //Log.d("上货架度","尝试");

                //userID actType InStockID appID actContent
                //Log.d("addAct","userID"+MyPreference.getInstance(mContext).getUserID()+"\nactType:changePreCount"+appID+"|"+InStockID);
                //Log.d("addAct",MyPreference.getInstance(mContext).getLoginName()+"将"+appID+"货单数量从"+m_preCount+"改为"+m_loaded);
                if( jsonTrays.length()==0) {
                    if (jsonAppIns != null) {
                        Log.d("appIns", jsonAppIns.toString());
                        appComplete_init();
                        appCompleteAD.show();
                    }
                }
            }
        });
    }


    void appComplete_init(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("完成货单请求");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        String msg = "";
        try{
            msg += "装箱单\n箱号:"+jsonAppCon.getString("wCSeries");
            msg += "\n提单号:"+ jsonAppCon.getString("wCTiDan");
            msg += "\n尝试完成装箱，请确认";

        }
        catch (JSONException e){
            e.printStackTrace();
            msg = "出错"+e.getMessage().toString();
        }
        builder.setMessage(msg);

        builder.setPositiveButton("请求完成", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread() {
                    @Override
                    public void run() {
                        Log.d("确认更改预入数量", "询问服务器");
                        Log.d("完成前还需要", "输入价格");
                        // appID;

                        Message msg = new Message();
                        if (ApplyAppInComplete(appOutID)) {
                            msg.what = COMPLETE_SUCCUSS;
                            //添加消息
                            MessageUtils mu = new MessageUtils(getString(R.string.dbAddress));
                            String para =MyPreference.getInstance(mContext).getUserID()+"|||||"+MyPreference.getInstance(mContext).getLoginName()+"完成了"+appOutID+"出库货单的装箱操作,箱号";
                            //UserID|InstockID|trayID|slotID|appID|actContent
                            mu.AddMessage(4,para);

                        } else {
                            msg.what = COMPLETE_FAIL;
                        }
                        handler.sendMessage(msg);
                    }
                }.start();
                dialog.dismiss();
            }
        });

        appCompleteAD = builder.create();
    }

    //确认入库完成 (货品全部上架，货物数量也对齐)
    public boolean ApplyAppInComplete(int _outAppID){
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("table","wAppOut"));
        params.add(new BasicNameValuePair("tKey","appStatus"));
        params.add(new BasicNameValuePair("tVal","2"));
        params.add(new BasicNameValuePair("idKey","wAppID"));
        params.add(new BasicNameValuePair("idVal",String.valueOf(_outAppID) ));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改出库单状态结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }

    @Override
    protected void onStart(){
        super.onStart();

        AppUtils.startService(this, mScanType);
        mScanType = GpdService.DEVICE_TYPE_RFID;
        GpdService.setActiveScanDevice(mScanType);
        GpdService.setScanHandler(mScanHandler);
        Log.d("apo页面","开始,scan服务启动");
    }

    @Override
    protected  void onResume(){
        super.onResume();
        Log.d("apo页面","恢复");
    }

    @Override
    protected  void onStop(){
        super.onStop();
        GpdService.removeScanHandler(mScanHandler);
        AppUtils.stopService();
        Log.d("apo页面", "开始,scan服务停止");
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_CREATE:

                    refreshPage();
                    break;
                case SCAN_RESPONSE:
                    break;
                case FORK_SUCCESS:
                    Toast.makeText(getBaseContext(), "叉车司机指定完成", Toast.LENGTH_SHORT).show();
                    TextView tmp = (TextView)findViewById(R.id.apo_OpFork);
                    tmp.setText("叉车:"+m_Driver_Name);
                    break;
                case FORK_FAIL:
                    Toast.makeText(getBaseContext(), "叉车司机指定失败", Toast.LENGTH_SHORT).show();
                    break;
                case CONNECTION_LOST:
                    Toast.makeText(AppsOutActivity.this,"网络出错"+getString(R.string.dbAddress)+"无法访问",Toast.LENGTH_LONG).show();
                    break;
                case UNLOAD_SUCCESS:
                    Toast.makeText(AppsOutActivity.this,"成功装箱一个托盘",Toast.LENGTH_LONG).show();
                    refreshPage();
                    break;
                case COMPLETE_SUCCUSS:
                    Toast.makeText(AppsOutActivity.this,"成功装箱",Toast.LENGTH_LONG).show();
                case COMPLETE_FAIL:
                    Toast.makeText(AppsOutActivity.this,"装箱失败",Toast.LENGTH_LONG).show();

                default:
                    break;
            }

        }
    };
    private Handler mScanHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d("ap页面扫描响应","apo页面扫描响应"+msg.what);
            switch (msg.what) {
                case GpdService.MSG_SCAN_BARCODE:
                    String barcode = (String) msg.obj;
                    break;
                case GpdService.MSG_RFID_TAG_ID:
                    String tagId = (String) msg.obj;

                    if (tagId.length() > 0) {
                        String scanRet = ScanResponse(tagId);
                        if (scanRet.equals("true")) {
                            Log.d("扫描到托盘", json_ScanedTrays.toString());
                            appTrayStatus_init();
                            trayActAD.show();
                        }
                        else {
                            Log.d("未登记托盘", scanRet);
                            Toast.makeText(getBaseContext(),"扫描到未注册托盘,本页面不支持绑定",Toast.LENGTH_LONG).show();
                            //RegisterTrays_init(scanRet);
                            //trayActAD.show();
                        }
                    } else {
                        Toast.makeText(getBaseContext(),"没扫描到任何托盘",Toast.LENGTH_LONG).show();
                        trayActAD.setMessage("未扫描到托盘,重新扫描");
                    }


                    break;

                default:
                    super.handleMessage(msg);
            }
        }

    };

    public  void refreshPage(){
        if(jsonAppCon != null) {
            try {
                TextView tmp = (TextView) findViewById(R.id.apo_Title);
                tmp.setText("装箱清单:" + jsonAppCon.getString("series"));   //出厂编号
                tmp = (TextView) findViewById(R.id.apo_agentName);
                tmp.setText(agentName);                                    //货代名
                tmp = (TextView) findViewById(R.id.apo_cType);
                tmp.setText(jsonAppCon.getString("wCType"));
                tmp = (TextView) findViewById(R.id.apo_wCSeries);
                tmp.setText(jsonAppCon.getString("wCSeries"));
                tmp = (TextView) findViewById(R.id.apo_wCSeal);
                tmp.setText(jsonAppCon.getString("wCSeal"));
                tmp = (TextView) findViewById(R.id.apo_fee);
                String appsStr = "装箱列表:<br>";
                appsStr += "货代公司 - - 唛头 - - 数量:<br>";
                //Log.d("jsonAppIns",jsonAppIns.length()+jsonAppIns.toString());
                for (int i = 0; i < jsonAppIns.length(); i++) {
                    appsStr += cutShort(jsonAppIns.getJSONObject(i).getString("deliverComp"), 7) + "- -" + jsonAppIns.getJSONObject(i).getString("appMaitou") + "- -" + jsonAppIns.getJSONObject(i).getString("appCount") + "箱<br>";
                }
                tmp.setText(Html.fromHtml(appsStr));

                LinearLayout apo_trayList = (LinearLayout) findViewById(R.id.traysView);

                for (int i = 0; i < jsonTrays.length(); i++) {
                    TextView text = new TextView(this);
                    String str = jsonTrays.getJSONObject(i).getString("wtID") + "-状态:" + jsonTrays.getJSONObject(i).getString("twStatus") + "-仓位:" + jsonTrays.getJSONObject(i).getString("wSlotID");
                    text.setText(str);
                    Log.i("------", text.toString());
                    apo_trayList.addView(text);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private String ScanResponse(String tag){
        //分解tag;只用第一个
        String[] rfids = tag.split(";");
        String rfidquery = "";

        //改双卡
        rfidquery += " `rfid` = \""+rfids[0]+"\" "+"OR `vrfid` = \""+rfids[0]+"\"";

        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wTrays` WHERE"+rfidquery;
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        Log.d("appPage页面 TraysLoad",comRequest+query);
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        json_ScanedTrays = getFirst(req);


        if(json_ScanedTrays.length()>0)
            return "true";
        else
            return tag;
    }

    public void getAppsData(){
        //获得appout和container
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wAppOut` o, `wContainers` c WHERE o.wAppID =\""+appOutID+"\" AND o.containerID = c.wCID";
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        jsonAppCon = getFirst(req);
        if(jsonAppCon == null)
            return;
        //Log.d("jsonAppcon",jsonAppCon.toString());

        //获得所有需要出库的托盘和货物
        query = "SELECT * FROM `wTrays`WHERE wtAppOutID = \""+appOutID+"\" ";
        comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        //参数
        params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        jsonTrays = get(req);
        Log.d("trays",jsonTrays.toString());

        //获得姓名
        query = "SELECT * FROM `wUsers`WHERE 1 ";
        comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        jsonUsers = get(req);
        Log.d("us",jsonUsers.toString());

        //获得司机
        query = "SELECT * FROM `wUsers`WHERE `job`=5";
        comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        jsonDrivers = get(req);
        Log.d("jsonDrivers",jsonDrivers.toString());

        //获得代理商名
        try{
            agentID = jsonAppCon.getString("agentID");
            query = "SELECT * FROM `wAgents`WHERE waID ="+agentID;
            comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
            params = new ArrayList<>();
            params.add(new BasicNameValuePair("query",query));
            req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
            JSONObject agent = getFirst(req);
            agentName = agent.getString("waName");
            Log.d("agent",agentName);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        //获得一堆入库单
        try{
            query = "SELECT * FROM `wAppIn` WHERE ";
            String [] appsInStr = jsonAppCon.getString("appIns").split(":");
            for(int i=0; i<appsInStr.length; i++){
                query += " `appID`="+appsInStr[i];
                if(i<(appsInStr.length-1))
                    query += " OR ";
            }
            comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
            params = new ArrayList<>();
            params.add(new BasicNameValuePair("query",query));
            req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
            //Log.d("真实地址",req.toString());
            jsonAppIns = get(req);
            Log.d("jsonAppIns",jsonAppIns.toString());
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }


    }

    //通过地址返回JSONArray
    public static JSONArray get(String url){
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        JSONArray json = null;
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                json = new JSONArray(EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return json;
    }

    //通过地址返回JSONArray
    public JSONObject getFirst(String url){
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        JSONArray json = null;
        JSONObject js = null;
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                json = new JSONArray(EntityUtils.toString(entity));
                js = json.getJSONObject(0);
            }
        }catch (Exception e){
            Message msg = new Message();
            msg.what = CONNECTION_LOST;
            handler.sendMessage(msg);
            //throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return js;
    }


    private String cutShort(String tar, int max){
        return tar.length()>max?tar.substring(0,max-1)+"..":tar;
    }

    /*ad2初始化*/
    void truckChosseAD_init(){
        int driverIdx = 0;
        final Spinner mSpinner = new Spinner(this);
        final String[] mItems ;
        try{
            if(jsonDrivers.length() == 0){
                mItems = new String[]{"没有可选司机"};
            }
            else {
                mItems = new String[jsonDrivers.length()];
                for (int i = 0; i < jsonDrivers.length(); i++) {
                    mItems[i] = jsonDrivers.getJSONObject(i).getString("wuName");
                    //如果目前选中司机的id在司机中，将spinner的选择项移动到已存司机位置
                    int _userID = jsonDrivers.getJSONObject(i).getInt("userID");
                    if(m_Driver!=0 && _userID==m_Driver )
                    {
                        m_Driver_Name = mItems[i];
                        driverIdx = i;
                    }
                }
            }
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }

        AlertDialog.Builder dbuilder = new AlertDialog.Builder(this);
        dbuilder.setTitle("选择叉车司机");
        dbuilder.setIcon(android.R.drawable.ic_dialog_info);

        if(mItems.length != 0) {
            dbuilder.setSingleChoiceItems(mItems, driverIdx,
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            m_Driver_Name =  mItems[which];
                            new Thread() {
                                @Override
                                public void run() {
                                    Log.d("叉车司机修改","启动"+m_Driver_Name);

                                    Message msg = new Message();
                                    if (changeFork(appOutID, m_Driver_Name)) {
                                        msg.what = FORK_SUCCESS;
                                        //添加消息
                                        MessageUtils mu = new MessageUtils(getString(R.string.dbAddress));
                                        String para = MyPreference.getInstance(AppsOutActivity.this).getUserID() + "|"  + "|||" + appOutID + "|" + MyPreference.getInstance(AppsOutActivity.this).getLoginName() + "给" +appOutID + "出库货单指定了司机" + m_Driver_Name;
                                        Log.d("消息", para);
                                        //InstockID|trayID|slotID|appID|actContent
                                        mu.AddMessage(0, para);

                                    } else {
                                        msg.what = FORK_FAIL;
                                    }
                                    handler.sendMessage(msg);

                                    Log.d("叉车司机修改","结束"+m_Driver_Name);
                                }
                            }.start();
                            dialog.dismiss();
                        }
                    }
            );
        }
        else {
            dbuilder.setMessage("没有可选司机");
        }

        dbuilder.setNegativeButton("取消",null);
        driverAD = dbuilder.create();

    }

    public boolean changeFork(int _appID, String forkName){
        int selForkUserID = 0;
        try{
            if(jsonDrivers.length()>0)
            {
                for(int i = 0; i<jsonDrivers.length(); i++){
                    if(jsonDrivers.getJSONObject(i).getString("wuName").equals(forkName)){
                        selForkUserID = Integer.parseInt(jsonDrivers.getJSONObject(i).getString("userID"));
                        break;
                    }
                }
            }
            else{
                Log.d("没有叉车司机可选","错误");
                return false;
            }
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }

        if(selForkUserID != 0){
            String url = getString(R.string.dbAddress);
            String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("table","wAppOut"));
            params.add(new BasicNameValuePair("tKey","OpFork"));
            params.add(new BasicNameValuePair("tVal",selForkUserID+""));
            params.add(new BasicNameValuePair("idKey","wAppID"));
            params.add(new BasicNameValuePair("idVal",_appID+"" ));
            String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(req);
            Log.d("修改司机url",req);
            try{
                HttpResponse res = client.execute(get);

                if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    HttpEntity entity = res.getEntity();
                    Log.d("修改司机结果",EntityUtils.toString(entity));
                }
            }catch (Exception e){
                Log.d("修改司机结果", "出错");
                e.printStackTrace();
                return false;
                //throw new RuntimeException(e);
            }finally {
                client.getConnectionManager().shutdown();
            }
            return true;
        }
        Log.d("没有forkUserID","错误");
        return false;
    }

    void appTrayStatus_init(){
        if(trayActAD != null)
            trayActAD.dismiss();
        //预入托盘数量不符合
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Html.fromHtml("<font color='#FF7F27'>托盘</font>"));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        int trayOutID = 0;
        //List<String> data = new ArrayList<String>();
        String str = "";
        try{
            String scanWtID = json_ScanedTrays.getString("wtAppOutID");

            if( !scanWtID.equals("")){
                trayOutID = Integer.parseInt(scanWtID);
            }

            if( trayOutID ==appOutID){
                //如果扫描到托盘属于出库单;
                String ts = json_ScanedTrays.getString("twStatus");
                if(ts.equals("仓库外")){
                    //考虑进行出仓、解除绑定操作
                    str += json_ScanedTrays.getString("wtID")+"号托盘\n";
                    str += "货物"+json_ScanedTrays.getString("twWareCount")+"箱\n";
                    str += "是否准备好装进箱号为\n"+jsonAppCon.getString("wCSeries")+"并出库";
                }
                else{
                    str += json_ScanedTrays.getString("wtID")+"号托盘,等待出库\n";
                    str += "货物"+json_ScanedTrays.getString("twWareCount")+"箱\n";
                    str += "货物位置\n"+jsonAppCon.getString("wCSeries")+"并出库";
                }
            }
            else{
                str += "不属于此装箱单，"+json_ScanedTrays.getString("wtID")+"号托盘";
                //扫描到托盘不属于出库单;
            }

        }
        catch (JSONException e){
            str += e.getMessage();
            e.printStackTrace();
            //throw new RuntimeException(e);
        }

        builder.setMessage(str);
        builder.setNegativeButton("取消",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("装箱",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("装箱",appOutID+"装箱单"+json_ScanedTrays.toString()+"托盘");
                //根据装箱单 jsonAppCon
                try {
                    int count = jsonAppCon.getInt("count");//Integer.parseInt(
                    float vol = jsonAppCon.getLong("volume");
                    Log.d("count", count + "-"+vol);
                    int tcount = json_ScanedTrays.getInt("twWareCount");
                    //装箱单appout count volume
                    UpdateAppOut(appOutID,count+tcount);
                    //货品wuint appOutID trayID updateTime
                    String wtID = json_ScanedTrays.getString("wtID");
                    UpdateUint(wtID,appOutID); //去掉trayID，更新时间，更新appOotID
                    //托盘 twStatus wtAppID wtAppOutID twWareCount updateTime
                    UpdateTray(wtID);

                    MessageUtils mu = new MessageUtils(getString(R.string.dbAddress));
                    String para = MyPreference.getInstance(mContext).getUserID() + "||" + wtID + "|||" + MyPreference.getInstance(mContext).getLoginName() + "装箱("+jsonAppCon.getString("wCSeries")+")装箱，加载"+tcount+"箱货物,托盘解除绑定";
                    //UserID|InstockID|trayID|slotID|appID|actContent
                    mu.AddMessage(7, para);

                    getAppsData();
                    Message msg = new Message();
                    msg.what = UNLOAD_SUCCESS;
                    handler.sendMessage(msg);
                }
                catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });

        trayActAD = builder.create();
    }

    public boolean UpdateAppOut(int outID, int count){

        String query = "UPDATE `wAppOut` SET `count`="+count+" WHERE `wAppID`="+outID;
        Log.d("updateAppOut",query);
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("updateQuery",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改appOut结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }

    public boolean UpdateUint(String wtID, int appOutID ){

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d("当前时间", df.format(new Date()));


        String query = "UPDATE `wUnit` SET `appOutID`="+appOutID+", `trayID`=0, `updateTime`=\""+df.format(new Date())+ "\" WHERE `trayID`="+wtID;
        Log.d("UpdateUint------",query);
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("updateQuery",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改Unit结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }

    //托盘 twStatus wtAppID wtAppOutID twWareCount updateTime
    public boolean UpdateTray(String wtID ){

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d("当前时间",df.format(new Date()));

        String query = "UPDATE `wTrays` SET `twStatus`=\"空闲\", `wtAppID`=0, `wtAppOutID`=0, `twWareCount`=0, `updateTime`=\""+df.format(new Date())+ "\" WHERE `wtID`="+wtID;
        Log.d("UpdateTray-----",query);
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("updateQuery",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改Tray结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }

}
