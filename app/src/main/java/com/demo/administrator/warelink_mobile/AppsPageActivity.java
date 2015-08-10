package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/5/14.
 */
public class AppsPageActivity extends Activity{
    public JSONArray jsonData_trays; //获取的托盘数据;
    public JSONArray jsonData_uints; //获取的物品数据;
    public JSONArray _drivers; //司机
    public JSONArray _users; //所有用户
    public JSONObject jsonData_appIn;
    public Map<String,String> map_slots; //获取的物品数据;
    public JSONArray jsonData_tidy; //获取的物品数据;
    public JSONArray json_ScanedTrays; //点击扫描，扫描到的托盘 可能每次会变，可能为空

    static final int LOAD_SUCCESS = 1; //获取所有信息成功
    static final int UPDATEAPPINFO = 100; //通过appID获取货单成功
    static final int MAITOU_SUCCESS = 101; //修改唛头成功
    static final int MAITOU_FALSE = 102; //修改唛头失败
    static final int FORK_SUCCESS = 103; //修改叉车司机成功
    static final int FORK_FALSE = 104;// 修改叉车司机失败
    static final int PRECOUNT_C_SUCCESS = 105; //修改预入数量成功
    static final int PRECOUNT_C_FAIL = 106; //修改预入数量失败
    static final int COMPLETE_SUCCUSS = 107;
    static final int COMPLETE_FAIL = 108;
    static final int CALCULATE_FEE = 109;
    static final int REGISTER_TRAY = 110;
    static final int CONNECTION_LOST = 111;

    private int appID = 0;
    private String InStockID = "";
    private String appName = "";
    private String et_Maitou = "";

    private int m_Driver;   //叉车司机id
    private int m_Counter; //理货员ID
    private String m_Driver_Name;
    private String m_Counter_Name;
    private int fee_total = 0;
    private String wtID = ""; //点击的托盘ID;
    private String TrayStatus = "未知";

    private int m_loaded = 0; //数完的货物
    private int m_preCount = 0; //预入的数量
    private int m_traysInApp = 0; //货单内托盘数量
    private int m_traysInShelf = 0; //货单内托盘在货架数量

    private AlertDialog maitouAD=null;
    private AlertDialog driverAD = null;
    private AlertDialog appCompleteAD = null;
    private AlertDialog priceAD = null;
    private AlertDialog trayListAD = null;
    private AlertDialog addVRFID = null;
    private String singleWtID = "";
    private boolean setSingleVRFIDLock = true;
    private String vRfid = "";
    private Context mContext;

    //费用
    final String[] feeStr = new String[] { "卸货费", "打单费", "托盘费", "自定义" };
    final String[] feeHint = new String[]{"15元","25元","1元/托","30元"};
    final boolean[] feeSelect = new boolean[] {true, true, true, false};
    final int[] feeInt = new int[]{15,25,1,30};

    //扫描类型
    private int mScanType;
    private String TrayCodes ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        //获取入库单
        setContentView(R.layout.activity_app_page);
        TextView tv_tmp = (TextView)findViewById(R.id.textView2);
        tv_tmp.setText("目前进入货单"+i.getExtras().getString("appID"));

        appID = Integer.parseInt(i.getExtras().getString("appID"));

        ValidateUpdate();


        //添加dialog

