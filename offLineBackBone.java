package com.example.offline;

import android.annotation.SuppressLint;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class offLineBackBone extends Service {
    protected static final int NOTIFICATION_ID = 1337;
    private static Service mCurrentService;
    private int counter = 0;
    private int counterTopApp = 0;

    SQLiteDatabase mDatabase,mDatabaseTimer;
    private static String TAG = "com.example.offline." + offLineBackBone.class.getSimpleName() + " :- ";
    public static final String DATABASE_NAME = "offLineApps.db";
    public static final Integer INTERVAL_DAILY = 4;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static List<AppUsageInfo> smallInfoList;
    Integer theAppId;
    String theAppName,thePackageName,theAppCriteria;
    private Context context;
    private static Timer timer;
    private static TimerTask timerTask;
    private static Timer timerTopApp;
    private static TimerTask timerTaskTopApp;
    public offLineBackBone()
    {
        super();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restartForeground();
        }
        mCurrentService = this;
    }
    @SuppressLint("DefaultLocale")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "restarting Service !!");
        counter = 0;
        counterTopApp = 0;
        if (intent == null) {
            ProcessMainClass bck = new ProcessMainClass();
            bck.launchService(this);
        }
        // make sure you call the startForeground on onStartCommand because otherwise
        // when we hide the notification on onScreen it will nto restart in Android 6 and 7
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            restartForeground();
        }
        startTimer();
        // return start sticky so if it is killed by android, it will be restarted with Intent null
        Toast.makeText(this, "Service Started & Start ID is: " + startId, Toast.LENGTH_LONG).show();
        return START_STICKY;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public void restartForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, "restarting foreground");
            try {
                myNotification notification = new myNotification();

                startForeground(NOTIFICATION_ID, notification.setNotification(this, "OffLine Notification", "OffLine Service Running In Background", R.drawable.ic_sleep));
                Log.i(TAG, "restarting foreground successful");
                startTimer();
            } catch (Exception e) {
                Log.e(TAG, "Error in notification " + e.getMessage());
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy called");
        // restart the never ending service
        Intent broadcastIntent = new Intent(MainActivity.MyGlobalVariables.RESTART_INTENT);
        sendBroadcast(broadcastIntent);
        stopTimerTask();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        /**
         * this is called when the process is killed by Android
         *
         * @param rootIntent
         */
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved called");
        // restart the never ending service
        Intent broadcastIntent = new Intent(MainActivity.MyGlobalVariables.RESTART_INTENT);
        sendBroadcast(broadcastIntent);
        // do not call stopTimerTask because on some phones it is called asynchronously
        // after you swipe out the app and therefore sometimes
        // it will stop the timer after it was restarted
        // stopTimerTask();
    }
    public void startTimer() {
        Log.i(TAG, "Starting timer");

        //set a new Timer - if one is already running, cancel it to avoid two running at the same time
        stopTimerTask();
        timer = new Timer();
        timerTopApp = new Timer();


        //initialize the TimerTask's job
        initializeTimerTask(); //Timer for collecting data of desired applications
        initializeTimerTaskTopApp(); //Timer for checking over-usage of applications

        Log.i(TAG, "Scheduling...");
        //schedule the timer, to wake up every 60 seconds
        timer.schedule(timerTask, 1000, 60000); //Gather the Data every minute
        timerTopApp.schedule(timerTaskTopApp,1000,3000); //Check over-usage of applications every 3 seconds
    }
    public void initializeTimerTask() {
        /**
         * it sets the timer to print the counterTopApp every x seconds (60 Seconds here)
         */
        Log.i(TAG, "initialising TimerTask");
        timerTask = new TimerTask()
        {
            public void run()
            {
                Log.i(TAG, "Inside timer ++++  " + (counter++) + " Minute(s) Passed . . .");
                performServiceTask(); //Do the data-collection part of desired applications
            }
        };
    }
    public void initializeTimerTaskTopApp() {
        /**
         * it sets the timer to print the counter every x seconds (5 Seconds here)
         */
        Log.i(TAG, "initialising TimerTaskTopApp");
        timerTaskTopApp = new TimerTask()
        {
            public void run()
            {
//                Log.i(TAG, "Inside TimerTaskTopApp +-+-+-  " + (counterTopApp++)*3 + " Second(s) Passed . . .");
                //Write A Separate Function To Check Top App, Its Actual Usage From History Table And If Usage >= Criteria, Show Home Screen To The User
                showHomeScreen(); //Check and Minimised Over-usage application and thereby restrict the usage of the said application
            }
        };
    }
    public void stopTimerTask() {
        /**
         * not needed
         */
        //stop the timer, if it's not already null
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
        if (timerTaskTopApp != null)
        {
            timerTaskTopApp.cancel();
            timerTaskTopApp = null;
        }
    }
    private void performServiceTask() {
        String tDate = "";
        tDate = getCurrentDate();
        long theUsageTime = 0;
        String ActualRunTimeToday="00:00";
        String sQueryHistory="";

        //GetEventStats here
        getAppStats(getApplicationContext(),tDate);
        //Declare variable for filtered usage list
        List<AppUsageInfo> resultList = new ArrayList<AppUsageInfo>();

        //Open Database Commands here
        //creating or Opening database offLineApps.db
        mDatabase = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);
        String sQuery = "SELECT * FROM main.offLineAppsListMaster WHERE appCriteria!='';";
        Cursor sCursor = mDatabase.rawQuery(sQuery,null);
        int sRecords = sCursor.getCount();
        if(sRecords > 0)
        {
            sCursor.moveToPosition(0);
            do {
                //Now loop through the records and gather getTotalTimeInForeground values for each.
                thePackageName = sCursor.getString(2);
                //Filter the UsageList now and narrow it down to our purpose of apps
                for(AppUsageInfo apps : smallInfoList)
                {
                    if(apps.packageName.equals(thePackageName))
                    {
                        resultList.add(apps);
                    }
                }
            } while (sCursor.moveToNext());
            //Now again use the same Cursor and Move ut to position 0 to use it for our purpose
            sCursor.moveToPosition(0);
            do {
                theAppId = sCursor.getInt(0);
                theAppName = sCursor.getString(1);
                thePackageName =sCursor.getString(2);
                theAppCriteria =sCursor.getString(3);
                for(int k=0;k<=resultList.size()-1;k++)
                {
                    if(thePackageName.equals(resultList.get(k).packageName))
                    {
                        theUsageTime = resultList.get(k).timeInForeground;
                        ActualRunTimeToday = getMillisToStringTime(theUsageTime);
                        sQueryHistory = "SELECT a.appId,a.appName,a.appPkgName,a.appCriteria,b.appId,b.appUsageDate,b.appUsageTime from main.offlineAppsListMaster a, main.offlineAppsUsageHistory b WHERE a.appCriteria != '' AND a.appId=b.appId AND a.appId=" + theAppId + " AND a.appPkgName='" + resultList.get(k).packageName + "' AND b.appUsageDate='" + tDate + "';";
                        Cursor sCursorHistory = mDatabase.rawQuery(sQueryHistory,null);
                        Integer sRecordsHistory = sCursorHistory.getCount();
                        Integer differenceInTime = compareTime(ActualRunTimeToday,theAppCriteria);
                        if(sRecordsHistory==0)
                        {
                            if(differenceInTime == -1) //ActualRunTimeToday < theAppCriteria,//No Record Found in History Table
                            {
                                // INSERT record of this app in History Table & continue
                                String iQueryHistory = "INSERT INTO main.offlineAppsUsageHistory(appId,appUsageDate,appUsageTime) VALUES (" + theAppId + ",'" + tDate + "','" + ActualRunTimeToday + "');";
                                Cursor iCursorHistory = mDatabase.rawQuery(iQueryHistory,null);
                                Integer iRecordsHistory = iCursorHistory.getCount();
//                                Log.i(TAG, "performServiceTask: History Table Insterted " + iRecordsHistory + " for " + theAppId + " From if(sRecordsHistory==0)");
                                iCursorHistory.close();
                            }
                            else if((differenceInTime == 0) || (differenceInTime == 1)) //ActualRunTimeToday => theAppCriteria, //NO Record Found in History Table
                            {
                                // INSERT record of this app in History Table & continue
                                String iQueryHistory = "INSERT INTO main.offlineAppsUsageHistory(appId,appUsageDate,appUsageTime) VALUES (" + theAppId + ",'" + tDate + "','" + ActualRunTimeToday + "');";
                                Cursor iCursorHistory = mDatabase.rawQuery(iQueryHistory,null);
                                Integer iRecordsHistory = iCursorHistory.getCount();
//                                Log.i(TAG, "performServiceTask: History Table Insterted " + iRecordsHistory + " for " + theAppId + " From if(sRecordsHistory==0)");
                                iCursorHistory.close();
                                //And now kill the application here
                                //Check if this App is in Foreground? If YES, then go to Home Screen. This will force the App in question to go in background and not allow user to access it.
                            }
                        }
                        else if(sRecordsHistory==1)
                        {
                            if(differenceInTime == -1) //ActualRunTimeToday < theAppCriteria, //One Record Found in History Table
                            {
                                String uQueryHistory = "UPDATE main.offlineAppsUsageHistory SET appUsageTime='" + ActualRunTimeToday + "' WHERE appId=" + theAppId + " AND appUsageDate='" + tDate + "';";
                                Cursor uCursorHistory = mDatabase.rawQuery(uQueryHistory,null);
                                Integer uRecordsHistory = uCursorHistory.getCount();
//                                Log.i(TAG, "performServiceTask: History Table Updated " + uRecordsHistory + " for " + theAppId + " From else if((differenceInTime == 0) || (differenceInTime == -1))");
                                uCursorHistory.close();
                            }
                            else if((differenceInTime == 0) || (differenceInTime == 1)) //ActualRunTimeToday => theAppCriteria, //One Record Found in History Table
                            {
                                String uQueryHistory = "UPDATE main.offlineAppsUsageHistory SET appUsageTime='" + ActualRunTimeToday + "' WHERE appId=" + theAppId + " AND appUsageDate='" + tDate + "';";
                                Cursor uCursorHistory = mDatabase.rawQuery(uQueryHistory,null);
                                Integer uRecordsHistory = uCursorHistory.getCount();
//                                Log.i(TAG, "performServiceTask: History Table Updated " + uRecordsHistory + " for " + theAppId + " From else if((differenceInTime == 0) || (differenceInTime == 0 OR 1))");
                                uCursorHistory.close();
                                //And now kill the application here
                                //Check if this App is in Foreground? If YES, then go to Home Screen. This will force the App in question to go in background and not allow user to access it.
                            }
                        }
                        sCursorHistory.close();
//                        Log.i(TAG, "Routine Task Of Service Performed . . .");
                    }
                }
            } while (sCursor.moveToNext());
            sCursor.close();
            mDatabase.close();
        }
        else
        {
            sCursor.close();
            mDatabase.close();
        }
    }
    public static void getAppStats(Context context,String todayDate) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        List<UsageEvents.Event> allEvents = new ArrayList<>();
        HashMap<String, AppUsageInfo> map = new HashMap<String, AppUsageInfo>();
        //transferred final data into modal class object
        long midNightTimeInMillis = getStartOfDay(todayDate);
        long endTime = System.currentTimeMillis();

