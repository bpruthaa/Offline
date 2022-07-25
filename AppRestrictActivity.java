package com.example.offline;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
public class AppRestrictActivity extends AppCompatActivity
{
    //Database Related Definitions
    public static final String DATABASE_NAME = "offLineApps.db";
    SQLiteDatabase mDatabase;
    public String thisTime;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //creating of Opening database offLineApps
        mDatabase = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);
        setContentView(R.layout.activity_app_restrict);
        Intent intent = getIntent();
        //Declare ActionBar Variable to get your hands on it
        ActionBar actionBar = getSupportActionBar();
        //Get AppName from Intent
        String thisAppName = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        TextView app = findViewById(R.id.AppName);
        app.setText(thisAppName);
        MainActivity.MyGlobalVariables.mythisAppName  = thisAppName;
        //Get PackageName from Intent
        String thisPackageName = intent.getStringExtra(MainActivity.EXTRA_MESSAGE1);
        TextView pkg = findViewById(R.id.PackageName);
        final ImageView myIcon = findViewById(R.id.imageView);
        pkg.setText(thisPackageName);
        MainActivity.MyGlobalVariables.mythisAppPkgName = thisPackageName;
        final View hoLine = findViewById(R.id.divider);
        final View hoLine1 = findViewById(R.id.divider1);
        final View hoLineThick = findViewById(R.id.dividerThick);
        final View hoLineThick1 = findViewById(R.id.dividerThick1);
        hoLine.setVisibility(hoLine.INVISIBLE);
        hoLine1.setVisibility(hoLine1.INVISIBLE);
        hoLineThick.setVisibility(hoLineThick.INVISIBLE);
        hoLineThick1.setVisibility(hoLineThick.INVISIBLE);
        final TextView lblHours = findViewById(R.id.textViewHours);
        final TextView lblMinutes = findViewById(R.id.textViewMinutes);
        lblHours.setVisibility(lblHours.INVISIBLE);
        lblMinutes.setVisibility(lblMinutes.INVISIBLE);
        final Switch mySwitch = findViewById(R.id.OnOffSwitch);
        final TimePicker picker = findViewById(R.id.timePicker1);
        //Change Interval of Minutes in step of increments of 5
        setTimePickerInterval(picker);
        picker.setIs24HourView(true);
        picker.setVisibility(picker.INVISIBLE);
        if (Build.VERSION.SDK_INT >= 23 ){
            picker.setHour(0);
            picker.setMinute(0);
        }
        assert actionBar != null;
        actionBar.setTitle("Enable / Disable " + thisAppName);
