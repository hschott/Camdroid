package org.hschott.camdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Toast;

import org.hschott.camdroid.processor.FrameProcessor;
import org.hschott.camdroid.processor.FrameProcessors;
import org.hschott.camdroid.util.StorageUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends ActionBarActivity {
    private CameraPreviewView mPreview;
    private ProcessFramesView mProcessorView;
    private Fragment mConfigUIFragment;

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

        this.mProcessorView.setFrameProcessor(FrameProcessors.PassThrough
                .newFrameProcessor(this.mProcessorView));
        this.getSupportActionBar().setSubtitle(
                FrameProcessors.PassThrough.name());

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Catch screen off event
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        this.registerReceiver(this.screenOffReceiver, f);

        mSystemUIHandler = new Handler();

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(systemUiVisibilityChangeListener);

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
                        FrameProcessor frameProcessor = CameraActivity.this.mProcessorView
                                .getFrameProcessor();

                        if (frameProcessor != null) {
                            CameraActivity.this.mConfigUIFragment = frameProcessor
                                    .getConfigUiFragment();
                            FragmentManager fm = CameraActivity.this
                                    .getSupportFragmentManager();
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace(R.id.configcontainer,
                                    CameraActivity.this.mConfigUIFragment, null);
                            ft.show(CameraActivity.this.mConfigUIFragment);
                            ft.commit();
                        }

                        return true;
                    }
                });
        MenuItemCompat
                .setShowAsAction(i1, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

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
        MenuItemCompat
                .setShowAsAction(i2, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        for (final FrameProcessors f : FrameProcessors.values()) {
            menu.add(Menu.NONE, f.ordinal(), Menu.NONE, f.name())
                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            hideSystemUI();
                            if (CameraActivity.this.mConfigUIFragment != null) {
                                ((UIFragment) CameraActivity.this.mConfigUIFragment)
                                        .remove();
                            }
                            CameraActivity.this.setFrameProcessor(f.ordinal());
                            return true;
                        }
                    });
        }
        return true;
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
        this.getSupportActionBar().setSubtitle(null);
        FrameProcessors t = FrameProcessors.values()[ordinal];
        this.mPreview.stopPreview();
        this.mProcessorView.setFrameProcessor(t
                .newFrameProcessor(this.mProcessorView));
        this.mPreview.startPreview();
        this.getSupportActionBar().setSubtitle(t.name());
    }

    public void takePicture() {
        Date date = new Date();
        final String formated = CameraActivity.this.sdf.format(date);

        File file = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), formated + "_" + "PROCESSED" + ".jpg");

        this.mProcessorView.takePicture(file);
        StorageUtils.updateMedia(CameraActivity.this.getApplicationContext(),
                file);

        Camera.PictureCallback callback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File file = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), formated + "_" + "CAPTURE" + ".jpg");
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
