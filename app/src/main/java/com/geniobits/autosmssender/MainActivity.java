package com.geniobits.autosmssender;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText txt_message;
    private EditText txt_number;
    private EditText txt_count;
    private EditText txt_delay;
    List<String> results=new ArrayList<>();
    private ArrayList<String> file_path = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt_message = findViewById(R.id.txt_message);
        txt_number = findViewById(R.id.txt_mobile_number);
        txt_count  = findViewById(R.id.txt_count);
        txt_delay = findViewById(R.id.txt_delay);
        Button btn_attachment = findViewById(R.id.btn_attachment);
        Button btn_whatsapp = findViewById(R.id.btn_whatsapp);
        Button btn_save_contacts = findViewById(R.id.btn_save_contacts);
        Button btn_delete_contacts = findViewById(R.id.btn_delete_contacts);
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_CONTACTS
                ).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();

        if(!isAccessibilityOn(getApplicationContext())){
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }


        btn_attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ChooserDialog dialog2 = new ChooserDialog(v.getContext())
                        .enableOptions(true)
                        .withFilter(false, true, "jpg","png")
                        .enableMultiple(true)
                        .withStartFile(Environment.getExternalStorageDirectory().getAbsolutePath())
                        .withChosenListener(new ChooserDialog.Result() {

                            @Override
                            public void onChoosePath(String path, File pathFile) {
                                Log.e("files",path);
                                if(path.toLowerCase().endsWith("jpg") || path.toLowerCase().endsWith("png"))
                                    file_path.add(path);
                            }
                        })
                        .withOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                Log.d("CANCEL", "CANCEL");
                                // MUST have
                                dialog.dismiss();
                            }
                        });

                dialog2.build();
                dialog2.show();
            }
        });


        btn_whatsapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] numbers = getNumbers(txt_number.getText());
                MySMSservice.startActionWHATSAPP(getApplicationContext(),txt_message.getText().toString(),
                        txt_count.getText().toString(),numbers,file_path.toArray(new String[0]),txt_delay.getText().toString());

            }
        });
        btn_save_contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (String value : getNumbers(txt_number.getText())) {
                    boolean result = saveContact("Temp_contact_geniobits", "+"+value);
                    Log.e(value, String.valueOf(result));

                }
                Toast.makeText(getApplicationContext(),"Contact Saved successfully. Please refresh WhatsApp",Toast.LENGTH_LONG).show();

            }
        });
        btn_delete_contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (String value : getNumbers(txt_number.getText())) {
                    deleteContact(getApplicationContext(), "+"+value, "Temp_contact_geniobits");
                }
                Toast.makeText(getApplicationContext(),"Contact Deleted successfully. Please refresh WhatsApp",Toast.LENGTH_LONG).show();

            }
        });

        IntentFilter intent = new IntentFilter("my.own.broadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(myLocalBroadcastReceiver,intent);

    }
    public boolean saveContact(String DisplayName, String MobileNumber){

        ArrayList <ContentProviderOperation> ops = new ArrayList < ContentProviderOperation > ();

        ops.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        //------------------------------------------------------ Names
        if (DisplayName != null) {
            // first and last names
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, DisplayName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "")
                    .build());
        }

        //------------------------------------------------------ Mobile Number
        if (MobileNumber != null) {
            ops.add(ContentProviderOperation.
                    newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, MobileNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());
        }
        // Asking the Contact provider to create a new contact
        try {
            getApplicationContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private String[] getNumbers(Editable text) {
        String numberString = text.toString().replace("+","");
        numberString = numberString.replace(" ","");
        String[] numbers = numberString.split(",");
        for(int i=0;i<numbers.length;i++){
            if(numbers[i].length()<=10)
                    numbers[i]="91"+numbers[i];
        }
        return numbers;
    }


    private BroadcastReceiver myLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
           String result= intent.getStringExtra("result");
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    };



    private boolean isAccessibilityOn(Context context) {
        int accessibilityEnabled = 0;
        final String service = context.getPackageName () + "/" + WhatAppAccessibilityService.class.getCanonicalName ();
        try {
            accessibilityEnabled = Settings.Secure.getInt (context.getApplicationContext ().getContentResolver (), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException ignored) {  }

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter (':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString (context.getApplicationContext ().getContentResolver (), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString (settingValue);
                while (colonSplitter.hasNext ()) {
                    String accessibilityService = colonSplitter.next ();

                    if (accessibilityService.equalsIgnoreCase (service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean deleteContact(Context ctx, String phone, String name) {
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
        Cursor cur = ctx.getContentResolver().query(contactUri, null, null, null, null);
        try {
            if (cur.moveToFirst()) {
                do {
                    if (cur.getString(cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)).equalsIgnoreCase(name)) {
                        String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                        ctx.getContentResolver().delete(uri, null, null);
                        return true;
                    }

                } while (cur.moveToNext());
            }

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        } finally {
            cur.close();
        }
        return false;
    }
}
