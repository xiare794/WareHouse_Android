package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
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

public class FrontPageActivity extends Activity {

    public JSONArray jsonData; //获取的appins;
    public JSONArray jsonOutData; //获取的appOuts;

    static final int LOAD_SUCCESS = 1;
    static final int UNREGIST_TRAY = 100;
    static final int SCAN_A_TRAY = 101;
    //扫描类型
    private int mScanType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        i.getExtras();
        setContentView(R.layout.activity_frontpage);


        //listItems = new ArrayList<Map<String, Object>>();

        /*
        new Thread(){
            @Override
            public void run() {
                //获取数据
                jsonData = getAppsData();
                jsonOutData = getAppsOutData();
                //获取数据成功后，去hanlder刷新界面
                Message msg = new Message();
                msg.what = LOAD_SUCCESS;
                handler.sendMessage(msg);
                Log.d("Thread线程启动","FrontPageActivity的线程恢复");
            }

        }.start();*/


    }


    @Override
    protected void onStart(){
        super.onStart();
        AppUtils.startService(this, mScanType);
        Log.i("StartService", "扫描服务开启.");
        mScanType = GpdService.DEVICE_TYPE_RFID;
        GpdService.setActiveScanDevice(mScanType);
        GpdService.setScanHandler(mScanHandler);
        Log.i("FA页面开始","开始，开始扫描");
        new Thread(){
            @Override
            public void run() {
                //获取数据
                jsonData = getAppsData();
                jsonOutData = getAppsOutData();
                //获取数据成功后，去hanlder刷新界面
                Message msg = new Message();
                msg.what = LOAD_SUCCESS;
                handler.sendMessage(msg);
                Log.d("Thread线程启动","FrontPageActivity的线程恢复");
            }

        }.start();
    }

    @Override
    protected void onResume(){
        super.onResume();

        Log.i("FA页面恢复","恢复");
    }
    @Override
    protected void onPause(){
        super.onPause();


        Log.i("FA页面暂停","暂停");
    }
    @Override
    protected void onStop(){
        super.onStop();
        AppUtils.stopService();
        GpdService.removeScanHandler(mScanHandler);
        Log.i("FA页面停止","停止停止扫描");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //GpdService.stopService();
        Log.i("FA页面销毁", "销毁 onDestory called.");
    }

    private final Handler mScanHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d("FA页面扫描响应","FA页面扫描响应"+msg.what);
            switch (msg.what) {
                case GpdService.MSG_SCAN_BARCODE:
                    String barcode = (String) msg.obj;
                    //onScanBarcode(barcode);
                    break;
                case GpdService.MSG_RFID_TAG_ID:
                    final String tagId = (String) msg.obj;
                    if (tagId.length() > 0){
                        int tagLength = tagId.split(";").length;
                        if(tagLength == 1){
                            //扫描到了托盘，并且只扫到了一个
                            //检查是否是托盘
                            new Thread(){
                                @Override
                                public void run() {
                                    String url = getString(R.string.dbAddress);
                                    String query = "SELECT * FROM `wTrays` WHERE `rfid`=\""+tagId+"\" OR vrfid=\""+tagId+"\"";
                                    String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
                                    //参数
                                    ArrayList<NameValuePair> params = new ArrayList<>();
                                    params.add(new BasicNameValuePair("query",query));
                                    String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);
                                    JSONArray jsonA = get(req);
                                    if(jsonA.length()==1){
                                        //使用handler
                                        Message msg = new Message();
                                        msg.what = SCAN_A_TRAY;
                                        msg.obj = tagId;
                                        handler.sendMessage(msg);

                                    }
                                    else {
                                        Log.d("未注册托盘","提醒"+jsonA.length());
                                        Message msg = new Message();
                                        msg.what = UNREGIST_TRAY;
                                        handler.sendMessage(msg);
                                    }
                                }

                            }.start();

                        }
                        else{
                            Toast.makeText(getBaseContext(), "扫描到多个托盘", Toast.LENGTH_SHORT);
                        }
                    }
                    else{

                        Toast.makeText(getBaseContext(),"未扫描托盘",Toast.LENGTH_SHORT);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case LOAD_SUCCESS:
                    refreshList();
                    break;
                case UNREGIST_TRAY:
                    Toast.makeText(getBaseContext(), "扫描到未注册托盘，先进行入库绑定", Toast.LENGTH_SHORT).show();
                    break;
                case SCAN_A_TRAY:
                    Intent i = new Intent(getApplicationContext(), TrayStatus_Activity.class);
                    i.putExtra("traysCode", (String)msg.obj);
                    startActivity(i);
                    break;
                default:
                    break;
            }

        }
    };
    private void refreshList(){
        Log.d("响应handler", "刷新显示响应");
        AppAdapter adapter = new AppAdapter(getBaseContext());
        ListView appSlv = (ListView)findViewById(R.id.AppsLV);
        appSlv.setAdapter(adapter);
        appSlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    int inLen = jsonData.length();
                    if(position<jsonData.length()) {
                        //通过Intent跳转到app内页，向内页传导app状态和appid
                        Intent i = new Intent(getApplicationContext(), AppsPageActivity.class);
                        i.putExtra("appID", jsonData.getJSONObject(position).getString("appID"));

                        i.putExtra("appName", jsonData.getJSONObject(position).getString("appName"));
                        i.putExtra("appMaitou", jsonData.getJSONObject(position).getString("appMaitou"));
                        i.putExtra("appBookingDate", jsonData.getJSONObject(position).getString("appBookingDate"));
                        i.putExtra("appPreCount", jsonData.getJSONObject(position).getString("appPreCount"));
                        i.putExtra("InStockID", jsonData.getJSONObject(position).getString("InStockID"));
                        startActivity(i);
                        //Log.d("测试点击","点击了"+jsonData.getJSONObject(position).getString("appName"));
                    }
                    else{
                        int oIdx = position-inLen;
                        Intent i = new Intent(getApplicationContext(), AppsOutActivity.class);
                        i.putExtra("appOutID", jsonOutData.getJSONObject(oIdx).getString("wAppID"));

                        startActivity(i);
                        Log.d("点击",jsonOutData.getJSONObject(oIdx).toString());
                    }
                }
                catch (JSONException e){
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public JSONArray getAppsData(){
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wappin` WHERE `appStatus`=1";
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        JSONArray jsonA = get(req);
        return jsonA;
    }

    public JSONArray getAppsOutData(){
        String url = getString(R.string.dbAddress);
        String query = "SELECT * FROM `wAppOut` app, `wContainers` c WHERE `appStatus`=1 AND app.containerID = c.wCID";
        String comRequest = "http://"+url+"/ws/warhouse/WebServer/phpSearch.php?";
        //参数
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("query",query));
        String req = comRequest+ URLEncodedUtils.format(params, HTTP.UTF_8);

        JSONArray jsonA = get(req);
        return jsonA;
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

    public final class ViewHolder{
        public TextView appItem_appName;
        public TextView appItem_appStatus;
        public TextView appItem_appType;
        public TextView appItem_bookingTime;
        public TextView appItem_count;
        public TextView appItem_deliver;
        public TextView appItem_InStockID;
        public TextView appItem_operators;
        public TextView appItem_comPercent;
    }


    public class AppAdapter extends BaseAdapter{
        private LayoutInflater mInflater;
        public AppAdapter(Context context){
            this.mInflater = LayoutInflater.from(context);
        }
        @Override
        public int getCount(){
            int len = jsonData.length()+jsonOutData.length();
            return len;
        }
        @Override
        public JSONObject getItem(int position){
            try{
                if(position<jsonData.length())
                    return jsonData.getJSONObject(position);
                else{
                    int outPos = position-jsonData.length();
                    return jsonOutData.getJSONObject(outPos);
                }
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
                convertView = mInflater.inflate(R.layout.item_apps,null);
                holder.appItem_appName = (TextView)convertView.findViewById(R.id.appItem_appName);
                holder.appItem_appStatus = (TextView)convertView.findViewById(R.id.appItem_appStatus);
                holder.appItem_appType = (TextView)convertView.findViewById(R.id.appItem_appType);
                holder.appItem_bookingTime = (TextView)convertView.findViewById(R.id.appItem_bookingTime);
                holder.appItem_count = (TextView)convertView.findViewById(R.id.appItem_count);
                holder.appItem_deliver = (TextView)convertView.findViewById(R.id.appItem_deliver);
                holder.appItem_InStockID = (TextView)convertView.findViewById(R.id.appItem_InStockID);
                holder.appItem_operators = (TextView)convertView.findViewById(R.id.appItem_operators);
                holder.appItem_comPercent = (TextView)convertView.findViewById(R.id.appCompletePercent);
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder)convertView.getTag();
            }
            try{
                if( !getItem(position).has("wCID")) {
                    //入库
                    holder.appItem_appName.setText((String) getItem(position).getString("appName"));
                    //0-初始,1-签署,2-进仓完成,3确认完成,4全部出库
                    String statusStr = "未知";
                    if (getItem(position).getString("appStatus").equals("0")) {
                        statusStr = "<font color='#666'>等待签署</font>";
                    } else if (getItem(position).getString("appStatus").equals("1")) {
                        statusStr = "<font color='#00CC33'>理货中</font>";
                    } else if (getItem(position).getString("appStatus").equals("2")) {
                        statusStr = "<font color='#00CC99'>完成</font>";
                    }
                    holder.appItem_appStatus.setText(Html.fromHtml(statusStr));
                    holder.appItem_appType.setText(Html.fromHtml("<font color='#FF7F27'>入库</font>"));
                    holder.appItem_bookingTime.setText((String) getItem(position).getString("appBookingDate").split(" ")[0]);
                    holder.appItem_count.setText("预:" + (String) getItem(position).getString("appPreCount"));
                    String deliverCompNameShort = SubString((String) getItem(position).getString("deliverComp"), 8);
                    holder.appItem_deliver.setText(deliverCompNameShort);
                    holder.appItem_InStockID.setText((String) getItem(position).getString("InStockID"));
                    holder.appItem_operators.setText((String) getItem(position).getString("OpInput"));
                    String a = (int) getItem(position).getInt("appCount") / getItem(position).getInt("appPreCount") * 100 + "%";
                    holder.appItem_comPercent.setText(a);
                }
                else{
                    holder.appItem_appName.setText((String) getItem(position).getString("wCType"));
                    holder.appItem_appStatus.setText("");
                    holder.appItem_appType.setText(Html.fromHtml("<font color='#FF7F27'>出库</font>"));
                    holder.appItem_count.setText("已装:" + (String) getItem(position).getString("count"));
                    holder.appItem_deliver.setText("提单:"+(String) getItem(position).getString("wCTiDan"));
                    holder.appItem_InStockID.setText("箱:"+(String) getItem(position).getString("wCSeries"));
                    holder.appItem_operators.setText((String) getItem(position).getString("OpInput"));
                    //String a = (int) getItem(position).getInt("appCount") / getItem(position).getInt("appPreCount") * 100 + "%";
                    holder.appItem_comPercent.setText("");
                    holder.appItem_bookingTime.setText("");
                }

            }
            catch (JSONException e){
                throw new RuntimeException(e);
            }

            return convertView;
        }
    }


    public String SubString(String src,int maxSize){
        if(src.length()>maxSize){
            return src.substring(0,maxSize-1)+"...";
        }
        else{
            return src;
        }
    }


}