        Button Btn2 = (Button)findViewById(R.id.maitouBtn);
        Btn2.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v){
                Log.d("唛头btn","点击");
                if(maitouAD==null) {
                    maitouAD_init();
                }
                maitouAD.show();
            }
        });


        Button trkBtn = (Button)findViewById(R.id.truckBtn);
        trkBtn.setOnClickListener(new Button.OnClickListener(){
            public  void onClick(View v){
                Log.d("按钮点击","弹出对话框，选择叉车人员司机");
                if(driverAD==null){
                    truckChosseAD_init();
                }
                driverAD.show();

            }
        });
        final Button priceBtn = (Button)findViewById(R.id.priceBtn);
        priceBtn.setOnClickListener(new Button.OnClickListener(){
            public  void onClick(View v){
                Log.d("按钮点击","弹出对话框，选择费用");
                if(priceAD==null){
                    priceAD_init();
                }
                priceAD.show();

            }
        });
        Button completeBtn = (Button)findViewById(R.id.completeBtn);
        completeBtn.setOnClickListener(new Button.OnClickListener(){
            public  void onClick(View v){
                Log.d("按钮点击","尝试完成App对话框");
                Log.d("完成度","预入数量"+m_preCount+";数完的数量"+m_loaded+";托盘数量"+m_traysInApp+";货架托盘数量"+m_traysInShelf);
                Log.d("上货架度","尝试");

                //userID actType InStockID appID actContent
                Log.d("addAct","userID"+MyPreference.getInstance(mContext).getUserID()+"\nactType:changePreCount"+appID+"|"+InStockID);
                //Log.d("addAct",MyPreference.getInstance(mContext).getLoginName()+"将"+appID+"货单数量从"+m_preCount+"改为"+m_loaded);
                if(m_preCount != m_loaded){
                    Toast.makeText(getBaseContext(),"入库数量不符", Toast.LENGTH_SHORT).show();
                    //预入数量更改 数量过关
                    appCountChange_init();
                    if(appCompleteAD != null)
                        appCompleteAD.show();

                }
                else if(m_traysInApp != m_traysInShelf){
                    Toast.makeText(getBaseContext(),"还有托盘未完成上架", Toast.LENGTH_SHORT).show();
                    //如果不是所有托盘都在货架上
                    appTrayStatus_init(false);
                    if(appCompleteAD != null)
                        appCompleteAD.show();

                }
                else{
                    if(fee_total == 0){
                        //费用已生成
                        Toast.makeText(getBaseContext(),"请求生成费用", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        //请求完成
                        appComplete_init();
                        if(appCompleteAD != null)
                            appCompleteAD.show();

                    }
                }

            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        if(trayListAD==null)
            appScanTray_init();

        Log.i("AP页面 恢复", "AP页面 恢复 扫描服务开启.");
        AppUtils.startService(this, mScanType);
        mScanType = GpdService.DEVICE_TYPE_RFID;
        GpdService.setActiveScanDevice(mScanType);
        GpdService.setScanHandler(mScanHandler);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();

    }
    @Override
    protected void onStop() {
        super.onStop();
        GpdService.removeScanHandler(mScanHandler);
        AppUtils.stopService();
        //GpdService.stopService();
        Log.i("AP页面 暂停", "扫描服务关闭 onDestory called.");
        new Thread() {
            @Override
            public void run() {
                Log.d("AP页面 暂停", "刷新app" + appID + "的实入数量" + m_loaded + ",结果" + ChangeAppCount(appID, m_loaded));
            }
        }.start();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }





    private Handler mScanHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d("ap页面扫描响应","ap页面扫描响应"+msg.what);
            switch (msg.what) {
                case GpdService.MSG_SCAN_BARCODE:
                    String barcode = (String) msg.obj;
                    //onScanBarcode(barcode);
                    break;
                case GpdService.MSG_RFID_TAG_ID:
                    String tagId = (String) msg.obj;

                    if (tagId.length() > 0) {
                        String scanRet = ScanResponse(tagId);
                        if (scanRet.equals("true")) {
                            Log.d("扫描到托盘", json_ScanedTrays.toString());
                            appTrayStatus_init(true);
                            trayListAD.show();
                        }
                        else {
                            Log.d("未登记托盘", scanRet);
                            RegisterTrays_init(scanRet);
                            trayListAD.show();
                        }
                    } else {
                        trayListAD.setMessage("未扫描到托盘,重新扫描");
                    }


                    break;

                default:
                    super.handleMessage(msg);
            }
        }

    };

    private String ScanResponse(String tag){
        //tag 是gopando扫描所得 多个rfid以;划分
        //json_ScanedTrays
        //分解tag
        String[] rfids = tag.split(";");
        String rfidquery = "";
        /*
        for(int i=0; i<rfids.length; i++){
            rfidquery += " `rfid` = \""+rfids[i]+"\" ";
            if(i<rfids.length-1)
                rfidquery += "OR";
        }*/
        //改双卡
        for(int i=0; i<rfids.length; i++){
            rfidquery += " `rfid` = \""+rfids[i]+"\" "+"OR `vrfid` = \""+rfids[i]+"\"";
            if(i<rfids.length-1)
                rfidquery += "OR";
        }

        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wTrays` WHERE"+rfidquery;
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        Log.d("appPage页面 TraysLoad",comRequest+query);
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        json_ScanedTrays = get(req);

        //如果length == 1 并且vrfid为空
        /*
        if(json_ScanedTrays.length() == 1){
            try{
                JSONObject obj = json_ScanedTrays.getJSONObject(0);
                if( obj.getString("vrfid").equals("")){
                    Toast.makeText(getBaseContext(),"没有另一侧卡片信息，请先注册",Toast.LENGTH_LONG).show();
                    singleWtID = obj.getString("wtID");
                    //return "single";
                    AddVRfid_init(obj.getString("wtID"),obj.getString("rfid"));
                    addVRFID.show();
                    //改双卡，跳转
                }
            }
            catch (JSONException e){
                throw new RuntimeException(e);
            }
        }*/
        if(json_ScanedTrays.length()>0)
            return "true";
        else
            return tag;
    }


    private void ValidateUpdate(){
        //查询信息重新更新页面
        new Thread(){
            @Override
            public void run() {
                //获取所有用户
                getUsers();
                if(_users == null)
                    return;
                //获取司机信息；
                getDrivers();
                //获取数据
                //获取价格
                getFee();
                jsonData_appIn = getApp(appID);
                try {
                    et_Maitou = jsonData_appIn.getString("appMaitou");
                    m_preCount = jsonData_appIn.getInt("appPreCount");
                    //Log.e("错误发生地",jsonData_appIn.get("OpFork").toString());
                    //if(jsonData_appIn.getString("OpFork")
                    m_Driver = jsonData_appIn.getInt("OpFork");
                    m_Counter = jsonData_appIn.getInt("OpCounter");
                    Log.d("理货员",m_Counter+"");
                    for(int i = 0; i<_users.length(); i++){
                        if(_users.getJSONObject(i).getInt("userID") == m_Driver)
                            m_Driver_Name = _users.getJSONObject(i).getString("wuName");
                        if(_users.getJSONObject(i).getInt("userID") == m_Counter )
                            m_Counter_Name = _users.getJSONObject(i).getString("wuName");
                    }
                }
                catch (JSONException e){
                    throw new RuntimeException(e);
                }

                //获取货单信息
                jsonData_trays = getTraysData(appID); //获取托盘信息 依靠appID;
                getSlotData();  //获取仓位信息

                Log.d("Thread线程启动", "AppsPageActivity的线程启动");
                m_traysInApp = jsonData_trays.length();
                m_traysInShelf = 0;
                m_loaded = 0;
                if(jsonData_trays.length()>0){
                    Log.d("线程中获取","托盘数为正请求物品数据");
                    int[] wtIDs = new int[jsonData_trays.length()];
                    try {
                        for (int i = 0; i < jsonData_trays.length(); i++) {
                            wtIDs[i] = jsonData_trays.getJSONObject(i).getInt("wtID");
                            m_loaded += jsonData_trays.getJSONObject(i).getInt("twWareCount");
                            if(jsonData_trays.getJSONObject(i).getString("twStatus").equals("货架"))
                                m_traysInShelf ++;
                            //query += jsonData_trays.getJSONObject(i).getInt("wtID");
                        }
                    }
                    catch (JSONException e){
                        throw new RuntimeException(e);
                    }
                    Log.d("LOADED装载的货物总数",m_loaded+"");
                    //Log.d("线程中获取","托盘query"+wtIDs);

                    jsonData_uints = getItemsData(wtIDs);
                    Log.d("测试物品长度",jsonData_uints.length()+"个货物");

                }
                //获取数据成功后，去hanlder刷新界面
                Message msg = new Message();
                msg.what = LOAD_SUCCESS;
                handler.sendMessage(msg);

            }

        }.start();

    }
    void appScanTray_init(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("扫描到下列托盘");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setMessage("空");
        builder.setPositiveButton("确认",null);
        trayListAD = builder.create();
    }

    /*
    void AddVRfid_init(String wtID,String rfid){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("为托盘"+wtID+"绑定另一侧芯片");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setMessage("托盘主卡信息:"+rfid+"\n副卡信息:");

        builder.setPositiveButton("确认",null);
        addVRFID = builder.create();

    }*/

    void appTrayStatus_init(boolean scaned){
        trayListAD.dismiss();
        //预入托盘数量不符合
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Html.fromHtml("<font color='#FF7F27'>扫描到</font>"));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        if(scaned)
        {
            List<String> data = new ArrayList<String>();
            try{
                if(json_ScanedTrays.length()>0){
                    for(int i=0; i<json_ScanedTrays.length(); i++){
                        JSONObject tmp = json_ScanedTrays.getJSONObject(i);
                        String str = tmp.getString("wtID")+"-";
                        wtID =tmp.getString("wtID");
                        if(tmp.getString("twStatus").equals("空闲")) {

                            str += "空闲托盘\n请点击添加货物";
                            TrayStatus = "空闲";
                        }
                        else {
                            str += "已占用托盘\n";
                            TrayStatus = "不可用";
                            if(tmp.getString("wSlotID").equals("0"))
                                str+= tmp.getString("twStatus");
                            else
                                str+= tmp.getString("wSlotID")+"仓位";
                            str += "-已存储"+tmp.getString("twWareCount")+"箱货物";
                        }


                        data.add(str);
                    }

                }
            }
            catch (JSONException e){
                throw new RuntimeException(e);
            }

            String[] items = new String[data.size()];
            Log.d("此时Data",data.toString());
            data.toArray(items);
            //int a = 78;
            //data.toArray(items);
            Log.d("数组转换",items.toString());
            Log.d("数组转换",data.size()+items[0]);
            Log.d("字符串",items[0]);

            //builder.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,items));

            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {


                    Log.d("点击","点击了"+item);
                    //Toast.makeText(getApplicationContext(), "就一个测试" + item, Toast.LENGTH_SHORT).show();
                    Log.d("点击托盘数据",appID+"|"+jsonData_tidy.toString()+"|"+InStockID+"|"+wtID);
                    if(TrayStatus.equals("空闲")) {
                        Intent i = new Intent(getApplicationContext(), PageRespActivity.class);
                        i.putExtra("appID", appID);
                        i.putExtra("appName", appName);
                        i.putExtra("traysInfo", jsonData_tidy.toString());
                        i.putExtra("InStockID", InStockID);
                        i.putExtra("wtID", wtID);
                        i.putExtra("TrayStatus", TrayStatus);
                        startActivity(i);
                    }
                    else{
                        dialog.dismiss();
                    }
                }
            });
        }
        else {
            builder.setMessage("按照实际理货数量登记\n已整理托盘:" + m_traysInApp + "托\n货架中托盘:" + m_traysInShelf + "托\n请调整托盘或等待托盘全部进入货架后完成");
        }
        builder.setPositiveButton("取消",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        trayListAD = builder.create();
    }

    void RegisterTrays_init(String traysCode){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Html.fromHtml("<font color='#FF7F27'>注册托盘</font>"));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        String[]trays = traysCode.split(";");
        String str = "扫描到未注册的托盘\n";
        Log.d("扫描到未注册的托盘",trays.toString());
        for(int i=0; i<trays.length; i++){
            str += trays[i]+"\n";
        }
        str += "是否注册以上"+trays.length+"个托盘";
        builder.setMessage(str);
        TrayCodes = traysCode;
        builder.setPositiveButton("注册",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread() {
                    @Override
                    public void run() {

                        if(RegisterTray()) {
                            MessageUtils mu = new MessageUtils(getString(R.string.dbAddress));
                            String para = MyPreference.getInstance(mContext).getUserID() + "|" + InStockID + "|||" + appID + "|" + MyPreference.getInstance(mContext).getLoginName() + "注册了新托盘";
                            //UserID|InstockID|trayID|slotID|appID|actContent
                            mu.AddMessage(5, para);
                            Message msg = new Message();
                            msg.what  = REGISTER_TRAY;
                            handler.sendMessage(msg);
                        }
                        else{
                            Log.e("注册托盘失败？",""+TrayCodes);
                        }
                    }
                }.start();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        trayListAD = builder.create();
    }
    boolean RegisterTray(){
        //更改货单已入数量
        Log.d("注册托盘信息",TrayCodes);
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("registerRfid",TrayCodes));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        Log.d("注册托盘url",req);
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            //Log.d("添加托盘返回",res.getStatusLine().getStatusCode()+"-返回状态");
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("注册托盘",EntityUtils.toString(entity));
                return true;

            }
        }catch (Exception e){
            e.printStackTrace();
            //Log.d("没能返回","难道走到这了？");
            //throw new RuntimeException(e);
        }finally {
            //Log.d("try结束后也会走到这里","已验证");
            client.getConnectionManager().shutdown();
        }
        return false;

    }



    void appComplete_init(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("完成货单请求");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setMessage("货单"+appName+"["+InStockID+"]\n唛头:"+et_Maitou+"\n共"+m_traysInShelf+"托货物\n费用:"+fee_total+"元");

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
                        if (ApplyAppInComplete(appID)) {
                            msg.what = COMPLETE_SUCCUSS;
                            //添加消息
                            MessageUtils mu = new MessageUtils(getString(R.string.dbAddress));
                            String para =MyPreference.getInstance(mContext).getUserID()+"|"+InStockID+"|||"+appID+"|"+MyPreference.getInstance(mContext).getLoginName()+"完成了"+appID+"货单的入库操作,入库编号"+InStockID;
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

    /* 预入数量更改 */
    void appCountChange_init(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("查询到货单实际数量不符");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage("按照实际理货数量登记\n预入数量:"+m_preCount+"\n实际数量:"+m_loaded);
        builder.setPositiveButton("确认更改", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which){
                new Thread() {
                    @Override
                    public void run() {
                        Log.d("确认更改预入数量","询问服务器");
                        // appID;

                        Message msg = new Message();
                        if(ChangePreCount(appID,m_loaded)){
                            //AddAct(appID,)
                            //userID actType InStockID appID actContent

                            msg.what = PRECOUNT_C_SUCCESS;

                        }
                        else{
                            msg.what = PRECOUNT_C_FAIL;
                        }
                        handler.sendMessage(msg);
                    }
                }.start();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消",null);
        appCompleteAD = builder.create();
    }
    /*价格显示*/
    void priceAD_init(){

        final EditText editFee = new EditText(getBaseContext());
        editFee.setHint("在此修改自定义费用");
        editFee.setInputType(InputType.TYPE_CLASS_NUMBER);

        editFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //int v = Integer.parseInt(s.toString());
                Log.d("更新字符",s.toString());
                try{
                    Integer a = Integer.parseInt(s.toString());
                    //feeStr[3] = "自定义 "+s.toString()+" 元";
                    feeHint[3] = s.toString()+"元";
                    feeInt[3] = a;
                }
                catch(Exception e){
                    System.out.println("字符串转换为整型失败");
                }
                //
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请点选费用");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setView(editFee);

        String[] ChoiceItem = new String[]{ feeStr[0]+feeHint[0], feeStr[1]+feeHint[1], feeStr[2]+feeHint[2], feeStr[3]+feeHint[3] };
        builder.setMultiChoiceItems(ChoiceItem, feeSelect, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                feeSelect[which] = isChecked;
            }
        });
        builder.setPositiveButton("确认费用更改", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread() {
                    @Override
                    public void run() {
                        Log.d("尝试添加费用",appID+"入库货单的费用");

                        if(SaveFeeForThisApp()){
                            Log.d("费用添加","成功");
                        }

                        int feeTotal = 0;
                        for (int i = 0; i < feeStr.length; i++) {
                            if (feeSelect[i]) {
                                if (i == 2) {
                                    feeTotal += feeInt[i] * m_traysInApp;
                                } else
                                    feeTotal += feeInt[i];
                            }
                        }
                        Log.d("总体费用", feeTotal + "");
                        fee_total = feeTotal;
                        Message msg = new Message();
                        msg.what = CALCULATE_FEE;
                        handler.sendMessage(msg);


                    }
                }.start();
                dialog.dismiss();
            }
        });
        builder.setNeutralButton("修改自定义费用",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //priceAD = null;
                priceAD_init();
                Toast.makeText(getBaseContext(),"自定义费用更改",Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);


        priceAD = builder.create();
    }
    //增加费用项
    private boolean SaveFeeForThisApp(){
        //Log.d("feeInt",feeInt.toString());
        //Log.d("feeStr",feeStr.toString());
        //Log.d("feeSelect",feeSelect.toString());

        //`wfAppID`,`wfAppType`,`wfValue`,`wfName`
        String FeeStr ="";
        for(int i=0; i<feeInt.length; i++){
            //Log.d(feeStr[i],feeSelect[i]+"");
            if(feeSelect[i]) {
                //Log.d("费用" + i, appID + "|入库" + feeStr[i] + "|" + feeInt[i] + "|" + ((i+1)<feeInt.length));
                if(i!= 0 )
                    FeeStr += "|";
                if(feeStr[i].equals("托盘费")) {
                    FeeStr += appID + "_" + "入库" + "_" + feeInt[i] * m_traysInApp + "_" + feeStr[i];
                }
                else{
                    FeeStr += appID + "_" + "入库" + "_" + feeInt[i] + "_" + feeStr[i];
                }
            }
        }

        Log.d("FeeStr",FeeStr+"");
        //wfID 自动 wfAppID库单号 wfAppID 入库 wfValue金额 时间php端自定

        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wTrays` WHERE";
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpFee.php?";

        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("insertFee","true"));
        params.add(new BasicNameValuePair("FeeStr",FeeStr));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        //Log.d("费用增加",req);
        //参数
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("费用增加结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }

        //json_ScanedTrays = get(req);
        return true;
    }

    /*唛头修改初始化*/
    void maitouAD_init(){
        final EditText editMaitou = new EditText(getBaseContext());
        editMaitou.setText(et_Maitou);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请输入唛头");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setView(editMaitou);
        builder.setPositiveButton("确认更改唛头", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which){
                et_Maitou = editMaitou.getText().toString();
                new Thread() {
                    @Override
                    public void run() {
                        Log.d("更改唛头启动","询问服务器");
                        Message msg = new Message();
                        if(changeMaitou(appID,et_Maitou)){
                            msg.what = MAITOU_SUCCESS;
                        }
                        else{
                            msg.what = MAITOU_FALSE;
                        }
                        handler.sendMessage(msg);

                    }
                }.start();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", null);

        maitouAD = builder.create();
    }

    /*ad2初始化*/
    void truckChosseAD_init(){
        int driverIdx = 0;
        final Spinner mSpinner = new Spinner(this);
        final String[] mItems ;
        try{
            if(_drivers.length() == 0){
                mItems = new String[]{"没有可选司机"};
            }
            else {
                mItems = new String[_drivers.length()];
                for (int i = 0; i < _drivers.length(); i++) {
                    mItems[i] = _drivers.getJSONObject(i).getString("wuName");
                    //如果目前选中司机的id在司机中，将spinner的选择项移动到已存司机位置
                    int _userID = _drivers.getJSONObject(i).getInt("userID");
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
                                    if (changeFork(appID, m_Driver_Name)) {
                                        msg.what = FORK_SUCCESS;
                                        //添加消息
                                        MessageUtils mu = new MessageUtils(getString(R.string.dbAddress));
                                        String para = MyPreference.getInstance(mContext).getUserID() + "|" + InStockID + "|||" + appID + "|" + MyPreference.getInstance(mContext).getLoginName() + "给" + appID + "货单指定了司机" + m_Driver_Name;
                                        Log.d("消息", para);
                                        //InstockID|trayID|slotID|appID|actContent
                                        mu.AddMessage(0, para);

                                    } else {
                                        msg.what = FORK_FALSE;
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



    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //虽然是一个列表，但分区域去刷新页面
            //托盘主控制站位
            //Uint填充数据
            if (msg.what == LOAD_SUCCESS) {
                //刷新elv2;
                //Log.d("AppsPage页面","获取数据成功");
                TidyData();
                refreshTraysList();
                updateUI_AppIn();
            }
            if(msg.what == UPDATEAPPINFO){
            }
            if(msg.what == MAITOU_SUCCESS){
                Toast.makeText(getBaseContext(), "修改唛头成功", Toast.LENGTH_SHORT).show();
                ValidateUpdate();
            }
            if(msg.what == MAITOU_FALSE){
                Toast.makeText(getBaseContext(), "修改唛头失败", Toast.LENGTH_SHORT).show();
            }
            if(msg.what == FORK_SUCCESS){
                Toast.makeText(getBaseContext(), "叉车司机指定完成", Toast.LENGTH_SHORT).show();


                ValidateUpdate();
            }
            if(msg.what == FORK_FALSE){
                Toast.makeText(getBaseContext(), "叉车司机指定失败", Toast.LENGTH_SHORT).show();
            }
            if(msg.what == PRECOUNT_C_SUCCESS){
                Toast.makeText(getBaseContext(), "叉车司机指定成功\n继续点击完成货单", Toast.LENGTH_SHORT).show();
                ValidateUpdate();
            }
            if(msg.what == PRECOUNT_C_FAIL){
                Toast.makeText(getBaseContext(), "叉车司机指定失败", Toast.LENGTH_SHORT).show();
            }
            if(msg.what == COMPLETE_SUCCUSS){
                Toast.makeText(getBaseContext(), "货单进仓完成\n请退出", Toast.LENGTH_SHORT).show();
            }
            if(msg.what == COMPLETE_FAIL){
                Toast.makeText(getBaseContext(), "货单进仓失败\n请检查", Toast.LENGTH_SHORT).show();
            }
            if(msg.what == CALCULATE_FEE){
                Toast.makeText(getBaseContext(), "费用合计为\n"+fee_total+"元", Toast.LENGTH_SHORT).show();
                //更改UI
                TextView priceTag = (TextView)findViewById(R.id.ap_PriceLabel);
                priceTag.setText("费用:"+fee_total+"元");
            }
            if(msg.what == REGISTER_TRAY){
                Toast.makeText(getBaseContext(), "托盘登记成功", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(getApplicationContext(), TrayStatus_Activity.class);
                i.putExtra("traysCode", TrayCodes);
                startActivity(i);
            }
            if(msg.what == CONNECTION_LOST){
                Toast.makeText(AppsPageActivity.this,"网络出错"+getString(R.string.dbAddress)+"无法访问",Toast.LENGTH_LONG).show();
                //break;
            }
        }
    };

    private void updateUI_AppIn(){
        Log.d("更新UI","更新");
        try{
            TextView tv_tmp = (TextView)findViewById(R.id.ap_appName);
            tv_tmp.setText( jsonData_appIn.getString("appName") );
            tv_tmp = (TextView)findViewById(R.id.ap_Maitou);
            tv_tmp.setText( jsonData_appIn.getString("appMaitou") );
            tv_tmp = (TextView)findViewById(R.id.ap_BookingTime);
            tv_tmp.setText( jsonData_appIn.getString("appBookingDate").split(" ")[0] );
            tv_tmp = (TextView)findViewById(R.id.ap_PreCount);
            tv_tmp.setText( jsonData_appIn.getString("appPreCount")+"/"+m_loaded );
            tv_tmp = (TextView)findViewById(R.id.ap_OpFork);
            tv_tmp.setText("司机:\n"+m_Driver_Name);
            tv_tmp = (TextView)findViewById(R.id.ap_Counter);
            tv_tmp.setText("理货:\n"+m_Counter_Name);


            InStockID = jsonData_appIn.getString("InStockID");
            appName = jsonData_appIn.getString("appName");
        }
        catch (JSONException e){
            Log.e("更新appIN时报错","尝试解析jsonData_appIn时出错");
            throw new RuntimeException(e);
        }
    }
    private void TidyData(){
        jsonData_tidy = new JSONArray();
        if(jsonData_trays.length()==0){
            try {
                JSONObject defaultNone = new JSONObject();
                defaultNone.put("traysItem_appType", "没有托盘");
                defaultNone.put("traysItem_traysCount","");
                defaultNone.put("traysItem_Maitou","唛头:空");
                defaultNone.put("traysItem_size","尺寸 未知");
                defaultNone.put("traysItem_itemCount","箱数 无");
                defaultNone.put("traysItem_traysPos","位置 未分配");
                jsonData_tidy.put(defaultNone); //暂时去掉，会引起添加货物的错误
                //Log.d("如果","如果不添加0整理项");
            }
            catch (JSONException e){
                throw new RuntimeException(e);
            }
        }
        else if(jsonData_trays.length()!=0 && jsonData_uints.length() == 0){
            //Log.e("位置1","jsonData_trays.length()!=0 && jsonData_uints.length() == 0");
            try {
                for(int i = 0; i<jsonData_trays.length(); i++) {
                    JSONObject defaultNone = new JSONObject();
                    defaultNone.put("traysItem_appType", String.valueOf(i+1));
                    defaultNone.put("traysItem_traysCount", "1");
                    defaultNone.put("traysItem_Maitou", "唛头:空");
                    defaultNone.put("traysItem_size", "尺寸 未知");
                    defaultNone.put("traysItem_itemCount", "箱数 无");
                    defaultNone.put("traysItem_traysPos", FindSlotPosBySlotID(jsonData_trays.getJSONObject(i).getString("wSlotID")));
                    jsonData_tidy.put(defaultNone);
                }
            }
            catch (JSONException e){
                throw new RuntimeException(e);
            }
        }
        else if(jsonData_trays.length()!=0 && jsonData_uints.length() != 0){
            try {
                //Log.e("位置2","jsonData_trays.length()!=0 && jsonData_uints.length() != 0");
                for(int i=0; i<jsonData_uints.length(); i++) {
                    //Log.d("遍历","第"+i+"波货物");
                    JSONObject dTray = new JSONObject();
                    //检查类型是否出现过
                    int _et_idx = checkItemExistType(jsonData_uints.getJSONObject(i).getString("itemType"));
                    //Log.d("货物格式是否存在",_et_idx+"存在指标，-1未不存在");
                    if(_et_idx !=-1){
                        //出现过的情况
                        dTray  = jsonData_tidy.getJSONObject(_et_idx);
                        //修改元素
                        //类型不修改
                        //托盘数加1
                        String tcStr = String.valueOf( Integer.parseInt(dTray.getString("traysItem_traysCount").split("托")[0]) +1) ;
                        dTray.put("traysItem_traysCount",tcStr+"托");

                        //唛头不需要修改
                        //尺寸不需要加

                        //托盘内箱数更改 15箱 x 2
                        dTray.put("traysItem_itemCount", dTray.getString("traysItem_itemCount").split("箱")[0]+"箱x"+tcStr);

                        //如果包含
                        if(!dTray.getString("traysItem_traysPos").contains( FindSlotPosBySlotID(FindSlotIDByTrayID(jsonData_uints.getJSONObject(i).getString("trayID")))) ){
                            String tpStr = dTray.getString("traysItem_traysPos")+"/"+FindSlotPosBySlotID(FindSlotIDByTrayID(jsonData_uints.getJSONObject(i).getString("trayID")));
                            dTray.put("traysItem_traysPos",tpStr);
                        }

                    }
                    else {

                        dTray.put("traysItem_appType", "类型:"+jsonData_uints.getJSONObject(i).getString("itemType"));
                        dTray.put("traysItem_traysCount", "1托");
                        dTray.put("traysItem_Maitou", jsonData_uints.getJSONObject(i).getString("wiName"));
                        String sizeStr = jsonData_uints.getJSONObject(i).getString("width")+"x"+jsonData_uints.getJSONObject(i).getString("length")+"x"+jsonData_uints.getJSONObject(i).getString("height");
                        dTray.put("traysItem_size", "尺寸:"+sizeStr);
                        dTray.put("traysItem_itemCount", jsonData_uints.getJSONObject(i).getString("count")+"箱");
                        dTray.put("traysItem_traysPos", FindSlotPosBySlotID(FindSlotIDByTrayID(jsonData_uints.getJSONObject(i).getString("trayID"))));
                        jsonData_tidy.put(dTray);
                    }

                }
            }
            catch (JSONException e){
                throw new RuntimeException(e);
            }
        }
    }
    private void refreshTraysList(){
        TrayAdapter adapter = new TrayAdapter(getBaseContext());
        ListView appSlv = (ListView)findViewById(R.id.ap_TraysList);
        appSlv.setAdapter(adapter);
    }

    private int checkItemExistType(String typeID){
        try{

            for(int i = 0; i<jsonData_tidy.length(); i++) {
                //Log.d("比较是否存在",typeID+" == "+jsonData_tidy.getJSONObject(i).getString("traysItem_appType"));
                if(typeID.equals(jsonData_tidy.getJSONObject(i).getString("traysItem_appType").split(":")[1]))
                    return i;
            }
            return -1;
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }
    }
    //获取货单信息
    public JSONObject getApp(int _appID){
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wAppIn` WHERE `appID`="+_appID;
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        JSONArray jsonA = get(req);
        JSONObject obj = new JSONObject();
        try{
            obj = jsonA.getJSONObject(0);
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }
        return obj;
    }
    //确认入库完成 (货品全部上架，货物数量也对齐)
    public boolean ApplyAppInComplete(int _appID){
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("table","wAppIn"));
        params.add(new BasicNameValuePair("tKey","appStatus"));
        params.add(new BasicNameValuePair("tVal","2"));
        params.add(new BasicNameValuePair("idKey","appID"));
        params.add(new BasicNameValuePair("idVal",String.valueOf(_appID) ));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改唛头结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }


    //更改货单已入数量
    public boolean ChangeAppCount(int _appID, int count_load){
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("table","wAppIn"));
        params.add(new BasicNameValuePair("tKey","appCount"));
        params.add(new BasicNameValuePair("tVal",count_load+""));
        params.add(new BasicNameValuePair("idKey","appID"));
        params.add(new BasicNameValuePair("idVal",String.valueOf(_appID) ));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改唛头结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            Message msg = new Message();
            msg.what = CONNECTION_LOST;
            handler.sendMessage(msg);
            return false;
            //throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }

    //更改货单预入数量
    public boolean ChangePreCount(int _appID, int count_load){
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("table","wAppIn"));
        params.add(new BasicNameValuePair("tKey","appPreCount"));
        params.add(new BasicNameValuePair("tVal",count_load+""));
        params.add(new BasicNameValuePair("idKey","appID"));
        params.add(new BasicNameValuePair("idVal",String.valueOf(_appID) ));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改唛头结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }

    //更改货单唛头
    public boolean changeMaitou(int _appID, String _maitou){
        //UPDATE `wAppIn` SET `appMaitou`= "MAITOU" WHERE `appID`=178
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        //参数 //table数据表 //tKey 目标数据名   //tVal 目标数据值   //idKey id数据名   //idVal id值
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("table","wAppIn"));
        params.add(new BasicNameValuePair("tKey","appMaitou"));
        params.add(new BasicNameValuePair("tVal",_maitou));
        params.add(new BasicNameValuePair("idKey","appID"));
        params.add(new BasicNameValuePair("idVal",String.valueOf(_appID) ));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        //JSONArray jsonA = get(req);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改唛头结果",EntityUtils.toString(entity));
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }
    public boolean changeFork(int _appID, String forkName){
        int selForkUserID = 0;
        try{
            if(_drivers.length()>0)
            {
                for(int i = 0; i<_drivers.length(); i++){
                    if(_drivers.getJSONObject(i).getString("wuName").equals(forkName)){
                        selForkUserID = Integer.parseInt(_drivers.getJSONObject(i).getString("userID"));
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
            params.add(new BasicNameValuePair("table","wAppIn"));
            params.add(new BasicNameValuePair("tKey","OpFork"));
            params.add(new BasicNameValuePair("tVal",selForkUserID+""));
            params.add(new BasicNameValuePair("idKey","appID"));
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
                Log.d("修改司机结果","出错");
                e.printStackTrace();
                //throw new RuntimeException(e);
            }finally {
                client.getConnectionManager().shutdown();
            }
            return true;
        }
        Log.d("没有forkUserID","错误");
        return false;
    }
    public void getUsers(){
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wUsers` WHERE 1";
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        Log.d("appPage页面 用户获取",comRequest+query);
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        _users = get(req);
        //Log.d("用户列表",_users.toString());
    }
    //获取司机
    public void getDrivers(){
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wUsers` WHERE `job`=5";
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        Log.d("appPage页面 司机获取",comRequest+query);
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        _drivers = get(req);
    }
    public void getFee(){
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wFee` WHERE `wfAppID`="+appID;
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        Log.d("appPage页面 价格获取",comRequest+query);
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        JSONArray fees = get(req);
        fee_total = 0; //费用清零
        try{
            for(int i = 0; i<fees.length(); i++){
                fee_total += fees.getJSONObject(i).getDouble("wfValue");
            }
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }
        Message msg = new Message();
        msg.what = CALCULATE_FEE;
        handler.sendMessage(msg);

    }

    //获取对应<托盘>Json数据
    public JSONArray getTraysData(int _appID){
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wTrays` WHERE `wtAppID`="+_appID;
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        Log.d("appPage页面 TraysLoad",comRequest+query);
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        JSONArray jsonA = get(req);
        return jsonA;
    }
    //获取对应货品Json数据
    private JSONArray getItemsData(int[] wtIDs){
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        String query = "SELECT * FROM `wUnit` WHERE ";
        query += "(";
        //输入值是索取对应trayId的格式
        for (int i=0; i<wtIDs.length; i++){
            if(i!= 0)
                query += " OR ";
            query += "`trayID`="+wtIDs[i];
        }
        query += ")";
        Log.d("appPage页面 ItemLoad",comRequest+query);
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        JSONArray jsonA = get(req);
        return jsonA;
    }
    //获取<仓位>信息
    private void getSlotData(){
        map_slots = new HashMap<String, String>();
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        String query = "SELECT `wSlotID`,`tsWareHouse`,`tsPos` FROM `wSlots` WHERE 1";

        Log.d("appPage页面 SlotLoad",comRequest+query);
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        JSONArray jsonA = get(req);
        try{
            for(int i = 0; i<jsonA.length(); i++){
                //Log.e(" map_slots.put",i+"  "+jsonA.getJSONObject(i).getString("wSlotID")+"  "+jsonA.getJSONObject(i).getString("tsPos"));
                map_slots.put(jsonA.getJSONObject(i).getString("wSlotID"), jsonA.getJSONObject(i).getString("tsPos"));
            }

        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }
        //return jsonA;
    }
    //通过地址返回JSONArray
    public JSONArray get(String url){
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        JSONArray json = null;
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                //Log.d("JSON",EntityUtils.toString(entity));
                json = new JSONArray(EntityUtils.toString(entity));
            }
        }catch (Exception e){
            Message msg = new Message();
            msg.what = CONNECTION_LOST;
            handler.sendMessage(msg);
            //throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return json;
    }

    public final class ViewHolder{
        public TextView trays_typeID;
        public TextView trays_traysCount;
        public TextView traysItem_Maitou;
        public TextView trays_Size;
        public TextView trays_Count;
        public TextView trays_Position;
    }


    public class TrayAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        public TrayAdapter(Context context){
            this.mInflater = LayoutInflater.from(context);
        }
        @Override
        public int getCount(){
            //Log.d("托盘的实际数据为","托盘数量"+jsonData_trays.length());
            return jsonData_tidy.length();
        }
        @Override
        public JSONObject getItem(int position){
            try{
                return jsonData_tidy.getJSONObject(position);
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public long getItemId(int position){
            return 0;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            ViewHolder holder = null;
            if(convertView ==null){
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_trays,null);
                holder.trays_typeID = (TextView)convertView.findViewById(R.id.traysItem_appType);
                holder.trays_traysCount = (TextView)convertView.findViewById(R.id.traysItem_traysCount);
                holder.traysItem_Maitou = (TextView)convertView.findViewById(R.id.traysItem_Maitou);
                holder.trays_Size = (TextView)convertView.findViewById(R.id.traysItem_size);
                holder.trays_Count = (TextView)convertView.findViewById(R.id.traysItem_itemCount);
                holder.trays_Position = (TextView)convertView.findViewById(R.id.traysItem_traysPos);
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder)convertView.getTag();
            }
            try{

                holder.trays_typeID.setText((String)getItem(position).getString("traysItem_appType"));
                holder.trays_traysCount.setText((String)getItem(position).getString("traysItem_traysCount"));
                holder.traysItem_Maitou.setText((String)getItem(position).getString("traysItem_Maitou"));
                holder.trays_Size.setText((String) getItem(position).getString("traysItem_size"));
                holder.trays_Count.setText((String) getItem(position).getString("traysItem_itemCount"));
                holder.trays_traysCount.setText((String)getItem(position).getString("traysItem_traysCount"));
                holder.trays_Position.setText((String)getItem(position).getString("traysItem_traysPos"));
            }
            catch (JSONException e){
                throw new RuntimeException(e);
            }

            return convertView;
        }
    }

    //查找slotID 返回对应的货仓名 类似A1
    private String FindSlotPosBySlotID(String slotID){
        //Log.e("FindSlotPosBySlotID","位置"+slotID);
        if(slotID == "" || slotID == "0"){
            return "不在仓位";
        }
        else{
            for(Map.Entry<String, String> entry:map_slots.entrySet()){
                //Log.e("查找仓位","目标"+slotID+"---"+entry.getKey());
                if(entry.getKey().equals(slotID)) {
                    //Log.e("找到归属仓位",entry.getValue());
                    return entry.getValue();
                }
            }
            return "X";
        }
    }
    //查找trayID 返回对应货仓ID
    private String FindSlotIDByTrayID(String trayID){
        //Log.e("FindSlotIDByTrayID","托盘"+trayID);
        try{
            for(int i =0; i<jsonData_trays.length(); i++){
                //Log.d("FindSlotIDByTrayID","仓位ID是"+jsonData_trays.getJSONObject(i).getString("wtID"));
                if(trayID.equals(jsonData_trays.getJSONObject(i).getString("wtID")))
                {
                    //Log.d("FindSlotIDByTrayID","找到");
                    return jsonData_trays.getJSONObject(i).getString("wSlotID");
                }
            }
            //Log.d("FindSlotIDByTrayID","没找到");
            return "0";
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }
    }




}


