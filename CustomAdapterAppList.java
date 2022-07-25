package com.example.offline;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CustomAdapterAppList extends BaseAdapter {
    private ArrayList<PackageInformation.InfoObject> mObjectApps;
    private Context mContext;
    SQLiteDatabase mDatabase;
    public CustomAdapterAppList(Context context, ArrayList<PackageInformation.InfoObject> apps, SQLiteDatabase nDatabase) {
        this.mContext = context;
        this.mObjectApps = apps;
        this.mDatabase = nDatabase;
    }
    @Override
    public int getCount() {
        return mObjectApps.size();
    }
    @Override
    public Object getItem(int position) {
        return mObjectApps.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        String sQuery;
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        convertView = inflater.inflate(R.layout.activity_custom_adapter_app_list, parent, false);
        holder = new ViewHolder();
        holder.appName = (TextView) convertView.findViewById(R.id.text_app_name);
        holder.appIcon = (ImageView) convertView.findViewById(R.id.image_app_icon);
        holder.appOption = (ImageView) convertView.findViewById(R.id.image_option);
        holder.appUsage = (TextView) convertView.findViewById(R.id.text_app_usage);
        //Set values
        final PackageInformation.InfoObject infoObject = mObjectApps.get(position);
        holder.appName.setText(infoObject.appname);
        holder.appIcon.setImageDrawable(infoObject.icon);
        sQuery = "SELECT * FROM main.offLineAppsListMaster WHERE appName='" + mObjectApps.get(position).appname + "' AND appPkgName='" + mObjectApps.get(position).packagename + "' AND appCriteria<>'';";
        Cursor sCursor = mDatabase.rawQuery(sQuery,null);
        int iRecords = sCursor.getCount();
        if(iRecords==0)
        {
            //App is not restricted. Set the Color of text to Black in List view Row
            holder.appName.setTextColor(Color.parseColor("#000000"));
        }
        else if(iRecords==1)
        {
            //App is Restricted. Change the Color of text to Red in List view Row
            holder.appName.setTextColor(Color.parseColor("#FF0000"));
            sCursor.moveToPosition(0);
            String appUCriteria = sCursor.getString(3);
            String thDate = getCurrentDate();

            String sQueryCombined = "SELECT a.appId,a.appName,a.appPkgName,a.appCriteria,b.appId,b.appUsageDate,b.appUsageTime from main.offlineAppsListMaster a, main.offlineAppsUsageHistory b WHERE a.appCriteria != '' AND a.appId=b.appId AND b.appUsageDate='" + thDate + "' AND a.appName='" + infoObject.appname + "' ORDER BY a.appId;";
            Cursor sCursorCombined = mDatabase.rawQuery(sQueryCombined,null);
            int sRecordsCombined = sCursorCombined.getCount();
            if(sRecordsCombined==1)
            {
                sCursorCombined.moveToPosition(0);
                infoObject.totalUsage = "Used for " + sCursorCombined.getString(6) + " from " + sCursorCombined.getString(3);
            }
            else if(sRecordsCombined==0)
            {
                infoObject.totalUsage = "Used for 00:00 from " + appUCriteria;
            }
            holder.appUsage.setText(infoObject.totalUsage);
            holder.appUsage.setTextColor(Color.parseColor("#0000FF"));
            holder.appUsage.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
            holder.appUsage.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            sCursorCombined.close();
        }
        sCursor.close();
        //Set BOLD text
        holder.appName.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        //Click action for app
        holder.appOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open the specific App Info page:
                String packageName = infoObject.packagename;
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageName));
                    mContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Open the generic Apps manager
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    mContext.startActivity(intent);
                }
            }
        });
        return convertView;
    }
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = Calendar.getInstance().getTime();
        return sdf.format(today);
    }
    private class ViewHolder {
        TextView appName;
        TextView appVersion;
        ImageView appIcon;
        ImageView appOption;
        TextView appUsage;
    }
}