//        Log.d(TAG, "Range start:" + dateFormat.format(midNightTimeInMillis));
//        Log.d(TAG, "Range end:" + dateFormat.format(endTime));

        assert usm != null;
        UsageEvents uEvents = usm.queryEvents(midNightTimeInMillis, endTime);

        while (uEvents.hasNextEvent())
        {
            UsageEvents.Event e = new UsageEvents.Event();
            uEvents.getNextEvent(e);
            if ((e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) || (e.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) || (e.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) || (e.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED))
            {
                allEvents.add(e);
                String key = e.getPackageName();
                if (map.get(key) == null)
                {
                    map.put(key, new AppUsageInfo(key));
                }
            }
//            Log.d(TAG, "getAppStats: PackageName : " + e.getPackageName() + "\tEventType: " + e.getEventType() + "\tTimeStamp: - " + e.getTimeStamp());
        }
        for(int i=0;i<allEvents.size()-1;i++)
        {
            UsageEvents.Event E0=allEvents.get(i);
            UsageEvents.Event E1=allEvents.get(i+1);
            if(!E0.getPackageName().equals(E1.getPackageName()) && E1.getEventType()==1)
            {
                //If this is true, E1 (launch event of an app) app launched
                map.get(E1.getPackageName()).launchCount++;
            }
            //for UsageTime of apps in time range
            if(E0.getEventType()==1 && E1.getEventType()==2 && E0.getClassName().equals(E1.getClassName()))
            {
                long diff = E1.getTimeStamp() - E0.getTimeStamp();
                map.get(E0.getPackageName()).timeInForeground+=diff;
            }
        }
        //transferred final data into modal class object
        smallInfoList = new ArrayList<>(map.values());
