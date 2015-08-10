package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gpd.sdk.service.GpdService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/5/21.
 */
public class PageRespActivity extends Activity {
    private  LinearLayout layout;
    //private int mScanType;
    //搜寻RFID代码反馈
    static final int SCAN_RFID_TRAY_VALID = 101; //扫描到的托盘可用
    static final int SCAN_RFID_TRAY_INVALID = 102;//扫描到的托盘，因为各种原因，托盘不可用
    static final int TRAY_READY = 103; //添加货物
    static final int TRAY_ADD_DONE = 104; //添加货物完毕

    public String TraysStatus = "";
    public int TraysID = 0;
    public int appID = 0;
    public String appName = "";
    public String InStockID ="";
    public List<String> useTempList;
    public Map<String,String> tempSet;
    public Map<String,String> tr;
    public boolean newTmpControl = false;
    public String tidyData="";

    public int ActivityStatus = 0; //0等待扫描，1选择某个配置， 2填写新配置， 3成功添加
    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        //获取入库单
        setContentView(R.layout.activity_page_response);

        //打印用户名
        Log.d("用户名和用户ID",MyPreference.getInstance(mContext).getLoginName()+":"+MyPreference.getInstance(mContext).getUserID());

        TraysID = Integer.parseInt(i.getExtras().getString("wtID"));
        appName= i.getExtras().getString("appName");
        TraysStatus = i.getExtras().getString("TrayStatus");
        //从托盘数据获取货物模板
        tidyData = i.getExtras().getString("traysInfo");
        Log.d("defaultNONE 模板", tidyData);
        appID = i.getExtras().getInt("appID");
        InStockID = i.getExtras().getString("InStockID");

        //使下测选择可见
        View aView = (View)findViewById(R.id.useTempBlock);
        aView.setVisibility(View.VISIBLE);

