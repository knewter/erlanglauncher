package com.isotope11.erlanglauncher;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpErlangExit;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {

  private static Context context;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MainActivity.context = getApplicationContext();

    setContentView(R.layout.activity_main);

    if (savedInstanceState == null) {
      getFragmentManager().beginTransaction()
              .add(R.id.container, new PlaceholderFragment())
              .commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * A placeholder fragment containing a simple view.
   */
  public class PlaceholderFragment extends Fragment {
    TextView mHello;

    public PlaceholderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_main, container, false);

      this.mHello = (TextView) rootView.findViewById(R.id.helloWorld);

      this.listFiles();
      //this.copyErlangIntoDataDir(); // Need to make this optional, check if it's there, or something...
      this.makeExecutable("erlang/bin/epmd");
      this.makeExecutable("erlang/bin/erl");
      this.listFiles();
      this.launchEpmd();
      this.launchErlangNode();
      this.listProcesses();
      JInterfaceTester task = new JInterfaceTester();
      task.execute();

      this.mHello.setText("All good...");

      return rootView;
    }

    public void makeExecutable(String path) {
      this.doCommand("/system/bin/chmod 777 /data/data/com.isotope11.erlanglauncher/files/" + path);
    }

    public void listFiles() {
      this.doCommand("/system/bin/ls -al /data/data/com.isotope11.erlanglauncher/files/erlang/bin");
    }

    public void launchEpmd() {
      this.doCommand("/data/data/com.isotope11.erlanglauncher/files/erlang/bin/epmd -daemon");
    }

    public void launchErlangNode() {
      this.doCommand("/data/data/com.isotope11.erlanglauncher/files/erlang/bin/erl -name foo@192.168.2.10 -setcookie test");
    }

    public void listProcesses() {
      this.doCommand("/system/bin/ps ax");
    }

    public void doCommand(String command) {
      try {
        // Executes the command.
        Process process = Runtime.getRuntime().exec(command);

        // Reads stdout.
        // NOTE: You can write to stdin of the command using
        //       process.getOutputStream().
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
          output.append(buffer, 0, read);
        }
        reader.close();

        // Waits for the command to finish.
        process.waitFor();

        // send output to the log
        Log.d("Fragment", output.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    protected void copyErlangIntoDataDir() {
      Log.d("Fragment", "copyErlangIntoDataDir start");

      InputStream erlangZipFileInputStream = null;
      try {
        erlangZipFileInputStream = getActivity().getApplicationContext().getAssets().open("erlang_R16B.zip");
      } catch (IOException e) {
        e.printStackTrace();
      }
      Decompress unzipper = new Decompress(erlangZipFileInputStream, "/data/data/com.isotope11.erlanglauncher/files/");
      unzipper.unzip();

      Log.d("Fragment", "copyErlangIntoDataDir done");
    }
  }

  public class JInterfaceTester extends AsyncTask<Object, Void, String>{
    @Override
    protected String doInBackground(Object... arg0) {
      testJInterface();
      return "k...";
    }

    public void testJInterface(){
      String server = "server@192.168.2.20";

      OtpNode self = null;
      OtpMbox mbox = null;
      try {
        self = new OtpNode("mynode", "test");
        mbox = self.createMbox("facserver");

        if (self.ping(server, 2000)) {
          System.out.println("remote is up");
        } else {
          System.out.println("remote is not up");
          return;
        }
      } catch (IOException e1) {
        e1.printStackTrace();
      }

      OtpErlangObject[] msg = new OtpErlangObject[2];
      msg[0] = mbox.self();
      msg[1] = new OtpErlangAtom("ping");
      OtpErlangTuple tuple = new OtpErlangTuple(msg);
      mbox.send("pong", server, tuple);

      while (true)
        try {
          OtpErlangObject robj = mbox.receive();
          OtpErlangTuple rtuple = (OtpErlangTuple) robj;
          OtpErlangPid fromPid = (OtpErlangPid) (rtuple.elementAt(0));
          OtpErlangObject rmsg = rtuple.elementAt(1);

          System.out.println("Message: " + rmsg + " received from:  "
                  + fromPid.toString());

          OtpErlangAtom ok = new OtpErlangAtom("stop");
          mbox.send(fromPid, ok);
          break;

        } catch (OtpErlangExit e) {
          e.printStackTrace();
          break;
        } catch (OtpErlangDecodeException e) {
          e.printStackTrace();
        }
    }

  }
}

class Decompress {
  private InputStream zip;
  private String loc;

  public Decompress(InputStream zipFileInputStream, String location) {
    zip = zipFileInputStream;
    loc = location;

    dirChecker("");
  }

  public void unzip() {
    try {
      ZipInputStream zin = new ZipInputStream(zip);
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        Log.d("Fragment", "Unzipping " + ze.getName());

        if (ze.isDirectory()) {
          dirChecker(ze.getName());
        } else {
          FileOutputStream fout = new FileOutputStream(loc + ze.getName());
          for (int c = zin.read(); c != -1; c = zin.read()) {
            fout.write(c);
          }

          zin.closeEntry();
          fout.close();
        }

      }
      zin.close();
    } catch (Exception e) {
      Log.e("Fragment", "unzip", e);
    }

  }

  private void dirChecker(String dir) {
    File f = new File(loc + dir);

    if (!f.isDirectory()) {
      f.mkdirs();
    }
  }
}
