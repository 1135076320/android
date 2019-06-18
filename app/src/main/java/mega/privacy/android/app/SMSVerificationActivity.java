package mega.privacy.android.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.WebViewActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TL;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaStringList;
import nz.mega.sdk.MegaStringListMap;

import static mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop.COUNTRY_CODE;
import static mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop.COUNTRY_NAME;
import static mega.privacy.android.app.lollipop.CountryCodePickerActivityLollipop.DIAL_CODE;
import static mega.privacy.android.app.lollipop.LoginFragmentLollipop.NAME_USER_LOCKED;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_VERIFY_CODE;

public class SMSVerificationActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface {
    
    public static final String SELECTED_COUNTRY_CODE = "COUNTRY_CODE";
    public static final String ENTERED_PHONE_NUMBER = "ENTERED_PHONE_NUMBER";
    private TextView helperText, selectedCountry, errorInvalidCountryCode, errorInvalidPhoneNumber, titleCountryCode, titlePhoneNumber, notNowButton;
    private View divider1, divider2;
    private ImageView errorInvalidPhoneNumberIcon;
    private RelativeLayout countrySelector;
    private EditText phoneNumberInput;
    private Button nextButton;
    private boolean isSelectedCountryValid, isPhoneNumberValid, isUserLocked, shouldDisableNextButton;
    private String selectedCountryCode, selectedCountryName, selectedDialCode;
    private ArrayList<String> countryCodeList;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("SMSVerificationActivity onCreate");
        super.onCreate(savedInstanceState);
        MegaApplication.smsVerifyShowed(true);
        setContentView(R.layout.activity_sms_verification);
        Intent intent = getIntent();
        if (intent != null) {
            isUserLocked = intent.getBooleanExtra(NAME_USER_LOCKED,false);
        }
        log("is user locked " + isUserLocked);
        
        //divider
        divider1 = findViewById(R.id.verify_account_divider1);
        divider2 = findViewById(R.id.verify_account_divider2);
        
        //titles
        titleCountryCode = findViewById(R.id.verify_account_country_label);
        titlePhoneNumber = findViewById(R.id.verify_account_phone_number_label);
        selectedCountry = findViewById(R.id.verify_account_selected_country);
        
        //set helper text
        helperText = findViewById(R.id.verify_account_helper);
        if (isUserLocked) {
            String text = getResources().getString(R.string.verify_account_helper_locked);
            helperText.setText(text);
            //TODO due to the 'learn more' url is unavailable, comment out the below for now.
//            int start = text.length();
//            text += getResources().getString(R.string.verify_account_helper_learn_more);
//            int end = text.length();
//            SpannableString spanString = new SpannableString(text);
//            ClickableSpan clickableSpan = new ClickableSpan() {
//                @Override
//                public void onClick(View textView) {
//                    //todo open external link
//                    log("Learn more clicked");
//                    String url = "https://mega.nz/terms";
//                    Intent openTermsIntent = new Intent(SMSVerificationActivity.this, WebViewActivityLollipop.class);
//                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    openTermsIntent.setData(Uri.parse(url));
//                    startActivity(openTermsIntent);
//                }
//
//                @Override
//                public void updateDrawState(TextPaint ds) {
//                    super.updateDrawState(ds);
//                    ds.setUnderlineText(false);
//                    ds.setColor(getResources().getColor(R.color.accentColor));
//                    ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
//                }
//            };
//            spanString.setSpan(clickableSpan,start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            helperText.setText(spanString);
//            helperText.setMovementMethod(LinkMovementMethod.getInstance());
//            helperText.setHighlightColor(Color.TRANSPARENT);
        } else {
            boolean isAchievementUser = megaApi.isAchievementsEnabled();
            log("is achievement user: " + isAchievementUser);
            if (isAchievementUser) {
                helperText.setText(R.string.sms_add_phone_number_dialog_msg_achievement_user);
            } else {
                helperText.setText(R.string.sms_add_phone_number_dialog_msg_non_achievement_user);
            }
        }
        
        
        
        //country selector
        countrySelector = findViewById(R.id.verify_account_country_selector);
        countrySelector.setOnClickListener(this);
        
