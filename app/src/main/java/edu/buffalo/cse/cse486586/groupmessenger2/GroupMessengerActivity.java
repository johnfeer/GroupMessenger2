package edu.buffalo.cse.cse486586.groupmessenger2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.lang.Math;

import static java.lang.Math.max;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
class Chunk implements Serializable {
    public String msg;
    public int msgid = 0;
    public int sendingProc = 0;
    public int seqNum = -1;
    public int procNum = 0;
    public boolean status = false;

    public Chunk(String a, int b, int c, int d, int e, boolean f){
        msg = a;
        msgid = b;
        sendingProc = c;
        seqNum = d;
        procNum = e;
        status = f;
    }
}

class ChunkComparator implements Comparator<Chunk> {
    public int compare(Chunk a, Chunk b){
        if(a.seqNum != b.seqNum){
            return a.seqNum - b.seqNum;
        }
        if(a.status != b.status){
            return (a.status ? 1:0) - (b.status ? 1:0);
        }
        return a.procNum - b.procNum;
    }
}

public class GroupMessengerActivity extends Activity {

    /*
     * We need objects for what goes on the heap and also the list of the suggested seq #'s
     * and PIDs.
     */
    class Suggestion{
        private int suggestedSeqNum = 0;
        private int suggestingProc = 0;
        public Suggestion(int a, int b){
            suggestedSeqNum = a;
            suggestingProc = b;
        }
    }

    class SuggestionComparator implements Comparator<Suggestion>{
        public int compare(Suggestion a, Suggestion b){
            if(b.suggestedSeqNum == a.suggestedSeqNum){
                return a.suggestingProc - b.suggestingProc;
            }
            return b.suggestedSeqNum - a.suggestedSeqNum;
        }
    }


    //default initial capacity is 11
    private PriorityQueue<Chunk> pq = new PriorityQueue<Chunk>(11, new ChunkComparator());
   // private PriorityQueue<Suggestion> suggestedSeqNums = new PriorityQueue<Suggestion>(5, new SuggestionComparator());
    private HashMap<String, PriorityQueue<Suggestion>> suggestedSeqNums = new HashMap<String, PriorityQueue<Suggestion>>();

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORTS = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    private static int numServers = 5;
    private int seqNum = -1;
    private int counter = 0;
    private int theCount = 0;
    private int thisPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        thisPort = Integer.parseInt(myPort);

