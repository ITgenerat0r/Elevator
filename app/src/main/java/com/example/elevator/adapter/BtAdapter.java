package com.example.elevator.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.elevator.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BtAdapter extends ArrayAdapter<ListItem> {
    public static final String DEF_ITEM_TYPE = "normal";
    public static final String TITLE_ITEM_TYPE = "title";
    public static final String DISCOVERY_ITEM_TYPE = "discovery";
    private List<ListItem> mainList;
    private List<ViewHolder> listViewHolders;
    private SharedPreferences preferences; // Объявляем переменную (класс) для хранения простых типов данных в памяти
    private boolean isDiscoveryType = false;


    // Constructor
    public BtAdapter(@NonNull Context context, int resource, List<ListItem> btList) {
        super(context, resource, btList);
        mainList = btList;
        listViewHolders = new ArrayList<>();
        preferences = context.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        switch (mainList.get(position).getItemType()){
            case TITLE_ITEM_TYPE:
                convertView = titleItem(convertView, parent);
                break;
            default:
                convertView = defaultItem(convertView, position, parent);
            break;
        }
        return convertView;
    }

    // сохраняем данные в память телефона
    private void savePref(int position){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(BtConsts.MAC_KEY, mainList.get(position).getBtDevice().getAddress());
        editor.apply();
        Log.d("MainLog", "Save MAC " + mainList.get(position).getBtDevice().getAddress() + " in preferences.");
    }

    // Класс для запоминания ListItem(структура<Name, CheckBox>) которые были отображены и пролистаны вверх
    static class ViewHolder{
        TextView tvBtName;
        CheckBox chBtSelected;
        TextView rssi;
    }

    // отрисовка обычного элемента
    private View defaultItem(View convertView, int position, ViewGroup parent){
        ViewHolder viewHolder;
        boolean hasViewHolder = false;
        if(convertView != null){
            hasViewHolder = (convertView.getTag() instanceof ViewHolder);
        }
        if(convertView == null || !hasViewHolder){
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bt_list_item, null, false);
            viewHolder.tvBtName = convertView.findViewById(R.id.tvBtName);
            //if(convertView.findViewById(R.id.tvBtName) == null){
            //    viewHolder.tvBtName = convertView.findViewById(R.id.tvBtName);
            //}
            viewHolder.rssi = convertView.findViewById(R.id.view_rssi);
            viewHolder.chBtSelected = convertView.findViewById(R.id.checkBox);
            convertView.setTag(viewHolder);
            listViewHolders.add(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.chBtSelected.setChecked(false);
        }

        if(mainList.get(position).getItemType().equals(BtAdapter.DISCOVERY_ITEM_TYPE)){
            viewHolder.chBtSelected.setVisibility(View.VISIBLE);
            isDiscoveryType = true;
        } else {
            viewHolder.chBtSelected.setVisibility(View.VISIBLE);
            isDiscoveryType = false;
        }
        if(mainList.get(position).getBtDevice().getName() == null){
            viewHolder.tvBtName.setText(mainList.get(position).getBtDevice().getAddress());
        }else{
            viewHolder.tvBtName.setText(mainList.get(position).getBtDevice().getName());
        }
        viewHolder.rssi.setText(mainList.get(position).getRssi());

        viewHolder.chBtSelected.setOnClickListener(v -> {
            if(!isDiscoveryType) {
                for (ViewHolder holder : listViewHolders) {
                    holder.chBtSelected.setChecked(false);
                }
                viewHolder.chBtSelected.setChecked(true);
                savePref(position);
            }
        });

        if(preferences.getString(BtConsts.MAC_KEY, "none").equals(mainList.get(position).getBtDevice().getAddress())){
            viewHolder.chBtSelected.setChecked(true);
        }

        isDiscoveryType = false;

        return convertView;

    }

    // отрисовка заголовка
    private View titleItem(View convertView, ViewGroup parent){
        boolean hasViewHolder = false;
        if(convertView != null){
            hasViewHolder = (convertView.getTag() instanceof ViewHolder); // вернет true если convertView имеет тег ViewHolder
        }
        if(convertView == null || hasViewHolder){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bt_list_item_title, null, false);
        }

        return convertView;

    }
}
