package com.trinaldi.opencvtutorial;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements OnTouchListener, CvCameraViewListener2 {

    private CameraBridgeViewBase mCamera;
    private Mat mRgba;
    private Scalar mColorHsv;
    private Scalar mColorRgba;

    TextView text_coordinates;
    TextView text_color;

    double x = -1;
    double y = -1;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mCamera.enableView();
                    mCamera.setOnTouchListener(MainActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        text_coordinates = (TextView) findViewById(R.id.text_coordinates);
        text_color = (TextView) findViewById(R.id.text_color);

        mCamera = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        mCamera.setVisibility(SurfaceView.VISIBLE);
        mCamera.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null)
            mCamera.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCamera != null)
            mCamera.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mColorRgba = new Scalar(255);
        mColorHsv = new Scalar(255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        return mRgba;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        double yLow = (double) mCamera.getHeight() * 0.2401961;
        double yHigh = (double) mCamera.getHeight() * 0.7696078;

        double xScale = (double)cols / (double) mCamera.getWidth();
        double yScale = (double)rows / (yHigh - yLow);

        x = event.getX();
        y = event.getY();

        y = y - yLow;

        x = x * xScale;
        y = y * yScale;

        if((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        text_coordinates.setText("X: " + Double.valueOf(x) + ", Y: " + Double.valueOf(y));

        Rect touchedRect = new Rect();

        touchedRect.x = (int)x;
        touchedRect.y = (int)y;

        touchedRect.width = 8;
        touchedRect.height = 8;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        mColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mColorHsv.val.length; i++)
            mColorHsv.val[i] /= pointCount;

        mColorRgba = convertScalarHsv2Rgba(mColorHsv);

        text_color.setText("Color: #" + String.format("%02X", (int) mColorRgba.val[0])
                + String.format("%02X", (int) mColorRgba.val[1])
                + String.format("%02X", (int) mColorRgba.val[2]));

        text_color.setTextColor(Color.rgb((int) mColorRgba.val[0],
                (int) mColorRgba.val[1],
                (int) mColorRgba.val[2]));

        return false;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}