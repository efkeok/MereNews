package androidnews.kiloproject.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.astuetz.PagerSlidingTabStrip;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.bumptech.glide.Glide;

import androidnews.kiloproject.fragment.BaseRvFragment;
import androidnews.kiloproject.fragment.ITHomeRvFragment;
import androidnews.kiloproject.util.FileCompatUtil;
import androidnews.kiloproject.util.GlideUtil;
import androidnews.kiloproject.util.ITHomeUtils;
import androidnews.kiloproject.widget.materialviewpager.MaterialViewPager;
import androidnews.kiloproject.widget.materialviewpager.header.HeaderDesign;

import com.jude.swipbackhelper.SwipeBackHelper;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidnews.kiloproject.R;
import androidnews.kiloproject.bean.net.PhotoCenterData;
import androidnews.kiloproject.fragment.GuoKrRvFragment;
import androidnews.kiloproject.fragment.VideoRvFragment;
import androidnews.kiloproject.fragment.ZhihuRvFragment;
import androidnews.kiloproject.fragment.MainRvFragment;
import androidnews.kiloproject.event.MessageEvent;
import androidnews.kiloproject.system.base.BaseActivity;


import cn.jzvd.Jzvd;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static androidnews.kiloproject.bean.data.CacheNews.CACHE_COLLECTION;
import static androidnews.kiloproject.bean.data.CacheNews.CACHE_HISTORY;
import static androidnews.kiloproject.fragment.BaseRvFragment.TYPE_REFRESH;
import static androidnews.kiloproject.system.AppConfig.CONFIG_BACK_EXIT;
import static androidnews.kiloproject.system.AppConfig.CONFIG_HEADER_COLOR;
import static androidnews.kiloproject.system.AppConfig.CONFIG_NIGHT_MODE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_RANDOM_HEADER;
import static androidnews.kiloproject.system.AppConfig.CONFIG_TYPE_ARRAY;
import static androidnews.kiloproject.system.AppConfig.NEWS_PHOTO_URL;
import static androidnews.kiloproject.system.AppConfig.TYPE_GUOKR;
import static androidnews.kiloproject.system.AppConfig.TYPE_VIDEO_END;
import static androidnews.kiloproject.system.AppConfig.TYPE_VIDEO_START;
import static androidnews.kiloproject.system.AppConfig.TYPE_ZHIHU;
import static androidnews.kiloproject.system.AppConfig.isNightMode;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    MaterialViewPager mViewPager;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    NavigationView navigation;
    FragmentStatePagerAdapter mPagerAdapter;

    PhotoCenterData photoData;
    int bgPosition = 0;
    int[] channelArray = new int[DEFAULT_PAGE];
    List<BaseRvFragment> fragments = new ArrayList<>();

    public static final int DEFAULT_PAGE = 4;

    private SPUtils spUtils;

    //分别为开始颜色，中间夜色，结束颜色
    public static final int colors1[] = {Color.parseColor("#a48ce0"),
            Color.parseColor("#8eadfd"),
            Color.parseColor("#6eead2")};
    public static final int colors2[] = {Color.parseColor("#16194c"),
            Color.parseColor("#214599"),
            Color.parseColor("#2386a5")};
    public static final int colors3[] = {Color.parseColor("#83ccd2"),
            Color.parseColor("#c3e7e7"),
            Color.parseColor("#ffffff")};
    public static final int colors4[] = {Color.parseColor("#a13dff"),
            Color.parseColor("#5f86fd"),
            Color.parseColor("#0578f9")};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewPager = (MaterialViewPager) findViewById(R.id.materialViewPager);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigation = (NavigationView) findViewById(R.id.navigation);

        spUtils = SPUtils.getInstance();
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 0, 0);
        drawerLayout.addDrawerListener(mDrawerToggle);
        final Toolbar toolbar = mViewPager.getToolbar();
        initToolbar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_add_lib:
                        startActivityForResult(new Intent(mActivity, ChannelActivity.class), SELECT_RESULT);
                        break;
                }
                return false;
            }
        });

        mViewPager.getPagerTitleStrip().setOnTabReselectedListener(new PagerSlidingTabStrip.OnTabReselectedListener() {
            @Override
            public void onTabReselected(int position) {
                BaseRvFragment fragment = fragments.get(position);
                if ( fragment != null)
                    fragment.requestData(TYPE_REFRESH);
            }
        });
        navigation.setNavigationItemSelectedListener(this);
        ColorStateList csl = getBaseContext().getResources().getColorStateList(R.color.navigation_menu_item_color);
        navigation.setItemTextColor(csl);

        SwipeBackHelper.getCurrentPage(this).setSwipeBackEnable(false);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void initSlowly() {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                String arrayStr = spUtils.getString(CONFIG_TYPE_ARRAY);
                if (TextUtils.isEmpty(arrayStr)) {
                    channelArray = new int[DEFAULT_PAGE];
                    for (int i = 0; i < DEFAULT_PAGE; i++) {
                        channelArray[i] = i;
                    }
                    e.onNext(true);
                } else {
                    String[] channelStrArray = arrayStr.split("#");
                    channelArray = new int[channelStrArray.length];
                    for (int i = 0; i < channelStrArray.length; i++) {
                        channelArray[i] = Integer.parseInt(channelStrArray[i]);
                    }
                    e.onNext(true);
                }
                saveChannel(channelArray);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            if (mPagerAdapter == null) {
                                mPagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
                                    @Override
                                    public Fragment getItem(int position) {
                                        BaseRvFragment fragment = createFragment(position);
                                        fragments.add(fragment);
                                        return fragment;
                                    }

                                    @Override
                                    public int getCount() {
                                        return channelArray.length;
                                    }

                                    @Override
                                    public CharSequence getPageTitle(int position) {
                                        String[] tags = getResources().getStringArray(R.array.address_tag);
                                        return tags[channelArray[position]];
                                    }
                                };
                                mViewPager.getViewPager().setAdapter(mPagerAdapter);

                                mViewPager.getViewPager().setOffscreenPageLimit(mViewPager.getViewPager().getAdapter().getCount());
                                mViewPager.getPagerTitleStrip().setViewPager(mViewPager.getViewPager());

                                EasyHttp.get(NEWS_PHOTO_URL)
                                        .readTimeOut(30 * 1000)//局部定义读超时
                                        .writeTimeOut(30 * 1000)
                                        .connectTimeout(30 * 1000)
                                        .timeStamp(true)
                                        .execute(new SimpleCallBack<String>() {
                                            @Override
                                            public void onError(ApiException e) {
                                                e.printStackTrace();
                                            }

                                            @Override
                                            public void onSuccess(final String s) {
                                                Observable.create(new ObservableOnSubscribe<Boolean>() {
                                                    @Override
                                                    public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                                                        String temp = s.replace(")", "}");
                                                        String response = temp.replace("cacheMoreData(", "{\"cacheMoreData\":");
                                                        if (!TextUtils.isEmpty(response) || TextUtils.equals(response, "{}")) {
                                                            try {
                                                                photoData = gson.fromJson(response, PhotoCenterData.class);
                                                                e.onNext(true);
                                                            } catch (Exception e1) {
                                                                e1.printStackTrace();
                                                                e.onNext(false);
                                                            }
                                                        } else e.onNext(false);
                                                        e.onComplete();
                                                    }
                                                }).subscribeOn(Schedulers.computation())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(new Consumer<Boolean>() {
                                                            @Override
                                                            public void accept(Boolean aBoolean) throws Exception {
                                                                if (aBoolean)
                                                                    startBgAnimate();
                                                                else
                                                                    SnackbarUtils.with(mViewPager).setMessage(getString(R.string.server_fail)).showError();
                                                            }
                                                        });
                                            }
                                        });
                            } else {
                                mPagerAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Glide.with(mActivity).resumeRequests();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_items, menu);//加载menu布局
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.nav_his:
                intent = new Intent(mActivity, CacheActivity.class);
                intent.putExtra("type", CACHE_HISTORY);
                startActivity(intent);
                break;
            case R.id.nav_coll:
                intent = new Intent(mActivity, CacheActivity.class);
                intent.putExtra("type", CACHE_COLLECTION);
                startActivity(intent);
                break;
            case R.id.nav_block:
                intent = new Intent(mActivity, BlockActivity.class);
                startActivityForResult(intent, BLOCK_RESULT);
                break;
            case R.id.nav_setting:
                intent = new Intent(mActivity, SettingActivity.class);
                startActivityForResult(intent, SETTING_RESULT);
                break;
            case R.id.nav_about:
                intent = new Intent(mActivity, AboutActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_theme:
                if (isNightMode) {
                    isNightMode = false;
                } else {
                    isNightMode = true;
                }
                spUtils.put(CONFIG_NIGHT_MODE, isNightMode);
                intent = getIntent();
                finish();
                startActivity(intent);
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) ||
                super.onOptionsItemSelected(item);
    }

    private long firstTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (Jzvd.backPress()) {
                return true;
            }
            if (spUtils.getBoolean(CONFIG_BACK_EXIT)
                    && System.currentTimeMillis() - firstTime > 2000) {
                SnackbarUtils.with(mViewPager).setMessage(getString(R.string.click_to_exit)).show();
                firstTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_RESULT:
                if (resultCode == RESULT_OK) {
                    data = getIntent();
                    finish();
                    startActivity(data);
                }
                break;
            case SETTING_RESULT:
                if (resultCode == RESULT_OK)
                    initSlowly();
                break;
            case BLOCK_RESULT:
                if (resultCode == RESULT_OK)
                    SnackbarUtils.with(mViewPager)
                            .setMessage(getResources()
                                    .getString(R.string.start_after_restart_list))
                            .show();
                break;
        }
    }

    private BaseRvFragment createFragment(int position){
        int type = channelArray[position];
        if (type >= TYPE_ZHIHU) {
            switch (type) {
                case TYPE_ZHIHU:
                    return new ZhihuRvFragment();
                case TYPE_GUOKR:
                    return new GuoKrRvFragment();
            }
            if (type >= TYPE_VIDEO_START && type <= TYPE_VIDEO_END)
                return VideoRvFragment.newInstance(channelArray[position]);
            else
                return ITHomeRvFragment.newInstance(channelArray[position]);
        }
        return MainRvFragment.newInstance(channelArray[position]);
    }

    Timer timer;
    TimerTask timerTask;

    private void startBgAnimate() {
        switch (spUtils.getInt(CONFIG_RANDOM_HEADER, 0)) {
            case 0:
                timer = new Timer();
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (mViewPager != null && photoData != null) {
                                        if (bgPosition == photoData.getCacheMoreData().size()) {
                                            bgPosition = 0;
                                        }
                                        mViewPager.setImageUrl(photoData.getCacheMoreData().get(bgPosition).getCover(), 300);
                                        bgPosition++;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };
                timer.schedule(timerTask, 0, 10 * 1000);
                break;
            case 1:
                mViewPager.setMaterialViewPagerListener(new MaterialViewPager.Listener() {
                    @Override
                    public HeaderDesign getHeaderDesign(int page) {
                        int postion = page % photoData.getCacheMoreData().size();

                        return HeaderDesign.fromColorResAndUrl(
                                R.color.deepskyblue,
                                photoData.getCacheMoreData().get(postion).getCover());
                    }
                    //execute others actions if needed (ex : modify your header logo)
                });
                mViewPager.setImageUrl(photoData.getCacheMoreData().get(0).getCover(), 200);
                break;
            case 2:
                mViewPager.setMaterialViewPagerListener(new MaterialViewPager.Listener() {
                    @Override
                    public HeaderDesign getHeaderDesign(int page) {
                        switch (page % 4) {
                            case 0:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.deepskyblue,
                                        new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors1));
                            case 1:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.deepskyblue,
                                        new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors2));
                            case 2:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.deepskyblue,
                                        new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors3));
                            case 3:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.deepskyblue,
                                        new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors4));
                        }
                        return null;
                    }
                    //execute others actions if needed (ex : modify your header logo)
                });
                mViewPager.setImageDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors1), 200);
                break;
            case 3:
                mViewPager.setMaterialViewPagerListener(new MaterialViewPager.Listener() {
                    @Override
                    public HeaderDesign getHeaderDesign(int page) {
                        switch (page % 5) {
                            case 0:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.slateblue,
                                        getResources().getDrawable(R.color.mediumslateblue));
                            case 1:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.goldenrod,
                                        getResources().getDrawable(R.color.gold));
                            case 2:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.palegreen,
                                        getResources().getDrawable(R.color.mediumseagreen));
                            case 3:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.firebrick,
                                        getResources().getDrawable(R.color.orangered));
                            case 4:
                                return HeaderDesign.fromColorResAndDrawable(
                                        R.color.steelblue,
                                        getResources().getDrawable(R.color.deepskyblue));
                        }
                        return null;
                    }
                    //execute others actions if needed (ex : modify your header logo)
                });
                mViewPager.setImageDrawable(getResources().getDrawable(R.color.deepskyblue), 200);
                break;
            case 4:
                int color = spUtils.getInt(CONFIG_HEADER_COLOR, 9999);
                if (color == 9999){
                    color = Color.parseColor("#FFA000");
                }
                mViewPager.setColor(color,200);
                break;
        }
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public static boolean saveChannel(int[] channelArray) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Integer integer : channelArray) {
                sb.append(integer + "#");
            }
            SPUtils.getInstance().put(CONFIG_TYPE_ARRAY, sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {/* Do something */}

    @Override
    protected void onPause() {
        super.onPause();
        Jzvd.releaseAllVideos();
        Glide.with(mActivity).pauseRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (spUtils.getInt(CONFIG_RANDOM_HEADER, 0) == 0
                && timer != null
                && timerTask != null)
            cancelTimer();
        EventBus.getDefault().unregister(this);
    }
}
