package nanodegree.ed.mo.prof.makeyourappmaterial.remote;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;
    private static String TAG = Config.class.toString();
    public static Context mContext;
    public static CoordinatorLayout mCoordinatorLayout;

    static {
        URL url = null;
        try {
            url = new URL("https://go.udacity.com/xyz-reader-json" );
        } catch (MalformedURLException ignored) {
            Log.e(TAG, "Please check your internet connection.");
        }

        BASE_URL = url;
    }
}
