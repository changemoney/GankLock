package me.wavever.ganklock.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.io.IOException;
import me.wavever.ganklock.R;
import me.wavever.ganklock.utils.LogUtil;
import me.wavever.ganklock.utils.PhotoUtil;
import me.wavever.ganklock.utils.SystemUtil;
import me.wavever.ganklock.utils.UIUtil;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnViewTapListener;

import static android.view.View.GONE;

/**
 * Created by wavever on 2016/9/21.
 */

public class PhotoActivity extends BaseActivity implements OnClickListener {

    private static final String TAG = PhotoActivity.class.getSimpleName() + "-->";

    public static final String KEY_PHOTO_URL = "key_photo_url";
    public static final String KEY_PHOTO_ID = "key_photo_id";
    public static final String KEY_ACTIVITY_JUMPED = "key_activity_jumped";
    public static final int ACTIVITY_JUMPER_FROM_DAILY = 0;
    public static final int ACTIVITY_JUMPER_FROM_MEIZHI = 1;

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1;

    private ImageView mPhoto;
    private Bitmap mBitmap;
    private String photoUrl;
    private String photoID;
    private PhotoViewAttacher mAttacher;
    private LinearLayout mOptionLayout;
    private TextView mSend;
    private TextView mSetWallPaper;
    private TextView mLockWallPaper;
    private TextView mSave;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //如果没有虚拟按键则设置为全屏
        if (!UIUtil.isHasNavigationBar(this)) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LOW_PROFILE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
        Picasso.with(this).load(photoUrl).into(mPhoto);
        mAttacher = new PhotoViewAttacher(mPhoto);
        mAttacher.setOnViewTapListener(new OnViewTapListener() {
            @Override public void onViewTap(View view, float x, float y) {
                hideBottomOption();
            }
        });
    }


    @Override protected int loadView() {
        return R.layout.activity_photo;
    }


    @Override protected void initView() {
        mPhoto = (ImageView) findViewById(R.id.photo_view);
        photoUrl = getIntent().getStringExtra(KEY_PHOTO_URL);
        mOptionLayout = (LinearLayout) findViewById(R.id.photo_bottom_option);
        mSave = (TextView) findViewById(R.id.photo_save);
        mSave.setOnClickListener(this);
        if(getIntent().getIntExtra(KEY_ACTIVITY_JUMPED,0)==0){
            mOptionLayout.setVisibility(GONE);
            photoID = getIntent().getStringExtra(KEY_PHOTO_ID);
            //TODO:可以添加到PhotoUtil中
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        //同步加载一张图片,注意只能在子线程中调用并且Bitmap不会被缓存到内存里.
                        mBitmap = Picasso.with(PhotoActivity.this).load(photoUrl).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }else {
            mSave.setVisibility(GONE);
            mSend = (TextView) findViewById(R.id.photo_share);
            mSend.setOnClickListener(this);
            mSetWallPaper = (TextView) findViewById(R.id.photo_wallpaper);
            mSetWallPaper.setOnClickListener(this);
            mLockWallPaper = (TextView) findViewById(R.id.photo_lock);
            mLockWallPaper.setOnClickListener(this);
        }


    }


    boolean isHide;
    private void hideBottomOption() {
        if (!isHide) {
            mOptionLayout.animate()
                .translationY(mOptionLayout.getHeight())
                .setInterpolator(new LinearInterpolator());
            isHide = true;
        } else {
            mOptionLayout.animate().translationY(0).setInterpolator(new LinearInterpolator());
            isHide = false;
        }
    }


    @Override public void onClick(View v) {
        switch (v.getId()) {
            case R.id.photo_share:
                PhotoUtil.sharePhoto(this, photoUrl);
                break;
            case R.id.photo_wallpaper:
                break;
            case R.id.photo_save:
                if(SystemUtil.isNetworkAvailable()){
                    downLoadMeizhi();
                }else{
                    Snackbar.make(mPhoto,"好像没网哎~",Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
    }


    private void downLoadMeizhi() {
        LogUtil.d(TAG + Build.VERSION.SDK_INT);
        if (VERSION.SDK_INT < Build.VERSION_CODES.M) {
            PhotoUtil.savePhotoByBitmap(this, mBitmap, photoID);
            return;
        }
        //没有授权
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED) {
            //第二个参数是需要申请的权限的字符串数组，支持一次性申请多个权限的，系统会通过对话框逐一询问用户是否授权。
            ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        } else {
            PhotoUtil.savePhotoByBitmap(this, mBitmap, photoID);
        }
    }


    /**
     * 处理申请权限的回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PhotoUtil.savePhotoByBitmap(this, mBitmap, photoID);
                } else {
                    Snackbar.make(mPhoto, "我的天哪！拒绝了怎么存？", Snackbar.LENGTH_LONG).setAction("点错啦..",
                        new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                downLoadMeizhi();
                            }
                        }).show();
                }
                break;
        }
    }

}
