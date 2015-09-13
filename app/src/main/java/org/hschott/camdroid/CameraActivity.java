package org.hschott.camdroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Fragment;
import android.app.FragmentManager;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.hschott.camdroid.util.StorageUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends Activity {
    private CameraPreviewView mPreview;
    private ProcessFramesView mProcessorView;

    private SimpleDateFormat sdf = new SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss-SS", Locale.US);

    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                CameraActivity.this.finish();
            }
        }
    };
    private Handler mSystemUIHandler;

    private Runnable systemUIHideRunner = new Runnable() {
        @Override
        public void run() {
            hideSystemUI();
        }
    };

    private View.OnSystemUiVisibilityChangeListener systemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                mSystemUIHandler.postDelayed(systemUIHideRunner, 6000);
            } else {
                mSystemUIHandler.removeCallbacks(systemUIHideRunner);
            }

        }
    };

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        this.setContentView(R.layout.camera);

        this.mPreview = (CameraPreviewView) this
                .findViewById(R.id.camera_view);

        this.mProcessorView = (ProcessFramesView) this
                .findViewById(R.id.processor_view);

        this.mProcessorView.setFrameProcessor(FrameProcessors.ColorSpace
                .newFrameProcessor(this.mProcessorView));
        this.getActionBar().setSubtitle(
                FrameProcessors.ColorSpace.name());

        final GestureDetector gesture = new GestureDetector(getApplicationContext(),
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                           float velocityY) {
                        final int SWIPE_MIN_DISTANCE = 120;
                        final int SWIPE_MAX_OFF_PATH = 250;
                        final int SWIPE_THRESHOLD_VELOCITY = 200;
                        try {
                            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                                return false;
                            }
                            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                                hideConfigurationUI();
                                return true;
                            }
                            if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                                showConfigurationUI();
                                return true;
                            }
                        } catch (Exception e) {
                            // nothing
                        }
                        return super.onFling(e1, e2, velocityX, velocityY);
                    }
                });

        View configUi = this.findViewById(R.id.configcontainer);
        configUi.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gesture.onTouchEvent(event);
            }
        });

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Catch screen off event
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        this.registerReceiver(this.screenOffReceiver, f);

        mSystemUIHandler = new Handler();

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(systemUiVisibilityChangeListener);

        final int[] TESSERACT_LANG_FROM_ID = {R.raw.eng};
        final String[] TESSERACT_LANG_TO_PATH = {"eng.traineddata"};

        StorageUtils.exportRaw(getApplicationContext(), new File(Environment.getExternalStorageDirectory(), "tessdata"),
                TESSERACT_LANG_FROM_ID, TESSERACT_LANG_TO_PATH);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem i1 = menu
                .add(Menu.NONE, Menu.NONE, Menu.NONE,
                        R.string.configure_processor)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        hideSystemUI();
                        if (hasConfigurationUI()) {
                            hideConfigurationUI();
                        } else {
                            showConfigurationUI();
                        }

                        return true;
                    }
                });
        i1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem i2 = menu
                .add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.take_picture)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        hideSystemUI();
                        CameraActivity.this.takePicture();
                        return true;
                    }
                });
        i2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        for (final FrameProcessors f : FrameProcessors.values()) {
            menu.add(Menu.NONE, f.ordinal(), Menu.NONE, f.name())
                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            hideSystemUI();
                            if (hasConfigurationUI()) {
                                hideConfigurationUI();
                            }
                            CameraActivity.this.setFrameProcessor(f.ordinal());
                            CameraActivity.this.showConfigurationUI();
                            return true;
                        }
                    });
        }
        return true;
    }

    protected boolean hasConfigurationUI() {
        return getFragmentManager().findFragmentById(R.id.configcontainer) != null;
    }

    protected void showConfigurationUI() {
        if (!hasConfigurationUI()) {
            FrameProcessor frameProcessor = this.mProcessorView
                    .getFrameProcessor();

            if (frameProcessor != null) {
                Fragment fragment = frameProcessor
                        .getConfigUiFragment(getApplicationContext());

                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                        .add(R.id.configcontainer, fragment)
                        .commit();
            }
        }
    }

    protected void hideConfigurationUI() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.configcontainer);

        if (fragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                    .remove(fragment).commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.screenOffReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mPreview.stopPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.mPreview.startPreview();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mPreview.createCamera();

        this.mPreview.addCameraFrameListener(this.mProcessorView);

        Toast.makeText(this.getApplicationContext(), R.string.select,
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mPreview.releaseCamera();

        this.mPreview.removeCameraFrameListener(this.mProcessorView);
    }

    protected void setFrameProcessor(int ordinal) {
        this.getActionBar().setSubtitle(null);
        FrameProcessors t = FrameProcessors.values()[ordinal];
        this.mPreview.stopPreview();
        this.mProcessorView.setFrameProcessor(t
                .newFrameProcessor(this.mProcessorView));
        this.mPreview.startPreview();
        this.getActionBar().setSubtitle(t.name());
    }

    public void takePicture() {
        Date date = new Date();
        final String formated = CameraActivity.this.sdf.format(date);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), formated + "_" + "PROCESSED" + ".jpg");

        this.mProcessorView.takePicture(file);
        StorageUtils.updateMedia(CameraActivity.this.getApplicationContext(),
                file);

        Camera.PictureCallback callback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), formated + "_" + "CAPTURE" + ".jpg");
                StorageUtils.writeFile(file, data);
                StorageUtils.updateMedia(
                        CameraActivity.this.getApplicationContext(), file);

                Toast.makeText(CameraActivity.this.getApplicationContext(),
                        R.string.picture_saved, Toast.LENGTH_SHORT).show();

                mSystemUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraActivity.this.mPreview.stopPreview();
                        CameraActivity.this.mPreview.startPreview();
                    }
                });
            }
        };

        this.mPreview.takePicture(callback);

    }
}
