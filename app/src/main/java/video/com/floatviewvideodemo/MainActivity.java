package video.com.floatviewvideodemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.List;

import video.com.floatviewvideodemo.floatingview.FloatingMagnetView;
import video.com.floatviewvideodemo.floatingview.FloatingView;
import video.com.floatviewvideodemo.floatingview.MagnetViewListener;

public class MainActivity extends AppCompatActivity {
    private String TAG = "sss  ";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "onCreate: 111111111111111" );

        AndPermission.with(this)
                .runtime()
                .permission(Permission.CAMERA,Permission.RECORD_AUDIO,Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        Log.e(TAG, "onAction: 同意 " );
                    }
                }).onDenied(new Action<List<String>>() {
            @Override
            public void onAction(List<String> data) {
                Log.e(TAG, "onAction:  取消" );
            }
        }).start();
        findViewById(R.id.tv_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FloatingView.get().add();
            }
        });
        findViewById(R.id.tv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FloatingView.get().remove();
            }
        });

        FloatingView.get().listener(new MagnetViewListener() {
            @Override
            public void onRemove(FloatingMagnetView magnetView) {
                Log.e(TAG, "onRemove: 关闭");
            }

            @Override
            public void onClick(FloatingMagnetView magnetView) {
                Log.e(TAG, "onClick: 点击");
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        FloatingView.get().attach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FloatingView.get().remove();
        FloatingView.get().detach(this);
    }
}
