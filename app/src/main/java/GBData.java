import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

public class GBData {
    private static final String TAG = "GBData";
    static ImageReader reader;
    private static Bitmap bitmap;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static int getColor(int x, int y) {
        if (reader == null) {
            Log.w(TAG, "getColor: reader is null");
            return -1;
        }

        Image image = reader.acquireLatestImage();

        if (image == null) {
            if (bitmap == null) {
                Log.w(TAG, "getColor: image is null");
                return -1;
            }
            return bitmap.getPixel(x, y);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        }
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();

        return bitmap.getPixel(x, y);
    }
}
