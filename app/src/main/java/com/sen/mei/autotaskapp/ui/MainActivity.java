package com.sen.mei.autotaskapp.ui;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.sen.mei.autotaskapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;
    private final List<String> fragmentTitleList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("AutoApp");

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // 检查并申请所需权限
        checkAndRequestPermissions();

        // 设置ViewPager2的适配器
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(new RecordActionsFragment());
        adapter.addFragment(new AutoTasksFragment());
        viewPager.setAdapter(adapter);

        // 使用TabLayoutMediator连接TabLayout和ViewPager2，并设置标签文本
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(getPageTitle(position)))
                .attach();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        //as you provide a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // 这里可添加跳转到设置页面的具体逻辑，暂时先留空
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public CharSequence getPageTitle(int position) {
        return fragmentTitleList.get(position);
    }

    class ViewPagerAdapter extends FragmentStateAdapter {

        private final List<Fragment> fragmentList = new ArrayList<>();

        public ViewPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getItemCount() {
            return fragmentList.size();
        }

        public void addFragment(Fragment fragment) {
            fragmentList.add(fragment);
            if (fragment instanceof RecordActionsFragment) {
                fragmentTitleList.add(getString(R.string.record_actions_tab_title));
            } else if (fragment instanceof AutoTasksFragment) {
                fragmentTitleList.add(getString(R.string.auto_tasks_tab_title));
            }
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // 检查读取外部存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        // 检查写入外部存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        // 检查通知权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i]!= PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "所有权限已成功获取", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "部分权限获取失败，请在您的操作中手动授予", Toast.LENGTH_SHORT).show();
            }
        }
    }
}