/*        System.out.println(smallInfoList);
        Log.d(TAG, "getAppStats: allEvents Size is: " + allEvents.size());
        Log.d(TAG, "getAppStats: Map Size is: " + map.size());
        for(int j=0;j<smallInfoList.size(); j++)
        {
            Log.d(TAG, "getAppStats: " + j + ". PackageName: " + smallInfoList.get(j).packageName + "\tLaunch Count: " + smallInfoList.get(j).launchCount + "\tTime In Foreground: " + smallInfoList.get(j).timeInForeground);
        }
        Log.d(TAG, "getAppStats: ________________________________________________________________________");
        Iterator it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove();
        }*/
    }
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date today = Calendar.getInstance().getTime();
        return sdf.format(today);
    }
    public static long getStartOfDay(String tDate) {
        //Specifying the pattern of input date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = tDate + " 00:00:00";
        Date thdAte = null;
        try
        {
            //formatting the dateString to convert it into a Date
            thdAte = sdf.parse(dateString);
            assert thdAte != null;
//            System.out.println("Given Time in Milliseconds is : "+ thdAte.getTime());

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(thdAte);
//            System.out.println("Given Time in milliseconds : "+calendar.getTimeInMillis());
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        assert thdAte != null;
        return thdAte.getTime();
    }
    @SuppressLint("DefaultLocale")
    private String getMillisToStringTime(long theUsageTime) {
        String ActualRunTimeToday = "";
        ActualRunTimeToday = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(theUsageTime),
                TimeUnit.MILLISECONDS.toMinutes(theUsageTime) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(theUsageTime))); // The change is in this line);
        return ActualRunTimeToday;
    }
    public Integer compareTime(String time1,String time2) {
        String pattern = "HH:mm";
        Integer returnValue=null;
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            Date date1 = sdf.parse(time1);
            Date date2 = sdf.parse(time2);
            assert date1 != null;
            returnValue = date1.compareTo(date2);
        } catch (ParseException e){
            e.printStackTrace();
        }
        //returnValue = 1 : time1 is bigger than time2, //returnValue = -1 : time1 is smaller than time2, //returnValue = 0 : time1 is equal to time2
        return returnValue;
    }
    private String getTopApp(Context context) {
        String topActivity = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            UsageStatsManager m = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (m != null)
            {
                long now = System.currentTimeMillis();
                List<UsageStats> stats = m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 60 * 1000, now);
//                Log.i(TAG, "Running app number in last 60 seconds : " + stats.size());
                if ((stats != null) && (!stats.isEmpty()))
                {
                    int j = 0;
                    for (int i = 0; i < stats.size(); i++)
                    {
                        if (stats.get(i).getLastTimeUsed() > stats.get(j).getLastTimeUsed())
                        {
                            j = i;
                        }
                    }
                    topActivity = stats.get(j).getPackageName();
                }
//                Log.i(TAG, "top running app is : "+topActivity);
            }
        }
        return topActivity;
    }
    public void showHomeScreen() {
        mDatabaseTimer = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);
        String theTopApp = getTopApp(getApplicationContext());
        String thDate = getCurrentDate();
        String sQueryCombined = "SELECT a.appId,a.appName,a.appPkgName,a.appCriteria,b.appId,b.appUsageDate,b.appUsageTime from main.offlineAppsListMaster a, main.offlineAppsUsageHistory b WHERE a.appCriteria != '' AND a.appId=b.appId AND b.appUsageDate='" + thDate + "' AND a.appCriteria <= b.appUsageTime ORDER BY a.appId;";
