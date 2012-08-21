package thu.ir.robot.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

import thu.ir.robot.R;
import thu.ir.robot.util.Zlog;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.iflytek.speech.RecognizerResult;
import com.iflytek.speech.SpeechConfig.RATE;
import com.iflytek.speech.SpeechError;
import com.iflytek.ui.RecognizerDialog;
import com.iflytek.ui.RecognizerDialogListener;

public class MainUI extends Activity implements RecognizerDialogListener {
    private TextView mShowArea;
    private EditText mInputArea;
    private Button mSendButton;
    private ImageView mChatIcon;
    private String mUID;
    private ProgressDialog dialog;
    private ChatTask chatTask;
    private SharedPreferences prefs;

    private SharedPreferences mSharedPreferences;
    private RecognizerDialog iatDialog;
    private LocationManager locationManager;

    public static final String ENGINE_POI = "poi";

    protected static final int PREFERENCE = 1;
    protected static final int QUIT_APP = 2;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, PREFERENCE, 0, R.string.menu_button_preference);
        menu.add(0, QUIT_APP, 0, R.string.menu_button_quit);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case PREFERENCE:
            Intent intent = new Intent(MainUI.this, PreferenceUI.class);
            startActivity(intent);
            break;

        case QUIT_APP:
            System.exit(0);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    class ChatTask extends AsyncTask<String, Void, String> {

        @Override
        public void onPreExecute() {
            dialog = ProgressDialog.show(MainUI.this, "", "正在连接...");
            dialog.setCancelable(true);
            dialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    cancelChat();
                }

            });
        }

        @Override
        protected String doInBackground(String... msg) {
            String resultData = "I:" + msg[0] + "\nRobot:";
            URL url = null;

            try {
                String serverAddress = prefs.getString(getString(R.string.server_editor), getString(R.string.default_server_editor));

                url = new URL("http://" + serverAddress + "/WeatherChatWebService/services/weatherchat?"
                                    + "id=" + mUID + "&"
                                    + "str=" + URLEncoder.encode(msg[0]) + "&"
                                    + "time=" + new Date().getTime());

                Zlog.d("Send request to " + url.toString());

                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setConnectTimeout(5000);

                InputStreamReader in = new InputStreamReader(urlConn.getInputStream());
                BufferedReader buffer = new BufferedReader(in);

                String inputLine = null;

                while (((inputLine = buffer.readLine()) != null)) {
                    resultData += inputLine + "\n";  
                }

                in.close();
                urlConn.disconnect(); 

                return resultData;
            } catch (SocketTimeoutException e) {
                resultData += getString(R.string.server_timeout);
            } catch (Exception e) {
                Zlog.e(e);
                resultData += "Oops...I died...\n";
            }

            return resultData;
        }

        protected void onPostExecute(String result) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            mShowArea.append(result);
            //mShowArea.append("LATITUDE : " + locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude() + "\n");
            //mShowArea.append("LONGITUDE : " + locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude() + "\n");

            int offset = mShowArea.getLineHeight() * mShowArea.getLineCount() - mShowArea.getHeight();

            if (offset > 0) {
                mShowArea.scrollTo(0, offset);
            }

            mInputArea.setEnabled(true);
            mSendButton.setEnabled(true);
        }

        @Override
        protected void onCancelled() {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            mInputArea.setEnabled(true);
            mSendButton.setEnabled(true);

            super.onCancelled();
        }
    }

    private void cancelChat() {
        if (chatTask != null) {
            chatTask.cancel(true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mShowArea = (TextView)findViewById(R.id.mainShowArea);
        mInputArea = (EditText)findViewById(R.id.inputArea);
        mSendButton = (Button)findViewById(R.id.sendButton);
        mChatIcon = (ImageView)findViewById(R.id.voiceInput);
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mUID = tm.getDeviceId();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }

            @Override
            public void onLocationChanged(Location location) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 600000, 0, locationListener);

        mInputArea.setOnKeyListener(new OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMsg();

                    return true;
                }
                return false;
            }
        });

        mSendButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                sendMsg();
            }
        });

        mChatIcon.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                showIatDialog();
            }
        });

        iatDialog = new RecognizerDialog(this, "appid=" + getString(R.string.app_id));
        iatDialog.setListener(this);

        mSharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.main);
    }

    public void sendMsg() {
        String msg = mInputArea.getText().toString();

        if (msg != null && msg.length() > 1) {
            chatTask = new ChatTask();
            chatTask.execute(msg);

            mInputArea.setEnabled(false);
            mSendButton.setEnabled(false);
        }

        mInputArea.setText("");
    }

    public void showIatDialog() {
        String engine = mSharedPreferences.getString(
                getString(R.string.preference_key_iat_engine),
                getString(R.string.preference_default_iat_engine));

        String area = null;
        if (ENGINE_POI.equals(engine)) {
            final String defaultProvince = getString(R.string.preference_default_poi_province);
            String province = mSharedPreferences.getString(
                    getString(R.string.preference_key_poi_province),
                    defaultProvince);
            final String defaultCity = getString(R.string.preference_default_poi_city);
            String city = mSharedPreferences.getString(
                    getString(R.string.preference_key_poi_city),
                    defaultCity);

            if (!defaultProvince.equals(province)) {
                area = "area=" + province;
                if (!defaultCity.equals(city)) {
                    area += city;
                }
            }
        }

        iatDialog.setEngine(engine, area, null);

        String rate = mSharedPreferences.getString(
                getString(R.string.preference_key_iat_rate),
                getString(R.string.preference_default_iat_rate));
        if(rate.equals("rate8k"))
            iatDialog.setSampleRate(RATE.rate8k);
        else if(rate.equals("rate11k"))
            iatDialog.setSampleRate(RATE.rate11k);
        else if(rate.equals("rate16k"))
            iatDialog.setSampleRate(RATE.rate16k);
        else if(rate.equals("rate22k"))
            iatDialog.setSampleRate(RATE.rate22k);
        mInputArea.setText(null);

        iatDialog.show();
    }

    @Override
    public void onEnd(SpeechError error) {
    }

    @Override
    public void onResults(ArrayList<RecognizerResult> results,boolean isLast) {
        StringBuilder builder = new StringBuilder();
        for (RecognizerResult recognizerResult : results) {
            builder.append(recognizerResult.text);
        }

        mInputArea.append(builder.toString());

        sendMsg();
    }
}