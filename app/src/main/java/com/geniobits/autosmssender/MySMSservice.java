package com.geniobits.autosmssender;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wafflecopter.multicontactpicker.ContactResult;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MySMSservice extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_SMS = "com.geniobits.autosmssender.action.SMS";
    private static final String ACTION_WHATSAPP = "com.geniobits.autosmssender.action.WHATSAPP";

    // TODO: Rename parameters
    private static final String MESSAGE = "com.geniobits.autosmssender.extra.PARAM1";
    private static final String COUNT = "com.geniobits.autosmssender.extra.PARAM2";
    private static final String MOBILE_NUMBER = "com.geniobits.autosmssender.extra.PARAM3";
    private static final String FILEPATH = "com.geniobits.autosmssender.extra.PARAM54";
    private static final String DELAY = "com.geniobits.autosmssender.extra.PARAM55";

    public MySMSservice() {
        super("MySMSservice");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method


    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */

    public static void startActionWHATSAPP(Context applicationContext, String message, String textcount, String[] numbers, String[] file_path, String delay) {
        Intent intent = new Intent(applicationContext, MySMSservice.class);
        intent.setAction(ACTION_WHATSAPP);
        intent.putExtra(MESSAGE, message);
        intent.putExtra(COUNT, textcount);
        intent.putExtra(MOBILE_NUMBER, numbers);
        intent.putExtra(FILEPATH,file_path);
        intent.putExtra(DELAY,delay);
        applicationContext.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WHATSAPP.equals(action)) {
                final String message = intent.getStringExtra(MESSAGE);
                final String count = intent.getStringExtra(COUNT);
                final String delay = intent.getStringExtra(DELAY);
                final String[] filepath = intent.getStringArrayExtra(FILEPATH);
                final String[] mobile_number = intent.getStringArrayExtra(MOBILE_NUMBER);
                handleActionWHATSAPP(message, count,mobile_number,filepath,delay);
            }
        }
    }

    private void handleActionWHATSAPP(String message, String count, String[] mobile_number, String[] filepath, String delay) {
        int dalayN=10000;
        if(!delay.equals("")){
            dalayN=Integer.parseInt(delay);
        }


        if(filepath.length!=0) {
            if(filepath.length==1) {
                try {
                    Log.e("File","Single");
                    send_single_image_to_contact_lists(mobile_number, count, filepath[0], message, dalayN);
                } catch (Exception e) {
                    sendBroadcastMessage("Result: " + e.toString());
                    Log.e("Error", e.toString());
                    e.printStackTrace();
                }
            }else{
                try {
                    Log.e("File","Multi");
                    send_multiple_image_to_contact_lists(mobile_number, count, filepath, message, dalayN);
                } catch (Exception e) {
                    sendBroadcastMessage("Result: " + e.toString());
                    Log.e("Error", e.toString());
                    e.printStackTrace();
                }
            }
        }else {
            try {
                Log.e("NO File","NOFILE");
                PackageManager packageManager = getApplicationContext().getPackageManager();
                if (mobile_number.length != 0) {
                    for (String s : mobile_number) {
                        for (int i = 0; i < Integer.parseInt(count.toString()); i++) {
                            String url = "https://api.whatsapp.com/send?phone=" + s + "&text=" + URLEncoder.encode(message + "   ", "UTF-8");
                            Intent whatappIntent = new Intent(Intent.ACTION_VIEW);
                            whatappIntent.setPackage("com.whatsapp.w4b");
                            whatappIntent.setData(Uri.parse(url));
                            whatappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (whatappIntent.resolveActivity(packageManager) != null) {
                                getApplicationContext().startActivity(whatappIntent);
                                Thread.sleep(dalayN);
                                sendBroadcastMessage("Result: " + s);
                            } else {
                                sendBroadcastMessage("Result: WhatsApp Not installed");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sendBroadcastMessage("Result: " + e.toString());
            }
        }

    }

    private void send_multiple_image_to_contact_lists(String[] mobile_number, String count, String[] filepath, String message, int dalayN) throws InterruptedException {
        final ArrayList<Uri> images =new ArrayList<>();
        final ArrayList<CharSequence> messagesList= new ArrayList<>();
        String[] meesageArray = message.split(",");
        int ic =0;
        for (String s : filepath) {
            images.add(FileProvider.getUriForFile(getApplicationContext(), getApplicationContext()
                    .getApplicationContext().getPackageName() + ".provider", new File(s)));
            if(ic<meesageArray.length)
                messagesList.add(meesageArray[ic]);
            else
                messagesList.add("");
            ic++;

        }
        PackageManager packageManager = getApplicationContext().getPackageManager();
        if (mobile_number.length != 0) {
            for (int j = 0; j < mobile_number.length; j++) {
                for (int i = 0; i < Integer.parseInt(count.toString()); i++) {
                    String number = mobile_number[j];
                    Intent sendIntent = new Intent("android.intent.action.MAIN");
                    sendIntent.putExtra(Intent.EXTRA_STREAM, images);
                    sendIntent.putExtra("jid", number + "@s.whatsapp.net");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, messagesList);
                    sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    sendIntent.setPackage("com.whatsapp.w4b");
                    sendIntent.setType("image/png");
                    if (sendIntent.resolveActivity(packageManager) != null) {
                        getApplicationContext().startActivity(sendIntent);
                        Thread.sleep(dalayN);
                        sendBroadcastMessage("Result: " + number);
                    } else {
                        sendBroadcastMessage("Result: WhatsApp Not installed");
                    }
                }

            }
        }

    }

    private void send_single_image_to_contact_lists(String[] mobile_number, String count, String filepath, String message, int dalayN) throws InterruptedException {
        PackageManager packageManager = getApplicationContext().getPackageManager();
        if (mobile_number.length != 0) {
            for (int j = 0; j < mobile_number.length; j++) {
                for (int i = 0; i < Integer.parseInt(count.toString()); i++) {
                    String number = mobile_number[j];
                    Intent sendIntent = new Intent("android.intent.action.MAIN");
                    Uri fileData = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext()
                            .getApplicationContext().getPackageName() + ".provider", new File(filepath));
                    sendIntent.putExtra(Intent.EXTRA_STREAM, fileData);
                    sendIntent.putExtra("jid", number + "@s.whatsapp.net");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    sendIntent.setPackage("com.whatsapp.w4b");
                    sendIntent.setType(getMimeType(fileData));
                    if (sendIntent.resolveActivity(packageManager) != null) {
                        getApplicationContext().startActivity(sendIntent);
                        Thread.sleep(dalayN);
                        sendBroadcastMessage("Result: " + number);
                    } else {
                        sendBroadcastMessage("Result: WhatsApp Not installed");
                    }
                }

            }
        }
    }


    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */


    public String getMimeType(Uri uri) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getApplicationContext().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }


    private void sendBroadcastMessage(String message){
        Intent localIntent = new Intent("my.own.broadcast");
        localIntent.putExtra("result",message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }




}
