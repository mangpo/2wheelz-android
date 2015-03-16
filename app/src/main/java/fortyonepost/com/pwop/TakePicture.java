package fortyonepost.com.pwop;//Created by DimasTheDriver Mar/2011 - Part of "Android: take a picture without displaying a preview" post. Available at: http://www.41post.com/?p=3794 .

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

public class TakePicture extends Activity implements SurfaceHolder.Callback
{
	//a variable to store a reference to the Image View at the main.xml file
	private ImageView iv_image;

    //a bitmap to display the captured image
	private Bitmap bmp;
	
	//Camera variables
	//a surface holder
	private SurfaceHolder sHolder;  
	//a variable to control the camera
	private Camera mCamera;

    //Server
    private boolean snapping = false;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //get the Image View at the main.xml file
        iv_image = (ImageView) findViewById(R.id.imageView);
        
        //get the Surface View at the main.xml file
        SurfaceView sv = (SurfaceView) findViewById(R.id.surfaceView);
        
        //Get a surface
        sHolder = sv.getHolder();
        
        //add the callback interface methods defined below as the Surface View callbacks
        sHolder.addCallback(this);
        
        //tells Android that this surface will have its data constantly replaced
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        final Handler handler = new Handler();
        final Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("server", "check status 1");
                handler.post(new Runnable() {

                    @SuppressWarnings("unchecked")
                    public void run() {
                        try {
                            Log.d("server", "check status 2");
                            new CheckStatusTask().execute();
                        }
                        catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask,0,3000);
    }

	@Override
	public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3)
	{
         Log.d("debug","surfaceChanged");
		 //get camera parameters
        Parameters parameters = mCamera.getParameters();
		 
		 //set camera parameters
	     mCamera.setParameters(parameters);
	     mCamera.startPreview();
    }

	@Override
	public void surfaceCreated(SurfaceHolder holder) 
	{
        Log.d("debug","surfaceCreated");
		// The Surface has been created, acquire the camera and tell it where
        // to draw the preview.
        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.d("log", "No camera on this device");
        } else {
            Log.d("log", "Has camera.");
        }
        int cameraId = findFrontFacingCamera();
        mCamera = Camera.open(cameraId);
        try {
           mCamera.setPreviewDisplay(holder);
           
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
        }

	}

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        Log.d("numberOfCameras", Integer.toString(numberOfCameras));
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                //Log.d("front", "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
        Log.d("debug","surfaceDestroyed");
		//stop the preview
		mCamera.stopPreview();
		//release the camera
        mCamera.release();
        //unbind the camera from this object
        mCamera = null;
	}

    private int id = 0;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";
        String imageFileName = "image_" + id + "#";
        id++;
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        String mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        Log.d("mCurrentPhotoPath",mCurrentPhotoPath);
        return image;
    }

    public void writeToFile(byte[] array, String filename) {
        try {
            FileOutputStream stream = new FileOutputStream(filename);
            stream.write(array);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(String sourceFileUri) {

        String fileName = sourceFileUri;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        int serverResponseCode = 0;

        if (!sourceFile.isFile()) {

            Log.e("uploadFile", "Source File not exist :"+sourceFileUri);

        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                Log.d("uploadFile", "POST upload");
                Log.d("uploadFile", "http://kaopad.cs.berkeley.edu:5000/upload");
                URL url = new URL("http://kaopad.cs.berkeley.edu:5000/upload");
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true); // Allow Outputs


                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Starts the query
                conn.connect();

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                sourceFile.delete();

                Log.i("uploadFile", "delete file on phone");


            } catch (MalformedURLException ex) {

                ex.printStackTrace();
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                e.printStackTrace();
                Log.e("Upload file to server", "Exception : " + e.getMessage());
            }

        } // End else block
    }


    public String checkStatus(String route) throws IOException {

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        int serverResponseCode = 0;
        InputStream is = null;
        String contentAsString = "0";

        try {
            // open a URL connection to the Servlet

            Log.d("checkStatus", "GET status");
            Log.d("checkStatus", "http://kaopad.cs.berkeley.edu:5000/" + route);
            URL url = null;
            url = new URL("http://kaopad.cs.berkeley.edu:5000/" + route);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Starts the query
            conn.connect();

            // Responses from the server (code and message)
            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            Log.i("checkStatus", "HTTP Response is : "
                    + serverResponseMessage + ": " + serverResponseCode);

            is = conn.getInputStream();
            contentAsString = readIt(is, 1);
            Log.i("checkStatus", "content : "
                    + contentAsString);


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return contentAsString;

    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }


    ///////////////////////// Picture //////////////////////////

    /*private class TakeMotionPictures extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            Log.d("TakeMotionPictures", "doInBackground");
            snapping = true;
            takePictures();
            snapping = false;
            return "0";
        }

    }*/

    private void takePicture() {
        //sets what code should be executed after the picture is taken
        final Camera.PictureCallback mCall = new Camera.PictureCallback()
        {
            @Override
            public void onPictureTaken(byte[] data, Camera camera)
            {
                //decode the data obtained by the camera into a Bitmap
                bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                //set the iv_image
                iv_image.setImageBitmap(bmp);
                try {
                    File f = createImageFile();
                    Log.d("file exists", Boolean.toString(f.exists()));
                    Log.d("file write", Boolean.toString(f.canWrite()));
                    Log.d("file isFile?", Boolean.toString(f.isFile()));
                    Log.d("file getPath", f.getAbsolutePath());
                    writeToFile(data, f.getAbsolutePath());
                    new UploadTask().execute(f.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.stopPreview();
                mCamera.startPreview();
            }
        };

        mCamera.takePicture(null, null, mCall);
    }

    ////////////////////// AsyncTask ///////////////////////


    private class UploadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            Log.d("debug", "doInBackground");
            uploadFile(urls[0]);
            return "done";
        }
    }

    private class SnapOffTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            Log.d("SnapOffTask", "doInBackground");
            try {
                checkStatus("snap?off");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "0";
        }
    }

    private long  start_time;

    private class CheckStatusTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {


            // params comes from the execute() call: params[0] is the url.
            Log.d("CheckStatusTask", "doInBackground");
            try {
                return checkStatus("status");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "0";
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            if(result.equals("1")) {
                if(!snapping) {
                    Calendar calendar = Calendar.getInstance();
                    start_time = System.currentTimeMillis();
                    Log.d("start_time",Long.toString(start_time));
                    snapping = true;
                } else {
                    Calendar calendar = Calendar.getInstance();
                    long time = System.currentTimeMillis();
                    Log.d("time",Long.toString(time-start_time));
                    if(time-start_time > 30000) {
                        new SnapOffTask().execute();
                        snapping = false;
                        return;
                    }
                }
                Log.d("CheckStatusTask", "take picture!!!!!!!!!!!");
                //new TakeMotionPictures().execute();
                takePicture();
            } else {
                snapping = false;
            }
        }
    }


}