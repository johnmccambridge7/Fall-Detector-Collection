/**
 * LocationService.java
 *
 * Created by Xiaochao Yang on Sep 11, 2011 4:50:19 PM
 *
 */

package edu.dartmouth.cs.myrunscollector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.meapsoft.FFT;

public class SensorsService extends Service implements SensorEventListener {

	private static final int mFeatLen = Globals.ACCELEROMETER_BLOCK_CAPACITY + 2;

	private File mFeatureFile;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private int mServiceTaskType;
	private String mLabel;
	private Instances mDataset;
	private Attribute mClassAttribute;
	private OnSensorChangedTask mAsyncTask;
	private int sensorType;

	private static ArrayBlockingQueue<Double> mAccBuffer;
	public static final DecimalFormat mdf = new DecimalFormat("#.##");

	@Override
	public void onCreate() {
		super.onCreate();

		mAccBuffer = new ArrayBlockingQueue<Double>(
				Globals.ACCELEROMETER_BUFFER_CAPACITY);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		sensorType = intent.getIntExtra(Globals.SENSOR_TYPE_TAG, 0);
		if (sensorType == 0) {
			mAccelerometer = mSensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
		else if (sensorType == 1) {
			mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
		Log.d("lucho", "sensortype was " + sensorType);

		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_FASTEST);

		Bundle extras = intent.getExtras();
		mLabel = extras.getString(Globals.CLASS_LABEL_KEY);

		if (sensorType == 0) {
			mFeatureFile = new File(Environment.getExternalStorageDirectory(), Globals.ACCELEROMETER_FILENAME);
		}
		else if (sensorType == 1) {
			mFeatureFile = new File(Environment.getExternalStorageDirectory(), Globals.GYROSCOPE_FILENAME);
		}

		mServiceTaskType = Globals.SERVICE_TASK_TYPE_COLLECT;

		// Create the container for attributes
		ArrayList<Attribute> allAttr = new ArrayList<Attribute>();

		// Adding FFT coefficient attributes
		DecimalFormat df = new DecimalFormat("0000");

		for (int i = 0; i < Globals.ACCELEROMETER_BLOCK_CAPACITY; i++) {
			allAttr.add(new Attribute(Globals.FEAT_FFT_COEF_LABEL + df.format(i)));
		}
		// Adding the max feature
		allAttr.add(new Attribute(Globals.FEAT_MAX_LABEL));

		// Declare a nominal attribute along with its candidate values
		ArrayList<String> labelItems = new ArrayList<String>(3);
		labelItems.add(Globals.CLASS_LABEL_NEUTRAL);
		labelItems.add(Globals.CLASS_LABEL_SLIP);
		labelItems.add(Globals.CLASS_LABEL_FALL);
		mClassAttribute = new Attribute(Globals.CLASS_LABEL_KEY, labelItems);
		allAttr.add(mClassAttribute);

		// Construct the dataset with the attributes specified as allAttr and
		// capacity 10000
		mDataset = new Instances(Globals.FEAT_SET_NAME, allAttr, Globals.FEATURE_SET_CAPACITY);

		// Set the last column/attribute (standing/walking/running) as the class
		// index for classification
		mDataset.setClassIndex(mDataset.numAttributes() - 1);

		Intent i = new Intent(this, CollectorActivity.class);
		// Read:
		// http://developer.android.com/guide/topics/manifest/activity-element.html#lmode
		// IMPORTANT!. no re-create activity
		i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

		Notification notification = new Notification.Builder(this)
				.setContentTitle(
						getApplicationContext().getString(
								R.string.ui_sensor_service_notification_title))
				.setContentText(
						getResources()
								.getString(
										R.string.ui_sensor_service_notification_content))
				.setSmallIcon(R.drawable.greend).setContentIntent(pi).build();
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification.flags = notification.flags
				| Notification.FLAG_ONGOING_EVENT;
		notificationManager.notify(0, notification);


		mAsyncTask = new OnSensorChangedTask();
		mAsyncTask.execute();

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		mAsyncTask.cancel(true);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mSensorManager.unregisterListener(this);
		Log.i("","");
		super.onDestroy();

	}

	private class OnSensorChangedTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {

			Instance inst = new DenseInstance(mFeatLen);
			inst.setDataset(mDataset);
			int blockSize = 0;
			FFT fft = new FFT(Globals.ACCELEROMETER_BLOCK_CAPACITY);
			double[] accBlock = new double[Globals.ACCELEROMETER_BLOCK_CAPACITY];
			double[] re = accBlock;
			double[] im = new double[Globals.ACCELEROMETER_BLOCK_CAPACITY];

			double max = Double.MIN_VALUE;

			while (true) {
				try {
					// need to check if the AsyncTask is cancelled or not in the while loop
					if (isCancelled () == true)
					{
						return null;
					}

					// Dumping buffer
					accBlock[blockSize++] = mAccBuffer.take().doubleValue();

					if (blockSize == Globals.ACCELEROMETER_BLOCK_CAPACITY) {
						blockSize = 0;

						// time = System.currentTimeMillis();
						max = .0;
						for (double val : accBlock) {
							if (max < val) {
								max = val;
							}
						}

						fft.fft(re, im);

						for (int i = 0; i < re.length; i++) {
							double mag = Math.sqrt(re[i] * re[i] + im[i]
									* im[i]);
							inst.setValue(i, mag);
							im[i] = .0; // Clear the field
						}

						// Append max after frequency component
						inst.setValue(Globals.ACCELEROMETER_BLOCK_CAPACITY, max);
						inst.setValue(mClassAttribute, mLabel);
						mDataset.add(inst);
						Log.i("new instance", mDataset.size() + "");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		protected void onCancelled() {

			Log.e("123", mDataset.size()+"");

			if (mServiceTaskType == Globals.SERVICE_TASK_TYPE_CLASSIFY) {
				super.onCancelled();
				return;
			}
			Log.i("in the loop","still in the loop cancelled");
			String toastDisp;

			if (mFeatureFile.exists()) {

				// merge existing and delete the old dataset
				DataSource source;
				try {
					// Create a datasource from mFeatureFile where
					// mFeatureFile = new File(getExternalFilesDir(null),
					// "features.arff");
					source = new DataSource(new FileInputStream(mFeatureFile));
					// Read the dataset set out of this datasource
					Instances oldDataset = source.getDataSet();
					oldDataset.setClassIndex(mDataset.numAttributes() - 1);
					// Sanity checking if the dataset format matches.
					if (!oldDataset.equalHeaders(mDataset)) {
						// Log.d(Globals.TAG,
						// oldDataset.equalHeadersMsg(mDataset));
						throw new Exception(
								"The two datasets have different headers:\n");
					}

					Log.d("lucho", "size is " + oldDataset.size() + " and new ds is " + mDataset.size());

					// Move all items over manually
					for (int i = 0; i < mDataset.size(); i++) {
						oldDataset.add(mDataset.get(i));
					}

					mDataset = oldDataset;
					// Delete the existing old file.
					mFeatureFile.delete();
					Log.i("delete","delete the file");
				} catch (Exception e) {
					e.printStackTrace();
				}
				toastDisp = getString(R.string.ui_sensor_service_toast_success_file_updated);

			} else {
				toastDisp = getString(R.string.ui_sensor_service_toast_success_file_created)   ;
			}
			Log.i("save","create saver here");
			// create new Arff file
			ArffSaver saver = new ArffSaver();
			// Set the data source of the file content
			saver.setInstances(mDataset);
			Log.e("1234", mDataset.size()+"");
			try {
				// Set the destination of the file.
				// mFeatureFile = new File(getExternalFilesDir(null),
				// "features.arff");
				saver.setFile(mFeatureFile);
				// Write into the file
				saver.writeBatch();
				Log.i("batch","write batch here");
				Toast.makeText(getApplicationContext(), toastDisp,
						Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				toastDisp = getString(R.string.ui_sensor_service_toast_error_file_saving_failed);
				e.printStackTrace();
			}

			Log.i("toast","toast here");
			super.onCancelled();
		}

	}

	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER || event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

//			Log.d("lucho", "Values are " + event.values[0] + " " + event.values[1] + " " +
//					event.values[2]);

			double m = Math.sqrt(event.values[0] * event.values[0]
					+ event.values[1] * event.values[1] + event.values[2]
					* event.values[2]);

			// Inserts the specified element into this queue if it is possible
			// to do so immediately without violating capacity restrictions,
			// returning true upon success and throwing an IllegalStateException
			// if no space is currently available. When using a
			// capacity-restricted queue, it is generally preferable to use
			// offer.

			try {
				mAccBuffer.add(new Double(m));
			} catch (IllegalStateException e) {

				// Exception happens when reach the capacity.
				// Doubling the buffer. ListBlockingQueue has no such issue,
				// But generally has worse performance
				ArrayBlockingQueue<Double> newBuf = new ArrayBlockingQueue<Double>(
						mAccBuffer.size() * 2);

				mAccBuffer.drainTo(newBuf);
				mAccBuffer = newBuf;
				mAccBuffer.add(new Double(m));
			}
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}