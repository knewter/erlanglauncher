package com.isotope11.erlanglauncher;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    public static class PlaceholderFragment extends Fragment {
        TextView mHello;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            this.mHello = (TextView) rootView.findViewById(R.id.helloWorld);

            this.copyErlangIntoDataDir();

            this.mHello.append(context.getFilesDir().toString());

            return rootView;
        }

        public void doCommand(String command, String arg0, String arg1) {
            try {
                // android.os.Exec is not included in android.jar so we need to use reflection.
                Class execClass = Class.forName("android.os.Exec");
                Method createSubprocess = execClass.getMethod("createSubprocess",
                        String.class, String.class, String.class, int[].class);
                Method waitFor = execClass.getMethod("waitFor", int.class);

                // Executes the command.
                // NOTE: createSubprocess() is asynchronous.
                int[] pid = new int[1];
                FileDescriptor fd = (FileDescriptor)createSubprocess.invoke(
                        null, command, arg0, arg1, pid);

                // Reads stdout.
                // NOTE: You can write to stdin of the command using new FileOutputStream(fd).
                FileInputStream in = new FileInputStream(fd);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String output = "";
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output += line + "\n";
                    }
                } catch (IOException e) {
                    // It seems IOException is thrown when it reaches EOF.
                }

                // Waits for the command to finish.
                waitFor.invoke(null, pid[0]);

                // send output to the textbox
                Log.d("Fragment", output);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e.getMessage());
            } catch (SecurityException e) {
                throw new RuntimeException(e.getMessage());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e.getMessage());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e.getMessage());
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void copyErlangIntoDataDir(){
            Log.d("Fragment", "copyErlangIntoDataDir start");

            File newEpmd = new File("/data/data/com.isotope11.erlanglauncher/files/epmd");
            try {
                newEpmd.createNewFile();
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newEpmd));
                String files[] = this.getActivity().getAssets().list("");
                for(int i = 0; i < files.length; i++){
                    Log.d("Fragment", "asset: " + files[i]);
                }
                BufferedInputStream in = new BufferedInputStream(this.getActivity().getAssets().open("otp_rel/erts-5.10.4/bin/epmd"));
                int b;
                while ((b = in.read()) != -1) {
                    out.write(b);
                }
                //
                out.flush();
                out.close();
                in.close();
                Log.d("Fragment", "copyErlangIntoDataDir pre chmod");
                // chmod?
                this.doCommand("/system/bin/chmod", "777", "/data/data/com.isotope11.erlanglauncher/files/epmd");
            } catch (IOException ex) {
                Log.d("Fragment", "copyErlangIntoDataDir exception: " + ex.getMessage());
            }
        }


        protected static String getSdCard() {
            try {
                String filesDir = context.getFilesDir().toString();
                // Executes the command.
                Process process = Runtime.getRuntime().exec("/system/bin/ls " + filesDir + "/");

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

                return output.toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void addText(String text){
            this.mHello.append(text);
        }
    }

}
