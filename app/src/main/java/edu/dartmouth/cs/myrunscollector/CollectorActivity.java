package edu.dartmouth.cs.myrunscollector;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;

import java.io.File;

public class CollectorActivity extends AppCompatActivity {

	private enum State {
		IDLE, COLLECTING, TRAINING, CLASSIFYING
	};

	private final String[] mLabels = { Globals.CLASS_LABEL_NEUTRAL, Globals.CLASS_LABEL_SLIP,
			Globals.CLASS_LABEL_FALL };

	private RadioGroup radioGroup;
	private final RadioButton[] radioBtns = new RadioButton[3];
	private RadioGroup sensorTypeGroup;
	private RadioButton accelorometerButton;
	private RadioButton gyroButton;
	private Intent mServiceIntent;
	private File mAccelerometerFile;
	private File mGyroFile;

	private State mState;
	private Button btnDelete;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		checkPermissions(this);
		radioGroup = (RadioGroup) findViewById(R.id.radioGroupLabels);
		sensorTypeGroup = (RadioGroup) findViewById(R.id.sensorTypeGroup);
		radioBtns[0] = (RadioButton) findViewById(R.id.radioneutral);
		radioBtns[1] = (RadioButton) findViewById(R.id.radioslip);
		radioBtns[2] = (RadioButton) findViewById(R.id.radiofall);
		accelorometerButton = findViewById(R.id.radioaccelerometer);
		accelorometerButton.toggle();
		gyroButton = findViewById(R.id.radiogyro);

		btnDelete = (Button) findViewById(R.id.btnDeleteData);

		mState = State.IDLE;
		mAccelerometerFile = new File(Environment.getExternalStorageDirectory(), Globals.ACCELEROMETER_FILENAME);
		mGyroFile = new File(Environment.getExternalStorageDirectory(), Globals.GYROSCOPE_FILENAME);
		mServiceIntent = new Intent(this, SensorsService.class);
	}

	public void onCollectClicked(View view) {

		if (mState == State.IDLE) {
			mState = State.COLLECTING;
			((Button) view).setText(R.string.ui_collector_button_stop_title);
			btnDelete.setEnabled(false);
			radioBtns[0].setEnabled(false);
			radioBtns[1].setEnabled(false);
			radioBtns[2].setEnabled(false);
			accelorometerButton.setEnabled(false);
			gyroButton.setEnabled(false);

			int acvitivtyId = radioGroup.indexOfChild(findViewById(radioGroup
					.getCheckedRadioButtonId()));
			Log.d("lucho", "act id is " + acvitivtyId);
			String label = mLabels[acvitivtyId];

			Bundle extras = new Bundle();
			extras.putString(Globals.CLASS_LABEL_KEY, label);
			int sensorType = sensorTypeGroup.indexOfChild(findViewById(sensorTypeGroup
					.getCheckedRadioButtonId()));
			if (sensorType == -1) sensorType = 0;
			extras.putInt(Globals.SENSOR_TYPE_TAG, sensorType);

			mServiceIntent.putExtras(extras);

			startService(mServiceIntent);

		} else if (mState == State.COLLECTING) {
			mState = State.IDLE;
			((Button) view).setText(R.string.ui_collector_button_start_title);
			btnDelete.setEnabled(true);
			radioBtns[0].setEnabled(true);
			radioBtns[1].setEnabled(true);
			radioBtns[2].setEnabled(true);
			accelorometerButton.setEnabled(true);
			gyroButton.setEnabled(true);

			stopService(mServiceIntent);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
		}
	}

	public void onDeleteDataClicked(View view) {

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			if (mAccelerometerFile.exists()) {
				mAccelerometerFile.delete();
			}
			if (mGyroFile.exists()) {
				mGyroFile.delete();
			}

			Toast.makeText(getApplicationContext(),
					R.string.ui_collector_toast_file_deleted,
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onBackPressed() {

		if (mState == State.TRAINING) {
			return;
		} else if (mState == State.COLLECTING || mState == State.CLASSIFYING) {
			stopService(mServiceIntent);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
					.cancel(Globals.NOTIFICATION_ID);
		}
		super.onBackPressed();
	}

	@Override
	public void onDestroy() {
		// Stop the service and the notification.
		// Need to check whether the mSensorService is null or not.
		if (mState == State.TRAINING) {
			return;
		} else if (mState == State.COLLECTING || mState == State.CLASSIFYING) {
			stopService(mServiceIntent);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
					.cancelAll();
		}
		finish();
		super.onDestroy();
	}

	public static void  checkPermissions(Activity activity){
		if(Build.VERSION.SDK_INT < 23)
			return;
		if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(activity, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
		}
	}
}