package com.example.mrokade.asanaalarm;

/**
 * Created by mrokade on 3/21/16.
 */
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.raizlabs.android.dbflow.sql.language.SQLite;

public class AlarmLockScreenActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_activity);

        Log.i("Lock Screen", "Created");
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Long taskId = getIntent().getLongExtra("taskId", 0);
        Task task = SQLite.select().from(Task.class).where(Task_Table.id.eq(taskId)).querySingle();
        TextView t = (TextView) findViewById(R.id.textView);
        t.setText(task.name);
        Button btn = (Button) findViewById(R.id.button3);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Lock Screen", "Destroying");
                Intent i = new Intent(getApplicationContext(), RingToneService.class);
                stopService(i);
                finish();
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.i("Lock Screen", "Destroyed");
        super.onDestroy();
    }
}