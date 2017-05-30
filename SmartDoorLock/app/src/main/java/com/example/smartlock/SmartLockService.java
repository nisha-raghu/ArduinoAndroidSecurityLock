package com.example.smartlock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.gdata.client.spreadsheet.ListQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.client.spreadsheet.WorksheetQuery;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

public class SmartLockService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.s
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        //Toast.makeText(this, "The new Service was Created", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For time consuming an long tasks you can launch a new thread here...
        IoTDeviceThread myThread = new IoTDeviceThread();
        myThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Toast.makeText(this, " Service Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        //Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }

    class IoTDeviceThread extends Thread {
        int count = 0;
        private static final String INNER_TAG = "IoTDeviceThread";

        ///////////////////////////// NOTIFICATION FUNCTIONS START //////////////////////////////////////////

        public void UnNotify(int id)
        {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(id);
        }


        public void NotifyUser(int id,String message)
        {
            Intent intent = new Intent(SmartLockService.this, SmartLockClient.class);
            PendingIntent pIntent = PendingIntent.getActivity(SmartLockService.this, 0, intent, 0);

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Calendar c = Calendar.getInstance();
            String tm = Integer.toString(c.get(Calendar.HOUR)) + ":" + Integer.toString(c.get(Calendar.MINUTE)) + ":" + Integer.toString(c.get(Calendar.SECOND));
            Notification n = new Notification.Builder(SmartLockService.this)
                    .setContentTitle(tm + " : IoT Device!")
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentIntent(pIntent)
                    .setSound(alarmSound)
                    .setAutoCancel(true).build();


            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            n.flags |= Notification.FLAG_ONLY_ALERT_ONCE;    // Dont vibrate or make notification sound

            notificationManager.notify(id, n);
        }


        ///////////////////////////// END OF NOTIFICATION FUNCTIONS ////////////////////////////////////////

        public void RequestForNewToken(SQLiteDatabase db)
        {
            try {
                //String email=DatabaseUtil.GetLastUser(db);
                String email=DatabaseUtil.GetLastUser(db);
                Log.d("SmartLockService", email);
                String token=
                        GoogleAuthUtil.getTokenWithNotification(
                                SmartLockService.this,
                                email,
                                "oauth2:https://spreadsheets.google.com/feeds https://docs.google.com/feeds", null);
                Log.d("SmartLockService", "Token: " + token);
                DatabaseUtil.UpdateToken(db,email,token);
            } catch (GooglePlayServicesAvailabilityException e) {
                Log.d("SmartLockService", "R U There :" + e.getMessage());
            } catch (UserRecoverableAuthException e) {
                Log.d("SmartLockService", "User :" + e.getMessage());
            } catch (IOException e) {
                Log.d("SmartLockService", "IO :" + e.getMessage());
            } catch (GoogleAuthException e) {
                Log.d("SmartLockService", "Auth :" + e.getMessage());
            } catch (Exception e) {
                Log.d("SmartLockService", "Alien :" + e.getMessage());
            }
        }

        public WorksheetEntry GetWorkSheetEntry(SpreadsheetService service, SpreadsheetEntry spreadSheet, String sheetName)
        {
            try {
                WorksheetQuery wq = new WorksheetQuery(spreadSheet.getWorksheetFeedUrl());
                WorksheetFeed feed = service.query(wq, WorksheetFeed.class);
                List<WorksheetEntry> sheets = feed.getEntries();
                for(WorksheetEntry we:sheets)
                {
                    if(we.getTitle().getPlainText().trim().equals(sheetName))
                        return we;
                }
            }
            catch(ServiceException e)
            {
                Log.d("SmartLockClient", e.getMessage());
            }
            catch(Exception e)
            {
                Log.d("SmartLockClient",e.getMessage());
            }
            return null;
        }

        public void GetSpreadSheetData()
        {
            SQLiteDatabase db = openOrCreateDatabase("IoTDeviceDB", Context.MODE_PRIVATE,null);
            String email = DatabaseUtil.GetLastUser(db);
            Log.d("SmartLockService","Email:" + email);
            String token = DatabaseUtil.GetTokenFromUser(db, email);
            Log.d("SmartLockService","Token:" + token);
            if(email.isEmpty())
            {
                NotifyUser(0, "IoTDevice: Select an email account!");
                // nothing to do... wait for user to select email account through the application.
                return;
            }
            else
            {
                UnNotify(0);
            }
            if(token.isEmpty())
            {
                RequestForNewToken(db);
                return;
            }
            try {
       SpreadsheetService s =new SpreadsheetService("SmartLockService", "https", "google.com");//Constructs an instance connecting to the Google Spreadsheets service for an application with the name applicationName and the given GDataRequestFactory and AuthTokenFactory.
                s.setAuthSubToken(token);

                // Define the URL to request.  This should never change.
                // (Magic URL good for all users.)
                URL SPREADSHEET_FEED_URL = new URL(
                        "https://spreadsheets.google.com/feeds/spreadsheets/private/full");

                // Make a request to the API and get all spreadsheets.
                SpreadsheetFeed feed;
                feed = s.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
                List<SpreadsheetEntry> spreadsheets = feed.getEntries();

                String sheets = "";
                // Iterate through all of the spreadsheets returned
                for (SpreadsheetEntry spreadsheet : spreadsheets) {
                    Log.d("IotDeviceServie", spreadsheet.getTitle().getPlainText());
                    String title =spreadsheet.getTitle().getPlainText();
                    if(title.trim().equals("TestData"))
                    {
                        Log.d("IotDeviceService","Found TestData!");
                        WorksheetEntry wentry = GetWorkSheetEntry(s,spreadsheet,"COMMAND");
                        if(wentry!=null)
                        {
                            Log.d("IotDeviceService","Found COMMAND sheet!");

                            ProcessSheetRows(s,wentry);
                        }
                        break;
                    }
                }
            } catch (ServiceException e) {
                Log.d("IotDeviceService", "Its Invalid: "+e.getMessage());
                RequestForNewToken(db);
            } catch (Exception e) {
                Log.d("IotDeviceService", "Alien :" + e.getMessage());
            }
        }



        public void UpdateCells(WorksheetEntry we, SpreadsheetService s, String dalarm, String heater, String cooler)
        {
            try {
                URL cellFeedUrl = we.getCellFeedUrl();
                CellFeed cellFeed = s.getFeed(cellFeedUrl,
                        CellFeed.class);

                cellFeed.insert(new CellEntry(2, 4, dalarm));
                cellFeed.insert(new CellEntry(2, 6, heater));
                cellFeed.insert(new CellEntry(2, 7,cooler));
            }
            catch(IOException exp)
            {
                Log.d("IotDeviceService",exp.getMessage());
            }
            catch(ServiceException exp){
                Log.d("IotDeviceService",exp.getMessage());
            }
            catch(Exception exp){
                Log.d("IotDeviceService",exp.getMessage());
            }
        }

        public void ProcessSheetRows(SpreadsheetService s, WorksheetEntry wentry)
        {
            SQLiteDatabase db = openOrCreateDatabase("IoTDeviceDB",Context.MODE_PRIVATE,null);
            try {
                // Fetch the list feed of the worksheet.
                ListQuery listQuery = new ListQuery(wentry.getListFeedUrl());
                ListFeed listFeed = (ListFeed) s.query(listQuery, ListFeed.class);
                String deviceUpdate="";

                if(listFeed.getEntries().size()>0)
                {
                    ListEntry le = listFeed.getEntries().get(0);
                    CustomElementCollection ce = le.getCustomElements();
                    try {

                        boolean bUpdate=false;
                        int dalarm = 0;
                        Cursor c = db.rawQuery("SELECT * FROM IotDataTable;", null);
                        if (c.moveToFirst()) {
                            Log.d("IotDeviceService","Updating remote...");
                            dalarm=c.getInt(5);
                            UpdateCells(wentry,s,Integer.toString(dalarm),Integer.toString(c.getInt(3)),Integer.toString(c.getInt(4)));
                            bUpdate=true;
                        }
                        c.close();
                        String dbreach = ce.getValue("dooropened");
                        String tlimithigh =ce.getValue("thresholdmax");
                        String tlimitlow = ce.getValue("thresholdmin");
                        if(bUpdate) {
                            Log.d("IotDeviceService","Updating local...");
                            String query="UPDATE IoTDataTable SET temp=" + ce.getValue("temperature") + ", "
                                    + "tlimithigh=" + tlimithigh + ", "
                                    + "tlimitlow=" + tlimitlow + ", "
                                    + "dbreach=" + dbreach + "; ";
                            db.execSQL(query);
                            Log.d("IotDeviceService",query);
                        }
                        if(dalarm == 1 && dbreach.compareToIgnoreCase("1")==0)
                            NotifyUser(1,"Security Breach: Door Opened!");
                        else
                            UnNotify(1);
                    }
                    catch (SQLiteException exp)
                    {
                        Log.d("SmartLockService",exp.getMessage());
                    }
                    catch(Exception exp){
                        Log.d("SmartLockService",exp.getMessage());
                    }
                }
            }
            catch (ServiceException e)
            {
                Log.d("IotDeviceService",e.getMessage());
            }
            catch(IOException e)
            {
                Log.d("IotDeviceService",e.getMessage());
            }
        }
        // Our main thread function loop
        public void run() {
            this.setName(INNER_TAG);
            while (true) { // let our thread running forever for our service
                try {
                    GetSpreadSheetData();
                    this.sleep(10000);       // Sleep 10 seconds
                }
                catch(Exception exp)
                {
                    Log.d("IotDeviceService",exp.getMessage());
                }
            }
            }
        }

}