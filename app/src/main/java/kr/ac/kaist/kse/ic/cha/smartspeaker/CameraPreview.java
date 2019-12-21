package kr.ac.kaist.kse.ic.cha.smartspeaker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer;

import static kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity.isSaved;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity.isSetting;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity.TakingPicture;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity.thresholdForDetection;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.esm.ESMService.ACTION_FORCE_TO_PLAY_ESM;

//Camera preview class showing preview surface taken from the camera
class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {

    private static Bitmap preBitmap = null;
    private static Bitmap bitmap = null;
    private static Bitmap diffBitmap = null;
    private static Bitmap roomBitmap = null;
    private static Bitmap saveBitmap = null;
    private static int settingDiff = 0;


    private static boolean textUpdate = false;

    //You can change pixel-level difference threshold here
    private static final int threashold = 64;

    private final String TAG = "CameraPreview";

    private final static BitmapFactory.Options options = new BitmapFactory.Options();

    static {
        options.inSampleSize = 1;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    int i = 0;
    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {

        }
    };
    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };
    private int mCameraID;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private int mDisplayOrientation;
    //ref : http://stackoverflow.com/q/37135675
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

            //width and height of an image
            int w = camera.getParameters().getPictureSize().width;
            int h = camera.getParameters().getPictureSize().height;
            int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);


            //convert byte array to bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);


            //Rotate image toward device
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            matrix.postRotate(180);

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);


            //convert byte array to bitmap
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] currentData = stream.toByteArray();



        }
    };
    private List<Size> mSupportedPreviewSizes;
    private Size mPreviewSize;
    private boolean isPreview = false;
    private AppCompatActivity mActivity;


    public CameraPreview(Context context, AppCompatActivity activity, int cameraID, SurfaceView surfaceView) {
        super(context);


        Log.d("@@@", "Preview");


        mActivity = activity;
        mCameraID = cameraID;
        mSurfaceView = surfaceView;


        mSurfaceView.setVisibility(View.VISIBLE);


        // SurfaceHolder.Callback detect creating or releasing surface
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

    }

    /**
     *
     * Calculate to show the camera preview on the screen for the orientation of Android device.
     */
    public static int calculatePreviewOrientation(Camera.CameraInfo info, int rotation) {
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    // When the surface is created, set where to preview on the screen.
    public void surfaceCreated(SurfaceHolder holder) {

        // Open an instance of the camera
        try {
            mCamera = Camera.open(mCameraID); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "Camera " + mCameraID + " is not available: " + e.getMessage());
        }


        // retrieve camera's info.
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, cameraInfo);

        mCameraInfo = cameraInfo;
        mDisplayOrientation = mActivity.getWindowManager().getDefaultDisplay().getRotation();

        int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);
        mCamera.setDisplayOrientation(orientation);

        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        requestLayout();

        // get Camera parameters
        Camera.Parameters params = mCamera.getParameters();

        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            // set the focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            // set Camera parameters
            mCamera.setParameters(params);
        }


        try {

            mCamera.setPreviewDisplay(holder);
            // Important: Call startPreview() to start updating the preview
            // surface. Preview must be started before you can take a picture.
            mCamera.startPreview();
            isPreview = true;
            Log.d(TAG, "Camera preview started.");
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Release the camera for other applications.
        if (mCamera != null) {
            if (isPreview)
                mCamera.stopPreview();
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            isPreview = false;
        }

    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        // Not to generate camera shutter sounds when taking pictures,
            // preview images when surface is changed are saved and used for real-time use

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.d(TAG, "Preview surface does not exist");
            return;
        }


        // stop preview before making changes
        try {
            mCamera.stopPreview();
            Log.d(TAG, "Preview stopped.");
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }


        int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);
        mCamera.setDisplayOrientation(orientation);


        Camera.Parameters param = mCamera.getParameters();

        param.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        mCamera.setParameters(param);


        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Camera.Parameters parameters = camera.getParameters();
                    int w = parameters.getPreviewSize().width;
                    int h = parameters.getPreviewSize().height;
                    int format = parameters.getPreviewFormat();

                    YuvImage image = new YuvImage(data, format, w, h, null);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Rect area = new Rect(0, 0, w, h);
                    image.compressToJpeg(area, 100, out);
                    byte[] currentData = out.toByteArray();

                    int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    matrix.postRotate(180);


                    bitmap = BitmapFactory.decodeByteArray(currentData, 0, currentData.length, options);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);

                    // To take a picture of a room where this app is installed as a basement picture.
                    // When clicking "picture" button in the main page, a picture is taken once.
                    if (TakingPicture) {
                        Update("picture taken");
                        Log.i("test","picture");
                        roomBitmap=bitmap;
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        roomBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        currentData = stream.toByteArray();
                        new SaveImageTask().execute(currentData);
                        TakingPicture = false;
                    }


                    //To change real time preview images into gray scale
                    bitmap = toGrayscale(bitmap);
                    //To add gaussian blur effect on real time preview images
                    bitmap = GaussianBlur.with(getContext()).render(bitmap);
                    bitmap = GaussianBlur.with(getContext()).render(bitmap);


                    //To detect movement duringa the operation time
                    //If it detects movement, a pixel-difference image is saved
                    if (ESMPlayer.isAfter15Mins() && !ESMPlayer.isPlaying() && ESMPlayer.isPlayingTime()) {
                        if (preBitmap != null && (diffBitmap = findDifference(bitmap, preBitmap)) != null) {
                            Intent intent = new Intent(ACTION_FORCE_TO_PLAY_ESM);
                            getContext().sendBroadcast(intent);
                            Log.i("picture","흠");
                            // Save different-pixel iamges
                            if(!isSaved) {
                                Log.i("picture","냐");
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                diffBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                currentData = stream.toByteArray();
                                new SaveImageTask().execute(currentData);

                                isSaved = true;
                            }
                        }
                    }



                    // For checking the number of different pixels for changing the photo-level threshold
                    // Only used at setting at the beginning

                    if (isSetting) {
                        if (preBitmap != null) {
                            settingDiff=findDifference2(bitmap, preBitmap);
                            Update(""+settingDiff);
                        }



                    }

                    preBitmap = bitmap;
                    data = null;
                    currentData = null;

                }
            });
            mCamera.startPreview();
            Log.d(TAG, "Camera preview started.");
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }


    }
    public void Update(String s){
        MainActivity.recording.setText(s);
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }


    private Bitmap findDifference(final Bitmap firstImage, final Bitmap secondImage) {
        if (firstImage.getHeight() != secondImage.getHeight() || firstImage.getWidth() != secondImage.getWidth())
            Log.i("test", "모");
        int diff = 0;
        Bitmap bitmap_new = secondImage.copy(secondImage.getConfig(), true);

        for (int i = 0; i < firstImage.getWidth(); i += 1) {
            for (int j = 0; j < firstImage.getHeight(); j += 1) {
                int pixel = firstImage.getPixel(i, j);
                int redValue = Color.red(pixel);
                int blueValue = Color.blue(pixel);
                int greenValue = Color.green(pixel);
                int grayValue = (redValue + greenValue + blueValue) / 3;

                int pixel2 = secondImage.getPixel(i, j);
                int redValue2 = Color.red(pixel2);
                int blueValue2 = Color.blue(pixel2);
                int greenValue2 = Color.green(pixel2);
                int grayValue2 = (redValue2 + greenValue2 + blueValue2) / 3;

                if (Math.abs(grayValue2 - grayValue) > threashold) {
                    bitmap_new.setPixel(i, j, Color.BLACK);
                    diff++;
                }else{
                    bitmap_new.setPixel(i, j, Color.WHITE);
                }
            }
        }


//        return diff > BroadcastThreashold ? bitmap_new : null;
        return diff > thresholdForDetection ? bitmap_new : null;
    }

    private int findDifference2(final Bitmap firstImage, final Bitmap secondImage) {
        if (firstImage.getHeight() != secondImage.getHeight() || firstImage.getWidth() != secondImage.getWidth())
            Log.i("test", "모");
//        Bitmap bitmap_new = secondImage.copy(secondImage.getConfig(), true);
        int diff = 0;
        for (int i = 0; i < firstImage.getWidth(); i += 1) {
            for (int j = 0; j < firstImage.getHeight(); j += 1) {
                int pixel = firstImage.getPixel(i, j);
                int redValue = Color.red(pixel);
                int blueValue = Color.blue(pixel);
                int greenValue = Color.green(pixel);
                int grayValue = (redValue + greenValue + blueValue) / 3;

                int pixel2 = secondImage.getPixel(i, j);
                int redValue2 = Color.red(pixel2);
                int blueValue2 = Color.blue(pixel2);
                int greenValue2 = Color.green(pixel2);
                int grayValue2 = (redValue2 + greenValue2 + blueValue2) / 3;

                if (Math.abs(grayValue2 - grayValue) > threashold) {
                    diff++;
                }else{

                }
            }
        }


//        return diff > BroadcastThreashold ? bitmap_new : null;
        return diff;
    }


    public void takePicture() {

        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;


            try {

                File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/camtest");
                if (!path.exists()) {
                    path.mkdirs();
                }
                String fileName = String.format("%d.jpg", System.currentTimeMillis());

                File outputFile = new File(path, fileName);

                outStream = new FileOutputStream(outputFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to "
                        + outputFile.getAbsolutePath());


                mCamera.startPreview();


                // 갤러리에 반영
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(outputFile));
                getContext().sendBroadcast(mediaScanIntent);


                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();
                    Log.d(TAG, "Camera preview started.");
                } catch (Exception e) {
                    Log.d(TAG, "Error starting camera preview: " + e.getMessage());
                }


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            data = null;

            return null;
        }

    }


}