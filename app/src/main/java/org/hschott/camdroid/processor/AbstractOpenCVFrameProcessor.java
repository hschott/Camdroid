package org.hschott.camdroid.processor;

import org.hschott.camdroid.FrameProcessor;
import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractOpenCVFrameProcessor implements FrameProcessor {

    protected FrameDrawer drawer;
    private FrameWorker[] workers;
    private ExecutorService executor;

    protected AbstractOpenCVFrameProcessor(FrameDrawer drawer) {
        this.drawer = drawer;
        workers = new FrameWorker[Math.max(Runtime.getRuntime().availableProcessors() - 1, 1)];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = createFrameWorker();
        }
    }

    @Override
    public void release() {
        this.executor.shutdownNow();

        for (FrameWorker worker : workers) {
            worker.release();
        }
    }

    @Override
    public void allocate(int width, int height) {
        for (FrameWorker worker : workers) {
            worker.allocate(width, height);
        }
        this.executor = Executors.newFixedThreadPool(workers.length);

    }

    @Override
    public boolean put(byte[] data) {
        boolean ret = false;
        for (FrameWorker worker : workers) {
            if (ret = worker.put(data)) {
                executor.execute(worker);
                break;
            }
        }
        return ret;
    }

}