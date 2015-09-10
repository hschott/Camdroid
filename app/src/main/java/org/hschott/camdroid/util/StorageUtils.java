package org.hschott.camdroid.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import static android.os.Environment.MEDIA_MOUNTED;

public final class StorageUtils {

    private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";

    final static String TAG = StorageUtils.class.getSimpleName();

    private static void copyAction(File srcFile, File destFile)
            throws FileNotFoundException, IOException {
        FileInputStream istream = new FileInputStream(srcFile);
        FileOutputStream ostream = new FileOutputStream(destFile);
        FileChannel input = istream.getChannel();
        FileChannel output = ostream.getChannel();

        try {
            input.transferTo(0, input.size(), output);
        } finally {
            istream.close();
            ostream.close();
            input.close();
            output.close();
        }
    }

    public static void copyDirectory(File srcDir, File destinationDir)
            throws IOException {
        // Renaming a file to an existing directory should fail
        if (destinationDir.exists() && destinationDir.isFile()) {
            Log.w(TAG, "Can't rename a file to a directory");
            return;
        }

        // Check to make sure we are not copying the directory into itself
        if (isCopyOnItself(srcDir.getAbsolutePath(),
                destinationDir.getAbsolutePath())) {
            Log.w(TAG, "Can't copy itself into itself");
            return;
        }

        // See if the destination directory exists. If not create it.
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdir()) {
                // If we can't create the directory then fail
                Log.w(TAG, "Couldn't create the destination directory");
                return;
            }
        }

        for (File file : srcDir.listFiles()) {
            if (file.isDirectory()) {
                copyDirectory(file, destinationDir);
            } else {
                File destination = new File(destinationDir.getAbsoluteFile()
                        + File.separator + file.getName());
                StorageUtils.copyFile(file, destination);
            }
        }

    }

    public static void copyFile(File srcFile, File destFile) throws IOException {
        // Renaming a file to an existing directory should fail
        if (destFile.exists() && destFile.isDirectory()) {
            Log.w(TAG, "Can't rename a file to a directory");
            return;
        }

        StorageUtils.copyAction(srcFile, destFile);

    }

    public static void exportAssets(Context context, File outdir,
                                    String[] frompath, String[] topath) {
        if (frompath.length != topath.length) {
            Log.e("TAG",
                    "Failed to export assets. frompath must be of the same length as topath");
        }

        try {
            for (int i = 0; i < frompath.length; i++) {

                File outfile = new File(outdir.getPath() + File.separator
                        + topath[i]);

                if (outfile.exists()) {
                    break;
                }

                outfile.getParentFile().mkdirs();

                InputStream is = context.getAssets().open(frompath[i]);
                FileOutputStream os = new FileOutputStream(outfile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();

            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to export assets", e);
        }
    }

    public static void exportRaw(Context context, File outdir, int[] fromid,
                                 String[] topath) {
        if (fromid.length != topath.length) {
            Log.e("TAG",
                    "Failed to export assets. fromid must be of the same length as topath");
        }

        try {
            for (int i = 0; i < fromid.length; i++) {

                File outfile = new File(outdir.getPath() + File.separator
                        + topath[i]);

                outfile.getParentFile().mkdirs();

                InputStream is = context.getResources().openRawResource(
                        fromid[i]);
                FileOutputStream os = new FileOutputStream(outfile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();

            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to export assets", e);
        }
    }

    public static File getTempFile(Context context, String prefix, String suffix) {
        File cacheDir = context.getCacheDir();
        if (!suffix.startsWith(".")) {
            suffix = "." + suffix;
        }
        try {
            File tmp = File.createTempFile(prefix, suffix, cacheDir);
            tmp.delete();

            return tmp;
        } catch (IOException e) {
            Log.e(TAG, "Failed to get temp file name. Exception is thrown: "
                    + e);
            return null;
        }
    }

    private static boolean hasExternalStoragePermission(Context context) {
        int perm = context
                .checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isCopyOnItself(String src, String dest) {

        // This weird test is to determine if we are copying or moving a
        // directory into itself.
        // Copy /sdcard/myDir to /sdcard/myDir-backup is okay but
        // Copy /sdcard/myDir to /sdcard/myDir/backup should throw an
        // INVALID_MODIFICATION_ERR
        if (dest.startsWith(src)
                && dest.indexOf(File.separator, src.length() - 1) != -1) {
            return true;
        }

        return false;
    }

    public static void moveDirectory(File srcDir, File destinationDir)
            throws IOException {
        // Renaming a file to an existing directory should fail
        if (destinationDir.exists() && destinationDir.isFile()) {
            Log.w(TAG, "Can't rename a file to a directory");
            return;
        }

        // Check to make sure we are not copying the directory into itself
        if (isCopyOnItself(srcDir.getAbsolutePath(),
                destinationDir.getAbsolutePath())) {
            Log.w(TAG, "Can't move itself into itself");
            return;
        }

        // If the destination directory already exists and is empty then delete
        // it. This is according to spec.
        if (destinationDir.exists()) {
            if (destinationDir.list().length > 0) {
                Log.w(TAG, "directory is not empty");
                return;
            }
        }

        // Try to rename the directory
        if (!srcDir.renameTo(destinationDir)) {
            // Trying to rename the directory failed. Possibly because we moved
            // across file system on the device.
            // Now we have to do things the hard way
            // 1) Copy all the old files
            // 2) delete the src directory
            StorageUtils.copyDirectory(srcDir, destinationDir);
            if (destinationDir.exists()) {
                removeDirRecursively(srcDir);
            } else {
                throw new IOException("moved failed");
            }
        }

    }

    public static void moveFile(File srcFile, File destFile) throws IOException {
        // Renaming a file to an existing directory should fail
        if (destFile.exists() && destFile.isDirectory()) {
            Log.w(TAG, "Can't rename a file to a directory");
        }

        // Try to rename the file
        if (!srcFile.renameTo(destFile)) {
            // Trying to rename the file failed. Possibly because we moved
            // across file system on the device.
            // Now we have to do things the hard way
            // 1) Copy all the old file
            // 2) delete the src file
            copyAction(srcFile, destFile);
            if (destFile.exists()) {
                srcFile.delete();
            } else {
                throw new IOException("moved failed");
            }
        }

    }

    protected static String readFile(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            StringBuffer result = new StringBuffer();
            while ((line = br.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }
            br.close();
            return result.toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read file: " + path, e);
            return null;
        }
    }

    public static boolean removeDirRecursively(File directory) {
        if (!directory.exists()) {
            return true;
        }

        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                removeDirRecursively(file);
            }
        }

        if (!directory.delete()) {
            Log.w(TAG, "could not delete: " + directory.getName());
            return false;
        } else {
            return true;
        }
    }

    public static boolean touchDir(File dirname) {
        if (!dirname.exists()) {
            if (!dirname.mkdirs()) {
                Log.w(TAG, "Unable to create external cache directory");
                return false;
            }
        }

        File nomedia = new File(dirname, ".nomedia");
        if (!nomedia.exists()) {
            try {
                return nomedia.createNewFile();
            } catch (IOException e) {
                Log.i(TAG,
                        "Can't create \".nomedia\" file in application external directory");
                return false;
            }
        }

        return nomedia.canWrite();
    }

    public static void updateMedia(Context ctx, File file) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
        ctx.sendBroadcast(intent);
    }

    public static void writeFile(File file, byte[] content) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            FileChannel fc = stream.getChannel();
            fc.write(ByteBuffer.wrap(content));
        } catch (IOException e) {
            Log.e(TAG, "Failed to write file: " + file, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    public static void writeFile(File file, String content) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            FileChannel fc = stream.getChannel();
            fc.write(Charset.defaultCharset().encode(content));
        } catch (IOException e) {
            Log.e(TAG, "Failed to write file: " + file, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    private StorageUtils() {
    }
}
