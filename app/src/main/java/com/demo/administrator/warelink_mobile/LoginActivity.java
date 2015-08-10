package com.demo.administrator.warelink_mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
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
 * Created by Administrator on 2015/3/17.
 */
public class LoginActivity extends Activity{
    private static final int COMPLETED = 0;
    private static final int NOTFOUNDUSER = -1;
    private static final int CONNECTION_LOST = 100;
    private TextView label;
    private CheckBox mcheckbox;
    private EditText loginName,loginPassword;
    private Context mContext;

    private int userID = 0;
    private String savedUserName = "";
    private String savedUserCode = "";
    private JSONArray jsonData; //获取的登陆信息

    ConnectionDetector cd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Intent i = getIntent();
        Toast.makeText(getBaseContext(),"如果主机连接不上，请检主机地址"+getString(R.string.dbAddress)+",或检查主机数据库程序",Toast.LENGTH_LONG).show();
        //Toast msg = Toast.makeText(getBaseContext(),"尝试登陆服务器",Toast.LENGTH_LONG);
        //msg.show();

        label = (TextView)findViewById(R.id.loginHint);
        mcheckbox = (CheckBox)findViewById(R.id.loginCheckBoxRememPS);
        loginName = (EditText)findViewById(R.id.loginUser);
        loginPassword = (EditText)findViewById(R.id.loginPassword);
        mContext = getBaseContext();
        //登陆
        Button submitBtn = (Button)findViewById(R.id.loginSubmit);
        submitBtn.setOnClickListener(new Button.OnClickListener(){
            public  void onClick(View v){
                //if(cd.isIpReachable(getString(R.string.dbAddress))) {
                    ProgressBar bar = (ProgressBar) findViewById(R.id.loginProgressBar);
                    bar.setIndeterminate(true);
                    bar.setVisibility(View.VISIBLE);
                    new Thread() {
                        @Override
                        public void run() {
                            TryLogin(1);
                        }
                    }.start();
                //}
                //else{
                //    Log.e("网络出了问题","休息一会再干活");
                //}
            }
        });