/*
            CONNECT TO DATABASE HERE AND CHECK VALUES OF THE APP PASSED AND ACCORDINGLY SET THE ON / OFF SWITCH
 */
        String sQuery = "SELECT appId,appName,appPkgName,appCriteria FROM main.offLineAppsListMaster WHERE appName='" + thisAppName + "' AND appPkgName='" + thisPackageName + "';";
        Cursor sCursor = mDatabase.rawQuery(sQuery,null);
        Integer iRecords = sCursor.getCount();
        if(iRecords==1)
        {
            sCursor.moveToPosition(0);
            String thisAppCriteria = sCursor.getString(3);
            if(thisAppCriteria.length()==0)
            {
                mySwitch.setChecked(false);
                hoLine.setVisibility(hoLine.INVISIBLE);
                hoLine1.setVisibility(hoLine1.INVISIBLE);
                hoLineThick.setVisibility(hoLineThick.INVISIBLE);
                hoLineThick1.setVisibility(hoLineThick1.INVISIBLE);
                lblHours.setVisibility(lblHours.INVISIBLE);
                lblMinutes.setVisibility(lblMinutes.INVISIBLE);
                picker.setVisibility(picker.INVISIBLE);
                myIcon.setImageResource(R.drawable.ic_cloud_connected);
            }
            else
            {
                mySwitch.setChecked(true);
                hoLine.setVisibility(hoLine.VISIBLE);
                hoLine1.setVisibility(hoLine1.VISIBLE);
                hoLineThick.setVisibility(hoLineThick.VISIBLE);
                hoLineThick1.setVisibility(hoLineThick1.VISIBLE);
                lblHours.setVisibility(lblHours.VISIBLE);
                lblMinutes.setVisibility(lblMinutes.VISIBLE);
                picker.setVisibility(picker.VISIBLE);
                String thisHour = new String(thisAppCriteria.substring(1,2));
                String thisMinute = thisAppCriteria.substring(3,5);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    picker.setHour(Integer.parseInt(thisHour));
                    picker.setMinute(Integer.parseInt(thisMinute)/5);
                }
                myIcon.setImageResource(R.drawable.ic_cloud_disconnected);
            }
        }
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(buttonView.isChecked() == true)
                {
                    hoLine.setVisibility(hoLine.VISIBLE);
                    hoLine1.setVisibility(hoLine1.VISIBLE);
                    hoLineThick.setVisibility(hoLineThick.VISIBLE);
                    hoLineThick1.setVisibility(hoLineThick1.VISIBLE);
                    lblHours.setVisibility(lblHours.VISIBLE);
                    lblMinutes.setVisibility(lblMinutes.VISIBLE);
                    picker.setVisibility(picker.VISIBLE);
                    myIcon.setImageResource(R.drawable.ic_cloud_disconnected);
                }
                if(buttonView.isChecked() == false)
                {
                    hoLine.setVisibility(hoLine.INVISIBLE);
                    hoLine1.setVisibility(hoLine1.INVISIBLE);
                    hoLineThick.setVisibility(hoLineThick.INVISIBLE);
                    hoLineThick1.setVisibility(hoLineThick1.INVISIBLE);
                    lblHours.setVisibility(lblHours.INVISIBLE);
                    lblMinutes.setVisibility(lblMinutes.INVISIBLE);
                    picker.setVisibility(picker.INVISIBLE);
                    myIcon.setImageResource(R.drawable.ic_cloud_connected);
                }
            }
        });
        picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
            {
                String thisHour = String.valueOf(hourOfDay);
                if(thisHour.length() == 1)
                {
                    thisHour = "0" + thisHour;
                }
                String thisMinute = String.valueOf(minute * 5);
                if(thisMinute.length()==1)
                {
                    thisMinute = "0" + thisMinute;
                }
                thisTime = thisHour + ":" + thisMinute;
                MainActivity.MyGlobalVariables.mythisTime = thisTime;
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        final Switch mySwitch = findViewById(R.id.OnOffSwitch);
        final TimePicker picker = findViewById(R.id.timePicker1);
        String uQuery;
        Cursor uCursor;
        final long affectedRowCount;
        switch (item.getItemId())
        {
            case android.R.id.home:
                if(mySwitch.isChecked() == false) //Switch is OFF
                {
                    uQuery = "UPDATE offlineAppsListMaster SET appCriteria='' WHERE appName='" + MainActivity.MyGlobalVariables.mythisAppName + "' AND appPkgName='" + MainActivity.MyGlobalVariables.mythisAppPkgName + "';";
                    uCursor = mDatabase.rawQuery(uQuery,null);
                    Integer iRecords = uCursor.getCount();
                }
                else if((mySwitch.isChecked() != false)) //Switch is ON
                {
                    if(MainActivity.MyGlobalVariables.mythisTime.toString().equals("00:00")) //Switch is ON and Time is "00:00"
                    {
                        uQuery = "UPDATE offlineAppsListMaster SET appCriteria='' WHERE appName='" + MainActivity.MyGlobalVariables.mythisAppName + "' AND appPkgName='" + MainActivity.MyGlobalVariables.mythisAppPkgName + "';";
                        uCursor = mDatabase.rawQuery(uQuery,null);
                        Integer iRecords = uCursor.getCount();
                    }
                    else if(!MainActivity.MyGlobalVariables.mythisTime.toString().equals("00:00")) //Switch is ON and Time is NOT "00:00"
                    {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            int tHour = picker.getHour();
                            int tMinute = picker.getMinute()*5;
                            if((tHour > 0) || (tMinute > 0))
                            {
                                String thisHour = String.valueOf(tHour);
                                String thisMinute = String.valueOf(tMinute);
                                if(thisHour.length() == 1)
                                {
                                    thisHour = "0" + thisHour;
                                }
                                if(thisMinute.length()==1)
                                {
                                    thisMinute = "0" + thisMinute;
                                }
                                thisTime = thisHour + ":" + thisMinute;
                                MainActivity.MyGlobalVariables.mythisTime = thisTime;
                            }
                        }
                        if(MainActivity.MyGlobalVariables.mythisTime.toString().isEmpty()) //Switch is ON and Time is EMPTY
                        {
                            uQuery = "UPDATE offlineAppsListMaster SET appCriteria='' WHERE appName='" + MainActivity.MyGlobalVariables.mythisAppName + "' AND appPkgName='" + MainActivity.MyGlobalVariables.mythisAppPkgName + "';";
                            uCursor = mDatabase.rawQuery(uQuery,null);
                            Integer iRecords = uCursor.getCount();
                        }
                        else //Switch is ON and Time is Greater than "00:00"
                        {
                            uQuery = "UPDATE offlineAppsListMaster SET appCriteria='"+ MainActivity.MyGlobalVariables.mythisTime +"' WHERE appName='" + MainActivity.MyGlobalVariables.mythisAppName + "' AND appPkgName='" + MainActivity.MyGlobalVariables.mythisAppPkgName + "';";
                            uCursor = mDatabase.rawQuery(uQuery,null);
                            Integer iRecords = uCursor.getCount();
                        }
                    }
                }
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed()
    {
        Toast.makeText(getApplicationContext(),"Please Use Back Arrow \'<--\'\nAt The Top To Go Back",Toast.LENGTH_LONG).show();
    }
    private void setTimePickerInterval(TimePicker timePicker)
    {
        try {
            Class<?> classForThisId = Class.forName("com.android.internal.R$id");
            // Field timePickerField = classForThisId.getField("timePicker");
            Field field = classForThisId.getField("minute");
            NumberPicker minutePicker = (NumberPicker) timePicker.findViewById(field.getInt(null));
            minutePicker.setMinValue(0);
            minutePicker.setMaxValue(11);
            ArrayList<String> displayedValues = new ArrayList<String>();
            for (int i = 0; i < 60; i += 5) {
                displayedValues.add(String.format("%02d", i));
            }
            minutePicker.setDisplayedValues(displayedValues
                    .toArray(new String[0]));
            minutePicker.setWrapSelectorWheel(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}