package com.example.offline;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
public class MainActivity extends AppCompatActivity
{
    public static final String EXTRA_MESSAGE = "com.example.offline.MESSAGE";
    public static final String EXTRA_MESSAGE1 = "com.example.offline.MESSAGE1";
    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 1101;
    //Database Related Definitions
    public static final String DATABASE_NAME = "offLineApps.db";
    SQLiteDatabase mDatabase;
    private static final int TIME_DELAY = 2000; //for back button press event onBackPressed();
    private static long back_pressed;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Setting the AppTheme to display the activity
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        final TextView tv = findViewById(R.id.tv);
        final ListView listViewApps= findViewById(R.id.list_app);
        //creating or Opening database offLineApps.db
        mDatabase = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);
        //Create Tables
        //1. offlineAppsListMaster
        createAppsListMasterTable();
        //2. offLineAppsUsageHistory
        createAppsUsageHistoryTable();
        //Get the information of all the installed applications on the device
        final PackageInformation packageInformation=new PackageInformation(MainActivity.this);
        ArrayList<PackageInformation.InfoObject> apps= null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            apps = packageInformation.getInstalledApps(mDatabase);
        }
        if (!hasPermission()) {
            startActivityForResult(
                    new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                    MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
        }
        //Now go for Filling the listView and User clicking an item and starting new activity
        final CustomAdapterAppList adapterAppList=new CustomAdapterAppList(MainActivity.this,apps,mDatabase);
        adapterAppList.notifyDataSetChanged();
        listViewApps.setAdapter(adapterAppList);
        listViewApps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /* Get the selected item text from ListView */
                PackageInformation.InfoObject selectedItem = (PackageInformation.InfoObject) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this,AppRestrictActivity.class);
                String thisAppName = selectedItem.appname;
                String thisPackageName = selectedItem.packagename;
                intent.putExtra(EXTRA_MESSAGE,thisAppName);
                intent.putExtra(EXTRA_MESSAGE1,thisPackageName);
                startActivity(intent);
            }
        });
        MyGlobalVariables.mythisTime="";
        MyGlobalVariables.mythisAppName="";
        MyGlobalVariables.mythisAppPkgName="";
    }
    private void createAppsListMasterTable() {
        //**********************************************
        // 1. this method will create the table offlineAppsListMaster
        //**********************************************
        //as we are going to call this method every time, we will launch the application
        //I have added IF NOT EXISTS to the SQL
        //so it will only create the table when the table is not already created
        mDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS offlineAppsListMaster (\n" +
                        "    appId INTEGER NOT NULL CONSTRAINT offlineAppsListMaster_pk PRIMARY KEY AUTOINCREMENT,\n" +
                        "    appName varchar(30) NOT NULL,\n" +
                        "    appPkgName varchar(70) NOT NULL,\n" +
                        "    appCriteria datetime DEFAULT ''\n" +
                        ");"
        );
    }
    private void createAppsUsageHistoryTable() {
        //**********************************************
        //2. this method will create the table offLineAppsUsageHistory
        //**********************************************
        //as we are going to call this method every time, we will launch the application
        //I have added IF NOT EXISTS to the SQL
        //so it will only create the table when the table is not already created
        mDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS offlineAppsUsageHistory (\n" +
                        "    appId INTEGER NOT NULL CONSTRAINT offlineAppsListMaster_fk REFERENCES offlineAppsListMaster(appId),\n" +
                        "    appUsageDate datetime NOT NULL,\n" +
                        "    appUsageTime datetime NOT NULL\n" +
                        ");"
        );
    }
    protected void onResume() {
        super.onResume();
        if(!isMyServiceRunning(offLineBackBone.class))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                RestartServiceBroadcastReceiver.scheduleJob(getApplicationContext());
            } else {
                ProcessMainClass bck = new ProcessMainClass();
                bck.launchService(getApplicationContext());
            }
        }
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    @Override
    public void onBackPressed() {
        if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(getBaseContext(), "Press once again to exit!", Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS) {
            if (!hasPermission()) {
                //“Apps with usage access”
                startActivityForResult(
                        new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                        MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean hasPermission() {
        //“Apps with usage access”
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            assert appOps != null;
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    public static class MyGlobalVariables {
        public static int CATEGORY_UNDEFINED = -1;
        public static int CATEGORY_GAME = 0;
        public static int CATEGORY_AUDIO = 1;
        public static int CATEGORY_VIDEO = 2;
        public static int CATEGORY_IMAGE = 3;
        public static int CATEGORY_SOCIAL = 4;
        public static int CATEGORY_NEWS = 5;
        public static int CATEGORY_MAPS = 6;
        public static int CATEGORY_PRODUCTIVITY = 7;
        public static String mythisAppName;
        public static String mythisAppPkgName;
        public static String mythisTime;
        public static final String RESTART_INTENT = "com.example.offline";
    }
    protected void onDestroy()
    {
        Log.i("MAINACT", "onDestroy: ");
        super.onDestroy();
    }
/*    public void startService() {
        //Not used in this module after adding code to run the service continuously
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startService(new Intent(getBaseContext(), offLineBackBone.class));
    }
    // Method to stop the service
    public void stopService() {
        stopService(new Intent(getBaseContext(), offLineBackBone.class));
    }*/
}