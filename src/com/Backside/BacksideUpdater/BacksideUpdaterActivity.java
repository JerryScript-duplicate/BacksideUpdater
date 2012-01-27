package com.Backside.BacksideUpdater;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class BacksideUpdaterActivity extends Activity {
	private static TextView textView;
	private TextView buttonTextView;
	private static final String BUILD_VERSION = "eng.jerry.20120120.1234"; //Build.VERSION.INCREMENTAL;
	private static final String[] SEPARATED_DATE = BUILD_VERSION.split("\\.");
	private static final int BUILD_DATE = Integer.parseInt(SEPARATED_DATE[2]);
	private int ALREADY_CHECKED = 0;
	private String theDate;
	private String theUrl;
	private String theChangeLog;
	private String romName;
	private static String line;
	private static String localFileName;
	private static String theFileSize;
	private static Long fileSize;
	private static String theMD5;
	private static Boolean goodMD5;
	private static String downloadMD5;
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
					// first time user clicks main button
					ALREADY_CHECKED = 1;
					// create a progress spinner to give the user
					// something to look at while we grab the manifest
					final ProgressDialog spinnerDialog = ProgressDialog.show(
							this, "", "Checking for newest version...", true);
					spinnerDialog.setOnDismissListener(new OnDismissListener() {
						public void onDismiss(DialogInterface dialog) {
							checkStatus(); // manifest download complete, run checks
						}
					});
					// create a separate thread to check the manifest
					new Thread(new Runnable() {
						public void run() {
							try {
								HttpClient client = new DefaultHttpClient();
								HttpGet request = new HttpGet("https://raw.github.com/JerryScript/BACKside-IHO/master/README");
								HttpResponse response = client.execute(request);
								// Get the response
								BufferedReader rd = new BufferedReader(new InputStreamReader(
										response.getEntity().getContent()));
								line = rd.readLine();
							} catch (IOException e) {
								// TODO possibly create error logging
								textView.setText(e.getMessage());
							}
							spinnerDialog.dismiss();
							return;
						}
					}).start();
				} else {
					if (ALREADY_CHECKED == 1){
						// second time user clicks main button
						// manifest has been checked
						if(upToDate){
							// if up to date, nothing else to do, so exit
							System.exit(0);
						} else {
							// if not up to date, open download URL, and set text views
							ALREADY_CHECKED = 2;
							Intent downloadUpdate = new Intent(Intent.ACTION_VIEW);
							downloadUpdate.setData(Uri.parse(theUrl));
							startActivity(downloadUpdate);
							textView.setText("Wait for download to complete\n\nReboot into recovery\n\nWipe cache & dalvik cache\nThen flash the zip file");
							buttonTextView.setText("Check Download Status");
						}
					} else {
						// third time user clicks main button, three cases available
						// --download is complete, ready to check md5sum
						// --download is in progress (could be frozen, need to handle this)
						// --download never started
						if (!checkFileSize(romName)) {
							// file download complete, check md5sum against manifest
							// create a progress spinner to give the user
							// something to look at while we grab the manifest
							final ProgressDialog spinnerDialog = ProgressDialog.show(
									this, "", "Checking the MD5 sum now...", true);
							spinnerDialog.setOnDismissListener(new OnDismissListener() {
								public void onDismiss(DialogInterface dialog) {
									md5dialog(); // show md5 dialogs
								}
							});
							// create a separate thread to check the md5sum
							new Thread(new Runnable() {
								public void run() {
									try {
										if (theMD5 == null || theMD5 == "" || localFileName == null) {
											goodMD5 = false;
										}
										String calculatedDigest = calculateMD5("/sdcard"+localFileName);
										if (calculatedDigest == null) {
											goodMD5 = false;
										} else {
											goodMD5 = calculatedDigest.equalsIgnoreCase(theMD5);
										}
									} catch (IOException e) {
										// TODO possibly add error logging
										textView.setText(e.getMessage());
									}
									spinnerDialog.dismiss();
									return;
								}
							}).start();
						} else {
							// file download has not completed, three possible cases
							// --download is in progress
							// --download is frozen
							// --user canceled the download
							// Nothing we can do till we pull downloads into the app
							ALREADY_CHECKED = 2;
							// check if download has started
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
				// TODO possibly add error logging
				textView.setText(e.getMessage());
				}
			break;
			}
		}

	public void RebootCmd() {
		// create a dialog choice to allow user to reboot directly into recovery
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
		// a function to show a custom message to the user in a toast window
		Toast toast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		LinearLayout toastView = (LinearLayout) toast.getView();
		ImageView imageCodeProject = new ImageView(getApplicationContext());
		imageCodeProject.setImageResource(R.drawable.custom_update_dialog_icon);
		toastView.addView(imageCodeProject, 0);
		toast.show();

	}
	
	private void checkStatus(){
		// split up the manifest into useable data
		textView.setText("");
		String[] separated = line.split(",");
		theDate = separated[0];
		theUrl = separated[1];
		theChangeLog = separated[2];
		theMD5 = separated[3];
		romName = separated[4];
		localFileName = "/download/"+romName;
		theFileSize = separated[5];
		// check the current build date against the manifest date
		upToDate = (BUILD_DATE >= Integer.parseInt(theDate));
		// check for the latest version in the downloaded directory
		String file = android.os.Environment.getExternalStorageDirectory().getPath() + localFileName;
		File f = new File(file);
		if (!upToDate) {
			// if we aren't up to date, see if we have downloaded yet
			if (f.exists()) {
				if (!checkFileSize(romName)){
					// if the download is complete, prompt user to check the md5sum
					showCustomToast("The latest build is already downloaded\n\nClick the button to check the MD5 sum");
					textView.setText("Latest build is already downloaded.\n\nReady to check md5\n\nBe patient after clicking!");
					buttonTextView.setText("Check MD5 Now");
					ALREADY_CHECKED = 2; // reset value so next button click returns to these functions
				} else {
					// download isn't complete, inform user and wait
					showCustomToast("Download not yet complete\n\nCheck the notification dropdown\nfor download status.\n\nOr press back to exit the Updater,\ndelete the partially downloaded file,\nand restart Updater to try again.");
					textView.setText("Download not complete.\n\nCheck the notification dropdown\nand try again.\n\nIf you have an incomplete download,\ndelete the partially downloaded file,\nand try again.");
					buttonTextView.setText("Check Download Status");
					ALREADY_CHECKED = 2; // reset value so next button click returns to these functions
				}
			} else {
				// if we haven't downloaded yet, prompt the user to download the new version
				textView.setText("Change Log "+theDate+"\n\n"+theChangeLog);
				showCustomToast("A new build is available:\n\nBACKside-IHO-VM670-"+theDate);
				buttonTextView.setText("Download Now");
			}
		} else {
			// if we are already up to date, inform the user
			textView.setText("Current: "+BUILD_DATE+"\n\nAvailable: "+theDate+"\n\nCheck again later");
			buttonTextView.setText("Already Up To Date");
		}

	}
	
	private void md5dialog (){
		// create dialogs depending on the results of the md5sum
		if(goodMD5){
			// if md5sum is good, alert user
			// and give option to reboot into recovery
			new AlertDialog.Builder(this)
			.setTitle("MD5 Verified!")
			.setIcon(R.drawable.download_complete_icon)
			.setMessage("Reboot into recovery,\nwipe cache & dalvik-cache,\nthen flash the zip file located\nin the download directory")
			.setPositiveButton("Reboot Recovery", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					textView.setText("Rebooting into Recovery...");
					RebootCmd();
				}
			})
			.setNegativeButton("Later", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					textView.setText("Yummy Gingerbread!");
					System.exit(0);
					}
			}).show();
		} else {
			// if md5sum is bad, alert user
			// and give options to delete and re-download the file
			new AlertDialog.Builder(this)
			.setTitle("Download Error")
			.setIcon(R.drawable.md5_error)
			.setMessage("The downloaded file md5\n"+downloadMD5+"\ndoes not match the build\n"+theMD5+"\n\nDELETE CORRUPT FILE\n\nThen download again!")
			.setPositiveButton("Delete & Download Again", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					ALREADY_CHECKED = 2;
					textView.setText("Wait for download to complete\n\nCheck the notification dropdown");
					buttonTextView.setText("Check Again");
					String file = android.os.Environment.getExternalStorageDirectory().getPath() + localFileName;
					File f = new File(file);
					f.delete();
					Intent downloadUpdate = new Intent(Intent.ACTION_VIEW);
					downloadUpdate.setData(Uri.parse(theUrl));
					startActivity(downloadUpdate);
					}
			})
			.setNegativeButton("Delete Manually", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					textView.setText("Yummy Gingerbread!");
					System.exit(0);
					}
			}).show();
		}
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
	
	public static boolean checkMD5() throws IOException {
		textView.setText("Checking the md5sum...");

		if (theMD5 == null || theMD5 == "" || localFileName == null) {
			return false;
			}
		
		String calculatedDigest = calculateMD5("/sdcard"+localFileName);
		
		if (calculatedDigest == null) {
			return false;
			}
		
		return calculatedDigest.equalsIgnoreCase(theMD5);
		
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

