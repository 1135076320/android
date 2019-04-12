package mega.privacy.android.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import mega.privacy.android.app.components.EditTextPIN;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.SMSVerificationActivity.ENTERED_PHONE_NUMBER;
import static mega.privacy.android.app.SMSVerificationActivity.SELECTED_COUNTRY_CODE;
import static mega.privacy.android.app.lollipop.LoginFragmentLollipop.NAME_USER_LOCKED;

public class SMSVerificationReceiveTxtActivity extends PinActivityLollipop implements MegaRequestListenerInterface, View.OnClickListener, View.OnLongClickListener, View.OnFocusChangeListener {

    private Toolbar toolbar;
    private ActionBar actionBar;
    private TextView backButton, pinError, resendTextView;
    private Button confirmButton;
    private EditTextPIN firstPin, secondPin, thirdPin, fourthPin, fifthPin, sixthPin;
    private LinearLayout inputContainer;
    private InputMethodManager imm;
    private boolean isErrorShown, firstTime, pinLongClick, allowResend, isUserLocked;
    private final int RESEND_TIME_LIMIT = 30 * 1000;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);

        //navigation bar
        setContentView(R.layout.activity_sms_verification_receive_txt);
        toolbar = findViewById(R.id.account_verification_toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.verify_account_enter_code_title).toUpperCase());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getColor(R.color.dark_primary_color));
        }

        //labels
        Intent intent = getIntent();
        if (intent != null) {
            String phoneNumber = "(" + intent.getStringExtra(SELECTED_COUNTRY_CODE) + ") " + intent.getStringExtra(ENTERED_PHONE_NUMBER);
            TextView phoneNumberLbl = findViewById(R.id.entered_phone_number);
            phoneNumberLbl.setText(phoneNumber);

            isUserLocked = intent.getBooleanExtra(NAME_USER_LOCKED,false);
        }

        //resend
        resendTextView = findViewById(R.id.verify_account_resend);
        String text = getResources().getString(R.string.verify_account_resend_label);
        int start = text.length();
        text += getResources().getString(R.string.verify_account_resend_button);
        int end = text.length();
        SpannableString spanString = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                backButtonClicked();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(getResources().getColor(R.color.accentColor));
                ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            }
        };
        spanString.setSpan(clickableSpan,start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        resendTextView.setText(spanString);
        resendTextView.setMovementMethod(LinkMovementMethod.getInstance());
        resendTextView.setHighlightColor(Color.TRANSPARENT);

        //buttons
        backButton = findViewById(R.id.verify_account_back_button);
        backButton.setOnClickListener(this);
        confirmButton = findViewById(R.id.verify_account_confirm_button);
        confirmButton.setOnClickListener(this);

        hideResendAndBackButton();
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                allowResend = true;
                showResendAndBackButton();
            }
        },RESEND_TIME_LIMIT);

        pinError = findViewById(R.id.verify_account_pin_error);

        //input fields
        imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        inputContainer = findViewById(R.id.verify_account_input_code_layout);
        firstPin = findViewById(R.id.verify_account_input_code_first);
        firstPin.setOnLongClickListener(this);
        firstPin.setOnFocusChangeListener(this);
        firstPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {

            }

            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (firstPin.length() != 0) {
                    secondPin.requestFocus();
                    secondPin.setCursorVisible(true);

                    if (firstTime && !pinLongClick) {
                        secondPin.setText("");
                        thirdPin.setText("");
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
                    } else if (pinLongClick) {
                        pasteClipboard();
                    }
                }
                if (isErrorShown) {
                    hideError();
                }
            }
        });

        secondPin = findViewById(R.id.verify_account_input_code_second);
        secondPin.setOnLongClickListener(this);
        secondPin.setOnFocusChangeListener(this);
        secondPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {

            }

            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (secondPin.length() != 0) {
                    thirdPin.requestFocus();
                    thirdPin.setCursorVisible(true);

                    if (firstTime && !pinLongClick) {
                        thirdPin.setText("");
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
                    } else if (pinLongClick) {
                        pasteClipboard();
                    }
                }
                if (isErrorShown) {
                    hideError();
                }
            }
        });

        thirdPin = findViewById(R.id.verify_account_input_code_third);
        thirdPin.setOnLongClickListener(this);
        thirdPin.setOnFocusChangeListener(this);
        thirdPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {

            }

            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (thirdPin.length() != 0) {
                    fourthPin.requestFocus();
                    fourthPin.setCursorVisible(true);

                    if (firstTime && !pinLongClick) {
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
                    } else if (pinLongClick) {
                        pasteClipboard();
                    }
                }
                if (isErrorShown) {
                    hideError();
                }
            }
        });

        fourthPin = findViewById(R.id.verify_account_input_code_fourth);
        fourthPin.setOnLongClickListener(this);
        fourthPin.setOnFocusChangeListener(this);
        fourthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {

            }

            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (fourthPin.length() != 0) {
                    fifthPin.requestFocus();
                    fifthPin.setCursorVisible(true);

                    if (firstTime && !pinLongClick) {
                        fifthPin.setText("");
                        sixthPin.setText("");
                    } else if (pinLongClick) {
                        pasteClipboard();
                    }
                }
                if (isErrorShown) {
                    hideError();
                }
            }
        });

        fifthPin = findViewById(R.id.verify_account_input_code_fifth);
        fifthPin.setOnLongClickListener(this);
        fifthPin.setOnFocusChangeListener(this);
        fifthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {

            }

            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (fifthPin.length() != 0) {
                    sixthPin.requestFocus();
                    sixthPin.setCursorVisible(true);

                    if (firstTime && !pinLongClick) {
                        sixthPin.setText("");
                    } else if (pinLongClick) {
                        pasteClipboard();
                    }
                }
                if (isErrorShown) {
                    hideError();
                }
            }
        });

        sixthPin = findViewById(R.id.verify_account_input_code_sixth);
        sixthPin.setOnLongClickListener(this);
        sixthPin.setOnFocusChangeListener(this);
        sixthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {

            }

            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (sixthPin.length() != 0) {
                    sixthPin.setCursorVisible(true);
                    Util.hideKeyboard(SMSVerificationReceiveTxtActivity.this);

                    if (pinLongClick) {
                        pasteClipboard();
                    }
                }
                if (isErrorShown) {
                    hideError();
                }
            }
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        firstPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb1 = firstPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb1.width = Util.scaleWidthPx(42,outMetrics);
        } else {
            paramsb1.width = Util.scaleWidthPx(25,outMetrics);
        }
        firstPin.setLayoutParams(paramsb1);
        LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)firstPin.getLayoutParams();
        textParams.setMargins(0,0,Util.scaleWidthPx(8,outMetrics),0);
        firstPin.setLayoutParams(textParams);

        secondPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb2 = secondPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb2.width = Util.scaleWidthPx(42,outMetrics);
        } else {
            paramsb2.width = Util.scaleWidthPx(25,outMetrics);
        }
        secondPin.setLayoutParams(paramsb2);
        textParams = (LinearLayout.LayoutParams)secondPin.getLayoutParams();
        textParams.setMargins(0,0,Util.scaleWidthPx(8,outMetrics),0);
        secondPin.setLayoutParams(textParams);
        secondPin.setEt(firstPin);

        thirdPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb3 = thirdPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb3.width = Util.scaleWidthPx(42,outMetrics);
        } else {
            paramsb3.width = Util.scaleWidthPx(25,outMetrics);
        }
        thirdPin.setLayoutParams(paramsb3);
        textParams = (LinearLayout.LayoutParams)thirdPin.getLayoutParams();
        textParams.setMargins(0,0,Util.scaleWidthPx(25,outMetrics),0);
        thirdPin.setLayoutParams(textParams);
        thirdPin.setEt(secondPin);

        fourthPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb4 = fourthPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb4.width = Util.scaleWidthPx(42,outMetrics);
        } else {
            paramsb4.width = Util.scaleWidthPx(25,outMetrics);
        }
        fourthPin.setLayoutParams(paramsb4);
        textParams = (LinearLayout.LayoutParams)fourthPin.getLayoutParams();
        textParams.setMargins(0,0,Util.scaleWidthPx(8,outMetrics),0);
        fourthPin.setLayoutParams(textParams);
        fourthPin.setEt(thirdPin);

        fifthPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb5 = fifthPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb5.width = Util.scaleWidthPx(42,outMetrics);
        } else {
            paramsb5.width = Util.scaleWidthPx(25,outMetrics);
        }
        fifthPin.setLayoutParams(paramsb5);
        textParams = (LinearLayout.LayoutParams)fifthPin.getLayoutParams();
        textParams.setMargins(0,0,Util.scaleWidthPx(8,outMetrics),0);
        fifthPin.setLayoutParams(textParams);
        fifthPin.setEt(fourthPin);

        sixthPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb6 = sixthPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb6.width = Util.scaleWidthPx(42,outMetrics);
        } else {
            paramsb6.width = Util.scaleWidthPx(25,outMetrics);
        }
        sixthPin.setLayoutParams(paramsb6);
        textParams = (LinearLayout.LayoutParams)sixthPin.getLayoutParams();
        textParams.setMargins(0,0,0,0);
        sixthPin.setLayoutParams(textParams);
        sixthPin.setEt(fifthPin);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                imm.showSoftInput(firstPin,0);
            }
        },900);
    }

    @Override
    public void onBackPressed() {
        log("onBackPressed");
        super.onBackPressed();
        if (allowResend) {
            finish();
        }
    }


    @Override
    public void onClick(View v) {
        log("on click ");
        switch (v.getId()) {
            case R.id.verify_account_back_button: {
                log("verify_account_back_button clicked");
                backButtonClicked();
                break;
            }
            case R.id.verify_account_confirm_button: {
                log("verify_account_confirm_button clicked");
                confirmButtonClicked();
                break;
            }
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        log("onLongClick");
        switch (v.getId()) {
            case R.id.verify_account_input_code_first:
            case R.id.verify_account_input_code_second:
            case R.id.verify_account_input_code_third:
            case R.id.verify_account_input_code_fourth:
            case R.id.verify_account_input_code_fifth:
            case R.id.verify_account_input_code_sixth: {
                pinLongClick = true;
                v.requestFocus();
            }

        }
        return false;
    }

    @Override
    public void onFocusChange(View v,boolean hasFocus) {
        log("onFocusChange");
        switch (v.getId()) {
            case R.id.pass_first: {
                if (hasFocus) {
                    firstPin.setText("");
                }
                break;
            }
            case R.id.pass_second: {
                if (hasFocus) {
                    secondPin.setText("");
                }
                break;
            }
            case R.id.pass_third: {
                if (hasFocus) {
                    thirdPin.setText("");
                }
                break;
            }
            case R.id.pass_fourth: {
                if (hasFocus) {
                    fourthPin.setText("");
                }
                break;
            }
            case R.id.pass_fifth: {
                if (hasFocus) {
                    fifthPin.setText("");
                }
                break;
            }
            case R.id.pass_sixth: {
                if (hasFocus) {
                    sixthPin.setText("");
                }
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            log("nav back pressed");
            backButtonClicked();
        }
        return super.onOptionsItemSelected(item);
    }

    void pasteClipboard() {
        log("pasteClipboard");
        pinLongClick = false;
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null) {
            String code = clipData.getItemAt(0).getText().toString();
            log("code: " + code);
            if (code != null && code.length() == 6) {
                boolean areDigits = true;
                for (int i = 0;i < 6;i++) {
                    if (!Character.isDigit(code.charAt(i))) {
                        areDigits = false;
                        break;
                    }
                }
                if (areDigits) {
                    firstPin.setText("" + code.charAt(0));
                    secondPin.setText("" + code.charAt(1));
                    thirdPin.setText("" + code.charAt(2));
                    fourthPin.setText("" + code.charAt(3));
                    fifthPin.setText("" + code.charAt(4));
                    sixthPin.setText("" + code.charAt(5));
                } else {
                    firstPin.setText("");
                    secondPin.setText("");
                    thirdPin.setText("");
                    fourthPin.setText("");
                    fifthPin.setText("");
                    sixthPin.setText("");
                }
            }
        }
    }

    private void hideError() {
        log("hideError");
        isErrorShown = false;
        pinError.setVisibility(View.GONE);
        firstPin.setTextColor(ContextCompat.getColor(this,R.color.name_my_account));
        secondPin.setTextColor(ContextCompat.getColor(this,R.color.name_my_account));
        thirdPin.setTextColor(ContextCompat.getColor(this,R.color.name_my_account));
        fourthPin.setTextColor(ContextCompat.getColor(this,R.color.name_my_account));
        fifthPin.setTextColor(ContextCompat.getColor(this,R.color.name_my_account));
        sixthPin.setTextColor(ContextCompat.getColor(this,R.color.name_my_account));
    }

    private void showError(String errorMessage) {
        log("showError");
        firstTime = false;
        isErrorShown = true;
        firstPin.setTextColor(ContextCompat.getColor(this,R.color.login_warning));
        secondPin.setTextColor(ContextCompat.getColor(this,R.color.login_warning));
        thirdPin.setTextColor(ContextCompat.getColor(this,R.color.login_warning));
        fourthPin.setTextColor(ContextCompat.getColor(this,R.color.login_warning));
        fifthPin.setTextColor(ContextCompat.getColor(this,R.color.login_warning));
        sixthPin.setTextColor(ContextCompat.getColor(this,R.color.login_warning));
        pinError.setVisibility(View.VISIBLE);
        if (errorMessage != null) {
            log("error message is " + errorMessage);
            pinError.setText(errorMessage);
        }
    }

    private void validateVerificationCode() {
        log("validateVerificationCode");
        if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1
                && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1) {
            Util.hideKeyboard(this);
            StringBuilder sb = new StringBuilder();
            sb.append(firstPin.getText());
            sb.append(secondPin.getText());
            sb.append(thirdPin.getText());
            sb.append(fourthPin.getText());
            sb.append(fifthPin.getText());
            sb.append(sixthPin.getText());
            String pin = sb.toString().trim();
            log("PIN: " + pin);
            if (pin != null) {
                megaApi.checkSMSVerificationCode(pin,this);
                return;
            }
        }
        showError(getString(R.string.verify_account_incorrect_code));
    }

    private void confirmButtonClicked() {
        log("confirmButtonClicked");
        validateVerificationCode();
    }

    private void backButtonClicked() {
        log("backButtonClicked");
        finish();
    }

    private void showResendAndBackButton() {
        log("showResendAndBackButton");
        backButton.setVisibility(View.VISIBLE);
        resendTextView.setVisibility(View.VISIBLE);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void hideResendAndBackButton() {
        log("hideResendAndBackButton");
        backButton.setVisibility(View.GONE);
        resendTextView.setVisibility(View.GONE);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    public static void log(String message) {
        Util.log("SMSVerificationReceiveTxtActivity",message);
    }


    @Override
    public void onRequestStart(MegaApiJava api,MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {

    }

    @Override
    public void onRequestFinish(final MegaApiJava api,MegaRequest request,MegaError e) {
        if (request.getType() == MegaRequest.TYPE_CHECK_SMS_VERIFICATIONCODE) {
            log("send verification code,get " +  e.getErrorCode());
            if (e.getErrorCode() == MegaError.API_EEXPIRED) {
                log("the code has been verified.");
                showError(getString(R.string.verify_account_error_code_verified));
            } else if (e.getErrorCode() == MegaError.API_EACCESS) {
                showError(getString(R.string.verify_account_error_reach_limit));
            } else if (e.getErrorCode() == MegaError.API_EEXIST) {
                showError(getString(R.string.verify_account_error_phone_number_register));
            } else if (e.getErrorCode() == MegaError.API_OK) {
                log("verification successful");
                Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_REFRESH_ADD_PHONE_NUMBER);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                showSnackbar(Constants.SNACKBAR_TYPE,inputContainer,getString(R.string.verify_account_successfully),-1);
                //showing the successful text for 2 secs, then finish itself back to previous page.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //haven't logged in, need to auto-login
                        if(api.getRootNode() == null) {
                            //auto login
                            Intent intent = new Intent(LoginActivityLollipop.AUTO_LOGIN);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        }
                        setResult(RESULT_OK);
                        finish();
                    }
                }, 2000);
            } else {
                log("invalid code");
                showError(getString(R.string.verify_account_error_invalid_code));
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {

    }
}