        //phone number input
        phoneNumberInput = findViewById(R.id.verify_account_phone_number_input);
        phoneNumberInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v,int actionId,KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    nextButtonClicked();
                    return true;
                }
                return false;
            }
        });
        phoneNumberInput.setImeActionLabel(getString(R.string.general_create),EditorInfo.IME_ACTION_DONE);
        phoneNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {
            
            }
            
            @Override
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                errorInvalidPhoneNumber.setVisibility(View.GONE);
                errorInvalidPhoneNumberIcon.setVisibility(View.GONE);
                divider2.setBackgroundColor(Color.parseColor("#8A000000"));
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                int inputLength = input == null ? 0 : input.length();
                if (inputLength > 0) {
                    titlePhoneNumber.setTextColor(Color.parseColor("#FF00BFA5"));
                    titlePhoneNumber.setVisibility(View.VISIBLE);
                } else {
                    phoneNumberInput.setHint(R.string.verify_account_phone_number_placeholder);
                    titlePhoneNumber.setVisibility(View.GONE);
                }
            }
        });
        
        //buttons
        nextButton = findViewById(R.id.verify_account_next_button);
        nextButton.setOnClickListener(this);
        
        notNowButton = findViewById(R.id.verify_account_not_now_button);
        notNowButton.setOnClickListener(this);
        
        if(isUserLocked){
            notNowButton.setVisibility(View.GONE);
        }else{
            notNowButton.setVisibility(View.VISIBLE);
        }
        
        //error message and icon
        errorInvalidCountryCode = findViewById(R.id.verify_account_invalid_country_code);
        errorInvalidPhoneNumber = findViewById(R.id.verify_account_invalid_phone_number);
        errorInvalidPhoneNumberIcon = findViewById(R.id.verify_account_invalid_phone_number_icon);
        
        //set saved state
        if(savedInstanceState != null){
            selectedCountryCode = savedInstanceState.getString(COUNTRY_CODE);
            selectedCountryName = savedInstanceState.getString(COUNTRY_NAME);
            selectedDialCode = savedInstanceState.getString(DIAL_CODE);
    
            if(selectedCountryCode != null && selectedCountryName != null && selectedDialCode != null){
                String label = selectedCountryName + " (" + selectedDialCode + ")";
                selectedCountry.setText(label);
                errorInvalidCountryCode.setVisibility(View.GONE);
                titleCountryCode.setVisibility(View.VISIBLE);
                titleCountryCode.setTextColor(Color.parseColor("#FF00BFA5"));
                selectedCountry.setTextColor(Color.parseColor("#DE000000"));
                divider1.setBackgroundColor(Color.parseColor("#8A000000"));
            }
        }
        megaApi.getCountryCallingCodes(this);
    }
    
    @Override
    public void onBackPressed() {
        log("onBackPressed");
        if(isUserLocked){
            return;
        }
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MegaApplication.smsVerifyShowed(false);
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.verify_account_country_selector): {
                log("verify_account_country_selector clicked");
                if (this.countryCodeList != null) {
                    launchCountryPicker();
                } else {
                    //TODO give the chance to re-load
                    log("Country code is not loaded");
                    megaApi.getCountryCallingCodes(this);
                }
                break;
            }
            case (R.id.verify_account_next_button): {
                log("verify_account_next_button clicked");
                nextButtonClicked();
                break;
            }
            case (R.id.verify_account_not_now_button):{
                log("verify_account_not_now_button clicked");
                finish();
                break;
            }
            default:
                break;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == Constants.REQUEST_CODE_COUNTRY_PICKER && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_CODE_COUNTRY_PICKER OK");
            selectedCountryCode = data.getStringExtra(COUNTRY_CODE);
            selectedCountryName = data.getStringExtra(COUNTRY_NAME);
            selectedDialCode = data.getStringExtra(DIAL_CODE);
            
            String label = selectedCountryName + " (" + selectedDialCode + ")";
            selectedCountry.setText(label);
            errorInvalidCountryCode.setVisibility(View.GONE);
            titleCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setTextColor(Color.parseColor("#FF00BFA5"));
            selectedCountry.setTextColor(Color.parseColor("#DE000000"));
            divider1.setBackgroundColor(Color.parseColor("#8A000000"));
        } else if (requestCode == Constants.REQUEST_CODE_VERIFY_CODE && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_CODE_VERIFY_CODE OK");
            TL.log(this,"@#@","onActivityResult REQUEST_CODE_VERIFY_CODE OK");
            setResult(RESULT_OK);
            finish();
        }
    }
    
    private void launchCountryPicker() {
        log("launchCountryPicker");
        Intent intent = new Intent(getApplicationContext(),CountryCodePickerActivityLollipop.class);
        intent.putStringArrayListExtra("country_code", this.countryCodeList);
        startActivityForResult(intent, Constants.REQUEST_CODE_COUNTRY_PICKER);
    }
    
    private void nextButtonClicked() {
        log("nextButtonClicked");
        Util.hideKeyboard(this);
        hideError();
        validateFields();
        if (isPhoneNumberValid && isSelectedCountryValid) {
            log("nextButtonClicked no error");
            hideError();
            RequestTxt();
        } else {
            showCountryCodeValidationError();
            showPhoneNumberValidationError(null);
        }
    }
    
    private void validateFields() {
        log("validateFields");
        //validate phone number
        String phoneNumber = PhoneNumberUtils.formatNumberToE164(phoneNumberInput.getText().toString(),selectedCountryCode);
        if(phoneNumber != null){
            isPhoneNumberValid = true;
        }else{
            phoneNumberInput.setHint("");
            isPhoneNumberValid = false;
        }
        
        if (selectedDialCode != null && selectedDialCode.length() >= 3) {
            isSelectedCountryValid = true;
        } else {
            isSelectedCountryValid = false;
        }
        
        log("validateFields isSelectedCountryValid " + isSelectedCountryValid + " isPhoneNumberValid " + isPhoneNumberValid);
    }
    
    private void showCountryCodeValidationError() {
        if (!isSelectedCountryValid) {
            if(selectedDialCode == null) {
                selectedCountry.setText("");
            } else {
                selectedCountry.setText(R.string.verify_account_counry_label);
            }
            log("show invalid country error");
            errorInvalidCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setVisibility(View.VISIBLE);
            titleCountryCode.setTextColor(Color.parseColor("#FFFF333A"));
            divider1.setBackgroundColor(Color.parseColor("#FFFF333A"));
        }
    }
    
    private void showPhoneNumberValidationError(String errorMessage) {
        if (!isPhoneNumberValid) {
            log("show invalid phone number error");
            errorInvalidPhoneNumber.setVisibility(View.VISIBLE);
            errorInvalidPhoneNumberIcon.setVisibility(View.VISIBLE);
            titlePhoneNumber.setVisibility(View.VISIBLE);
            titlePhoneNumber.setTextColor(Color.parseColor("#FFFF333A"));
            divider2.setBackgroundColor(Color.parseColor("#FFFF333A"));
            if (errorMessage != null) {
                errorInvalidPhoneNumber.setText(errorMessage);
            }
        }
    }
    
    private void hideError() {
        log("hide Errors");
        errorInvalidCountryCode.setVisibility(View.GONE);
        errorInvalidPhoneNumber.setVisibility(View.GONE);
        errorInvalidPhoneNumberIcon.setVisibility(View.GONE);
        titleCountryCode.setTextColor(Color.parseColor("#FF00BFA5"));
        titlePhoneNumber.setTextColor(Color.parseColor("#FF00BFA5"));
        divider1.setBackgroundColor(Color.parseColor("#8A000000"));
        divider2.setBackgroundColor(Color.parseColor("#8A000000"));
    }
    
    private void RequestTxt() {
        log("RequestTxt shouldDisableNextButton is " + shouldDisableNextButton);
        if(!shouldDisableNextButton){
            nextButton.setBackground(getDrawable(R.drawable.background_button_disable));
            String phoneNumber = PhoneNumberUtils.formatNumberToE164(phoneNumberInput.getText().toString(),selectedCountryCode);
            log(" RequestTxt phone number is " + phoneNumber);
            shouldDisableNextButton = true;
            megaApi.sendSMSVerificationCode(phoneNumber,this);
        }
    }
    
    public static void log(String message) {
        Util.log("SMSVerificationActivity",message);
    }
    
    @Override
    public void onRequestStart(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
        shouldDisableNextButton = false;
        nextButton.setBackground(getDrawable(R.drawable.background_accent_button));
        nextButton.setTextColor(Color.WHITE);
        if (request.getType() == MegaRequest.TYPE_SEND_SMS_VERIFICATIONCODE) {
            log("send phone number,get code" + e.getErrorCode());
            if (e.getErrorCode() == MegaError.API_OK) {
                log("will receive sms");
                String enteredPhoneNumber = phoneNumberInput.getText().toString();
                Intent intent = new Intent(this,SMSVerificationReceiveTxtActivity.class);
                intent.putExtra(SELECTED_COUNTRY_CODE,selectedDialCode);
                intent.putExtra(ENTERED_PHONE_NUMBER,enteredPhoneNumber);
                intent.putExtra(NAME_USER_LOCKED,isUserLocked);
                startActivityForResult(intent,REQUEST_CODE_VERIFY_CODE);
            } else if (e.getErrorCode() == MegaError.API_ETEMPUNAVAIL) {
                log("reached limitation.");
                errorInvalidPhoneNumber.setVisibility(View.VISIBLE);
                errorInvalidPhoneNumber.setTextColor(Color.parseColor("#FFFF333A"));
                errorInvalidPhoneNumber.setText(R.string.verify_account_error_reach_limit);
            } else if (e.getErrorCode() == MegaError.API_EACCESS) {
                log("already verified");
                isPhoneNumberValid = false;
                String errorMessage = getResources().getString(R.string.verify_account_invalid_phone_number);
                showPhoneNumberValidationError(errorMessage);
            }else if (e.getErrorCode() == MegaError.API_EARGS) {
                log("Invalid phone number");
                isPhoneNumberValid = false;
                String errorMessage = getResources().getString(R.string.verify_account_invalid_phone_number);
                showPhoneNumberValidationError(errorMessage);
            } else if (e.getErrorCode() == MegaError.API_EEXIST) {
                log("Phone number has been registered");
                isPhoneNumberValid = false;
                String errorMessage = getResources().getString(R.string.verify_account_error_phone_number_register);
                showPhoneNumberValidationError(errorMessage);
            } else {
                log("sms TYPE_SEND_SMS_VERIFICATIONCODE " + e.getErrorString());
                isPhoneNumberValid = false;
                String errorMessage = getResources().getString(R.string.verify_account_invalid_phone_number);
                showPhoneNumberValidationError(errorMessage);
            }
        }

        if (request.getType() == MegaRequest.TYPE_GET_COUNTRY_CALLING_CODES) {
            if (e.getErrorCode() == MegaError.API_OK) {
                ArrayList<String> codedCountryCode = new ArrayList<>();
                MegaStringListMap listMap = request.getMegaStringListMap();
                MegaStringList keyList = listMap.getKeys();
                for (int i = 0; i < keyList.size(); i++) {
                    String key = keyList.get(i);
                    StringBuffer contentBuffer = new StringBuffer();
                    contentBuffer.append(key + ":");
                    for (int j = 0; j < listMap.get(key).size(); j++) {
                        contentBuffer.append(listMap.get(key).get(j) + ",");
                    }
                    codedCountryCode.add(contentBuffer.toString());
                }
                this.countryCodeList = codedCountryCode;
            } else {
                log("the country code is not responded correctly");
            }
        }
    }
    
    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
    
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(COUNTRY_CODE, selectedCountryCode);
        outState.putCharSequence(COUNTRY_NAME, selectedCountryName);
        outState.putCharSequence(DIAL_CODE, selectedDialCode);
        super.onSaveInstanceState(outState);
    }
    
//    private void setContryDetails(){
//        selectedCountryCode = data.getStringExtra(COUNTRY_CODE);
//        selectedCountryName = data.getStringExtra(COUNTRY_NAME);
//        selectedDialCode = data.getStringExtra(DIAL_CODE);
//
//        String label = selectedCountryName + " (" + selectedDialCode + ")";
//        selectedCountry.setText(label);
//        errorInvalidCountryCode.setVisibility(View.GONE);
//        titleCountryCode.setVisibility(View.VISIBLE);
//        titleCountryCode.setTextColor(Color.parseColor("#FF00BFA5"));
//        selectedCountry.setTextColor(Color.parseColor("#DE000000"));
//        divider1.setBackgroundColor(Color.parseColor("#8A000000"));
//    }

}
