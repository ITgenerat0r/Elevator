package com.example.elevator.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.elevator.R;
import com.example.elevator.bluetooth.ConnectThread;

import java.util.List;

public class LiftAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private List<String > numberFloor;

    public LiftAdapter(Context c, List<String> floors) {
        context = c;
        numberFloor = floors;
    }

    @Override
    public int getCount() {
        return numberFloor.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        Log.d("MainLog", "getView()");
        if (inflater == null){
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if (convertView == null){
            convertView = inflater.inflate(R.layout.lift_item, null);
        }

        TextView textView = convertView.findViewById(R.id.number);
        textView.setText("" + (position + 1));

//        Log.d("MainLog", "End getView()");
        return convertView;
//        return null;
    }
}
