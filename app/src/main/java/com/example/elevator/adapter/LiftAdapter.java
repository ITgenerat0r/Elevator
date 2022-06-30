package com.example.elevator.adapter;

import android.annotation.SuppressLint;
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

    final String TAG = "Address_adapter() -> Lift_Adapter()";
    private Context context;
    private LayoutInflater inflater;
    private List<String > numberFloor;

    @SuppressLint("LongLogTag")
    public LiftAdapter(Context c, List<String> floors) {
        Log.d(TAG, "Constructor()");
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
