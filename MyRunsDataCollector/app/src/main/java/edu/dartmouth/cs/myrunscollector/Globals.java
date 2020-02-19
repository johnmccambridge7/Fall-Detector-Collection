/**
 * Globals.java
 * 
 * Created by Xiaochao Yang on Dec 9, 2011 1:43:35 PM
 * 
 */

package edu.dartmouth.cs.myrunscollector;


// More on class on constants:
// http://www.javapractices.com/topic/TopicAction.do?Id=2

public abstract class Globals {

	// Debugging tag
	public static final String TAG = "MyRuns";
	
	
	public static final int ACCELEROMETER_BUFFER_CAPACITY = 2048;
	public static final int ACCELEROMETER_BLOCK_CAPACITY = 64;
	
	public static final int ACTIVITY_ID_STANDING = 0;
	public static final int ACTIVITY_ID_WALKING = 1;
	public static final int ACTIVITY_ID_RUNNING = 2;
	public static final int ACTIVITY_ID_OTHER = 2;
	
	public static final int SERVICE_TASK_TYPE_COLLECT = 0;
	public static final int SERVICE_TASK_TYPE_CLASSIFY = 1;
	
	public static final String ACTION_MOTION_UPDATED = "MYRUNS_MOTION_UPDATED";
	
	public static final String CLASS_LABEL_KEY = "label";	
	public static final String CLASS_LABEL_STANDING = "standing";
	public static final String CLASS_LABEL_WALKING = "walking";
	public static final String CLASS_LABEL_RUNNING = "running";
	public static final String CLASS_LABEL_OTHER = "others";
	
	public static final String FEAT_FFT_COEF_LABEL = "fft_coef_";
	public static final String FEAT_MAX_LABEL = "max";
	public static final String FEAT_SET_NAME = "accelerometer_features";

	public static final String FEATURE_FILE_NAME = "features.arff";
	public static final String RAW_DATA_NAME = "raw_data.txt";
	public static final int FEATURE_SET_CAPACITY = 10000;
	
	public static final int NOTIFICATION_ID = 1;
	
	
  
}
