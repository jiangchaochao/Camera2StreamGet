package cn.yview.camera2streamget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SurfaceView previewSurface;
    private SurfaceHolder previewSurfaceHolder;

    private MediaCodec mediaEncodeUp;      //上传流
    private MediaCodec mediaEncodeSave;    //保存流
    private MediaFormat mediaFormatUp;     //上传编码格式
    private MediaFormat mediaFormatSave;  //保存编码格式

    private Surface mediaUpSurface;       //上传流使用的surface
    private Surface MediaSaveSurface;    //保存流使用的surface
    private CaptureRequest.Builder mPreviewBuilder = null;

    private String codecType = MediaFormat.MIMETYPE_VIDEO_AVC; //压缩格式
    private CameraDevice cameraDevice;    //camera
    private boolean upRecordFlag = false; //录制标志
    private boolean saveRecordFlag = false; //录制标志
    private MediaCodec.BufferInfo upBufferInfo = null;
    private MediaCodec.BufferInfo saveBufferInfo = null;
    FileOutputStream fileOutputStream;
    FileOutputStream fileOutputStream1;
    private CameraCharacteristics cameraCharacteristics;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewSurface = findViewById(R.id.previewSurface);
        previewSurfaceHolder = previewSurface.getHolder();
        previewSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    fileOutputStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/720.h264"));
                    fileOutputStream1 = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/1080.h264"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                openCamera(1920, 1080);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });


    }
    /*open camera*/
    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height)
    {
        /*获得相机管理服务*/
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            /*获取CameraID列表*/
            String[] cameralist = manager.getCameraIdList();
            manager.openCamera(cameralist[0], CameraOpenCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /*close camera*/
    private void closeCamera()
    {

    }

    /*摄像头打开回调*/
    CameraDevice.StateCallback CameraOpenCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            try {
                upBufferInfo = new MediaCodec.BufferInfo();
                /*创建编码器*/
                mediaEncodeUp =  MediaCodec.createEncoderByType(codecType);
                /*设置编码参数*/
                mediaFormatUp = MediaFormat.createVideoFormat(codecType, 1280, 720);
                /*设置颜色格式*/
                mediaFormatUp.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                /*设置比特率*/
                mediaFormatUp.setInteger(MediaFormat.KEY_BIT_RATE, 2000000);
                /*设置帧率*/
                mediaFormatUp.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
                /*设置关键帧间隔时间（S）*/
                mediaFormatUp.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
                /*将设置好的参数配置给编码器*/
                mediaEncodeUp.configure(mediaFormatUp, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                /*使用surface代替mediacodec数据输入buffer*/
                mediaUpSurface = mediaEncodeUp.createInputSurface();


                saveBufferInfo = new MediaCodec.BufferInfo();
                mediaEncodeSave = MediaCodec.createEncoderByType(codecType);
                mediaFormatSave = MediaFormat.createVideoFormat(codecType, 1920, 1080);
                mediaFormatSave.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mediaFormatSave.setInteger(MediaFormat.KEY_BIT_RATE, 4000000);
                mediaFormatSave.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
                mediaFormatSave.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
                mediaEncodeSave.configure(mediaFormatSave, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                MediaSaveSurface = mediaEncodeSave.createInputSurface();

                /*设置预览尺寸*/
                previewSurfaceHolder.setFixedSize(1920, 1080);
//
//                /*创建预览请求*/
                mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.addTarget(previewSurfaceHolder.getSurface());
                mPreviewBuilder.addTarget(mediaUpSurface);
                mPreviewBuilder.addTarget(MediaSaveSurface);
                /*创建会话*/
                camera.createCaptureSession(Arrays.asList(previewSurfaceHolder.getSurface(), mediaUpSurface, MediaSaveSurface), Sessioncallback, null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    /*录制视频回调*/
    CameraCaptureSession.StateCallback Sessioncallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
                startMediaCodecRecording();
                session.setRepeatingRequest(mPreviewBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            //开始录制


        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    /*开始录制视频*/
    private void startMediaCodecRecording()
    {
        /*上传流模拟录制，保存到本地*/
        Thread recordThread = new Thread(){
            @Override
            public void run() {
                super.run();
                if (mediaEncodeUp == null)
                {
                    return ;
                }
                Log.d("MediaCodec", "上传流开始录制###################" );
                upRecordFlag = true;
                mediaEncodeUp.start();
                while (upRecordFlag)
                {
                    int status = mediaEncodeUp.dequeueOutputBuffer(upBufferInfo, 10000);
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER)
                    {
                        Log.e("MdiaCodec " , " time out");
                    }else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                    {
                        Log.e("MediaCodec", "format changed");
                    }else if (status >= 0)
                    {
                        ByteBuffer data = mediaEncodeUp.getOutputBuffer(status);
                        if (data != null)
                        {
                            upBufferInfo.presentationTimeUs = SystemClock.uptimeMillis() * 1000;
                            //写文件
                            try {
                                Log.e("MainActivity: " , "写文件");
                                byte[] bb = new byte[data.remaining()];
                                data.get(bb, 0, bb.length);
                                fileOutputStream.write(bb);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        // releasing buffer is important
                        mediaEncodeUp.releaseOutputBuffer(status, false);
                        final int endOfStream = upBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                        if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM) break;
                    }
                }

                mediaUpSurface.release();
                mediaUpSurface = null;
                mediaEncodeUp.stop();
                mediaEncodeUp.release();
                mediaEncodeUp = null;
            }
        };
        /*开始录制*/
            recordThread.start();


        /*保存到本地流录制*/
        Thread recordThread1 = new Thread()
        {
            public void run() {
                super.run();
                if (mediaEncodeSave == null)
                {
                    return ;
                }
                Log.d("MediaCodec", "本地流开始录制###################" );
                upRecordFlag = true;
                mediaEncodeSave.start();
                while (upRecordFlag)
                {
                    int status = mediaEncodeSave.dequeueOutputBuffer(saveBufferInfo, 10000);
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER)
                    {
                        Log.e("MdiaCodec " , " time out");
                    }else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                    {
                        Log.e("MediaCodec", "format changed");
                    }else if (status >= 0)
                    {
                        ByteBuffer data = mediaEncodeSave.getOutputBuffer(status);
                        if (data != null)
                        {
                            saveBufferInfo.presentationTimeUs = SystemClock.uptimeMillis() * 1000;
                            //写文件
                            try {
//                                Log.e("MainActivity: " , "写文件");
                                byte[] bb = new byte[data.remaining()];
                                data.get(bb, 0, bb.length);
                                fileOutputStream1.write(bb);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        // releasing buffer is important
                        mediaEncodeSave.releaseOutputBuffer(status, false);
                        final int endOfStream = saveBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                        if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM) break;
                    }
                }

                MediaSaveSurface.release();
                MediaSaveSurface = null;
                mediaEncodeSave.stop();
                mediaEncodeSave.release();
                mediaEncodeSave = null;
            }
        };
        recordThread1.start();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            mediaUpSurface.release();
            mediaEncodeUp.stop();
            mediaEncodeUp.release();
            cameraDevice.close();
            fileOutputStream.close();

            MediaSaveSurface.release();
            mediaEncodeSave.stop();
            mediaEncodeSave.release();
            fileOutputStream1.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
