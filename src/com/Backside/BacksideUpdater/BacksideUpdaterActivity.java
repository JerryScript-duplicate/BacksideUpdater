package com.Backside.BacksideUpdater;


import java.io.BufferedReader;
import java.io.DataOutputStream;
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

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class BacksideUpdaterActivity extends Activity {
	private static TextView textView;
	private static TextView buttonTextView;
	private static final String manifestURL = "https://raw.github.com/JerryScript/BACKside-IHO/master/legacy";
	private static final String BUILD_VERSION = Build.VERSION.INCREMENTAL;
	private static final String[] SEPARATED_DATE = BUILD_VERSION.split("\\.");
	private static final int BUILD_DATE = Integer.parseInt(SEPARATED_DATE[2]);
	private static int ALREADY_CHECKED = 0;
	private String theDate;
	private int choosenDate = 0;
	private static String theUrl;
	private String theChangeLog = "";
	private String romName;
	private static String line;
	private static String localFileName;
	private static String theFileSize;
	private static Long fileSize;
	private static String theMD5;
	private static String downloadMD5;
	private static Boolean goodMD5;
	private Boolean upToDate;
	private Boolean alreadyDownloaded;
	private static String recoveryName;
	private static String recoveryMessage;
	private static String lastRecoveryMessage;
	private static int recoveryStepCount = 0;
    private static final int REQUEST_CODE_PICK_FILE = 999;
    private static final int REQUEST_CODE_PICK_RECOVERY = 1000;
    private static Context theView;
    GestureDetector gd;
	
/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.main);
		theView = this;
		textView = (TextView) findViewById(R.id.pagetext);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		textView.setText("Click above to check for the lastest update\n\n\nPress your menu key to check\nthe md5sum of a file you have\nalready downloaded");
		buttonTextView = (TextView) findViewById(R.id.BacksideUpdaterButton);
		buttonTextView.setVisibility(4);
		
		alreadyDownloaded = false;
		checkManifest();
		gd = new GestureDetector(getBaseContext(), sogl);
	}

	// Completely exit app when back button is pressed
	@Override
	public void onBackPressed() {
	    System.exit(0);
	}
	
	// Keep portrait orientation only, I'm lazy
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	// Handle longpress to launch easter egg
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        return gd.onTouchEvent(event);
    }

    GestureDetector.SimpleOnGestureListener sogl = new GestureDetector.SimpleOnGestureListener() {
        public boolean onDown(MotionEvent event) {
			return true;
        }
        public void onLongPress(MotionEvent event) {
        	// add easter egg
        }   
    };

	// Create menu_key menus
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, 1, 1, "Choose ROM Zip From SDCard");
		menu.add(1, 2, 2, "Show Changelog");
		menu.add(1, 3, 3, "Show All Versions");
		menu.add(1, 4, 4, "Install A Recovery");
		menu.add(1, 5, 5, "Exit");
		return true;
	}

        @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case 1:
			textView.setText("Press Menu Key For Options");
			alreadyDownloadedHandler();
			return true;
		case 2:
			textView.setGravity(3);
			textView.setText("Changelog "+romName+":\n\n"+theChangeLog);
			return true;
		case 3:
			textView.setText("Press Menu Key For Options");
			showExtendedManifest();
			return true;
		case 4:
			textView.setText("Press Menu Key For Options");
			TextView myMsg = new TextView(theView);
			myMsg.setText("Click Choose Recovery\nto install a recovery image\nstored on your sdcard");
			myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
			new AlertDialog.Builder(theView)
			.setTitle("Install Recovery Image")
			.setView(myMsg)
			.setPositiveButton("Choose Recovery From SDcard", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					recoveryName = "recovery.img";
			        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			        intent.setDataAndType(Uri.fromFile(new File("/sdcard/download")), "file/*");
			        startActivityForResult(intent, REQUEST_CODE_PICK_RECOVERY);	
					}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// nothing to do
				}
			}).show();
			return true;
		case 5:
			System.exit(0);
			return true;
	}
		return super.onOptionsItemSelected(item);
	}
	
	// This is the main function, most events are funneled through here
	public void myClickHandler(View view) {
		textView.setGravity(17);
		switch (view.getId()) {
		case R.id.BacksideUpdaterButton:
			try {
				if (ALREADY_CHECKED == 0) {
					// first time user clicks main button
					ALREADY_CHECKED = 1;
					textView.setText("Checking packages against the manifest, standby...");
					checkStatus();
				} else {
					if (ALREADY_CHECKED == 1){
						// second time user clicks main button
						// manifest has been checked
						if(upToDate && !alreadyDownloaded){
							// if up to date, nothing else to do, so exit
							System.exit(0);
						} else {
							// if not up to date, open download URL, and set text views
							ALREADY_CHECKED = 2;
							downloadUpdateNow();
						}
					} else {
						// ALREADY_CHECKED equals 2
						// third time user clicks main button, three cases available
						// --download is complete, ready to check md5sum
						// --download is in progress (could be frozen, need to handle this)
						// --download never started
						if (!checkFileSize(romName)) {
							// file download complete, check md5sum against manifest
							textView.setText("Checking MD5");
							checkMD5(localFileName, false);
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
								// Download is still in progress
								showCustomToast("Download not yet complete\n\nCheck the notification dropdown\nfor download status.\n\nOr press back to exit the Updater,\ndelete the partially downloaded file,\nand restart Updater to try again.");
								textView.setText("Download not yet complete\n\nCheck the notification dropdown\nfor download status.\n\nOr press back to exit the Updater,\ndelete the partially downloaded file,\nand restart Updater to try again.");
							} else {
								// Download has not begun
								ALREADY_CHECKED = 1;
								showCustomToast("Download has not started\n\nClick the button to try again now,\n\nOr Press Menu Key For Options");
								textView.setText("Download has not started\n\nClick the button to try again now,\n\nOr Press Menu Key For Options");
								buttonTextView.setText("Download Now");
							}
						}
					}
				}
			} catch (Exception e) {
				textView.setText(e.getMessage());
			}
			break;
		}
	}

	// Check for network connectivity, still could crash during network changes
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	// Download the update manifest
	public void checkManifest() {
		// check for network connection
		if (isNetworkAvailable()){
			// create a progress spinner to give the user
			// something to look at while we grab the manifest
			final ProgressDialog manifestDialog = ProgressDialog.show(
					this, "BacksideUpdater\nChecking For Updates", "Downloading manifest now...", true);
			if (!alreadyDownloaded) {
				manifestDialog.setOnDismissListener(new OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						checkStatus(); // manifest download complete, run checks
					}
				});
			}
			// create a separate thread to check the manifest
			new Thread(new Runnable() {
				public void run() {
					try {
						HttpClient client = new DefaultHttpClient();
						HttpGet request = new HttpGet(manifestURL);
						HttpResponse response = client.execute(request);
						// Get the response
						BufferedReader rd = new BufferedReader(new InputStreamReader(
								response.getEntity().getContent()));
						line = rd.readLine();
					} catch (IOException e) {
						textView.setText(e.getMessage());
					}
					manifestDialog.dismiss();
					return;
				}
			}).start();
			buttonTextView.setText("Checking...");
		} else {
			// No network connection is available let the user know,
			// and give option to open wireless settings
			TextView myMsg = new TextView(theView);
			myMsg.setText("No network connection available.\n\nEnable either WiFi or 3g under\n\nSettings -- Wireless & Networks");
			myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
			new AlertDialog.Builder(theView)
			.setTitle("Network Error!")
			.setIcon(R.drawable.md5_error)
			.setView(myMsg)
			.setPositiveButton("Open Wireless Settings", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
			        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
			        System.exit(0);
					}
			})
			.setNegativeButton("Try Again Later", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					System.exit(0);
				}
			}).show();
		}
	}
	
	// show a list of older builds
	private void showExtendedManifest(){
		final String[] oldManifest = line.split("~");
		final String[] oldVersion = new String[oldManifest.length];
		int cntr = 0;
		for (String i : oldManifest) {
			String[] nextVersion = i.split(",");
			String[] stuff = nextVersion[4].split("\\.");
			String[] romStuff = stuff[0].split("-");
			if (cntr == 0) {
				oldVersion[cntr] = romStuff[3] + "  (newest)";
			} else {
				oldVersion[cntr] = romStuff[3];
			}
			cntr ++;
		}
		new AlertDialog.Builder(theView)
		.setTitle("BACKside-IHO Builds - 2012")
		.setItems(oldVersion, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	choosenDate = item;
		    	checkStatus();
		    }
		}).show();
	}

	// Parse the manifest and check current status
	private void checkStatus(){
		ALREADY_CHECKED = 1;
		// split up the manifest into useable data
		textView.setText("");
		String[] romVersions = line.split("~");
		String[] separated = romVersions[choosenDate].split(",");
		theDate = separated[0];
		theUrl = separated[1];
		// format the changelog
		theChangeLog = separated[2].replace("--", "\n\n--");
		theMD5 = separated[3];
		romName = separated[4];
		localFileName = "/download/"+romName;
		theFileSize = separated[5];
		// check the current build date against the manifest date
		upToDate = (BUILD_DATE >= Integer.parseInt(theDate) && choosenDate == 0);
		if (upToDate) { buttonTextView.setVisibility(4); } else { buttonTextView.setVisibility(0); }
		// check for the latest version in the downloaded directory
		String file = android.os.Environment.getExternalStorageDirectory().getPath() + localFileName;
		File f = new File(file);
		if (!upToDate) {
			// if we aren't up to date, see if we have downloaded yet
			if (f.exists()) {
				if (!checkFileSize(romName)){
					// if the download is complete, prompt user to check the md5sum
					showCustomToast("The latest build is already downloaded\n\nClick the button to check the MD5 sum");
					textView.setText("Latest build is already downloaded.\n\nReady to check md5\nof downloaded file.\n\nOr Press Menu Key For Options");
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
				textView.setGravity(3);
				textView.setText("Changelog "+romName+":\n\n"+theChangeLog);
				if (choosenDate == 0){
					showCustomToast("A new build is available:\n\nBACKside-IHO-VM670-"+theDate);
				} else {
					showCustomToast("Click the button to download\n\nBACKside-IHO-VM670-"+theDate);
				}
				buttonTextView.setText("Download Now");
			}
		} else {
			// if we are already up to date, inform the user
			textView.setText("Already Up To Date!\n\nCurrent: "+BUILD_DATE+"\n\nAvailable: "+theDate+"\n\nCheck again later\n\nPress Menu Key For Options");
			buttonTextView.setText("Already Up To Date");
		}

	}
	
	// Let user check any already downloaded file against the manifest md5sum
	public void alreadyDownloadedHandler() {
		textView.setGravity(17);
		try {
			alreadyDownloaded = true;
			ALREADY_CHECKED = 2;
			String[] romVersions = line.split("~");
			String[] separated = romVersions[choosenDate].split(",");
			theDate = separated[0];
			theUrl = separated[1];
			theChangeLog = separated[2];
			theMD5 = separated[3];
			romName = separated[4];
			theFileSize = separated[5];
			// check the current build date against the manifest date
			upToDate = (BUILD_DATE >= Integer.parseInt(theDate));
	        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
	        intent.setDataAndType(Uri.fromFile(new File("/sdcard/download")), "file/*");
	        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);	
			
		} catch (Exception e) {
			textView.setText(e.getMessage());
		}
	}
	
	// Open browser to download url from the manifest
	public static void downloadUpdateNow(){
		Intent downloadUpdate = new Intent(Intent.ACTION_VIEW);
		downloadUpdate.setData(Uri.parse(theUrl));
		theView.startActivity(downloadUpdate);
		textView.setText("Wait for download to complete\n\nBackup Before You Flash!\n\nReboot into recovery\n\nWipe cache & dalvik cache\nThen flash the zip file");
		buttonTextView.setText("Check Download Status");
	}
	
	// create dialogs depending on the results of the md5sum
	private static void md5Dialog (final String fileName, final Boolean downloaded){
		if(goodMD5){
			// if md5sum is good, alert user
			// and give option to reboot into recovery
			TextView myMsg = new TextView(theView);
			myMsg.setText("MD5 Verified!\nReboot into recovery,\nwipe cache & dalvik-cache,\nthen flash the zip file");
			myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
			new AlertDialog.Builder(theView)
			.setTitle("MD5")
			.setIcon(R.drawable.download_complete_icon)
			.setView(myMsg)
			.setPositiveButton("Reboot Recovery", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					textView.setText("Rebooting into Recovery...");
					RebootCmd();
				}
			})
			.setNegativeButton("Install Later", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					textView.setText("Press Menu Key For Options");
					}
			}).show();
		} else {
			// if md5sum is bad, alert user
			// and give options to delete and re-download the file
			buttonTextView.setVisibility(0);
			TextView myMsg = new TextView(theView);
			myMsg.setText("The downloaded file md5\n"+downloadMD5+"\ndoes not match the build\n"+theMD5+"\n\nDELETE CORRUPT FILE\n\nThen download again!");
			myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
			new AlertDialog.Builder(theView)
			.setTitle("Download Error")
			.setIcon(R.drawable.md5_error)
			.setView(myMsg)
			.setPositiveButton("Delete & Download Again", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					ALREADY_CHECKED = 2;
					textView.setText("Click the top button to download again");
					buttonTextView.setText("Download Now");
					File f = new File(fileName);
					f.delete();
					downloadUpdateNow();
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
	
	// Check the md5sum in a separate thread to avoid hanging the main thread
	public static String checkMD5(final String fileName, Boolean downloaded) throws IOException {
		String thisFileName = fileName;
		if (thisFileName.substring(thisFileName.length() - 3).equalsIgnoreCase("zip")) {
			final String md5FileName = (!downloaded) ? android.os.Environment.getExternalStorageDirectory().getPath() + fileName : fileName;
			buttonTextView.setVisibility(4);
			textView.setText("Checking MD5");
			final ProgressDialog md5Dialog = ProgressDialog.show(
					theView, "Checking The MD5", "Calculating md5 checksum...", true);
			md5Dialog.setOnDismissListener(new OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					md5Dialog(fileName, true); // show md5 dialogs
				}
			});
			// create a separate thread to check the md5sum
			new Thread(new Runnable() {
				public void run() {
					try {
						String calculatedDigest = calculateMD5(md5FileName);
						if (calculatedDigest == null) {
							goodMD5 = false;
							textView.setText("Error Checking MD5!\n\nPlease close the app\nand try again.");
							}
						goodMD5 = calculatedDigest.equalsIgnoreCase(theMD5);
					} catch (IOException e) {
						textView.setText(e.getMessage());
					}
					md5Dialog.dismiss();
					return;
				}
			}).start();
		} else {
			badFilePath(0);
		}
		return "done";
	}
	
	// Use exec to calculate the downloaded file's md5sum
	public static String calculateMD5(String fileName) throws IOException {
		downloadMD5 = "";
		if (fileName != null && !fileName.toString().equals("")) {
			java.lang.Process process = Runtime.getRuntime().exec("md5sum "+fileName);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String[] results =  bufferedReader.readLine().split(" ");
			downloadMD5 = results[0];
		}
		return downloadMD5;
	}
	
	// create a dialog choice to allow user to reboot directly into recovery
	public static void RebootCmd() {
		TextView myMsg = new TextView(theView);
		myMsg.setText("Are you sure you want to\nreboot into recovery now?");
		myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
		new AlertDialog.Builder(theView)
	    .setTitle("Reboot into Recovery")
	    .setView(myMsg)
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
				textView.setText("Press Menu Key For Options");
	        }
	    }).show();
	}

	// Used to determine if download is complete
	public static boolean checkFileSize(String fileName) {
		try {
			File file = new File("/sdcard"+localFileName);
			fileSize = file.length() / 1024 / 1024;
			return (fileSize < Long.valueOf(theFileSize));
		} catch (Exception e) {
			return true;
		}
	}
	
	// bad file handler
	private static String badFilePath(int whichFileType){
		if (whichFileType == 0){
			textView.setText("Error selecting file!\n\nIf you have already downloaded,\npress your menu key to select\nit in file the manager.");
		} else {
			textView.setText("Error selecting file!\n\nRecovery file extension should be img\n\nIf you have already downloaded,\npress your menu key to select\nit in file the manager.");
		}
		return "bad";
	}
	
	// a function to show a custom message to the user in a toast window
	public static void showCustomToast(String str) {
		Toast toast = Toast.makeText(theView, str, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		LinearLayout toastView = (LinearLayout) toast.getView();
		ImageView customIcon = new ImageView(theView);
		customIcon.setImageResource(R.drawable.custom_update_dialog_icon);
		toastView.addView(customIcon, 0);
		toast.show();
	}
	
	public static void recoveryFinishedDialog() {
		String[] rmsg = lastRecoveryMessage.split(" ");
		if (rmsg[0].equals("flashing") && recoveryStepCount > 20) {
			TextView myMsg = new TextView(theView);
			myMsg.setText("Finished installing\n\n" + recoveryName + "\n\nReboot into recovery now?");
			myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
			new AlertDialog.Builder(theView)
			.setTitle("Recovery Flashed")
			.setIcon(R.drawable.icon_installing)
			.setView(myMsg)
			.setPositiveButton("Reboot Recovery", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					textView.setText("Rebooting into Recovery...");
					RebootCmd();
				}
			})
			.setNegativeButton("Reboot Later", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					textView.setText("Press Menu Key For Options");
					}
			}).show();
		} else {
			TextView myMsg = new TextView(theView);
			myMsg.setText("Error installing " + recoveryName + "\n\nCheck the file before trying again.");
			myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
			new AlertDialog.Builder(theView)
			.setTitle("Error Flashing Recovery!")
			.setIcon(R.drawable.md5_error)
			.setView(myMsg)
			.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					textView.setText("Press your menu key to try\ninstalling a recovery again.\n\nBe sure the file is valid!");
				}
			})
			.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					System.exit(0);
					}
			}).show();
		}
	}
	
	public static String installRecovery(final String recoveryFile) {
		String thisFileName = recoveryFile;
		if (thisFileName.substring(thisFileName.length() - 3).equalsIgnoreCase("img")) {
			TextView myMsg = new TextView(theView);
			myMsg.setText("Are you sure you want to install\n\n" + recoveryFile);
			myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
			new AlertDialog.Builder(theView)
			.setTitle("Install Recovery Now")
			.setView(myMsg)
			.setPositiveButton("Install Now", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					doInstallRecovery(recoveryFile);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// nothing to do for now
					}
			}).show();
		} else {
			badFilePath(1);
		}
		return "done";
	}

	public static String doInstallRecovery(final String recoveryFile) {
		recoveryName = recoveryFile;
		final ProgressDialog recoveryDialog = ProgressDialog.show(
				theView, "Installing Recovery", "Installing " + recoveryName + "...", true);
		recoveryDialog.setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				recoveryFinishedDialog(); // Show final reboot dialog
			}
		});
		final Handler progressHandler = new Handler() {
	        public void handleMessage(Message msg) {
	        	textView.setGravity(80);
	            textView.setText(recoveryMessage);
	        }
	    };		// create a separate thread to check the md5sum
		new Thread(new Runnable() {
			public void run() {
	            String[] str ={"su","-c","flash_image recovery " + recoveryName};
	            StringBuffer outputStr = new StringBuffer();
				String readInput;
				String readOutput;
				try {
			        java.lang.Process process = Runtime.getRuntime().exec(str);
					BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				    while ((readOutput = outputReader.readLine()) != null) {
				        outputStr.append(readOutput.toString() + "\n");
				        progressHandler.sendMessage(progressHandler.obtainMessage());
				        recoveryStepCount++;
						recoveryMessage = outputStr.toString();
						lastRecoveryMessage = readOutput.toString();
				    }
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				    while ((readInput = bufferedReader.readLine()) != null) {
				        outputStr.append(readInput.toString() + "\n");
				        progressHandler.sendMessage(progressHandler.obtainMessage());
						recoveryMessage = outputStr.toString();
						lastRecoveryMessage = readInput.toString();
				    }
				    bufferedReader.close();
				    
				    } catch (IOException e) {
				    	System.err.print(e);
				    	recoveryMessage = outputStr.toString() + " " + e.getMessage();
			    	}
				recoveryDialog.dismiss();
				return;
			}
		}).start();
		return "done";
	}

	// Handle file picker's results
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == 999 || requestCode == 1000) {
				if (data != null) {
					// obtain the filename
					try {
					Uri fileUri = data.getData();
					if (fileUri != null && !fileUri.toString().equals("")) {
						String filePath = fileUri.getPath();
						if (filePath != null) {
							Intent checkDownloadedMD5 = new Intent();
							try {
								if (requestCode == 999) {
									checkDownloadedMD5.setAction(com.Backside.BacksideUpdater.BacksideUpdaterActivity.checkMD5(filePath, true));
								} else {
									checkDownloadedMD5.setAction(com.Backside.BacksideUpdater.BacksideUpdaterActivity.installRecovery(filePath));									
								}
							} catch (IOException e) {
								checkDownloadedMD5.setAction(com.Backside.BacksideUpdater.BacksideUpdaterActivity.badFilePath(0));
							}
							sendBroadcast(checkDownloadedMD5);
						}
					}
					} catch (Exception e) {
						textView.setText("Error:\n\n"+e.getMessage());
					}
				} else {
					badFilePath(0);
				}
			}
		}
		
	}
	
	
}

