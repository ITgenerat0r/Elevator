package com.example.elevator.adapter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



import com.example.elevator.ListElevatorsActivity;
import com.example.elevator.MainActivity;
import com.example.elevator.R;
import com.example.elevator.Settings;
import com.example.elevator.qrcode.QRcode;
//import tarun0.com.zxing_standalone
import com.google.zxing.client.android.CaptureActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;



public class Address_adapter extends ArrayAdapter<ListItemAddress> {



    final String TAG = this.getClass().getSimpleName();
    final static String string_address = "Добавить адрес";

    private Context contextG;
    private List<ListItemAddress> mainList;
    private List<ViewHolder> listViewHolders;
    private SharedPreferences preferences; // Объявляем переменную (класс) для хранения простых типов данных в памяти
    private SetAddressInPreferences storage;
    private boolean changed;

    // Класс для запоминания ListItemAddress, которые были отображены и пролистаны вверх
    static class ViewHolder{
        TextView comment_field;
        Button Address_field;
        Button delete_field;
    }




    @RequiresApi(api = Build.VERSION_CODES.M)
    public Address_adapter(@NonNull Context context, int resource, List<ListItemAddress> btList) {
        super(context, resource, btList);
        changed = false;
        contextG = context;
        ListItemAddress elem = new ListItemAddress();
        elem.setComment("");
        elem.setAddress(string_address);
        mainList = btList;
        mainList.add(elem);
        listViewHolders = new ArrayList<>();
        preferences = context.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        convertView = defaultItem(convertView, position, parent);
        return convertView;
    }

    public boolean isChanged() {
        return changed;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private View defaultItem(View convertView, int position, ViewGroup parent){
        ViewHolder viewHolder;
        boolean hasViewHolder = false;
        if(convertView != null){
            hasViewHolder = (convertView.getTag() instanceof BtAdapter.ViewHolder);
        }
        if(convertView == null || !hasViewHolder){ // true = new
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, null, false);
            viewHolder.Address_field = convertView.findViewById(R.id.button_address);
            viewHolder.delete_field = convertView.findViewById(R.id.button_delete);
            viewHolder.comment_field = convertView.findViewById(R.id.textView_comment);
            convertView.setTag(viewHolder);
            listViewHolders.add(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String fill = mainList.get(position).getAddress();
        if(fill != string_address){
            fill = mainList.get(position).getName() + " (" + fill + ")";
            Log.d(TAG, "fill = " + fill);
        }
        viewHolder.Address_field.setText(fill);
        if(mainList.get(position).getAddress() == string_address) {
            viewHolder.delete_field.setBackground(null);
        } else {
            //
        }
        viewHolder.comment_field.setText(mainList.get(position).getComment());


        viewHolder.delete_field.setOnClickListener(v -> {
            if(mainList.get(position).getAddress() == string_address){
                return;
            }
            Log.d(TAG, "Pressed 'Delete' button" + position);
            int ln = viewHolder.Address_field.getText().toString().length();
            String addres_for_delete = viewHolder.Address_field.getText().toString().substring(ln-18, ln-1);
            // Удалить везде вручную, в preferences, в mainList и в ListViewHolders
            Log.d(TAG, "Delete " + addres_for_delete);
            storage = new SetAddressInPreferences(this.contextG);
//            storage.read();
            storage.delete(addres_for_delete);
            storage.write();
//            mainList.remove(position);
//            listViewHolders.remove(position);
            changed = true;
        });

        viewHolder.Address_field.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(BtConsts.MY_ADDRESS, mainList.get(position).getAddress());
            editor.apply();
            if(mainList.get(position).getAddress().equals(string_address)){
                Log.d(TAG, "Pressed 'Add' button");
//                Intent settings_activity = new Intent(contextG, Settings.class);
//                contextG.startActivity(settings_activity);

                Intent intent = new Intent(contextG, CaptureActivity.class);
                intent.setAction("com.google.zxing.client.android.SCAN");
                intent.putExtra("SAVE_HISTORY", false);
                contextG.startActivity(intent);
//                startActivityForResult(intent, 0);
            } else {
                Log.d(TAG, "Pressed 'Edit' button" + position);
                Intent settings_activity = new Intent(contextG, Settings.class);
                contextG.startActivity(settings_activity);
            }
//            changed = true;
        });



        return convertView;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }



//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == 0) {
//            if (resultCode == RESULT_OK) {
//                String contents = data.getStringExtra("SCAN_RESULT");
//                Log.d(TAG, "contents: " + contents);
//            } else if (resultCode == RESULT_CANCELED) {
//                Log.d(TAG, "RESULT_CANCELED");
//            }
//        }
//    }


}
