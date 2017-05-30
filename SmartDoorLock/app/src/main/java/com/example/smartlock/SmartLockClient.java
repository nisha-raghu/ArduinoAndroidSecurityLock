package com.example.smartlock;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;


public class SmartLockClient extends Activity {
    //Declares
    String mToken="";
    String mEmail="";
    Handler handler = null;
    SQLiteDatabase mDatabase;
    public boolean bActionInProgress=false;
    //Override Functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iot_device_client);
        mDatabase = openOrCreateDatabase("IoTDeviceDB", Context.MODE_PRIVATE, null);
        mEmail = DatabaseUtil.GetLastUser(mDatabase);
        if(mEmail=="") {
            pickUserAccount();
        }
        //we have a user account/email
        RefreshData(true);

        startService(new Intent(this, SmartLockService.class));
        //handler provies comm bet UI and background process
        handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                //GetSpreadSheetList();
                handler.postDelayed(this, 5000);
                RefreshData(false);
            }
        };
        handler.postDelayed(r, 5000);
        setLockAlert(2);

        ((Switch)findViewById(R.id.switch1)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateSwitchData();
            }
        });
        ((Switch)findViewById(R.id.switch2)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateSwitchData();
            }
        });

        ((Switch)findViewById(R.id.switch3)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateSwitchData();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.iot_device_client, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        //if (id == R.id.action_settings) {
          //  DatabaseUtil.ClearTables(mDatabase);
            //return true;
        //}
        return super.onOptionsItemSelected(item);
    }
    //My Functions
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                // With the account name acquired, go get the auth token
                Toast.makeText(this, mEmail, Toast.LENGTH_SHORT).show();
                Log.d("SmartLockService", "Get Name Launched...");
                DatabaseUtil.SetLastUser(mDatabase,mEmail);
            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, "Please pick an account!", Toast.LENGTH_SHORT).show();
            }
        }
        // Later, more code will go here to handle the result from some exceptions...
    }

    public String Not_Null(String object)
    {
        return object==null?"":object;
    }


    //change images and store values in db
    private void RefreshData(boolean bStartup){
        bActionInProgress = true;
        TemperatureView tv = (TemperatureView) findViewById(R.id.tmpView);

        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS IoTDataTable(temp REAL, tlimithigh REAL, tlimitlow REAL, heater INTEGER,cooler INTEGER, dalarm INTEGER, dbreach INTEGER);");
            Cursor c = mDatabase.rawQuery("SELECT * FROM IoTDataTable;", null);
            boolean bNext = c.moveToFirst();
            if (c.moveToFirst()) {
                tv.temperature = c.getDouble(0);
                Double tlimith = c.getDouble(1);
                Double tlimitl = c.getDouble(2);
                int heater= c.getInt(3);
                int cooler=c.getInt(4);
                int dalarm = c.getInt(5);
                int dbreach = c.getInt(6);
                if(bStartup) {
                    ((Switch) findViewById(R.id.switch1)).setChecked(heater == 1 ? true : false);
                    ((Switch) findViewById(R.id.switch2)).setChecked(cooler == 1 ? true : false);
                    ((Switch) findViewById(R.id.switch3)).setChecked(dalarm == 1 ? true : false);
                }
                if(dalarm == 1)
                    setLockAlert(dbreach);
                else
                    setLockAlert(2);
                tv.invalidate();
                Log.d("IotDeviceService","DBREACH="+Integer.toString(dbreach));
            }
            c.close();
        }
        catch(SQLiteException exp){
            Log.w("SmartLockClient", exp.getMessage());
        }
        catch(Exception exp){
            Log.w("SmartLockClient", exp.getMessage());
        }
        //updateSwitchData();

        bActionInProgress=false;
    }

    public void updateSwitchData()
    {
        int heater = ((Switch)findViewById(R.id.switch1)).isChecked()?1:0;
        int cooler = ((Switch)findViewById(R.id.switch2)).isChecked()?1:0;
        int alarm = ((Switch)findViewById(R.id.switch3)).isChecked()?1:0;
        try {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS IoTDataTable(temp REAL, tlimithigh REAL, tlimitlow REAL, heater INTEGER,cooler INTEGER, dalarm INTEGER, dbreach INTEGER);");
            Cursor c = mDatabase.rawQuery("SELECT * FROM IoTDataTable;", null);
            if (c.moveToFirst()) {
                mDatabase.execSQL("UPDATE IoTDataTable SET heater=" + Integer.toString(heater) + ", cooler="+Integer.toString(cooler)+", dalarm="+Integer.toString(alarm)+";");
            } else {
                mDatabase.execSQL("INSERT INTO IoTDataTable VALUES(0, 0,0, " + Integer.toString(heater) + "," + Integer.toString(cooler)+ ","+Integer.toString(alarm)+",1);");
            }
            c.close();
        }
        catch(SQLiteException exp)
        {
            Log.w("IoTDeviceClient", exp.getMessage());
        }
        catch(Exception exp)
        {
            Log.w("IoTDeviceClient",exp.getMessage());
        }

    }

    private void setLockAlert(int iLock)
    {
        if(iLock==0) {
            ((ImageView) findViewById(R.id.alertView)).setImageResource(R.drawable.lock);

        }
        else if(iLock==1) {
            ((ImageView) findViewById(R.id.alertView)).setImageResource(R.drawable.unlock);

        }
        else if(iLock==2)
            ((ImageView) findViewById(R.id.alertView)).setImageResource(R.drawable.lock_disabled);

    }
}