        //大页面的事件添加  有必要？
        layout=(LinearLayout)findViewById(R.id.page_response);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Log.d("反馈页面","onClick");
                Toast.makeText(getApplicationContext(), "提示：点击窗口外部关闭窗口！",
                        Toast.LENGTH_SHORT).show();
            }
        });

        //软键盘控制 阻止自动弹出软键盘  也许会去掉
        //EditText et = (EditText)findViewById(R.id.scan_rfidCode);
        //et.setInputType(InputType.TYPE_NULL);

        //页面初始化
        useTempList = new ArrayList<String>();
        //Log.d("InstockID",InStockID);
        try {
            JSONArray tidyD = new JSONArray(tidyData.toString());
            for(int j = 0; j<tidyD.length(); j++){
                String tmp = "";
                JSONObject tmpO = tidyD.getJSONObject(j);
                tmp =getDateOnly(tmpO.getString("traysItem_appType"))+" "+tmpO.getString("traysItem_Maitou")+" "+getDateOnly(tmpO.getString("traysItem_size"))+" "+tmpO.getString("traysItem_itemCount");
                if(!tmpO.getString("traysItem_appType").equals("没有托盘"))
                    useTempList.add(tmp);
            }
            Log.d("测试", tidyD.getJSONObject(0).toString());
        }
        catch (JSONException e){
            throw new RuntimeException(e);
        }

        useTempList.add("新增托盘装载");
        final ListView useTempLV = (ListView)findViewById(R.id.item_trays_setlist);
        useTempLV.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,useTempList));
        useTempLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("如果","点击useTempList"+useTempList.get(position).toString());

                if(useTempList.size()-1 == position){
                    //弹出toast
                    Toast msg = Toast.makeText(getBaseContext(), "新配置" + TraysStatus + TraysID, Toast.LENGTH_LONG);
                    msg.show();
                    newTmpControl = true;
                }
                else {

                    String[] sS = useTempList.get(position).toString().split(" ");
                    if( sS[2].split("x").length!=3){
                        //请忽略配置，重新设置
                        Toast msg = Toast.makeText(getBaseContext(), "此项配置出错，请重新配置" + TraysStatus + TraysID, Toast.LENGTH_LONG);
                        msg.show();
                        View aView = (View)findViewById(R.id.fillNewOneBlock);
                        aView.setVisibility(View.VISIBLE);
                        View bView = (View)findViewById(R.id.useTempBlock);
                        bView.setVisibility(View.INVISIBLE);
                    }
                    else {
                        tempSet = new HashMap<String, String>();
                        tempSet.put("type", sS[0]);
                        tempSet.put("name", sS[1]);
                        tempSet.put("length", sS[2].split("x")[0]);
                        tempSet.put("width", sS[2].split("x")[1]);
                        tempSet.put("height", sS[2].split("x")[2]);
                        tempSet.put("count", sS[3].split("箱")[0]);
                        Log.d("点击某个配置", tempSet.toString()+"开始向输入框配置");
                        EditText tmp =(EditText)findViewById(R.id.input_Unit_Length);
                        tmp.setText(tempSet.get("length"));
                        tmp =(EditText)findViewById(R.id.input_Unit_Width);
                        tmp.setText(tempSet.get("width"));
                        tmp =(EditText)findViewById(R.id.input_Unit_Height);
                        tmp.setText(tempSet.get("height"));
                        tmp =(EditText)findViewById(R.id.input_Unit_Count);
                        tmp.setText(tempSet.get("count"));
                    }
                }
                View aView = (View)findViewById(R.id.fillNewOneBlock);
                aView.setVisibility(View.VISIBLE);
                View bView = (View)findViewById(R.id.useTempBlock);
                bView.setVisibility(View.GONE);

            }
        });
    }
    @Override
    protected void onStart(){
        super.onStart();
        //GPD Service启动 也许会去掉
        //AppUtils.startService(this, mScanType);
        Log.i("StartService", "扫描服务开启.");
        //mScanType = GpdService.DEVICE_TYPE_RFID;
        //GpdService.setActiveScanDevice(mScanType);
        //GpdService.setScanHandler(mHandler); //扫描物理按键的反应



    }
    @Override
    protected void onStop(){
        super.onStop();
        //AppUtils.stopService();
        Log.i("Destory PageResponse", "扫描服务关闭 onDestory called.");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    //检验传递进来的参数时候可以添加一个新的托盘组合
    public String CheckValidate(){

        if(!TraysStatus.equals("空闲"))
        {
            return "托盘状态不可用";
        }
        if(TraysID == 0){
            return "托盘ID不可用";
        }
        if(newTmpControl){
            tempSet = new HashMap<String, String>();
            tempSet.put("type", useTempList.size()+"");
            tempSet.put("name",appName );
            EditText tmp = (EditText)findViewById(R.id.input_Unit_Length);
            tempSet.put("length", tmp.getText().toString());
            tmp = (EditText)findViewById(R.id.input_Unit_Width);
            tempSet.put("width", tmp.getText().toString());
            tmp = (EditText)findViewById(R.id.input_Unit_Height);
            tempSet.put("height", tmp.getText().toString());
            tmp = (EditText)findViewById(R.id.input_Unit_Count);
            tempSet.put("count", tmp.getText().toString());

        }
        if(tempSet == null){
            return "货品配置不可用";
        }
        if(appID == 0){
            return  "货单id属性有问题";
        }
        EditText tmp =(EditText)findViewById(R.id.input_Unit_Length);
        if(tmp.getText().toString().length() == 0)
            return "缺少长度";
        tmp =(EditText)findViewById(R.id.input_Unit_Width);
        if(tmp.getText().toString().length() == 0)
            return "缺少宽度";
        tmp =(EditText)findViewById(R.id.input_Unit_Height);
        if(tmp.getText().toString().length() == 0)
            return "缺少高度";
        tmp =(EditText)findViewById(R.id.input_Unit_Count);
        if(tmp.getText().toString().length() == 0)
            return "缺少数量";
        return ""+1;
    }



    @Override
    public boolean onTouchEvent(MotionEvent event){
        //finish();
        Log.d("反馈页面","onTouchEvent");
        return true;
    }
    public void ConfirmAddItem(View v) {
        Log.d("ConfirmAddItem","尝试确认添加");
        String cmd = CheckValidate();
        if(cmd.equals("1")){

            //进行检查并启动网络更改数据库线程
            //appID trayID  tempSet(type,length,width,height,count)


            new Thread(){
                @Override
                public void run() {
                    //1.添加货物uint tempSet.type;length;width;height;count;appID;trayID;updateTime
                    //2.Log 理货员谁，给托盘确认了多少什么规格的货物,时间戳
                    //3.修改tray twWareCount和twStatus，wtAppID UpdateTime
                    // MyPreference.getInstance(mContext).getLoginName() .getUserID()
                    String UserName = MyPreference.getInstance(mContext).getLoginName();
                    int UserID = MyPreference.getInstance(mContext).getUserID();
                    Map<String,String> uintData =new HashMap<String, String>();
                    uintData.put("userName",MyPreference.getInstance(mContext).getLoginName());
                    uintData.put("userID",String.valueOf(MyPreference.getInstance(mContext).getUserID()));
                    uintData.put("appID",String.valueOf(appID));
                    uintData.put("trayID",String.valueOf(TraysID));
                    uintData.put("itemType",tempSet.get("type"));
                    uintData.put("length",tempSet.get("length"));
                    uintData.put("width",tempSet.get("width"));
                    uintData.put("height",tempSet.get("height"));
                    uintData.put("count",tempSet.get("count"));
                    uintData.put("name",tempSet.get("name"));
                    uintData.put("InStockID",InStockID);
                    //uintData含有 userName userID appID trayID length width height count

                    JsonUtils addUint = new JsonUtils(getString(R.string.dbAddress));
                    String resultJStr = addUint.InsertUintForTray(uintData);
                    try{
                        JSONArray jsonA= new JSONArray(resultJStr);
                        String InsertUnitForTray_editTray = jsonA.getJSONObject(0).getString("InsertUnitForTray_editTray");
                        String InsertUnitForTray_addUint = jsonA.getJSONObject(1).getString("InsertUnitForTray_addUint");
                        String InsertUnitForTray_AddLog = jsonA.getJSONObject(2).getString("InsertUnitForTray_AddLog");

                        String retStr = "";
                        if(InsertUnitForTray_editTray.equals("true")&& InsertUnitForTray_addUint.equals("true")&&InsertUnitForTray_AddLog.equals("true")){
                            //都成功 屏显成功信息并返回
                            //Toast msg = Toast.makeText(getBaseContext(), "添加货物成功", Toast.LENGTH_LONG);
                            //msg.show();
                            retStr += "成功添加货物";

                            //添加消息
                            MessageUtils mu = new MessageUtils(getString(R.string.dbAddress));
                            String para =MyPreference.getInstance(mContext).getUserID()+"|"+InStockID+"|||"+appID+"|"+MyPreference.getInstance(mContext).getLoginName()+"给"+appID+"货单新绑定了一托盘,增加了"+tempSet.get("count")+"箱货";
                            //InstockID|trayID|slotID|appID|actContent
                            mu.AddMessage(2,para);
                        }
                        else{

                            if(!InsertUnitForTray_editTray.equals("true")){
                                retStr += InsertUnitForTray_editTray;
                            }
                            if(!InsertUnitForTray_addUint.equals("true")){
                                retStr += InsertUnitForTray_addUint;
                            }
                            if(!InsertUnitForTray_AddLog.equals("true")){
                                retStr += InsertUnitForTray_AddLog;
                            }
                        }

                        Message uiMsg = new Message();
                        uiMsg.what = TRAY_ADD_DONE;
                        uiMsg.obj = retStr;

                        uiHandler.sendMessage(uiMsg);
                    }
                    catch (JSONException e){
                        //Toast msg = Toast.makeText(getBaseContext(), "返回时解析发生错误:"+resultJStr, Toast.LENGTH_LONG);
                        //msg.show();
                        throw new RuntimeException(e);

                    }


                    Log.d("插入结果",resultJStr);
                }

            }.start();

        }
        else {
            Toast msg = Toast.makeText(getBaseContext(), cmd, Toast.LENGTH_LONG);
            msg.show();
            Message uiMsg = new Message();
            uiMsg.what = TRAY_READY;
            uiHandler.sendMessage(uiMsg);

        }
    }

    public void exitbutton0(View v) {
        Log.d("反馈页面","exitbutton0");
    }

    private Handler uiHandler = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case SCAN_RFID_TRAY_VALID:
                    Log.d("handler","RFID已搜所到可以更改图形");
                    View aView = (View)findViewById(R.id.useTempBlock);
                    aView.setVisibility(View.VISIBLE);
                    break;
                case SCAN_RFID_TRAY_INVALID:
                    Log.d("handler","RFID已搜所到托盘不可用");
                    break;
                case TRAY_READY:
                    break;
                case TRAY_ADD_DONE:
                    String retMsg = msg.obj.toString();
                    Toast tMsg = Toast.makeText(getBaseContext(), retMsg, Toast.LENGTH_LONG);
                    tMsg.show();
                    //跳转回ApssPage
                    Intent i = new Intent(getApplicationContext(),AppsPageActivity.class);
                    i.putExtra("appID",String.valueOf(appID));
                    //i.putExtra("traysInfo",jsonData_tidy.toString());
                    i.putExtra("InStockID",InStockID);
                    startActivity(i);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    /*
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GpdService.MSG_SCAN_BARCODE:
                    String barcode = (String) msg.obj;
                    onScanBarcode(barcode);
                    break;
                case GpdService.MSG_RFID_TAG_ID:
                    String tagId = (String) msg.obj;

                    //分拆扫描托盘
                    if(tagId.split(";").length>1) {
                        Log.d("多个托盘处理",tagId);
                        Toast.makeText(getBaseContext(),"扫描到多个托盘\n查询第一个",Toast.LENGTH_SHORT);
                        onRfidTagId(tagId.split(";")[0]); //修改显示
                        //开启线程 查询对应id 包含网络进程
                        searchRFIDCode(tagId.split(";")[0]);
                    }
                    else {
                        onRfidTagId(tagId); //修改显示
                        //开启线程 查询对应id 包含网络进程
                        searchRFIDCode(tagId);
                    }
                    break;

                default:
                    super.handleMessage(msg);
            }
        }

    };
    private void searchRFIDCode(final String rfid){
        new Thread(){
            @Override
            public void run() {
                //获得托盘
                JsonUtils ad = new JsonUtils(getString(R.string.dbAddress));
                JSONObject tray0 = ad.getTray(rfid);
                try {
                    Log.d("托盘",tray0.toString());
                    Log.d("搜索RFID",tray0.getString("twStatus"));
                    TraysStatus = tray0.getString("twStatus"); //设置托盘状态；
                    if(tray0.getInt("twWareCount") == 0 && tray0.getString("twStatus").equals("空闲")){
                        TraysStatus = "可用";
                    }
                    TraysID = tray0.getInt("wtID");
                }
                catch (JSONException e){
                    throw new RuntimeException(e);
                }

                //搜索到数据库的托盘后判断：托盘可用进入下一步，不可用建议重新扫描
                Message msg = new Message();
                if(TraysStatus.equals("可用")) {
                    msg.what = SCAN_RFID_TRAY_VALID;
                }else{
                    msg.what = SCAN_RFID_TRAY_INVALID;
                    //Toast tShow = Toast.makeText(getBaseContext(), "扫描到的托盘不可用，重新选择托盘", Toast.LENGTH_LONG);
                    //tShow.show();
                }
                uiHandler.sendMessage(msg);
                Log.d("Thread线程启动","FrontPageActivity的线程启动");
            }

        }.start();
    }*/


    /*
    private void onScanBarcode(String barcode) {
        EditText tar = (EditText)findViewById(R.id.scan_rfidCode);
        TextView tarStatus = (TextView)findViewById(R.id.scan_trayStatus);
        tar.setText(barcode);
        tarStatus.setText("成功扫描到二维码");
    }

    private void onRfidTagId(String tagId) {
        EditText tar = (EditText)findViewById(R.id.scan_rfidCode);
        TextView tarStatus = (TextView)findViewById(R.id.scan_trayStatus);
        tar.setText(tagId);
        tarStatus.setText("成功扫描到RFID码");
    }*/
    public void onScanButtonClick(View v) {
        //clearView();
        GpdService.requestScan();
    }

    public String getDateOnly(String tmpString){
        Log.d("getDateOnly",tmpString);
        if(tmpString.split(":").length>1) {
            return tmpString.split(":")[1];
        }
        else
            return null;
    }


}
