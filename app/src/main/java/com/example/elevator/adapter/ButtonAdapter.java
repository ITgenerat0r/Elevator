package com.example.elevator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.elevator.R;

import java.util.ArrayList;
import java.util.List;



public class ButtonAdapter extends ArrayAdapter<BtnItem> {

    private List<BtnItem> mainList;
    private List<BtnHolder> listBtnHolders;

    public ButtonAdapter(@NonNull Context context, int resource, List<BtnItem> btnList) {
        super(context, resource, btnList);
        mainList = btnList;
        listBtnHolders = new ArrayList<>();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        convertView = defaultItem(convertView, position, parent);
        return convertView;
    }

    static class BtnHolder{
        Button btn;
    }

    private View defaultItem(View convertView, int position, ViewGroup parent){
        BtnHolder btnHolder;
        boolean hasBtnHolder = false;
        if(convertView != null){
            hasBtnHolder = (convertView.getTag() instanceof BtnHolder);
        }
        if(convertView == null || !hasBtnHolder){
            btnHolder = new BtnHolder();
            btnHolder.btn.setText(position + 1);
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.button_item, null, false);
            btnHolder.btn = convertView.findViewById(R.id.btn_lift);
            convertView.setTag(btnHolder);
            listBtnHolders.add(btnHolder);
        } else {
            btnHolder = (BtnHolder) convertView.getTag();
        }

        btnHolder.btn.setOnClickListener(v -> {
            //
        });
        return convertView;
    }
}