        try {
            Log.i(TAG, "my port is" + myPort);
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * Get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final EditText editText = (EditText) findViewById(R.id.editText1);
                        String msg = editText.getText().toString() + "\n";
                        Log.i(TAG, "MSG ON CLICK IS " + msg);
                        editText.setText(""); // This is one way to reset the input box.
                        new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg, myPort);
                    }
                });
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @SuppressLint("WrongThread")
        @Override
        protected Void doInBackground(ServerSocket... sockets) {


            /*
             * Receives messages and passes them
             * to onProgressUpdate().ois.close();
             *
             * Begin John Feerick code
             */
            while(true) {
                try {
                    /*
                     * The following code, as well as that of client task was partially created using
                     * https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
                     */
                    ServerSocket serverSocket = sockets[0];
                    Log.i(TAG, "HERE7\t" + counter + "\t" +thisPort);
                    Socket clisco = serverSocket.accept();
                    Log.i(TAG, "HERE8\t" + counter + "\t" +thisPort);
                    //declare new input stream as variable, get servers output stream same as client side

                    /*
                     * Start pa2b
                     */
                    ObjectInputStream ois = new ObjectInputStream(clisco.getInputStream());
                    Log.i(TAG, "HERE9\t" + counter + "\t" +thisPort);
                    Chunk ping;

                    ping = (Chunk) ois.readObject();
                    Log.i(TAG, "HERE0\t" + counter + "\t" +thisPort);




                    if(ping.msg != null && ping.msgid != -1 && ping.sendingProc != -1 && ping. seqNum == -1 && ping.procNum == -1) {
                        seqNum += 1;
                        pq.add(new Chunk(ping.msg, ping.msgid, ping.sendingProc, seqNum, thisPort, false));
                        Log.i(TAG, "HEREa\t" + counter + "\t" +thisPort +"\t"+ ping.msg);
                        openTheGates();
                        Log.i(TAG, "HERE\t"+ counter + "\t" +thisPort +"\t"+ ping.msg);
                        ObjectOutputStream oos = new ObjectOutputStream(clisco.getOutputStream());
                        Log.i(TAG, "HEREc\t"+ counter + "\t" +thisPort +"\t"+ ping.msg);
                        oos.writeObject(new Chunk(null, ping.msgid, thisPort, seqNum, -1, false));
                        Log.i(TAG, "HEREd\t"+ counter + "\t" +thisPort +"\t"+ ping.msg);
                        ois.close();
                        Log.i(TAG, "HEREe\t"+ counter + "\t" +thisPort +"\t"+ ping.msg);
                        oos.close();
                        clisco.close();

                    }
                    else if(ping.msg == null && ping.msgid != -1 && ping.sendingProc != -1 && ping.seqNum != -1 && ping.procNum != -1){

                        /*
                         * note that right here I think the pdf I'm using might be broken.
                         * It says to modify the message to have seqNum equal to ping.seqNum,
                         * not their new max. Actually that seems right bc like what does the receiving
                         * proc have to do with it.
                         */
                        seqNum = max(seqNum, ping.seqNum);
                        Log.i(TAG, "CHECKING PQ FOR msgid="+ping.msgid+"\nsendingProc="+ping.sendingProc+"\nseqNum"+ping.seqNum +"\nsuggestingProc="+ping.procNum);
                        for(Chunk c : pq){
                           //DOUBLE CHECK THESE! ! ! ! ! ! ! !

                            if(c.msgid == ping.msgid && c.sendingProc == ping.sendingProc){
                                pq.add(new Chunk(c.msg, c.msgid, ping.sendingProc, ping.seqNum, ping.procNum, true));
                                Log.i(TAG, "HERE0000\t"+ counter + "\t" +thisPort +"\t"+ c.msg);
                                pq.remove(c);
                                break;
                            }
                        }
//                        Chunk[] chunks = Arrays.asList(pq.toArray()).toArray(new Chunk[pq.toArray().length]);
//                        Arrays.sort(chunks);
                        for(Chunk c : pq){
                            Log.i(TAG, "\nFINAL CHUNK: MSG=" + c.msg + " \nmsgid="+c.msgid+" \nsender="+c.sendingProc+" \nseqNum="+c.seqNum+" \nsuggester="+c.procNum +" \nstatus="+c.status);
                        }
                        openTheGates();
                        PrintWriter out = new PrintWriter(clisco.getOutputStream());
                        out.println("Acknowledgement!");
                        ois.close();
                        out.close();
                        clisco.close();

                    }
                    else{
                        ois.close();
                        clisco.close();
                        Log.e(TAG, "This was not supposed to happen, chunk format error to server");
                    }
                    /*
                     * ok now I am working on method to keep going through and delivering messages
                     * to throw into the right spots above aka openTheGates
                     */

                        //pa2a shit below
//                    String msg;
//                    InputStreamReader inStream = new InputStreamReader(clisco.getInputStream());
//                    BufferedReader in = new BufferedReader(inStream);
//
//                    msg = in.readLine();
//                    if(msg == null){
//                        Log.i(TAG, "rip");
//                        continue;
//                    }openTheGates();
//                    ContentValues cv = new ContentValues();
//                    cv.put("key", String.valueOf(msgCount));
//                    msgCount++;
//                    cv.put("value", msg);
//                    Log.i(TAG, "Server msg received:" + msg);
//
//                    Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
//                    getContentResolver().insert(uri, cv);
//
//                    onProgressUpdate(msg);


                } catch (IOException e) {
                    Log.e(TAG, "ServerTask socket IOException:" + e.getMessage());
                }
                catch(ClassNotFoundException e){
                    Log.e(TAG,"Why dont u just fuck off");
                }
            }
            /*
             * End John Feerick code
             */
        }

        protected void openTheGates(){
            /*
             * Method which delivers messages to server in the event that the pq is updated
             */
            if(pq.size() > 0){
                while(pq.peek().status) {
                    Log.i(TAG, "gates" + "\t" + pq.peek().msg);
                    Chunk chunk = pq.remove();

                    String[] dumb = new String[2];
                    dumb[0] = Integer.valueOf(theCount).toString();
                    dumb[1] = chunk.msg;
                    onProgressUpdate(dumb);
                    Log.i(TAG, "on progress update " + dumb[1] + " with seq # = " + dumb[0] + "\t" +chunk.msg);
                    ContentValues cv = new ContentValues();
                    Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
                    Log.i(TAG, "bout to add msg " + chunk.msg + " with seq # = " + theCount + "\t" +chunk.msg);
                    cv.put("key", String.valueOf(theCount));
                    cv.put("value", chunk.msg);
                    getContentResolver().insert(uri, cv);
                    Log.i(TAG, "added msg " + chunk.msg + " with seq # = " + theCount + "\t" +chunk.msg);

                    theCount++;
                    if (pq.size() == 0) {
                        break;
                    }
                }
            }



            return;
        }

        protected void onProgressUpdate(final String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            /*
             * Begin John Feerick code
             *
             *
             * The following code was obtained from the top reponse at https://stackoverflow.com/questions/
             * 5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi
             */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String strReceived = strings[0].trim();
                    String msg = strings[1].trim();
                    TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                    remoteTextView.append(strReceived + "~" + msg + "\t\n");

                }
            });

            /*
             * End John Feerick code
             */
            return;
        }
    }
    /*
     * Saturday. attempting to implement mutual exclusion in client tasks by having a "done" int
     * which is incremented after the end of the initial for loop. Going to wrap the for loop in it
     * to make sure that the counter variable has not changed somewhere else.
    */

    private class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {
            /*
             * Begin John Feerick code
             */
            int tracker = 0;
            try {
                tracker = counter + 1;
                counter += 1;
                for(int i = 0; i < numServers; i++) {


                    String remotePort = REMOTE_PORTS[i];
                    /*
                     * Implementation of socket timeouts sourced from
                     * https://stackoverflow.com/questions/4969760/setting-a-timeout-for-socket-operations
                     */
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
//                    socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//                            Integer.parseInt(remotePort)), 3000);

                    String msgToSend = msgs[0];
                    /*
                     * TODO: Fill in your client code that sends out a message.
                     *
                     *
                     * Begin John Feerick code
                     */
                    Chunk msg1 = new Chunk(msgToSend, counter, thisPort, -1, -1, false);

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    Log.i(TAG, "HERE1\t"+ counter + "\t" + msgToSend);

                    oos.writeObject(msg1);
                    Log.i(TAG, "HERE2\t"+ counter + "\t" + msgToSend);


                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    Log.i(TAG, "HERE3\t"+ counter + "\t" + msgToSend);
                    Chunk ack = (Chunk) ois.readObject();
                    Log.i(TAG, "HERE4\t"+ counter + "\t" + msgToSend);
                    oos.close();
                    Log.i(TAG, "here5\t"+ counter + "\t" + msgToSend);
                    ois.close();

                    Log.i(TAG, "HERE6\t"+ counter + "\t" + msgToSend);
                    if(ack.msg != null || ack.msgid == -1 || ack.seqNum == -1 || ack.procNum != -1 || ack.sendingProc == -1){
                        Log.e(TAG, "ack is wrong on client side");
                    }

                    PriorityQueue<Suggestion> curQ = suggestedSeqNums.get(Integer.valueOf(ack.msgid).toString());
                    if(curQ == null){
                        suggestedSeqNums.put(Integer.valueOf(ack.msgid).toString(), new PriorityQueue<Suggestion>(1, new SuggestionComparator()));
                        curQ = suggestedSeqNums.get(Integer.valueOf(ack.msgid).toString());
                    }
                    curQ.add(new Suggestion(ack.seqNum, ack.sendingProc));
                    socket.close();
                    //PA2A shit
//                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                    out.println(msgToSend);
//                    in.readLine();
//                    out.close();
//                    in.close();


                    /*
                     * End John Feerick code
                     */
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");

            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException:"+e.getMessage());

            } catch (ClassNotFoundException e){
                    Log.e(TAG, "ClientTask socket classnotfoundexception:"+e.getMessage());
                }
//              catch (SocketTimeoutException e){
//                Log.e(TAG, "Socket Timeout Exception " + e.getMessage());
//              }

            PriorityQueue<Suggestion> curQ = suggestedSeqNums.get(Integer.valueOf(tracker).toString());
            if(curQ.size() != numServers){
                Log.e(TAG, "Number of acks to client not equal to numServers");
            }
            Log.i(TAG, "HEREf\t" + tracker + "\t" + msgs[0]);
            try{
                for(int i = 0; i < numServers; i++) {

                    String remotePort = REMOTE_PORTS[i];

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));

                    int nuSeqNum = curQ.peek().suggestedSeqNum;
                    Chunk msg2 = new Chunk(null, tracker, thisPort, nuSeqNum, curQ.peek().suggestingProc, false);

                    /*
                     * TODO: Fill in your client code that sends out a message.
                     *
                     *
                     * Begin John Feerick code
                     */
                    //Chunk msg1 = new Chunk(msgToSend, thisPort, counter, -1, -1, false);

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    Log.i(TAG, "HEREg\t"+ tracker+ "\t" + msgs[0]);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    Log.i(TAG, "HEREh\t"+ tracker+ "\t" + msgs[0]);
                    oos.writeObject(msg2);
                    Log.i(TAG, "HEREi\t"+ tracker+ "\t" + msgs[0]);
                    in.readLine();
                    Log.i(TAG, "HERE\t"+ tracker+ "\t" + msgs[0]);
                    oos.close();
                    in.close();
                    socket.close();

                }
            }
            catch(UnknownHostException e){
                Log.e(TAG, e.toString());
                }
            catch(IOException e){
                Log.e(TAG, e.toString());
            }
            return null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
