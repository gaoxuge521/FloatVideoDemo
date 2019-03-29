package video.com.floatviewvideodemo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.Toast;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import video.com.floatviewvideodemo.floatingview.FloatingView;
import video.com.floatviewvideodemo.floatingview.utils.ScreenSizeUtil;
import video.com.floatviewvideodemo.floatingview.utils.VideoUtils;
import video.com.floatviewvideodemo.floatview2.FloatingViewListener2;
import video.com.floatviewvideodemo.floatview2.FloatingViewManager2;

public class MainActivity extends AppCompatActivity implements FloatingViewListener2, View.OnClickListener {
    private String TAG = "sss  ";
    private FloatingViewManager2 mFloatingViewManager;
    private View floatView;
    private Context mContest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "onCreate: 111111111111111" );
        mContest = this;
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




        findViewById(R.id.tv_open2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFloatingView(MainActivity.this);
            }
        });
        findViewById(R.id.tv_close2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mFloatingViewManager!=null){
                    mFloatingViewManager.removeAllFloatingView();
                }
                cancleVideo();
            }
        });

    }

    /**
     * 显示悬浮窗
     *
     * @param context
     */
    private void showFloatingView(Context context) {
        Log.e(TAG, "手机系统版本：" + Build.VERSION.SDK_INT);
        // API22以下直接启动
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            startVideoService();
        } else {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                showDialog();
            } else {
                startVideoService();
            }
        }
    }


    private long mLastTouchDownTime;
    private static final int TOUCH_TIME_THRESHOLD = 150;
    private  ImageView mIcon;
    //UI
    private ImageView mRecordControl;
    private ImageView mPauseRecord;
    private SurfaceView surfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Chronometer mRecordTime;

    //录像机状态标识
    private int mRecorderState;

    public static final int STATE_INIT = 0;
    public static final int STATE_RECORDING = 1;
    public static final int STATE_PAUSE = 2;

    //DATA
    private boolean isPause;     //暂停标识
    private boolean isRecording; // 标记，判断当前是否正在录制
    private long mRecordCurrentTime = 0;  //录制时间间隔
    private long mPauseTime = 0;           //录制暂停时间间隔

    // 存储文件
    // 存储文件
    private File mVecordFile;
    private Camera mCamera;
    private MediaRecorder mediaRecorder;
    private String currentVideoFilePath;
    private String saveVideoPath = "";

    private void startVideoService() {

        floatView = View.inflate(this, R.layout.en_floating_view, null);

        mIcon = floatView.findViewById(R.id.icon);
        surfaceView = (SurfaceView) floatView.findViewById(R.id.record_surfaceView);
        mRecordControl = (ImageView)floatView. findViewById(R.id.record_control);
        mRecordTime = (Chronometer) floatView.findViewById(R.id.record_time);
        mPauseRecord = (ImageView) floatView.findViewById(R.id.record_pause);
        mRecordControl.setOnClickListener(this);
        mPauseRecord.setOnClickListener(this);
        mPauseRecord.setEnabled(false);
        //配置SurfaceHolder
        mSurfaceHolder = surfaceView.getHolder();
        // 设置Surface不需要维护自己的缓冲区
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // 设置分辨率
        mSurfaceHolder.setFixedSize(320, 280);
        // 设置该组件不会让屏幕自动关闭
        mSurfaceHolder.setKeepScreenOn(true);
        //回调接口
        mSurfaceHolder.addCallback(mSurfaceCallBack);

        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);

        if(mFloatingViewManager==null){
            mFloatingViewManager = new FloatingViewManager2(MainActivity.this, MainActivity.this);
        }

        FloatingViewManager2.Configs configs = new FloatingViewManager2.Configs();

        configs.floatingViewX = dm.widthPixels / 2;
        configs.floatingViewY = dm.heightPixels / 4;
        configs.floatingViewWidth = ScreenSizeUtil.Dp2Px(100);
        configs.floatingViewHeight = ScreenSizeUtil.Dp2Px(150);
        configs.overMargin = -(int) (8 * dm.density);
        configs.animateInitialMove = false;
        mFloatingViewManager.addFloatingView(floatView, configs);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startVideo();
            }
        },1000);
    }
    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("请开启悬浮窗权限");
        builder.setPositiveButton("开启", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + MainActivity.this.getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 100;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            startVideoService();
        }
    }


    public void startVideo(){
        Log.e(TAG, "onClick: 录制" );
        if (getSDPath(MyApp.getInstance()) == null)
            return;

        //视频文件保存路径，configMediaRecorder方法中会设置
        currentVideoFilePath = getSDPath(MyApp.getInstance()) + getVideoName();
        //开始录制视频
        if (!startRecord())
            return;

        refreshControlUI();

        mRecorderState = STATE_RECORDING;
        Log.e(TAG, "startVideo: 0000000000000000     "+mRecorderState);
    }

    public void cancleVideo(){
        Log.e(TAG, "onClick: 停止视频录制" );
        //停止视频录制
        stopRecord();
        //先给Camera加锁后再释放相机
        if(mCamera!=null){
            mCamera.lock();
        }

        releaseCamera();

        refreshControlUI();

        //判断是否进行视频合并
        if ("".equals(saveVideoPath)) {
            saveVideoPath = currentVideoFilePath;
        }else {
            mergeRecordVideoFile();
        }
        mRecorderState = STATE_INIT;

        //延迟一秒跳转到播放器，（确保视频合并完成后跳转） TODO 具体的逻辑可根据自己的使用场景跳转
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                            Intent intent = new Intent(mContest, PlayVideoActivity.class);
//                            Bundle bundle = new Bundle();
//                            bundle.putString("videoPath", saveVideoPath);
//                            intent.putExtras(bundle);
//                            startActivity(intent);
//                            finish();
            }
        },1000);
        Log.e(TAG, "cancleVideo: 11111111111111111       "+mRecorderState);
    }
    private SurfaceHolder.Callback mSurfaceCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            initCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            if (mSurfaceHolder.getSurface() == null) {
                return;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            releaseCamera();
        }
    };

    private MediaRecorder.OnErrorListener OnErrorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mediaRecorder, int what, int extra) {
            try {
                if (mediaRecorder != null) {
                    mediaRecorder.reset();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };




    /**
     * 初始化摄像头
     *
     * @author liuzhongjun
     * @date 2016-3-16
     */
    private void initCamera() {
        if (mCamera != null) {
            releaseCamera();
        }

        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        if (mCamera == null) {
            Toast.makeText(mContest, "未能获取到相机！", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            //将相机与SurfaceHolder绑定
            mCamera.setPreviewDisplay(mSurfaceHolder);
            //配置CameraParams
            configCameraParams();
            //启动相机预览
            mCamera.startPreview();
        } catch (IOException e) {
            //有的手机会因为兼容问题报错，这就需要开发者针对特定机型去做适配了
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }
    /**
     * 设置摄像头为竖屏
     *
     * @author lip
     * @date 2015-3-16
     */
    private void configCameraParams() {
        Camera.Parameters params = mCamera.getParameters();
        //设置相机的横竖屏(竖屏需要旋转90°)
//        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
//            params.set("orientation", "portrait");
//            mCamera.setDisplayOrientation(90);
//        } else {
//            params.set("orientation", "landscape");
//            mCamera.setDisplayOrientation(0);
//        }
        params.set("orientation", "landscape");
        mCamera.setDisplayOrientation(0);
        //设置聚焦模式
//        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        params.setFocusMode(params.getFocusMode());
        //缩短Recording启动时间
        params.setRecordingHint(true);
        //影像稳定能力
        if (params.isVideoStabilizationSupported())
            params.setVideoStabilization(true);
        mCamera.setParameters(params);
    }


    /**
     * 设置摄像头为竖屏
     *
     * @author liuzhongjun
     * @date 2016-3-16
     */
    private void setCameraParams() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            //设置相机的很速屏幕
//            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
//                params.set("orientation", "portrait");
//                mCamera.setDisplayOrientation(90);
//            } else {
//                params.set("orientation", "landscape");
//                mCamera.setDisplayOrientation(0);
//            }
            params.set("orientation", "landscape");
            mCamera.setDisplayOrientation(0);
            //设置聚焦模式
//            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            params.setFocusMode(params.getFocusMode());
            //缩短Recording启动时间
            params.setRecordingHint(true);
            //是否支持影像稳定能力，支持则开启
            if (params.isVideoStabilizationSupported())
                params.setVideoStabilization(true);
            mCamera.setParameters(params);
        }
    }


    /**
     * 释放摄像头资源
     *
     * @author liuzhongjun
     * @date 2016-2-5
     */
    private void stopCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 释放摄像头资源
     *
     * @author liuzhongjun
     * @date 2016-2-5
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    /**
     * 开始录制视频
     */
    public boolean startRecord() {

        initCamera();
        //录制视频前必须先解锁Camera
        if(mCamera!=null){
            mCamera.unlock();
        }

        configMediaRecorder();
        try {
            //开始录制
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.e(TAG, "startRecord: "+e.getMessage() );
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 停止录制视频
     */
    public void stopRecord() {
        // 设置后不会崩
        if(mediaRecorder!=null){
            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setPreviewDisplay(null);
            //停止录制
            mediaRecorder.stop();
            mediaRecorder.reset();
            //释放资源
            mediaRecorder.release();
            mediaRecorder = null;
        }

    }

    public void pauseRecord() {


    }

    /**
     * 合并录像视频方法
     */
    private void mergeRecordVideoFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] str = new String[]{saveVideoPath, currentVideoFilePath};
                    //将2个视频文件合并到 append.mp4文件下
                    VideoUtils.appendVideo(mContest, getSDPath(mContest) + "append.mp4", str);
                    File reName = new File(saveVideoPath);
                    File f = new File(getSDPath(mContest) + "append.mp4");
                    //再将合成的append.mp4视频文件 移动到 saveVideoPath 路径下
                    f.renameTo(reName);
                    if (reName.exists()) {
                        f.delete();
                        new File(currentVideoFilePath).delete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 点击中间按钮，执行的UI更新操作
     */
    private void refreshControlUI() {
        try {
            if (mRecorderState == STATE_INIT) {
                //录像时间计时
                if(mRecordTime!=null){
                    mRecordTime.setBase(SystemClock.elapsedRealtime());
                    mRecordTime.start();
                }

                if(mRecordControl!=null){
                    mRecordControl.setImageResource(R.drawable.recordvideo_stop);
                    //1s后才能按停止录制按钮
                    mRecordControl.setEnabled(false);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mRecordControl.setEnabled(true);
                        }
                    }, 1000);
                }
                if(mPauseRecord!=null){
                    mPauseRecord.setVisibility(View.GONE);
                    mPauseRecord.setEnabled(true);
                }



            } else if (mRecorderState == STATE_RECORDING) {
                if(mRecordTime!=null){
                    mPauseTime = 0;
                    mRecordTime.stop();
                }
                if(mRecordControl!=null){
                    mRecordControl.setImageResource(R.drawable.recordvideo_start);
                }
                if(mPauseRecord!=null){
                    mPauseRecord.setVisibility(View.GONE);
                    mPauseRecord.setEnabled(false);
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 点击暂停继续按钮，执行的UI更新操作
     */
    private void refreshPauseUI() {
        if (mRecorderState == STATE_RECORDING) {
            mPauseRecord.setImageResource(R.drawable.control_play);

            mPauseTime = SystemClock.elapsedRealtime();
            mRecordTime.stop();
        } else if (mRecorderState == STATE_PAUSE) {
            mPauseRecord.setImageResource(R.drawable.control_pause);

            if (mPauseTime == 0) {
                mRecordTime.setBase(SystemClock.elapsedRealtime());
            } else {
                mRecordTime.setBase(SystemClock.elapsedRealtime() - (mPauseTime - mRecordTime.getBase()));
            }
            mRecordTime.start();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record_control: {
                if (mRecorderState == STATE_INIT) {

                    Log.e(TAG, "onClick: 录制" );
                    if (getSDPath(MyApp.getInstance()) == null)
                        return;

                    //视频文件保存路径，configMediaRecorder方法中会设置
                    currentVideoFilePath = getSDPath(MyApp.getInstance()) + getVideoName();
                    //开始录制视频
                    if (!startRecord())
                        return;

                    refreshControlUI();

                    mRecorderState = STATE_RECORDING;

                } else if (mRecorderState == STATE_RECORDING) {
                    Log.e(TAG, "onClick: 停止视频录制" );
                    //停止视频录制
                    stopRecord();
                    //先给Camera加锁后再释放相机
                    mCamera.lock();
                    releaseCamera();

                    refreshControlUI();

                    //判断是否进行视频合并
                    if ("".equals(saveVideoPath)) {
                        saveVideoPath = currentVideoFilePath;
                    }else {
                        mergeRecordVideoFile();
                    }
                    mRecorderState = STATE_INIT;

                    //延迟一秒跳转到播放器，（确保视频合并完成后跳转） TODO 具体的逻辑可根据自己的使用场景跳转
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                            Intent intent = new Intent(mContest, PlayVideoActivity.class);
//                            Bundle bundle = new Bundle();
//                            bundle.putString("videoPath", saveVideoPath);
//                            intent.putExtras(bundle);
//                            startActivity(intent);
//                            finish();
                        }
                    },1000);
                } else if (mRecorderState == STATE_PAUSE) {
                    Log.e(TAG, "onClick: 代表视频暂停录制时 点击中心按钮" );
                    //代表视频暂停录制时，点击中心按钮
//                    Intent intent = new Intent(CustomRecordActivity.this, PlayVideoActivity.class);
//                    Bundle bundle = new Bundle();
//                    bundle.putString("videoPath", saveVideoPath);
//                    intent.putExtras(bundle);
//                    startActivity(intent);
//                    finish();
                }
                break;
            }

            case R.id.record_pause: {
                if (mRecorderState == STATE_RECORDING) {
                    //正在录制的视频，点击后暂停

                    //取消自动对焦
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
//                            if (success)
//                                CustomRecordActivity.this.mCamera.cancelAutoFocus();
                        }
                    });

                    stopRecord();

                    refreshPauseUI();


                    //判断是否进行视频合并
                    if ("".equals(saveVideoPath)) {
                        saveVideoPath = currentVideoFilePath;
                    } else {
                        mergeRecordVideoFile();
                    }

                    mRecorderState = STATE_PAUSE;

                } else if (mRecorderState == STATE_PAUSE) {

                    if (getSDPath(MyApp.getInstance()) == null)
                        return;

                    //视频文件保存路径，configMediaRecorder方法中会设置
                    currentVideoFilePath = getSDPath(MyApp.getInstance()) + getVideoName();

                    //继续视频录制
                    if (!startRecord()) {
                        return;
                    }
                    refreshPauseUI();

                    mRecorderState = STATE_RECORDING;
                }
            }
        }

    }


    /**
     * 配置MediaRecorder()
     */

    private void configMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.reset();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setOnErrorListener(OnErrorListener);

        //使用SurfaceView预览
        mediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        //1.设置采集声音
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置采集图像
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //2.设置视频，音频的输出格式 mp4
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        //3.设置音频的编码格式
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //设置图像的编码格式
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //设置立体声
//        mediaRecorder.setAudioChannels(2);
        //设置最大录像时间 单位：毫秒
