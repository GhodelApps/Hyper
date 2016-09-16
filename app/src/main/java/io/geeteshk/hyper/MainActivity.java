package io.geeteshk.hyper;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.geeteshk.hyper.fragment.AssistFragment;
import io.geeteshk.hyper.fragment.CreateFragment;
import io.geeteshk.hyper.fragment.HelpFragment;
import io.geeteshk.hyper.fragment.ImproveFragment;
import io.geeteshk.hyper.fragment.PilotFragment;
import io.geeteshk.hyper.fragment.SettingsFragment;
import io.geeteshk.hyper.helper.Pref;

/**
 * Main activity to show all main content
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static Context mContext;
    private static FragmentManager mManager;

    /**
     * Layout that holds NavigationDrawer
     */
    private static DrawerLayout mDrawerLayout;
    /**
     * Toolbar object for activity
     */
    private static Toolbar mToolbar;
    /**
     * Items to be listed in drawer
     */
    private static String[] mItems;
    private static NavigationView mDrawer;
    /**
     * Drawer toggle displayed on toolbar
     */
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * Used to select items in drawer
     *
     * @param context  used to store in preferences
     * @param manager  used to update fragments
     * @param position of selected item
     */
    public static void update(Context context, FragmentManager manager, int position) {
        mDrawerLayout.closeDrawer(GravityCompat.START);
        Fragment fragment = null;

        switch (position) {
            case 0:
                fragment = new CreateFragment();
                break;
            case 1:
                fragment = new ImproveFragment();
                break;
            case 2:
                fragment = new PilotFragment();
                break;
            case 3:
                fragment = new AssistFragment();
                break;
            case 4:
                fragment = new SettingsFragment();
                break;
            case 5:
                fragment = new HelpFragment();
                break;
        }

        if (context == null) {
            context = mContext;
        }

        if (manager == null) {
            manager = mManager;
        }

        manager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitAllowingStateLoss();

        Pref.store(context, "last_fragment", position);
        mToolbar.setTitle(mItems[position]);
        mDrawer.setCheckedItem(mDrawer.getMenu().getItem(position).getItemId());
    }

    /**
     * Used to change theme of application
     *
     * @param activity that is being changed
     * @param dark     theme
     */
    public static void changeTheme(AppCompatActivity activity, boolean dark) {
        Pref.store(activity, "dark_theme", dark);
        activity.recreate();
    }

    /**
     * Called when the activity is first created
     *
     * @param savedInstanceState restore when onResume is called
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File projectDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "Hyper");
        boolean fileTest = projectDir.exists();
        if (!fileTest) {
            fileTest = projectDir.mkdir();
        }

        if (!fileTest) {
            Toast.makeText(this, R.string.project_dir_error, Toast.LENGTH_LONG).show();
            finish();
        }

        if (Pref.get(this, "dark_theme", false)) {
            setTheme(R.style.Hyper_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mItems = getResources().getStringArray(R.array.drawer_items);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        setSupportActionBar(mToolbar);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.action_drawer_open, R.string.action_drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawer = (NavigationView) findViewById(R.id.drawer);
        final List<MenuItem> items = new ArrayList<>();
        Menu menu = mDrawer.getMenu();

        for (int i = 0; i < menu.size(); i++) {
            items.add(menu.getItem(i));
        }

        mDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                if (!item.isChecked()) {
                    item.setChecked(true);
                }

                mDrawerLayout.closeDrawers();
                update(MainActivity.this, getSupportFragmentManager(), items.indexOf(item));
                return true;
            }
        });

        mContext = this;
        mManager = getSupportFragmentManager();
        update(this, getSupportFragmentManager(), Pref.get(this, "last_fragment", 0));
    }

    /**
     * Called after activity is created
     *
     * @param savedInstanceState restored when onResume is called
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    /**
     * Called when config is changed
     *
     * @param newConfig new configuration
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Called when activity is resumed
     */
    @Override
    protected void onResume() {
        super.onResume();
        mToolbar.setTitle(mItems[Pref.get(this, "last_fragment", 0)]);
    }

    /**
     * Called when back button is pressed
     */
    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case 1337:
                MainActivity.update(this, getSupportFragmentManager(), 4);
                break;
            case 1:
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.new_pin);
                EditText editText = new EditText(MainActivity.this);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setPadding(60, 14, 60, 24);
                final TextInputLayout layout = new TextInputLayout(MainActivity.this);
                layout.addView(editText);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(60, 16, 60, 16);
                layout.setLayoutParams(params);
                builder.setView(layout);
                builder.setPositiveButton(R.string.accept, null);
                final AppCompatDialog dialog = builder.create();
                dialog.show();

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        assert layout.getEditText() != null;
                        String newPin = layout.getEditText().getText().toString();
                        if (newPin.length() != 4) {
                            layout.setError(getString(R.string.pin_four_digits));
                        } else {
                            Pref.store(MainActivity.this, "pin", newPin);
                            dialog.dismiss();
                        }
                    }
                });
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
