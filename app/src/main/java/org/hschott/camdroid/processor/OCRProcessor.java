package org.hschott.camdroid.processor;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hschott.camdroid.ConfigurationFragment;
import org.hschott.camdroid.OnCameraPreviewListener;
import org.hschott.camdroid.R;
import org.hschott.camdroid.util.StorageUtils;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;
import com.googlecode.tesseract.android.TessBaseAPI.PageSegMode;

public class OCRProcessor extends AbstractOpenCVFrameProcessor {

    static final String LOG = OCRProcessor.class.getSimpleName();

    private static int min = 16;
    private static int max = 255;
    private static int blocksize = 21;
    private static int reduction = 32;


    public OCRProcessor(OnCameraPreviewListener.FrameDrawer drawer) {
        super(drawer);
    }

    @Override
    public Fragment getConfigUiFragment(Context context) {
        return Fragment.instantiate(context, OCRUIFragment.class.getName());
    }

    public static class OCRUIFragment extends
            ConfigurationFragment {

        @Override
        public int getLayoutId() {
            return R.layout.ocr_ui;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = super
                    .onCreateView(inflater, container, savedInstanceState);

            SeekBar minSeekBar = (SeekBar) v.findViewById(R.id.min);
            minSeekBar.setMax(255);
            minSeekBar.setProgress(min);

            minSeekBar
                    .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                min = progress;
                                OCRUIFragment.this
                                        .showValue(min);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            SeekBar maxSeekBar = (SeekBar) v.findViewById(R.id.max);
            maxSeekBar.setMax(255);
            maxSeekBar.setProgress(max);

            maxSeekBar
                    .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                max = progress;
                                OCRUIFragment.this
                                        .showValue(max);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            SeekBar blocksizeSeekBar = (SeekBar) v.findViewById(R.id.blocksize);
            blocksizeSeekBar.setMax(32);
            blocksizeSeekBar.setProgress(blocksize);

            blocksizeSeekBar
                    .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                if (progress % 2 == 0) {
                                    blocksize = progress + 3;
                                } else {
                                    blocksize = progress + 2;
                                }
                                OCRUIFragment.this
                                        .showValue(blocksize + "px");
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            SeekBar reductionSeekBar = (SeekBar) v.findViewById(R.id.reduction);
            reductionSeekBar.setMax(64);
            reductionSeekBar.setProgress(reduction + 1);

            reductionSeekBar
                    .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                reduction = progress - 1;
                                OCRUIFragment.this
                                        .showValue(reduction);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            return v;
        }
    }

    @Override
    public FrameWorker createFrameWorker() {
        return new OCRFrameWorker(drawer);
    }

    public static class OCRFrameWorker extends AbstractOpenCVFrameWorker {

        private static final int SIMPLETEXT_MIN_SCORE = 60;

        private TessBaseAPI tessBaseAPI;

        private Paint paint;
        private Rect bounds;
        int lines;
        private String simpleText = new String();

        public OCRFrameWorker(OnCameraPreviewListener.FrameDrawer drawer) {
            super(drawer);

            this.tessBaseAPI = new TessBaseAPI();
            this.tessBaseAPI.setPageSegMode(PageSegMode.PSM_AUTO_OSD);

            this.tessBaseAPI.setVariable(TessBaseAPI.VAR_ACCURACYVSPEED,
                    String.valueOf(50));
            this.tessBaseAPI.init(Environment.getExternalStorageDirectory().getPath(), "eng");

            paint = new Paint();
            paint.setColor(Color.RED);
            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 11.0, drawer.getDisplayMetrics());
            paint.setTextSize(size);

            bounds = new Rect();
            paint.getTextBounds("Q", 0, 1, bounds);

            lines = drawer.getDisplayMetrics().heightPixels / bounds.height();

        }

        @Override
        protected void draw() {
            Utils.matToBitmap(out, this.bmp);
            Canvas canvas = new Canvas(this.bmp);

            int y = bounds.height();
            int c = 1;
            for (String line : simpleText.split("\n")) {
                canvas.drawText(line, bounds.width(), y, paint);
                y = y + bounds.height();
                if (c >= lines)
                    break;;
            }

            this.drawer.drawBitmap(this.bmp);

        }

        protected void execute() {
            out = gray();

            Imgproc.equalizeHist(out, out);
            Core.normalize(out, out, min, max, Core.NORM_MINMAX);

            Imgproc.adaptiveThreshold(out, out, 255, Imgproc.THRESH_BINARY,
                    Imgproc.ADAPTIVE_THRESH_MEAN_C, blocksize, reduction);

            byte[] data = new byte[(int) out.total()];
            out.get(0, 0, data);

            this.tessBaseAPI.setImage(data, out.width(), out.height(),
                    out.channels(), (int) out.step1());

            String utf8Text = this.tessBaseAPI.getUTF8Text();
            int score = this.tessBaseAPI.meanConfidence();
            this.tessBaseAPI.clear();


            if (score >= SIMPLETEXT_MIN_SCORE && utf8Text.length() > 0) {
                simpleText = utf8Text;
            } else {
                simpleText = new String();
            }
        }

    }

}
