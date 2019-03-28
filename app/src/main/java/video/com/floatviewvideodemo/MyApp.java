package video.com.floatviewvideodemo;

import android.app.Application;

/**
 * create om  2019/3/28.
 * Created by  gaoxuge
 * email android_gaoxuge@163.com
 * 功能描述
 */
public class MyApp extends Application {
    private static MyApp instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    public static MyApp getInstance() {
        return instance;
    }
}