        cd = new ConnectionDetector(getApplicationContext());

    }

    public void TryLogin(int debug) {
        String host = (String)getString(R.string.dbAddress);
        String url = "http://"+host+"/ws/warhouse/WebServer/phpSearch.php?";
        //HttpPost request = new HttpPost(url);
        //带参数 方法2 NameValuePair
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("table", "wusers"));
        //postParameters.add(new BasicNameValuePair("param2", "param2_value"));
        String req = url+ URLEncodedUtils.format(params, HTTP.UTF_8);

        //JSONArray jsonA =  new JSONArray();
        JSONObject result = new JSONObject();
        jsonData = get(req);

        //String userNameCODE = loginName.getText().toString();
        String LoginBox_ps = loginPassword.getText().toString();
        String LoginBox_userName = loginName.getText().toString();
        Boolean findMatch = false;
        int mode = 0;
        try {
            if(jsonData!=null) {
                for (int i = 0; i < jsonData.length(); i++) {
                    result = (JSONObject) jsonData.getJSONObject(i);
                    String userName = result.getString("wuName");
                    String userCodedb = result.getString("CODE");
                    String psdb = result.getString("wuPassword");


                    if ((userName.equals(LoginBox_userName) && psdb.equals(LoginBox_ps)) || (userCodedb.equals(LoginBox_userName) && psdb.equals(LoginBox_ps))) {
                        Log.e("LoginAc登陆", result.getInt("userID") + "是用户ID" + ",用户名是" + userName + ",登陆缩写是" + userCodedb);
                        userID = result.getInt("userID");
                        findMatch = true;
                        mode = result.getInt("job");
                        savedUserName = userName;
                        savedUserCode = userCodedb;
                        break;
                    }
                }
            }
        }catch (JSONException e){
            throw new RuntimeException(e);
        }

        //登陆成功
        if(findMatch){
            //登陆成功，记录
            Log.d("测试打印d", "登陆成功");
            if(mcheckbox.isChecked()){
                MyPreference.getInstance(mContext).SetLoginName(savedUserName);
                MyPreference.getInstance(mContext).SetLoginCode(savedUserCode);
                MyPreference.getInstance(mContext).SetPassword(LoginBox_ps.toString());
                MyPreference.getInstance(mContext).SetIsSavePwd(mcheckbox.isChecked());
                MyPreference.getInstance(mContext).setUserID(userID);
            }else{
                MyPreference.getInstance(mContext) .SetLoginName("");
                MyPreference.getInstance(mContext).SetPassword("");
                MyPreference.getInstance(mContext).SetLoginCode("");
                MyPreference.getInstance(mContext).SetIsSavePwd(mcheckbox.isChecked());
                MyPreference.getInstance(mContext).setUserID(0);
            }

            if(debug == 1) {
                Log.d("测试打印d", "开始跳转");
                if(mode == 7){ //决定跳转哪个类型
                    Intent i = new Intent(getApplicationContext(), MonitorActivity.class);
                    i.putExtra("id", "登陆成功" + savedUserName.toString());
                    startActivity(i);

                }
                else if(mode == 5){
                    //跳转司机
                    Intent i = new Intent(getApplicationContext(),TruckHomeActivity.class);
                    Log.d("跳转司机界面","司机"+savedUserName+"登陆");
                    i.putExtra("userName",savedUserName.toString());
                    startActivity(i);
                }
                else{
                    Intent i = new Intent(getApplicationContext(), FrontPageActivity.class);
                    i.putExtra("id", "登陆成功" + savedUserName);
                    startActivity(i);
                }


            }

        }
        //发送handler事件
        Message msg = new Message();
        if(findMatch)
            msg.what = COMPLETED;
        else
            msg.what = NOTFOUNDUSER;
        handler.sendMessage(msg);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mContext = getBaseContext();
        mcheckbox.setChecked(MyPreference.getInstance(mContext).IsSavePwd());
        if (mcheckbox.isChecked()) {
            loginName.setText(MyPreference.getInstance(mContext)
                    .getLoginName());
            loginPassword
                    .setText(MyPreference.getInstance(mContext).getPassword());
        }
    }

    public JSONArray get(String url){
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        JSONArray json = null;
        try{
            Log.d("目标",url);
            HttpResponse res = client.execute(get);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = res.getEntity();
                String retStr = EntityUtils.toString(entity);
                Log.d("返回文字",retStr);
                json = new JSONArray(retStr);
                //json = new JSONArray(new JSONTokener(new InputStreamReader(entity.getContent(),HTTP.UTF_8)));
            }
            else{
                Log.e("网络失败", url+"地址拒绝访问");
                //Toast.makeText(,"fdsaf",Toast.LENGTH_SHORT);
                Thread.currentThread().sleep(5000);

            }
        }catch (Exception e){
            //e.printStackTrace();
            Log.e("网络失败", url+"地址拒绝访问,估计是服务器没开");
            //throw new RuntimeException(e);
            //Thread.currentThread().sleep(5000);
            Message msg = new Message();
            msg.what = CONNECTION_LOST;
            handler.sendMessage(msg);

        }finally {
            client.getConnectionManager().shutdown();
        }
        //Toast.makeText(mContext,"fdsaf",Toast.LENGTH_SHORT);
        return json;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == COMPLETED) {
                label.setText("登陆成功");
                ProgressBar bar =(ProgressBar)findViewById(R.id.loginProgressBar);
                bar.setVisibility(View.INVISIBLE);
            }
            else if(msg.what == NOTFOUNDUSER){
                label.setText("未找到合适用户，请重新确认");
                ProgressBar bar =(ProgressBar)findViewById(R.id.loginProgressBar);
                bar.setVisibility(View.INVISIBLE);
            }
            else if(msg.what == CONNECTION_LOST){
                Toast.makeText(LoginActivity.this,"网络出错"+getString(R.string.dbAddress)+"无法访问",Toast.LENGTH_LONG).show();
            }
        }
    };
}
