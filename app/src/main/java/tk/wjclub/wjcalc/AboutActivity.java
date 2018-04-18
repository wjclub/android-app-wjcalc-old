package tk.wjclub.wjcalc;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

public class AboutActivity extends AppCompatActivity implements AboutGenericFragment.OnFragmentInteractionListener, AboutLicenseFragment.OnFragmentInteractionListener, AboutUsFragment.OnFragmentInteractionListener {


    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FrameLayout fl = (FrameLayout) findViewById(R.id.fragment2);
            fl.removeAllViews();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    transaction.replace(R.id.fragment2, new AboutGenericFragment()).commit();
                    return true;
                case R.id.navigation_dashboard:
                    transaction.replace(R.id.fragment2, new AboutLicenseFragment()).commit();
                    return true;
                case R.id.navigation_notifications:
                    transaction.replace(R.id.fragment2, new AboutUsFragment()).commit();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment2, new AboutGenericFragment()).commit();
        setContentView(R.layout.activity_about);
        mTextMessage = (TextView) findViewById(R.id.message);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private int fetchColor(@ColorRes int color) {
        return ContextCompat.getColor(this, color);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
