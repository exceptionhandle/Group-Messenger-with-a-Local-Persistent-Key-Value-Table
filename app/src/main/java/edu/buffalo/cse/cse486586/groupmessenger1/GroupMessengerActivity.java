package edu.buffalo.cse.cse486586.groupmessenger1;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    public   ContentResolver mContentResolver;
    public   Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    private ContentValues initValues(int serno, String msg) {
        ContentValues cv = new ContentValues();
        cv.put("key", Integer.toString(serno));
        cv.put("value", msg);
        return cv;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        mContentResolver = getContentResolver();

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        try {
            findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    final EditText editText = (EditText) findViewById(R.id.editText1);
                    String msg = editText.getText().toString() + "\n";
                    editText.setText("");
                    TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                    localTextView.append("\t" + msg);
                    TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                    remoteTextView.append("\n");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Buttom Exception");
            e.printStackTrace();
        }

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        private String msgReceived;
        private DataInputStream input;
        private  final String TAG = ServerTask.class.getName();
        private  final String KEY_FIELD = "key";
        private  final String VALUE_FIELD = "value";
        private  int serNo = 0;
        private  ContentValues mContentValues;

        private boolean testInsert(ContentValues mContentValues ) {
            try {

                mContentResolver.insert(mUri, mContentValues);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
                return false;
            }

            return true;
        }


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];


            try {
                do {
                    input = new DataInputStream(serverSocket.accept().getInputStream());
                    msgReceived = input.readUTF();
                    mContentValues = initValues(serNo, msgReceived);
                    testInsert(mContentValues);
                    Log.e(TAG,"values inserted : "+serNo+" "+msgReceived);
                    serNo++;
                    Log.e(TAG,mContentValues.toString());
                    publishProgress(msgReceived);
                } while (true);
            } catch (Exception e) {
                e.printStackTrace();
            }


            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            localTextView.append("\n");


            String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";


            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }

    }
    private class ClientTask extends AsyncTask<String, Void, Void> {
        private DataOutputStream output;

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                int iter = 0;
                int[] REMOTE_PORT={11108,11112,11116,11120,11124};

/*                mContentValues = initValues(serNo,strReceived);
                Log.e(TAG,"values inserted : "+serNo+" "+strReceived);
                serNo++;
                Log.e(TAG,mContentValues.toString());
                try {
                    mContentResolver.insert(mUri, mContentValues);

                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
*/
                while(iter<5) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            REMOTE_PORT[iter]);

                    String msgToSend = msgs[0];
                    output = new DataOutputStream(socket.getOutputStream());
                    output.writeUTF(msgToSend);
                    output.flush();

                    socket.close();
                    iter ++;
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }

    }
}