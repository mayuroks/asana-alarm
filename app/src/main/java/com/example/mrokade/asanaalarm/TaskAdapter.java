package com.example.mrokade.asanaalarm;

/**
 * Created by mrokade on 3/21/16.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by mrokade on 3/16/16.
 */
public class TaskAdapter extends ArrayAdapter<Task> {

    public TaskAdapter(Context context, ArrayList<Task> tasks) {
        super(context, 0, tasks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Task task = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.task_item, parent, false);
        }
        TextView alarmTime = (TextView) convertView.findViewById(R.id.alarmTime);
        TextView taskName = (TextView) convertView.findViewById(R.id.taskName);

        Date date = task.dueDate;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm  dd MMMM");
        if (date == null) {
            alarmTime.setText("SHIT");
        } else {
            alarmTime.setText(sdf.format(date));
        }
//        alarmTime.setText("wow");
        taskName.setText(task.name);

        return convertView;
    }
}