//        mediaRecorder.setMaxDuration(60 * 1000);
        //设置最大录制的大小 单位，字节
//        mediaRecorder.setMaxFileSize(1024 * 1024);
        //音频一秒钟包含多少数据位
        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mediaRecorder.setAudioEncodingBitRate(44100);
        if (mProfile.videoBitRate > 2 * 1024 * 1024)
            mediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
        else
            mediaRecorder.setVideoEncodingBitRate(1024 * 1024);
        mediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);

        //设置选择角度，顺时针方向，因为默认是逆向90度的，这样图像就是正常显示了,这里设置的是观看保存后的视频的角度
        mediaRecorder.setOrientationHint(90);
        //设置录像的分辨率
        mediaRecorder.setVideoSize(352, 288);

        //设置录像视频输出地址
        mediaRecorder.setOutputFile(currentVideoFilePath);
    }

    /**
     * 创建视频文件保存路径
     */
    public static String getSDPath(Context context) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(context, "请查看您的SD卡是否存在！", Toast.LENGTH_SHORT).show();
            return null;
        }

        File sdDir = Environment.getExternalStorageDirectory();
        File eis = new File(sdDir.toString() + "/wenhuazhifa/");
        if (!eis.exists()) {
            eis.mkdir();
        }
        return sdDir.toString() + "/wenhuazhifa/";
    }

    private String getVideoName() {
        return "whzf_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
    }




    public void setIconImage(@DrawableRes int resId){
        mIcon.setImageResource(resId);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (event != null) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLastTouchDownTime = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_UP:
                    if (isOnClickEvent()) {
//                        dealClickEvent();
                    }
                    break;
            }
        }
        return true;
    }

    protected boolean isOnClickEvent() {
        return System.currentTimeMillis() - mLastTouchDownTime > TOUCH_TIME_THRESHOLD;
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

        stopRecord();
        stopCamera();
        if(mFloatingViewManager!=null){
            mFloatingViewManager.removeAllFloatingView();
        }
        mFloatingViewManager = null;
    }

    @Override
    public void onFinishFloatingView() {

    }

}
