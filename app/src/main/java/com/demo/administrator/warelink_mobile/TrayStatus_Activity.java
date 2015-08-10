package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

/**
 * Created by Administrator on 2015/7/29.
 */
public class TrayStatus_Activity extends Activity {

    //扫描类型
    private int mScanType;

    //扫描到的序列号
    private String traysCode;

    private JSONObject traySelf;
    private JSONObject traySelfAppIn;

    static final int SELFTRAY_GETTED = 100;
    static final int SETRFID_SUCCESS = 101;
    static final int SETRFID_FAIL = 102;
    static final int SCAN_V_TRAY = 103;


    //private AlertDialog bindAD=null;
    private String et_Vrfid = "";
    private String wtID = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traystatus);

        Intent i = getIntent();
        traysCode = i.getExtras().getString("traysCode");
        Button bindBtn = (Button)findViewById(R.id.trayPage_bindBtn);
        bindBtn.setEnabled(false);
        bindBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d("绑定btn", "将"+wtID+"绑定"+et_Vrfid);
                if(!et_Vrfid.equals("")) {
                    new Thread() {
                        @Override
                        public void run() {
                            Log.d("更改vrfid启动", "询问服务器");
                            Message msg = new Message();
                            if (setvRFID()) {
                                msg.what = SETRFID_SUCCESS;
                            } else {
                                msg.what = SETRFID_FAIL;
                            }
                            mScanHandler.sendMessage(msg);

                        }
                    }.start();
                }

            }
        });

        new Thread(){
            @Override
            public void run() {
            //获取数据
            traySelf = getSelfTray(traysCode);
            //jsonData = getAppsData();
            //获取数据成功后，去hanlder刷新界面
            traySelfAppIn = getSelfTray_AppIn(traySelf);
            Log.d("查找到的AppIn",traySelfAppIn==null?"未查找到appIn":traySelfAppIn.toString());
            Message msg = new Message();
            msg.what = SELFTRAY_GETTED;
            mScanHandler.sendMessage(msg);
            Log.d("Thread线程启动","FrontPageActivity的线程启动");
            }
        }.start();
    }
    @Override
    protected void onStart(){
        super.onStart();
        AppUtils.startService(this, mScanType);
        Log.i("TS StartService", "扫描服务开启");
        mScanType = GpdService.DEVICE_TYPE_RFID;
        GpdService.setActiveScanDevice(mScanType);
        GpdService.setScanHandler(this.mScanHandler);
    }

    @Override
    protected void onResume(){     super.onResume();     }

    @Override
    protected void onPause() {
        super.onPause();

    }
    @Override
    protected void onStop(){
        super.onStop();
        GpdService.removeScanHandler(this.mScanHandler);
        AppUtils.stopService();
        //GpdService.stopService();
        Log.i("TS页面暂停", "TS页面暂停 扫描服务关闭");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }





    public void bindAD_init(){
        final EditText editVrfid = new EditText(getBaseContext());
        editVrfid.setHint("请扫描托盘另一侧rfid芯片，并点击确认");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("绑定另一张芯片卡");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setView(editVrfid);

        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which){
                //先检查扫描结果是否等于主芯片卡
                if(traysCode.equals(editVrfid.getText().toString()) && editVrfid.getText().toString().equals("")){
                    Toast.makeText(getBaseContext(),"扫描了重复的卡片或为空，请扫描另一侧卡片",Toast.LENGTH_LONG);
                    return;
                }
                et_Vrfid = editVrfid.getText().toString();

                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", null);

        //bindAD = builder.create();

    }

    public boolean setvRFID(){
        String url = getString(R.string.dbAddress);
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpUpdate.php?";
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("table","wTrays"));
        params.add(new BasicNameValuePair("tKey","vrfid"));
        params.add(new BasicNameValuePair("tVal",et_Vrfid));
        params.add(new BasicNameValuePair("idKey","wtID"));
        params.add(new BasicNameValuePair("idVal",wtID ));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(req);
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                Log.d("修改vrfid结果",EntityUtils.toString(entity));
                return true;
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }

    private JSONObject getSelfTray(String code){
        String rfidCode = code.split(";")[0];
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wTrays` WHERE rfid = \""+rfidCode+"\" OR vrfid = \""+rfidCode+"\"";
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        Log.d("TrayStatus 获取",comRequest+query);
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        return get(req);
    }

    private JSONObject getSelfTray_AppIn(JSONObject tray){
        //String rfidCode = code.split(";")[0];
        Log.d("查找托盘AppIn信息",tray.toString());
        String appID = "0";
        try{
            appID = tray.getString("wtAppID")==""?"0":tray.getString("wtAppID");
        }
        catch (JSONException e){
            e.printStackTrace();
        }
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wAppIn` WHERE appID = \""+appID+"\"";
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        Log.d("TrayStatus 获取",comRequest+query);
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
        return get(req);
    }

    //通过地址返回JSONArray
    public static JSONObject get(String url){
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        JSONArray jsonA = null;
        JSONObject json = null;
        try{
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                //Log.d("JSON",EntityUtils.toString(entity));
                jsonA = new JSONArray(EntityUtils.toString(entity));
                if(jsonA.length()==0)
                    return null;
                json = jsonA.getJSONObject(0);
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            client.getConnectionManager().shutdown();
        }
        return json;
    }

    private final Handler mScanHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d("TS页面扫描响应","TS扫描"+msg.what);
            switch (msg.what) {
                case GpdService.MSG_SCAN_BARCODE:
                    String barcode = (String) msg.obj;
                    //onScanBarcode(barcode);
                    break;
                case GpdService.MSG_RFID_TAG_ID:
                    String tagId = (String) msg.obj;
                    if (tagId.length() > 0){
                        int tagLength = tagId.split(";").length;
                        if(tagLength == 1){
                            traysCode = tagId;


                            new Thread(){
                                @Override
                                public void run() {
                                    //获取数据
                                    JSONObject obj = getSelfTray(traysCode);

                                    if(obj==null){
                                        Log.d("扫描","扫到未注册卡片，编入副卡信息");
                                        et_Vrfid = traysCode;
                                        Message msg = new Message();
                                        msg.what = SCAN_V_TRAY;
                                        mScanHandler.sendMessage(msg);
                                        //Log.d("obj",obj.toString()+"length");
                                    }
                                    else{
                                        Log.d("扫描","!null="+obj.toString());
                                        traySelf = obj;
                                        traySelfAppIn = getSelfTray_AppIn(traySelf);
                                        if(traySelfAppIn != null)
                                            Log.d("查找到的AppIn",traySelfAppIn.toString());
                                        Message msg = new Message();
                                        msg.what = SELFTRAY_GETTED;
                                        mScanHandler.sendMessage(msg);
                                    }

                                    //jsonData = getAppsData();
                                    //获取数据成功后，去hanlder刷新界面

                                    //Log.d("Thread线程启动","FrontPageActivity的线程启动");
                                }
                            }.start();
                            /*
                            //扫描到了托盘，并且只扫到了一个
                            TextView tv_viceC = (TextView)findViewById(R.id.trayPage_trayViceCard);
                            TextView tv_mainC = (TextView)findViewById(R.id.trayPage_trayMainCard);
                            et_Vrfid = tagId;
                            if(tagId.equals(tv_viceC.getText())){
                                //扫描到副托盘
                                tv_viceC.setText(Html.fromHtml("托盘副编码:<font color='#DAA520'>"+tagId+"</font>"));
                            }
                            else if(tagId.equals(tv_mainC.getText())){
                                tv_mainC.setText(Html.fromHtml("托盘主编码:<font color='#DAA520'>"+tagId+"</font>"));
                            }
                            else {
                                Log.d("扫描到非主副编码",tagId+"....");
                                //tv_viceC.setText("托盘副编码:" + tagId + "(未绑定)");
                            }*/
                        }
                        else{
                            et_Vrfid = "";
                            Toast.makeText(getBaseContext(),"扫描到多个托盘",Toast.LENGTH_SHORT);
                        }
                    }
                    else{
                        et_Vrfid = "";
                        Toast.makeText(getBaseContext(),"未扫描托盘",Toast.LENGTH_SHORT);
                    }
                    break;
                case SELFTRAY_GETTED:
                    TrayGettedUpdate();
                    break;
                case SCAN_V_TRAY:
                    TextView tmp = (TextView)findViewById(R.id.trayPage_trayViceCard);
                    tmp.setText(Html.fromHtml("托盘副编码:<font color='#DAA520'>"+et_Vrfid+"</font>(等待绑定)"));
                    break;
                case SETRFID_SUCCESS:
                    Toast.makeText(getBaseContext(),"绑定成功",Toast.LENGTH_SHORT);
                    TextView tv_tmp = (TextView)findViewById(R.id.trayPage_trayViceCard);
                    tv_tmp.setText(Html.fromHtml("托盘副编码:<font color='#DAA520'>"+et_Vrfid+"</font>(已绑定)"));
                    break;
                case SETRFID_FAIL:
                    Toast.makeText(getBaseContext(),"绑定失败",Toast.LENGTH_SHORT);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };


    public void TrayGettedUpdate(){
        //traySelf;
        //traysCode;
        try{

            //扫描到的信息是托盘
            TextView tmp = (TextView) findViewById(R.id.trayPage_trayID);
            tmp.setText("托盘-" + traySelf.getString("wtID"));
            wtID = traySelf.getString("wtID");
            Log.d("扫描到卡片信息", "code:" + traysCode + ";trayMainRfid:" + traySelf.getString("rfid") + ";vrfid:" + traySelf.getString("vrfid"));

            tmp = (TextView) findViewById(R.id.trayPage_trayInfo);
            String tmpStr = "托盘信息:";
            //判断是入库舱外还是出库舱外
            if(traySelf.getString("twStatus").equals("仓库外")) {
                if (traySelf.getString("wtAppOutID").equals("") || traySelf.getString("wtAppOutID").equals("0")) {
                    tmpStr += "理货完成" + "\n";
                }
                else{
                    tmpStr += "装箱中" + "\n";
                }
            }
            else if(traySelf.getString("twStatus").equals("货架")){
                tmpStr += "已上架" + "\n";
            }
            else{
                tmpStr += traySelf.getString("twStatus") + "\n";
            }

            tmpStr += "托盘货物数量:"+traySelf.getString("twWareCount")+"\n";
            if(traySelfAppIn != null) {
                tmpStr += "唛头:"+traySelfAppIn.getString("appMaitou")+ "\n";
                tmpStr += "进仓编号:"+traySelfAppIn.getString("InStockID")+ "\n";

            }
            tmp.setText(tmpStr);
            if (traysCode.equals(traySelf.getString("rfid"))) {
                tmp = (TextView) findViewById(R.id.trayPage_trayMainCard);
                tmp.setText(Html.fromHtml("主卡片信息:<font color='#DAA520'>" + traysCode + "</font>"));
                if (traySelf.getString("vrfid").equals("")) {
                    //副卡无信息
                    tmp = (TextView) findViewById(R.id.trayPage_trayViceCard);
                    tmp.setText("副卡片信息:等待扫描添加");
                    Button bindBtn = (Button) findViewById(R.id.trayPage_bindBtn);
                    bindBtn.setEnabled(true);
                } else {
                    tmp = (TextView) findViewById(R.id.trayPage_trayViceCard);
                    tmp.setText("副卡片信息:" + traySelf.getString("vrfid"));
                }
            } else {
                //逻辑在验证下
                Log.d("本业托盘", "扫描信息和主卡不相符");
                tmp = (TextView) findViewById(R.id.trayPage_trayMainCard);
                tmp.setText("主卡片信息:" + traySelf.getString("rfid"));
                tmp = (TextView) findViewById(R.id.trayPage_trayViceCard);
                tmp.setText(Html.fromHtml("副卡片信息:<font color='#DAA520'>" + traySelf.getString("vrfid") + "</font>"));
            }

        }
        catch (JSONException e){
            e.printStackTrace();
        }

    }

}
