package com.example.mrokade.asanaalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private ArrayList<Task> items;
    private ArrayAdapter<Task> itemsAdapter;
    private ListView lvItems;
    public static String token = "";
    public static final String asanaApiUrl = "https://app.asana.com/api/1.0";
    public static final String asanaOauthUrl = "https://app.asana.com/-/oauth_token";
    public final String redirectUrl = "https://www.asanaalarm.co/oauth";
    public final OkHttpClient client = new OkHttpClient();
    public static long remindTagId;
    private AlarmManager alarmMgr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FlowManager.init(this);

        // INIT setup
        lvItems = (ListView) findViewById(R.id.listView);

        //read saved items from DB and populate itemsAdapter
        readItemsDB();
        itemsAdapter = new TaskAdapter(this, items);
        lvItems.setAdapter(itemsAdapter);

        // items remove listener
        setupListViewListener();
//        getAsanaToken();
    }

    private void setupListViewListener() {
        lvItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String name = items.get(position).name;
                List<Task> tasks = SQLite.select().from(Task.class)
                        .where(Task_Table.name.eq(name))
                        .queryList();
                Task t = tasks.get(0);
                t.delete();
                items.remove(position);
                itemsAdapter.notifyDataSetChanged();
                return true;
            }
        });
    }

    public void stopAlarm(View v) {
        Intent webIntent = new Intent(getApplicationContext(), WebAuthActivity.class);
        startActivity(webIntent);
    }

    public void syncTasks(View v) {
        // get token
        token = getAsanaToken();

        // TAG: get remind id
        Request requestTagId = new Request.Builder()
                .addHeader("Authorization", "Bearer " + token)
                .url(asanaApiUrl + "/tags")
                .build();

        client.newCall(requestTagId).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                JSONObject json;
                try {
                    json = new JSONObject(res);
                    JSONArray jsonArray = json.getJSONArray("data");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = (JSONObject) jsonArray.get(i);
                        if (obj.get("name").toString().equals("remind")) {
                            remindTagId = obj.getLong("id");
                            Log.i("REMIND TAG", Long.toString(remindTagId));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // TAG/TASKS:
                String tagTasksUrl = asanaApiUrl + "/tags/" + Long.toString(remindTagId) + "/tasks";

                Request requestTagTasks = new Request.Builder()
                        .addHeader("Authorization", "Bearer " + token)
                        .url(tagTasksUrl)
                        .build();

                client.newCall(requestTagTasks).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String res = response.body().string();
                        Log.i("TAGGED TASKS", res);
                        try {
                            JSONObject json = new JSONObject(res);
                            JSONArray jsonArray = json.getJSONArray("data");
                            Log.i("TAGGED TASKS", jsonArray.toString());

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = (JSONObject) jsonArray.get(i);
                                Long id = obj.getLong("id");
                                String name = obj.getString("name");
                                Task task = null;
                                task = SQLite.select().from(Task.class)
                                        .where(Task_Table.id.eq(id))
                                        .querySingle();

                                if (task == null) {
                                    // For a new task
                                    task = new Task();
                                    task.setName(name);
                                    task.setId(id);
                                    task.save();
                                    getTaskDueDate(token, id, true);
                                } else {
                                    // For already existing task
//                                            task = tasks.get(0);
                                    getTaskDueDate(token, id, false);
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void readItemsDB() {
        //read from DB
        List<Task> tasks = SQLite.select().from(Task.class).queryList();
        items = new ArrayList<Task>();
        if(tasks.isEmpty()){
            Log.i("TASKS", "list is empty");
        } else {
            Log.i("TASKS size", Integer.toString(tasks.size()));
            int len = tasks.size();
            for (int i=0; i < len; i++) {
                Date date = new Date();
                items.add(tasks.get(i));
            }
        }
    }

    private String getAsanaToken() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        token = sharedPreferences.getString("ASANA_TOKEN", null);
        String tokenTime = sharedPreferences.getString("ASANA_TOKEN_TIMESTAMP", null);
        String refreshToken = sharedPreferences.getString("ASANA_REFRESH_TOKEN", null);
        // access token is stored in sharedpreferences
        // if its older than 1 hr get a new one
        if(token != null && tokenTime != null) {
            Date dateToken = new Date(tokenTime);
            Date dateNow = new Date();
            if (dateNow.getTime() - dateToken.getTime() <= 1*60*60*1000) {
                return token;
            }
        }

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", Constants.CLIENT_ID)
                .add("client_secret", Constants.CLIENT_SECRET)
                .add("redirect_uri", redirectUrl)
                .add("refresh_token", refreshToken)
                .build();

        Request request = new Request.Builder()
                .url(asanaOauthUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                JSONObject resJson;
                try {
                    resJson = new JSONObject(res);
                    token = resJson.get("access_token").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Date date = new Date();
        sharedPreferences.edit().putString("ASANA_TOKEN", token);
        sharedPreferences.edit().putString("ASANA_TOKEN_TIMESTAMP", date.toString());
        return token;
    }

    private void getTaskDueDate (String token, final Long taskId, final Boolean addToItems){
        String taskUrl = asanaApiUrl + "/tasks/" + Long.toString(taskId);

        Request request = new Request.Builder()
                .url(taskUrl)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                JSONObject json;
                JSONObject data;
                final Date date;
                final Task task;
                try {
                    json = new JSONObject(res);
                    data = json.getJSONObject("data");
                    String dueDate = data.get("due_at").toString();
                    Log.i("DATETIME", dueDate);
                    String format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Calendar calendar = Calendar.getInstance();

                    try {
                        date = sdf.parse(dueDate);
                        calendar.setTime(date);
                        task = SQLite.select().from(Task.class).where(Task_Table.id.eq(taskId)).querySingle();
                        task.setDueDate(date);
                        Log.i("SAVING TASK", Long.toString(task.id) + " - " + date.toString());
                        task.save();
//                        readItemsDB();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (addToItems) {
                                    items.add(task);
                                }
                                itemsAdapter.notifyDataSetChanged();
                            }
                        });
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    // If date is in future only then set alarm
                    if (calendar.compareTo(Calendar.getInstance()) > 0) {
                        Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);
                        intent.putExtra("KILL", false);
                        PendingIntent alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
                        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void dumpTasksfromDB () {
        List<Task> tasks = SQLite.select().from(Task.class).queryList();
        for (Task t: tasks) {
            Log.i("DB VIEW", Long.toString(t.id) + " - " + t.name + " - " + t.dueDate.toString());
        }
    }

    private void lockScreenAlarm() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + 1);
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        intent.putExtra("KILL", false);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), alarmIntent);
    }

    private void clearCurrentCookies() {
        android.webkit.CookieManager cm = android.webkit.CookieManager.getInstance();
//        cm.removeAllCookies(null);
        cm.setCookie("https://asana.com", null);
    }

    private void checkSharedPrefs() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Log.i("TOKEN", sharedPref.getString("ASANA_ACCESS_TOKEN", "Nothing found"));
        Log.i("CODE", sharedPref.getString("ASANA_GRANT_CODE", "Nothing found"));
        Log.i("REFRESH_TOKEN", sharedPref.getString("ASANA_REFRESH_TOKEN", "Nothing found"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