//        Log.i(TAG, "showHomeScreen: " + sQueryCombined);
        Cursor sCursorCombined = mDatabaseTimer.rawQuery(sQueryCombined,null);
        int sRecordsCombined = sCursorCombined.getCount();
        if(sRecordsCombined>0)
        {
            sCursorCombined.moveToPosition(0);
            do {
                String tAppName = sCursorCombined.getString(1);
                String tPackageName = sCursorCombined.getString(2);
                String tAppCriteria = sCursorCombined.getString(3);
                String tAppUsageToday = sCursorCombined.getString(6);
                if(tPackageName.equals(theTopApp))
                {
                    startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
                    Log.i(TAG, "showHomeScreen: Sorry! " + tAppName + " is used for " + tAppUsageToday + " against " + tAppCriteria + " today.");
                }
            }while (sCursorCombined.moveToNext());
            sCursorCombined.close();
            mDatabaseTimer.close();
        }
        else if(sRecordsCombined==0)
        {
            sCursorCombined.close();
            mDatabaseTimer.close();
        }
    }
    public static Service getmCurrentService() {
        return mCurrentService;
    }
    public static void setmCurrentService(Service mCurrentService) {
        offLineBackBone.mCurrentService = mCurrentService;
    }
    private static class AppUsageInfo {
        Drawable appIcon;
        String appName, packageName;
        long timeInForeground;
        int launchCount;

        AppUsageInfo(String pName) {
            this.packageName=pName;
        }
    }
}