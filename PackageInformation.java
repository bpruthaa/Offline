package com.example.offline;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PackageInformation {
    private Context mContext;
    public PackageInformation(Context context) {
        mContext = context;
    }
    private String sQuery;
    private String iQuery;
    public static final String TAG = "PackageInformation";
    /*
     * Get all the installed app excluding system apps
     * */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public ArrayList<InfoObject> getInstalledApps(SQLiteDatabase mDatabase) {
        ArrayList<InfoObject> listObj = new ArrayList<>();
        final PackageManager packageManager = mContext.getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        //Now Loop Through Applications / Packages array
        for (ApplicationInfo applicationInfo : packages) {
//            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) { //Disable this to get list of all apps.
            InfoObject newInfo = new InfoObject();
            newInfo.appname = applicationInfo.loadLabel(packageManager).toString();
            newInfo.packagename = applicationInfo.packageName;
            newInfo.icon = applicationInfo.loadIcon(packageManager);
            newInfo.launchactivity = packageManager.getLaunchIntentForPackage(applicationInfo.packageName);
            newInfo.tt = applicationInfo.category;
                if (newInfo.launchactivity != null) {
//                    Log.d(TAG, "DEVENBHATT getInstalledApps: \tPackageName : " + newInfo.packagename + " \t Category : " + newInfo.tt);
                    if((newInfo.tt == MainActivity.MyGlobalVariables.CATEGORY_GAME) || (newInfo.tt == MainActivity.MyGlobalVariables.CATEGORY_SOCIAL) || (newInfo.tt == MainActivity.MyGlobalVariables.CATEGORY_NEWS))
                    {
                        //Check the master table to confirm the master entry of this app and insert it if not found
                        sQuery = "SELECT * FROM main.offLineAppsListMaster WHERE appName='" + newInfo.appname + "' AND appPkgName='" + newInfo.packagename + "';";
                        Cursor sCursor = mDatabase.rawQuery(sQuery, null);
                        Integer iRecords = sCursor.getCount();
                        if(iRecords==0)
                        {
                            iQuery = "INSERT INTO main.offLineAppsListMaster(appName,appPkgName,appCriteria) VALUES('" + newInfo.appname + "','" + newInfo.packagename + "','');";
                            Cursor iCursor = mDatabase.rawQuery(iQuery,null);
                            Integer iRecordsInserted = iCursor.getCount();
                            if(iRecordsInserted==0)
                            {
                                //Close the cursor of the Insert Query as the record is inserted and the cursor is no longer required for this iteration of the loop
                                iCursor.close();
                            }
                        }
                        else
                        {
                            //Close the cursor of Select Query as the record is selected and the cursor is no longer required for this iteration of the loop
                            sCursor.close();
                        }
                        listObj.add(newInfo);
                    }

            }
//             }//Disable this to get list of all apps.
        }
//      Sort the listObj on app name
        Collections.sort(listObj, new Comparator<InfoObject>() {
            @Override
            public int compare(InfoObject o1, InfoObject o2) {
                return o1.appname.compareTo(o2.appname);
            }
        });
        return listObj;
    }
    public class InfoObject {
        public String appname = "";
        public String packagename = "";
        public Intent launchactivity;
        public Drawable icon;
        public int tt;
        public String totalUsage;
    }
}