package info.doufm.android.activity;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.umeng.analytics.MobclickAgent;

import java.util.Timer;
import java.util.TimerTask;

import cn.pedant.SweetAlert.SweetAlertDialog;
import info.doufm.android.R;
import info.doufm.android.constans.Constants;
import info.doufm.android.constans.NetworkConstans;
import info.doufm.android.custom.PlayUIInterface;
import info.doufm.android.info.MusicInfo;
import info.doufm.android.playview.RotateAnimator;
import info.doufm.android.service.MusicService;
import info.doufm.android.service.MyBinder;
import info.doufm.android.user.User;
import info.doufm.android.utils.CheckApplicationVersion;
import info.doufm.android.utils.MusicInfoUtils;
import info.doufm.android.utils.SharedPreferencesUtils;
import info.doufm.android.utils.TimeFormat;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener,PlayUIInterface,View.OnClickListener{
    private static final String TAG = "MainActivity";

    private Toolbar toolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private int mThemeNum = 0;

    private ListView mDrawerList;
    private ServiceConnection connection;
    private MyBinder myBinder;
    private Button btnPlayMusic;
    private Button btnNextMusic;
    private Button btnPreMusic;
    private Button btnPlayMode;
    private Button btnLove;
    private static SweetAlertDialog mErrorDialog;

    private ImageView ivDiskCover;
    private ImageView ivNeedle;

    private TextView tvTotalTime;
    private TextView tvCurrentTime;
    private LinearLayout llLeftSlideMenu;
    private RelativeLayout rlUserLogin;
    private TextView tvUserLoginTitle;
    private SeekBar seekBar;
    private Timer timer;
    private MyPlayViewHandler myPlayViewHandler;
    private int mMusicDuration;
    private int mCurrentTime;
    private RotateAnimator mRotateAnimator;
    private Animation needleUpAnim;
    private Animation needleDownAnim;
    private Animation needleUpDownAnim;
    private int colorNum;
    private int mPlayListNum;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate");
        Log.v(TAG, "onCreate");
        Log.v(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        System.out.println("test git");
        //init toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar_custom);
        toolbar.setTitle("DouFM");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//add back icon

        tvTotalTime = (TextView)findViewById(R.id.totalTimeText);
        tvCurrentTime = (TextView) findViewById(R.id.currentTimeText);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        //监听seekbar的状态
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                Log.v(TAG,"onProgressChanged");
                mCurrentTime = progress;
                tvCurrentTime.setText(TimeFormat.msToMinAndS(mCurrentTime));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                Log.v(TAG,"onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Log.v(TAG,"onStopTrackingTouch");
                myBinder.mService.mediaPlayer.seekTo(mCurrentTime);
                seekBar.setProgress(mCurrentTime);

            }
        });
        btnPlayMusic = (Button) findViewById(R.id.btn_start_play);
        btnPlayMusic.setOnClickListener(this);
        btnNextMusic = (Button) findViewById(R.id.btn_play_next);
        btnNextMusic.setOnClickListener(this);
        btnPreMusic = (Button) findViewById(R.id.btn_play_previous);
        btnPreMusic.setOnClickListener(this);
        btnPlayMode = (Button) findViewById(R.id.btn_play_mode);
        btnPlayMode.setOnClickListener(this);
        btnLove = (Button) findViewById(R.id.btn_love);
        btnLove.setOnClickListener(this);
        llLeftSlideMenu = (LinearLayout) findViewById(R.id.ll_slide_left_menu);
        rlUserLogin = (RelativeLayout) findViewById(R.id.rl_slide_menu_header);
        rlUserLogin.setOnClickListener(this);
        tvUserLoginTitle = (TextView) findViewById(R.id.tv_user_name);

        ivDiskCover = (ImageView) findViewById(R.id.iv_disk);
        ivNeedle = (ImageView) findViewById(R.id.iv_needle);

        //init drawerlayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,toolbar,R.string.drawer_open,R.string.drawer_close);
        mDrawerToggle.syncState();//ActionBarDrawerToggle与DrawerLayout的状态同步，并将ActionBarDrawerToggle中的drawer图标，设置为ActionBar的Home-Button的icon
        mDrawerLayout.setDrawerListener(mDrawerToggle);//设置抽屉监听

        //init drawer menu
        initDrawerMenu();

        //add item click listener to channle list
        mDrawerList.setOnItemClickListener(this);

        timer = new Timer();
        //move seekbar handler
        myPlayViewHandler = new MyPlayViewHandler();//接受数据，移动seekbar
        mRotateAnimator = new RotateAnimator(this,ivDiskCover);//碟片的旋转动画
        needleUpAnim = AnimationUtils.loadAnimation(this,R.anim.rotation_up);
        needleDownAnim = AnimationUtils.loadAnimation(this, R.anim.rotation_down);
        needleUpDownAnim = AnimationUtils.loadAnimation(this,R.anim.rotation_up_down);
        //get sum of theme color
        colorNum = Constants.BACKGROUND_COLORS.length;
        //get number of theme
        mThemeNum = SharedPreferencesUtils.getInt(MainActivity.this, Constants.THEME, 11);
        toolbar.setBackgroundColor(Color.parseColor(Constants.ACTIONBAR_COLORS[mThemeNum]));
        mDrawerLayout.setBackgroundColor(Color.parseColor(Constants.BACKGROUND_COLORS[mThemeNum]));
        //get number of channel
        mPlayListNum = SharedPreferencesUtils.getInt(MainActivity.this, Constants.PLAYLIST, 0);

        updateLoginArea();//更新登录ui，只有主动登录才可以登录



    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume!");
        MobclickAgent.onPageStart(TAG);
        MobclickAgent.onResume(this);
        //create service connetction
        createServiceConnection();//这里相当于安装了链接监听器
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);//create start stop destroy
        bindService(intent, connection, Service.BIND_AUTO_CREATE);//绑定开启，这里开启和绑定的应该是同一个服务，但是当页面退出时这个服务并不会消失，因为还start过
        //create onbind unbind destroy
        //先创建service，一旦链接成功执行connection里面的方法
    }

    @Override
    protected void onPause() {
        Log.v(TAG,"onPause");
        super.onPause();
        MobclickAgent.onPageEnd(TAG);
        MobclickAgent.onPause(this);
        myBinder.setUIUpdateInterface(null);
        unbindService(connection);
        myBinder = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG,"onDestory!");
        Intent intent = new Intent(this, MusicService.class);
    //    timer.cancel();
    //    stopService(intent);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_start_play:
                if(myBinder.mService.isPlaying){
                    myBinder.mService.pausePlayMusic();
                }else{
                    myBinder.mService.startPlayMusic();
                }
                break;

            case R.id.btn_play_next:
                myBinder.mService.playNextMusic();
                break;

            case R.id.btn_play_previous:
                myBinder.mService.playPreMusic();
                break;

            case R.id.btn_play_mode://循环或者随机模式
                if(myBinder.mService.isLoopPlaying){
                    btnPlayMode.setBackgroundResource(R.drawable.bg_btn_shuffle);
                    myBinder.mService.isLoopPlaying = false;
                }else {
                    btnPlayMode.setBackgroundResource(R.drawable.bg_btn_one);
                    myBinder.mService.isLoopPlaying = true;
                }
                break;

            case R.id.btn_love:
                if(User.getInstance().getLogin()){
                    MusicInfo musicInfo = myBinder.getPlayMusicInfo();
                    if(musicInfo.isLoved()){
                        musicInfo.setLoved(false);
                        Toast.makeText(getApplicationContext(), "为您取消收藏", Toast.LENGTH_SHORT).show();
                    }else {
                        musicInfo.setLoved(true);
                        Toast.makeText(getApplicationContext(), "为您收藏本歌", Toast.LENGTH_SHORT).show();
                    }
                    updateBtnLoved(musicInfo,true);

                }else{
                    showTips();
                }
                break;

            case R.id.rl_slide_menu_header:
                mDrawerLayout.closeDrawers();
                Intent intent = new Intent();
                intent.putExtra(Constants.EXTRA_THEME,mThemeNum);
                if(User.getInstance().getLogin()){//如果已经登录就进入用户页面，如果没有就进入登录页面
                    intent.setClass(MainActivity.this,UserActivity.class);
                    startActivityForResult(intent, Constants.REQUEST_USER_CODE);
                }else {
                    intent.setClass(MainActivity.this,LoginActivity.class);
                    startActivityForResult(intent, Constants.REQUEST_LOGIN_CODE);
                }
                break;

            default:
                break;
        }
    }

    private void showTips() {
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("尚未登录，是否马上登录?")
                .setCancelText("不了，下次")
                .setConfirmText("马上登录")
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        Intent intent = new Intent();
                        intent.putExtra(Constants.EXTRA_THEME,mThemeNum);
                        intent.setClass(MainActivity.this, LoginActivity.class);
                        startActivityForResult(intent, Constants.REQUEST_LOGIN_CODE);
                        sDialog.dismiss();
                    }
                }).show();
    }

    /**
     * 用户更新喜欢按钮的背景和跟新用户的喜欢歌曲
     * @param musicInfo
     */
    private void updateBtnLoved(MusicInfo musicInfo,boolean isNeedUpdate){

        if(musicInfo.isLoved()){//设置背景图
            btnLove.setBackgroundResource(R.drawable.bg_btn_loved);
        }else {
            btnLove.setBackgroundResource(R.drawable.bg_btn_love);
        }

        if(isNeedUpdate){//是否需要更新，这里一直为帧
            Log.v(TAG,"updateBtnLoved");
            //上传服务器
            if (User.getInstance().getLogin()) {
                if (musicInfo.getKey() != null) {
                    try {
                        if(musicInfo.isLoved()){
                            MusicInfoUtils.uploadUserOp("favor", musicInfo);
                        }else{
                            MusicInfoUtils.uploadUserOp("dislike", musicInfo);
                        }

                    } catch (AuthFailureError authFailureError) {
                        authFailureError.printStackTrace();
                    }
                }
            }
        }


    }





    private void initDrawerMenu(){
        mDrawerList = (ListView) findViewById(R.id.lv_nav_drawer);
        mDrawerList.setVerticalScrollBarEnabled(false);//使子列表可以垂直滚动
    }


    //通过回调接口，更新播放的UI，主要在mybind里面回调此接口进行更新
    @Override
    public void updatePlayViewUI(int mode, int percent) {
        //Log.v(TAG,"updatePlayViewUI");
        switch (mode){
            case NetworkConstans.STATE_START_PLAY_MUSIC:
                ivNeedle.startAnimation(needleUpDownAnim);
                mRotateAnimator.play();
                startUpdateSeekBar();
                break;
            case NetworkConstans.STATE_PLAYING_MUSIC:
                ivNeedle.startAnimation(needleDownAnim);
                mRotateAnimator.play();
                btnPlayMusic.setBackgroundResource(R.drawable.btn_stop_play);
                break;
            case NetworkConstans.STATE_STOP_MUSIC:
                ivNeedle.startAnimation(needleUpAnim);
                mRotateAnimator.pause();
                btnPlayMusic.setBackgroundResource(R.drawable.btn_start_play);
                break;
            case NetworkConstans.UPDATE_MUSIC_INFO:
                MusicInfo musicInfo = myBinder.getPlayMusicInfo();
                if(musicInfo != null){
                    toolbar.setTitle(musicInfo.getTitle());
                    toolbar.setSubtitle(musicInfo.getArtist());
                    startUpdateSeekBar();
                    updateBtnLoved(musicInfo,false);
                }
                break;
            case NetworkConstans.UPDATE_MUSIC_COVER:
                Bitmap coverBitmap = myBinder.getConverBitmap();
                if(coverBitmap != null){
                    ivDiskCover.setImageBitmap(mRotateAnimator.getCroppedBitmap(coverBitmap));
                }
                break;
            case NetworkConstans.STATE_CHANGE_MUSIC:
                Log.v(TAG,"mRotateAnimator.reset");
                break;
            case NetworkConstans.UPDATE_SHOW_DIALOG:
                showNoNetworkDialog();
                break;
            case NetworkConstans.UPDATE_BUF_PECENT://设置缓冲进度条
                seekBar.setSecondaryProgress(mMusicDuration * percent / 100);
                break;
            case NetworkConstans.CHECK_IS_ERROR:
                if(myBinder.isError){
                    myBinder.isError = false;
                    showNoNetworkDialog();
                }
                break;
            default:
                break;
        }
    }

    private void startUpdateSeekBar(){
        Log.v(TAG,"startUpdateSeekBar");
        if(myBinder.mService.isPlaying){
            updatePlayViewUI(NetworkConstans.STATE_PLAYING_MUSIC,0);
        }else {
            updatePlayViewUI(NetworkConstans.STATE_STOP_MUSIC,0);
        }

        mMusicDuration = myBinder.mService.mediaPlayer.getDuration();
        tvTotalTime.setText(TimeFormat.msToMinAndS(mMusicDuration));
        tvCurrentTime.setText(TimeFormat.msToMinAndS(0));
        seekBar.setMax(mMusicDuration);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                myPlayViewHandler.sendEmptyMessage(NetworkConstans.STATE_PLAYING_MUSIC);
            }
        },200,200);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(llLeftSlideMenu)) {//如果关闭就打开，如果打开就关闭
                mDrawerLayout.closeDrawer(llLeftSlideMenu);
                mDrawerLayout.setFocusableInTouchMode(true);//主页面的交点失去，变暗
            } else {
                mDrawerLayout.openDrawer(llLeftSlideMenu);
                mDrawerLayout.setFocusableInTouchMode(false);
            }
        }else if (item.getItemId() == R.id.app_about_team) {
            new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("DouFM - Android客户端3.0")
                    .setContentText(getResources().getString(R.string.title_activity_about))
                    .show();
        }if (item.getItemId() == R.id.switch_theme) {
            int colorIndex = (int) (Math.random() * colorNum);
            if (colorIndex == colorNum) {
                colorIndex--;
            }
            if (colorIndex < 0) {
                colorIndex = 0;
            }
            toolbar.setBackgroundColor(Color.parseColor(Constants.ACTIONBAR_COLORS[colorIndex]));
            mDrawerLayout.setBackgroundColor(Color.parseColor(Constants.BACKGROUND_COLORS[colorIndex]));
            mThemeNum = colorIndex;
            SharedPreferencesUtils.putInt(MainActivity.this, Constants.THEME, mThemeNum);
            SharedPreferencesUtils.putInt(MainActivity.this, Constants.PLAYLIST, mPlayListNum);//几号频道
            updateRLUserLoginBg();//更新用户登录背景
        }

        return super.onOptionsItemSelected(item);
    }

    @Override//点击频道列表
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(myBinder != null){
            myBinder.mService.changeChannle(i);//切换到位置i的频道
            mDrawerLayout.closeDrawers();//关闭抽屉
            mPlayListNum = i;//记录频道序号
            mDrawerList.setSelection(i);//选中当前条目
        }
    }

    private void createServiceConnection(){
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if(myBinder == null){
                    Log.v(TAG,"onServiceConnected");
                    myBinder = (MyBinder) service;//自定义配置IBinder,主要实现与远程对象的交互接口，在这里获取了bind,从而获取了service的引用
                    myBinder.setUIUpdateInterface(MainActivity.this);//继承传导设置接口，可以在myBinder里面调用这个接口，但实现却在这里，这也是回调接口的意思，当然也可以在这里调用
                    //更新消息
                    updatePlayViewUI(NetworkConstans.UPDATE_MUSIC_INFO, 0);//调用接口，实现在下面
                    updatePlayViewUI(NetworkConstans.UPDATE_MUSIC_COVER,0);
                    updatePlayViewUI(NetworkConstans.CHECK_IS_ERROR, 0);
                    myBinder.setMainActivity(MainActivity.this);//设置他的绑定Activity
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                myBinder.setMusicChannelList(mDrawerList);
                            }catch (IllegalStateException e){  //网络可能会不畅通
                                Log.v(TAG,e.getMessage());
                                e.printStackTrace();
                            }

                        }
                    }).start();

                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                myBinder = null;
            }
        };
    }

    private void updateRLUserLoginBg() {
        rlUserLogin.setBackgroundResource(Constants.SLIDE_MENU_HEADERS[SharedPreferencesUtils.getInt(this, Constants.THEME, 11)]);
    }

    private void updateLoginArea(){
        updateRLUserLoginBg();
        if(User.getInstance().getLogin()){
            tvUserLoginTitle.setText("您好," + User.getInstance().getUserName());
        } else {
            tvUserLoginTitle.setText("请使用睿思账号登陆");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case Constants.REQUEST_LOGIN_CODE:
                if(resultCode == LoginActivity.LOGIN_SUCCESS){
                    updateLoginArea();
                    AsyncTask task = new AsyncTask() {
                        @Override
                        protected Object doInBackground(Object[] params) {
                            MusicInfoUtils.downloadLoveList(MainApplication.mContext);
                            return null;
                        }
                    };
                    task.execute();

                }
            case Constants.REQUEST_USER_CODE:
                if(resultCode == 100){
                    updateLoginArea();
                }
                break;
            case Constants.REQUEST_WIFI_SETTING_CODE:
                mErrorDialog.dismiss();
                break;
        }
    }

    private void showNoNetworkDialog(){
        if(mErrorDialog == null){
            mErrorDialog = new SweetAlertDialog(MainActivity.this,SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("网络连接出错啦...")
                    .setCancelText("设置网络")
                    .setConfirmText("退出应用")
                    .showCancelButton(true)
                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            Intent intent = null;
                            if (Build.VERSION.SDK_INT > 11) {
                                //android 3.0 以上版本跳转至Wifi设置界面
                                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            } else {
                                //android 3.0 一下版本跳转至Wifi设置界面方法
                                intent = new Intent();
                                ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.WirelessSettings");
                                intent.setComponent(componentName);
                                intent.setAction("android.intent.action.VIEW");
                            }
                            startActivityForResult(intent, Constants.REQUEST_WIFI_SETTING_CODE);
                        }
                    })
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            MainActivity.this.finish();
                        }
                    });

        }
        if(!mErrorDialog.isShowing()){
            mErrorDialog.show();
        }
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }//在栈中向后移动

    //更新seekbar的位置
    private class MyPlayViewHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case NetworkConstans.STATE_PLAYING_MUSIC:
                    try{
                        if(myBinder == null)
                            return;
                        int current = myBinder.mService.mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(current);
                    }catch (IllegalStateException e){
                        timer.cancel();
                    }

                    break;
            }


        }
    }


}
