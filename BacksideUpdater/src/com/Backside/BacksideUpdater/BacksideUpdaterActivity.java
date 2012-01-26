package com.Backside.BacksideUpdater;

import java.io.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.content.Context;


public class BacksideUpdaterActivity extends Activity {
	private String updateManifestUrl = "https://raw.github.com/JerryScript/BACKside-IHO/master/README";
	private TextView textView;
	private TextView buttonTextView;
	private static final String BUILD_VERSION = Build.VERSION.INCREMENTAL;
	private static final String[] SEPARATED_DATE = BUILD_VERSION.split("\\.");
	private static final int BUILD_DATE = Integer.parseInt(SEPARATED_DATE[2]);
	private int ALREADY_CHECKED = 0;
	private String theDate;
	private String theUrl;
	private String theChangeLog;
	private Boolean upToDate;
    private PowerManager mPowerManager;

	
/** Called when the activity is first created. */


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		textView = (TextView) findViewById(R.id.pagetext);
		textView.setText("Click to check for the lastest update");
		
		buttonTextView = (TextView) findViewById(R.id.BacksideUpdaterButton);

	    Activity sContext = this;
		mPowerManager = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);

	}

	@Override
	public void onBackPressed() {

	    System.exit(0);
	}
	
	public void myClickHandler(View view) {
		switch (view.getId()) {
		case R.id.BacksideUpdaterButton:
			try {
				if (ALREADY_CHECKED == 0) {
					ALREADY_CHECKED = 1;
					textView.setText("");
					HttpClient client = new DefaultHttpClient();
					HttpGet request = new HttpGet(updateManifestUrl);
					HttpResponse response = client.execute(request);
					// Get the response
					BufferedReader rd = new BufferedReader(new InputStreamReader(
							response.getEntity().getContent()));
					String line = rd.readLine();
					String[] separated = line.split(",");
					theDate = separated[0];
					theUrl = separated[1];
					theChangeLog = separated[2];
					upToDate = (Integer.parseInt(theDate) >  BUILD_DATE);
					if (upToDate) {
						textView.setText(theChangeLog);
						new AlertDialog.Builder(this)
					    .setTitle("Change Log "+theDate)
					    .setMessage(theChangeLog)
					    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int whichButton) {
								textView.setText("A new build is available:\nBACKside-IHO-VM670-"+theDate);
					        }
					    }).show();
						buttonTextView.setText("Download Now");
					} else {
						textView.setText("Current: "+BUILD_DATE+"\n\nAvailable: "+theDate+"\n\nCheck again later");
						buttonTextView.setText("Already Up To Date");
					}
				} else {
					if (ALREADY_CHECKED == 1){
						if(!upToDate){
							System.exit(0);
						} else {
							ALREADY_CHECKED = 2;
							Intent downloadUpdate = new Intent(Intent.ACTION_VIEW);
							downloadUpdate.setData(Uri.parse(theUrl));
							startActivity(downloadUpdate);
							textView.setText("Wait for download to complete\nReboot into recovery\nWipe cache & dalvik cache\nThen flash the zip file");
							buttonTextView.setText("Reboot into Recovery Now");
						}
					} else {
						new AlertDialog.Builder(this)
					    .setTitle("Wait for download to complete!")
					    .setMessage("Check the notification dropdown!\n\nOnce the download completes,\nyou can reboot into recovery.\nWipe cache & dalvik,\nThen flash the zip file")
					    .setPositiveButton("Reboot Recovery", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int whichButton) {
								textView.setText("Rebooting into Recovery...");
								RebootCmd("reboot", "recovery", mPowerManager);
					        }
					    })
					    .setNegativeButton("Later", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int whichButton) {
								textView.setText("Yummy Gingerbread!");
								System.exit(0);
					        }
					    }).show();
					}
				}
			}

			catch (Exception e) {
				System.out.println("Nay, did not work");
				textView.setText(e.getMessage());
				}
			break;
			}
		}

	public void RebootCmd(String cmd, String args, PowerManager powermanager) {

		new AlertDialog.Builder(this)
	    .setTitle("Reboot into Recovery")
	    .setMessage("Are you sure you want\nto reboot into recovery now?")
	    .setPositiveButton("Reboot Recovery", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
				textView.setText("Rebooting into Recovery...");
		    	try {
		    		String[] str ={"su","-c","reboot recovery"};
		    		Runtime.getRuntime().exec(str);
		    		} catch (Exception e){
		    			System.out.println("failed to exec reboot recovery");
		    			}
	        }
	    })
	    .setNegativeButton("Later", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
				textView.setText("Yummy Gingerbread!");
				System.exit(0);
	        }
	    }).show();
	}
	
}

