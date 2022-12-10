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
//import tarun0.com.zxing_standalone
import com.example.elevator.objects.Device;
import com.example.elevator.objects.Elevator;
import com.example.elevator.objects.Storage;
import com.google.zxing.client.android.CaptureActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;



public class Address_adapter extends ArrayAdapter<Elevator> {



    final String TAG = this.getClass().getSimpleName();
//    final static String string_address = "Добавить адрес";

    private Context contextG;
    private List<Elevator> mainList;
    private List<ViewHolder> listViewHolders;
    private SharedPreferences preferences; // Объявляем переменную (класс) для хранения простых типов данных в памяти
    private Storage storage;
    private boolean changed;

    // Класс для запоминания Elevator, которые были отображены и пролистаны вверх
    static class ViewHolder{
        TextView comment_field;
        Button Address_field;
        Button delete_field;
    }




    public Address_adapter(@NonNull Context context, int resource, List<Elevator> btList) {
        super(context, resource, btList);
        Log.d(TAG, "Constructor -> " + TAG);
        changed = false;
        contextG = context;
        Elevator elem = new Elevator();
        elem.setDescription("");
        try {
            elem.setDescription(context.getString(R.string.add_an_elevator));
        } catch (Exception e){
            Log.d(TAG, "elem.setId() failed. " + e.toString());
        }

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
        String fill = mainList.get(position).getDescription();
        if(fill != contextG.getString(R.string.add_an_elevator)){
//            fill = fill + " (" + mainList.get(position).getId() + ")";
            Log.d(TAG, "fill = " + fill);
        }
        viewHolder.Address_field.setText(fill);
        if(position + 1 == mainList.size()) {
            viewHolder.delete_field.setBackground(null);
            viewHolder.comment_field.setText("");
        } else {
            viewHolder.comment_field.setText("" + mainList.get(position).getId());
        }


        viewHolder.delete_field.setOnClickListener(v -> {
            if(position + 1 == mainList.size()){
                return;
            }
            Log.d(TAG, "Pressed 'Delete' button on position " + position);
            String addres_for_delete = viewHolder.comment_field.getText().toString();
            // Удалить везде вручную, в preferences, в mainList и в ListViewHolders
            Log.d(TAG, "Delete " + addres_for_delete);
            storage = new Storage(this.contextG);
            storage.deleteElevatorByID(Long.parseLong(addres_for_delete));
            storage.write();
//            mainList.remove(position);
//            listViewHolders.remove(position);
            changed = true;
        });

        viewHolder.Address_field.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(BtConsts.MY_ADDRESS, "" + mainList.get(position).getId());
            editor.apply();
            Log.d(TAG, String.format("Pressed %d/%d", position, mainList.size()));
            if(position + 1 == mainList.size()){
                Log.d(TAG, "Pressed 'Add' button");
//                Intent settings_activity = new Intent(contextG, Settings.class);
//                contextG.startActivity(settings_activity);

                Intent intent = new Intent(contextG, CaptureActivity.class);
                intent.setAction("com.google.zxing.client.android.SCAN");
                intent.putExtra("SAVE_HISTORY", false);
                contextG.startActivity(intent);
//                startActivityForResult(intent, 0);
            } else {
                Log.d(TAG, "Pressed 'Edit' button on position" + position);
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
