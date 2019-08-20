package br.gov.serpro.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * This class echoes a string called from JavaScript.
 */
public class ReceiveFilePlugin extends CordovaPlugin {

    private String mAction;
    private ArrayList<Parcelable> mStreamsToImport = new ArrayList<Parcelable>();
    private long inicio;
    private long fim;
    private String mensagem = null;
    private static final String DEFAULT_STORAGE_LOCATION = Environment.getExternalStorageDirectory() + "/MeuImpostoDeRenda"; //+ BuildConfig.DEFAULT_STORAGE_LOCATION;
    private static Context context;
    private HashMap<String, CallbackContext> events = new HashMap<String, CallbackContext>();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        mAction = this.cordova.getActivity().getIntent().getAction();
        context = this.cordova.getActivity();

        if (Intent.ACTION_SEND.equals(mAction)) {
            mStreamsToImport.add(this.cordova.getActivity().getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
            importarDeclaracao();
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(mAction)) {
            mStreamsToImport = this.cordova.getActivity().getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            importarDeclaracao();
        }
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("onComplete")) {
            onComplete(callbackContext);
        }

        if (action.equals("newIntent")) {
            newIntent(callbackContext);
        }

        return true;
    }

    private void importarDeclaracao() {
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... params) {

                inicio = Calendar.getInstance().getTimeInMillis();
                fim = 0;

                JSONObject retorno = new JSONObject();

                try {
                    for (Parcelable mStream : mStreamsToImport) {
                        Uri uri = (Uri) mStream;
                        Log.i("REC_F", uri.toString());
                        if (uri != null) {
                            File file = createCacheFileFromUri(context, uri);
                            File destFile = new File(DEFAULT_STORAGE_LOCATION, file.getName());
                            FileUtils.copyFile(file, destFile);
//                            retorno = true;

                            retorno.put("success", true);
                            retorno.put("file", destFile.getAbsolutePath());

                            Log.i("REC_F", retorno.toString());
                        }
                    }

                } catch (final Exception e) {

                    try {

                        retorno.put("success", false);
                        retorno.put("exception", new JSONObject(){{
                            put("name", e.getClass().getName());
                            put("message", e.getMessage());
                        }});
                    } catch (JSONException jsonErr) {
                        throw new RuntimeException(jsonErr);
                    }

                    e.printStackTrace();
                }

                return retorno;
            }

            @Override
            protected void onPostExecute(JSONObject result) {

                if (events.containsKey("onComplete")) {

                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);

                    CallbackContext callback = events.get("onComplete");
                    pluginResult.setKeepCallback(true);

                    callback.sendPluginResult(pluginResult);

                }

//                long timeDelay = 1000;
//                if (fim == 0) {
//                    fim = Calendar.getInstance().getTimeInMillis();
//                    long diff = fim - inicio;
//                    timeDelay = (diff < timeDelay) ? timeDelay - diff : 0;
//                }
//
//                if (mensagem == null) {
//                    if (result && (mStreamsToImport != null && mStreamsToImport.size() == 1)) {
//                        mensagem = "Arquivo transferido com sucesso!";
//                    } else if (result && (mStreamsToImport != null && mStreamsToImport.size() > 1)) {
//                        mensagem = "Arquivos transferidos com sucesso!";
//                    } else if (!result && (mStreamsToImport != null && mStreamsToImport.size() == 1)) {
//                        mensagem = "Arquivo inválido ou corrompido!";
//                    } else {
//                        mensagem = "Arquivos inválidos ou corrompidos!";
//                    }
//                }
//
            }
        }.execute();
    }

    public static String getFileName(Context context, Uri uri) {

        String fileName;

        Cursor returnCursor =
                context.getContentResolver().query(uri, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */
        if(returnCursor != null) {
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            fileName = returnCursor.getString(nameIndex);
        }else{
            fileName = uri.getLastPathSegment();
        }

        return fileName;
    }



    public static File createCacheFileFromUri(Context context, Uri uri) throws IOException {

        String fileName = getFileName(context, uri);
        File file = File.createTempFile("transfered-file", null);
        InputStream stream = context.getContentResolver().openInputStream(uri);
        FileUtils.copyInputStreamToFile(stream, file);

        File cacheFile = new File(context.getCacheDir(), fileName);
        if(cacheFile.exists()) {

            FileUtils.forceDelete(cacheFile);
        }

        FileUtils.moveFile(file, cacheFile);

        if(cacheFile.exists()) {
            return cacheFile;
        }

        return null;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    public void newIntent(CallbackContext callbackContext) {
        events.put("newIntent", callbackContext);
        importarDeclaracao();
    }

    public void onComplete(CallbackContext callbackContext) {
        events.put("onComplete", callbackContext);
    }

}
