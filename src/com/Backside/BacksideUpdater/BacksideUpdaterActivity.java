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
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class BacksideUpdaterActivity extends Activity {
	private String updateManifestUrl = "https://raw.github.com/JerryScript/BACKside-IHO/master/README";
	private static TextView textView;
	private TextView buttonTextView;
	private static final String BUILD_VERSION = Build.VERSION.INCREMENTAL;
	private static final String[] SEPARATED_DATE = BUILD_VERSION.split("\\.");
	private static final int BUILD_DATE = Integer.parseInt(SEPARATED_DATE[2]);
	private int ALREADY_CHECKED = 0;
	private String theDate;
	private String theUrl;
	private String theChangeLog;
	private String romName;
	private String theMD5;
	private static String localFileName;
	private static String theFileSize;
	private static Long fileSize;
	private static String downloadMD5;
	private static Boolean downloadComplete;
	private Boolean upToDate;

	
/** Called when the activity is first created. */


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		textView = (TextView) findViewById(R.id.pagetext);
		textView.setText("Click to check for the lastest update\n\nBe patient after clicking!");
		
		buttonTextView = (TextView) findViewById(R.id.BacksideUpdaterButton);

		
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
					theMD5 = separated[3];
					romName = separated[4];
					localFileName = "/download/"+romName;
					theFileSize = separated[5];
					upToDate = (BUILD_DATE >= Integer.parseInt(theDate));
					String file = android.os.Environment.getExternalStorageDirectory().getPath() + localFileName;
					File f = new File(file);
					if (!upToDate) {
						if (f.exists()) {
							if (!checkFileSize(romName)){
								showCustomToast("The latest build is already downloaded\n\nClick the button to check the MD5 sum");
								textView.setText("Latest build is already downloaded.\n\nReady to check md5\n\nBe patient after clicking!");
								buttonTextView.setText("Check MD5 Now");
								ALREADY_CHECKED = 2;
							} else {
								showCustomToast("Download not yet complete\n\nCheck the notification dropdown\nfor download status.\n\nOr press back to exit the Updater,\ndelete the partially downloaded file,\nand restart Updater to try again.");
								textView.setText("Download not complete.\n\nCheck the notification dropdown\nand try again.\n\nIf you have an incomplete download,\ndelete the partially downloaded file,\nand try again.");
								buttonTextView.setText("Check Download Status");
								ALREADY_CHECKED = 2;
							}
						} else {
							textView.setText("Change Log "+theDate+"\n\n"+theChangeLog);
							showCustomToast("A new build is available:\n\nBACKside-IHO-VM670-"+theDate);
							buttonTextView.setText("Download Now");
						}
					} else {
						textView.setText("Current: "+BUILD_DATE+"\n\nAvailable: "+theDate+"\n\nCheck again later");
						buttonTextView.setText("Already Up To Date");
					}
				} else {
					if (ALREADY_CHECKED == 1){
						if(upToDate){
							System.exit(0);
						} else {
							ALREADY_CHECKED = 2;
							Intent downloadUpdate = new Intent(Intent.ACTION_VIEW);
							downloadUpdate.setData(Uri.parse(theUrl));
							startActivity(downloadUpdate);
							textView.setText("Wait for download to complete\n\nReboot into recovery\n\nWipe cache & dalvik cache\nThen flash the zip file");
							buttonTextView.setText("Check Download Status");
						}
					} else {
						downloadComplete = checkFileSize(romName);
						if (!(downloadComplete)) {
							if(checkMD5(theMD5, romName)){
								new AlertDialog.Builder(this)
								.setTitle("MD5 Verified!")
								.setIcon(R.drawable.download_complete_icon)
								.setMessage("Reboot into recovery,\nwipe cache & dalvik-cache,\nthen flash the zip file located\nin the download directory")
								.setPositiveButton("Reboot Recovery", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										textView.setText("Rebooting into Recovery...");
										RebootCmd("reboot", "recovery");
									}
								})
								.setNegativeButton("Later", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										textView.setText("Yummy Gingerbread!");
										System.exit(0);
										}
								}).show();
							} else {
								new AlertDialog.Builder(this)
								.setTitle("Download Error")
								.setIcon(R.drawable.md5_error)
								.setMessage("The downloaded file md5\n"+downloadMD5+"\ndoes not match the build\n"+theMD5+"\nTry downloading again!")
								.setPositiveButton("Download again", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										ALREADY_CHECKED = 2;
										textView.setText("Wait for download to complete\n\nCheck the notification dropdown");
										buttonTextView.setText("Check Again");
										Intent downloadUpdate = new Intent(Intent.ACTION_VIEW);
										downloadUpdate.setData(Uri.parse(theUrl));
										startActivity(downloadUpdate);
										}
								})
								.setNegativeButton("Later", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
										textView.setText("Yummy Gingerbread!");
										System.exit(0);
										}
								}).show();
							}
						} else {
							ALREADY_CHECKED = 2;
							String file = android.os.Environment.getExternalStorageDirectory().getPath() + localFileName;
							File f = new File(file);
							if (f.exists()) {
								showCustomToast("Download not yet complete\n\nCheck the notification dropdown\nfor download status.\n\nOr press back to exit the Updater,\ndelete the partially downloaded file,\nand restart Updater to try again.");
							} else {
								ALREADY_CHECKED = 1;
								showCustomToast("Download has not started\n\nClick the button to try again now.");
								buttonTextView.setText("Download Now");
							}
						}
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

	public void RebootCmd(String cmd, String args) {

		new AlertDialog.Builder(this)
	    .setTitle("Reboot into Recovery")
	    .setMessage("Are you sure you want to\nreboot into recovery now?")
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
	
	public void showCustomToast(String str) {
		Toast toast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		LinearLayout toastView = (LinearLayout) toast.getView();
		ImageView imageCodeProject = new ImageView(getApplicationContext());
		imageCodeProject.setImageResource(R.drawable.custom_update_dialog_icon);
		toastView.addView(imageCodeProject, 0);
		toast.show();

	}
	
	public static boolean checkFileSize(String fileName) {
		try {
		File file = new File("/sdcard"+localFileName);
		
		fileSize = file.length() / 1024 / 1024;
		
		return (fileSize < Long.valueOf(theFileSize));
		} catch (Exception e) {
			return true;
		}
		
		
	}
	
	public static boolean checkMD5(String md5, String fileName) throws IOException {
		textView.setText("Checking the md5sum...");

		if (md5 == null || md5 == "" || fileName == null) {
			return false;
			}
		
		String calculatedDigest = calculateMD5("/sdcard"+localFileName);
		
		if (calculatedDigest == null) {
			return false;
			}
		
		return calculatedDigest.equalsIgnoreCase(md5);
		
	}
	
	public static String calculateMD5(String fileName) throws IOException {
		java.lang.Process process = Runtime.getRuntime().exec("md5sum "+fileName);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
		process.getInputStream()));
		
		String[] results =  bufferedReader.readLine().split(" ");
		downloadMD5 = results[0];
		
		return downloadMD5;
	
	}
	
}

