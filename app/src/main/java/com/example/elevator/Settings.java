package com.example.elevator;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.elevator.adapter.BtConsts;
import com.example.elevator.adapter.ListItemAddress;
import com.example.elevator.adapter.SetAddressInPreferences;

import static android.widget.Toast.LENGTH_LONG;

public class Settings extends AppCompatActivity {
    private SharedPreferences preferences; // Объявляем переменную (класс) для хранения простых типов данных в памяти
    private SetAddressInPreferences storage;
    private CheckBox checkBox_autodown;
    private EditText editText_floor;
    private TextView current_address;
    private EditText home_address;
    private EditText editText_comment;
    private Button btn_set_addr;
    final static String string_address = "Добавить адрес";
    private ListItemAddress itemAddress;
    private CharSequence buffer; // для home_address, хранит предыдущее значение




    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Settings", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = this.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        storage = new SetAddressInPreferences(this);

        checkBox_autodown = findViewById(R.id.autodown);
        editText_floor = findViewById(R.id.floor);
        current_address = findViewById(R.id.current_address);
        home_address = findViewById(R.id.home_address);
        btn_set_addr = findViewById(R.id.btn_set_address);
        editText_comment = findViewById(R.id.editText_comment);

        String cur_addr = preferences.getString(BtConsts.CURRENT_ADDRESS, "none");
        if (cur_addr.length() > 3) current_address.setText(cur_addr.substring(0, cur_addr.length()-3));

        itemAddress = storage.getItemAddress(preferences.getString(BtConsts.MY_ADDRESS, "none"));
        home_address.setText(itemAddress.getAddress());
        editText_comment.setText(itemAddress.getComment());
        editText_floor.setText("" + itemAddress.getFloor());
        checkBox_autodown.setChecked(itemAddress.isAuto());


        home_address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                Log.d("TEXT_CHANGED_LISTENER", "beforeTextChanged");
//                Log.d("TEXT_CHANGED_LISTENER", s.toString());
//                Log.d("TEXT_CHANGED_LISTENER", String.format("start = %d, count = %d, after = %d", start, count, after));

//                buffer = s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                Log.d("TEXT_CHANGED_LISTENER", "onTextChanged");
//                Log.d("TEXT_CHANGED_LISTENER", s.toString());
//                Log.d("TEXT_CHANGED_LISTENER", String.format("start = %d, before = %d, count = %d", start, before, count));
                if(count > 14) home_address.setText(home_address.getText().toString().substring(0, 13));
            }

            @Override
            public void afterTextChanged(Editable s) {
//                Log.d("TEXT_CHANGED_LISTENER", "afterTextChanged");
//                Log.d("TEXT_CHANGED_LISTENER", s.toString());
                if(check_all(s.toString())){
                    buffer = s;
                }
            }


        });

    }


    private boolean check(char c){
        switch (c){
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
                return true;
            default:
                return false;
        }
    }

    private boolean check_all(String s){
        if(s.length() != 14) return false;
        int shift = 1;
        for(char i : s.toCharArray()){
            if(shift > 3) shift = 1;
            if(shift == 3){
                if(i != ':'){
                    return false;
                }
            } else if(!check(i)) {
                return false;
            }
            shift++;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void applySettings(View v){
        if(!itemAddress.getAddress().equals(string_address)){
            storage.delete(itemAddress.getAddress());
        }
        itemAddress.setAddress(home_address.getText().toString());
        itemAddress.setComment(editText_comment.getText().toString());
        itemAddress.setAuto(checkBox_autodown.isChecked());
        itemAddress.setFloor(Byte.parseByte(editText_floor.getText().toString()));

        if(check_all(itemAddress.getAddress())){
            storage.addItem(itemAddress);
            storage.write();
        } else {
            Toast toast = Toast.makeText( this , "Неверный формат адреса, (Требуется: **:**:**:**:**)", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 10 , 20);
            toast.show();
        }

        Log.d("MainLog", "Apply settings button!");
        finish();
    }

    private void savePref(){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(BtConsts.MY_FLOOR, Integer.parseInt(editText_floor.getText().toString()));
        editor.putBoolean(BtConsts.LIFT_ME, checkBox_autodown.isChecked());
        editor.putString(BtConsts.MY_ADDRESS, home_address.getText().toString());
        editor.apply();
    }

    // Функция кнопки "Установить"
    public void setAddress(View view){
        home_address.setText(current_address.getText());
    }
}