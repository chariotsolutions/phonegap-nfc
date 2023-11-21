package com.chariotsolutions.nfc.plugin;


import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class IncomingCallActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler(Looper.myLooper());
    private View mContentView;

    private WebView webView = null;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            if (Build.VERSION.SDK_INT >= 30) {
                mContentView.getWindowInsetsController().hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        binding = ActivityIncomingcallBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());

        mVisible = true;
//        mControlsView = binding.fullscreenContentControls;
//        mContentView = binding.fullscreenContent;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(_getResource(getApplicationContext(), "activity_incomingcall", "layout"));

        if(getSupportActionBar()!=null) {
            this.getSupportActionBar().hide();
        }

        // Set up the user interaction to manually show or hide the system UI.
//        mContentView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                toggle();
//            }
//        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.


        int callAnserButtonId = _getResource(getApplicationContext(), "cl_answer_button", "id");

        Button callAnserButton = findViewById(callAnserButtonId);


        callAnserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent answerMainIntent = new Intent(getApplicationContext(), NfcActivity.class);
                answerMainIntent.putExtra("notification_id", getIntent().getIntExtra("notification_id", 0));
                answerMainIntent.putExtra("data", getIntent().getStringExtra("data"));
                startActivity(answerMainIntent);
                finish();
            }
        });

        int callDeclineButtonId = _getResource(getApplicationContext(), "cl_decline_button", "id");

        Button callDeclineButton = findViewById(callDeclineButtonId);

        callDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        int webviewId = _getResource(getApplicationContext(), "webView", "id");

//        webView = mContentView.findViewById(_getResource(getApplicationContext(), "webView", "id"));
        webView = findViewById(webviewId);
        // Enable JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Enable WebRTC
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Handle WebChromeClient for WebRTC permissions
        webView.setWebChromeClient(new WebChromeClient());

        // Load the HTML/JavaScript content


        // Set a window variable from the activity
        if (getIntent().getStringExtra("data").length() > 0) {
            try {
                JSONObject jo = new JSONObject(getIntent().getStringExtra("data"));
                // disabling this for now
                if (jo.getString("connection_id").length() < 0) {
                    webView.loadUrl("file:///android_asset/peerjs.html");
                    webView.setWebViewClient(new WebViewClient() {

                        public void onPageFinished(WebView view, String url) {
                            try {
                                setWindowVariableFromActivity("connection_id", jo.getString("connection_id"));
                            } catch (JSONException e) {
                            //                throw new RuntimeException(e);
                            }
                        }
                    });

                }
            } catch (JSONException e) {
//                throw new RuntimeException(e);
            }
        }


    }

    private void setWindowVariableFromActivity(String variableName, String variableValue) {
        if (webView != null) {
            String script = "setWindowVariable('" + variableName + "', '" + variableValue + "')";
            webView.evaluateJavascript(script, null);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if (intent.getIntExtra("ACTION", -1) == 0) {
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                int notif_id = intent.getIntExtra("notification_id", 0);
                if (notif_id != 0) {
                    manager.cancel(notif_id);
                    finish();
                }
            }

            if (intent.getIntExtra("ACTION", -1) == 1) {
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                int notif_id = intent.getIntExtra("notification_id", 0);
                if (notif_id != 0) {
                    manager.cancel(notif_id);
                    finish();
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            mContentView.getWindowInsetsController().show(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelNotification();
    }

    private void cancelNotification() {
        int notif_id = getIntent().getIntExtra("notification_id", 0);
        if (notif_id != 0) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(notif_id);
        }
    }
    private int _getResource(Context ctx, String name, String type) {
        String package_name = ctx.getPackageName();
        Resources resources = ctx.getResources();
        return resources.getIdentifier(name, type, package_name);
    }
}