package com.example.elevator;

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
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.elevator.adapter.BtConsts;
import com.example.elevator.objects.Elevator;
import com.example.elevator.objects.Storage;

public class Settings extends AppCompatActivity {
    final static String TAG = "Settings";
    private SharedPreferences preferences; // Объявляем переменную (класс) для хранения простых типов данных в памяти
    private Storage storage;
    private CheckBox checkBox_autodown;
    private EditText editText_floor;
    private TextView current_address;
    private EditText editText_address;
    private EditText editText_name;
    private EditText editText_comment;
    private Button btn_set_addr;
    final static String string_address = "Добавить адрес";
    private Elevator itemAddress;
    private CharSequence buffer; // для editText_address, хранит предыдущее значение

    private String cur_addr;
    private String cur_name;




    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = this.getSharedPreferences(BtConsts.MY_PREF, Context.MODE_PRIVATE);
        storage = new Storage(this);

        checkBox_autodown = findViewById(R.id.autodown);
        editText_floor = findViewById(R.id.floor);
        current_address = findViewById(R.id.current_address);
        editText_address = findViewById(R.id.home_address);
        editText_name = findViewById(R.id.editText_name);
        btn_set_addr = findViewById(R.id.btn_set_address);
        editText_comment = findViewById(R.id.editText_comment);

        cur_addr = preferences.getString(BtConsts.MAC_KEY, "none");
        cur_name = preferences.getString(BtConsts.LAST_NAME, "last_name");
        current_address.setText(cur_addr);

        itemAddress = storage.getById(Long.parseLong(preferences.getString(BtConsts.MY_ADDRESS, "0")));
        editText_address.setText("" + itemAddress.getId());
        editText_name.setText(itemAddress.getDescription());
        editText_comment.setText("" + itemAddress.isAuto());
        editText_floor.setText("" + itemAddress.getFloor());
        checkBox_autodown.setChecked(itemAddress.isAuto());


        editText_address.addTextChangedListener(new TextWatcher() {
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
                if(count > 14) editText_address.setText(editText_address.getText().toString().substring(0, 13).toUpperCase());
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
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                return true;
            default:
                return false;
        }
    }

    private boolean check_all(String s){
        Log.d(TAG, "check_all(" + s + ");");
        if(s.length() != 17) return false;
        Log.d(TAG, "s.length() == 17");
        int shift = 1;
        for(char i : s.toCharArray()){
//            Log.d(TAG, "i = " + i);
            if(shift == 3){
                if(i != ':'){
                    Log.d(TAG, "Caution: i != ':'! (i == " + i + ")");
                    return false;
                }
            } else if(!check(i)) {
                Log.d(TAG, "Caution: check(i) == false! (i == " + i + ")");
                return false;
            }
            shift++;
            if(shift > 3) shift = 1;
        }
        Log.d(TAG, "check_all() == true");
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void applySettings(View v){
        if(!("" + itemAddress.getId()).equals(string_address)){
            storage.deleteElevatorByID(itemAddress.getId());
        }
        itemAddress.setId(Long.parseLong(editText_address.getText().toString().toUpperCase()));
        itemAddress.setDescription(editText_name.getText().toString());
//        itemAddress.set(editText_comment.getText().toString());
        itemAddress.setAuto(checkBox_autodown.isChecked());
        itemAddress.setFloor(Byte.parseByte(editText_floor.getText().toString()));

        if(storage.getById(itemAddress.getId()).getId() == 0){
            storage.addElevator(itemAddress);
            storage.write();
        } else {
            Toast toast = Toast.makeText( this , "Wrong ID", Toast.LENGTH_LONG);
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
        editor.putString(BtConsts.MY_ADDRESS, editText_address.getText().toString());
        editor.apply();
    }

    // Функция кнопки "Установить"
    @SuppressLint("SetTextI18n")
    public void setAddress(View view){
        String addr = current_address.getText().toString().toUpperCase();
        Log.d("MainLog", "current address = <" + addr + ">, length = " + addr.length());
        if(addr.length() == 17) {
            editText_address.setText(addr.substring(0, addr.length() - 7));
            editText_address.append(addr.substring(addr.length() - 7 , addr.length()));
        }
        editText_name.setText(cur_name);
//        editText_address.setText(cur_addr);
    }
}