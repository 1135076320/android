package mega.privacy.android.app.components.twemoji;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiBackspaceClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiClickListener;
import mega.privacy.android.app.components.twemoji.listeners.OnEmojiLongClickListener;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Util;

public class EmojiKeyboard extends LinearLayout {

    private Activity activity;
    private EmojiEditTextInterface editInterface;
    private RecentEmoji recentEmoji;
    private VariantEmoji variantEmoji;
    private EmojiVariantPopup variantPopup;
    private View rootView;
    private ImageButton emojiIcon;
    private int keyboardHeight;
    private OnEmojiClickListener onEmojiClickListener;
    private OnEmojiBackspaceClickListener onEmojiBackspaceClickListener;
    private OnPlaceButtonListener buttonListener;


    boolean isListenerActivated  = true;


    private boolean isLetterKeyboardShown = false;
    private boolean isEmojiKeyboardShown = false;

    //Long click in EMOJI
    final OnEmojiLongClickListener longClickListener = new OnEmojiLongClickListener() {
        @Override
        public void onEmojiLongClick(@NonNull final EmojiImageView view, @NonNull final Emoji emoji) {
            if(isListenerActivated){
                variantPopup.show(view, emoji);
            }
        }
    };

    //Click in EMOJI
    final OnEmojiClickListener clickListener = new OnEmojiClickListener() {
        @Override
        public void onEmojiClick(@NonNull final EmojiImageView imageView, @NonNull final Emoji emoji) {
            if(isListenerActivated) {
                editInterface.input(emoji);
                recentEmoji.addEmoji(emoji);
                imageView.updateEmoji(emoji);

                if (onEmojiClickListener != null) {
                    onEmojiClickListener.onEmojiClick(imageView, emoji);
                }
                variantPopup.dismiss();
            }
        }
    };


    public EmojiKeyboard(Context context) {
        super(context);
        init(null, 0);
    }

    public EmojiKeyboard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public EmojiKeyboard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EmojiKeyboard(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr);
    }

    public void setOnPlaceButtonListener(OnPlaceButtonListener buttonListener) {
        this.buttonListener = buttonListener;
    }
    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.EmojiKeyboard, defStyle, 0);
        a.recycle();
        this.rootView = getRootView();
        this.variantEmoji = new VariantEmojiManager(getContext());
        this.recentEmoji = new RecentEmojiManager(getContext());
        this.variantPopup = new EmojiVariantPopup(rootView, clickListener);

        final EmojiView emojiView = new EmojiView(getContext(), clickListener, longClickListener, recentEmoji, variantEmoji);
        emojiView.setOnEmojiBackspaceClickListener(new OnEmojiBackspaceClickListener() {
            @Override
            public void onEmojiBackspaceClick(final View v) {
                editInterface.backspace();
                if (onEmojiBackspaceClickListener != null) {
                    onEmojiBackspaceClickListener.onEmojiBackspaceClick(v);
                }
            }
        });
        addView(emojiView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(keyboardHeight, MeasureSpec.EXACTLY));
    }

    public void init(Activity context, EmojiEditTextInterface editText, ImageButton emojiIcon) {
        this.editInterface = editText;
        this.emojiIcon = emojiIcon;
        this.activity = context;

        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        keyboardHeight = outMetrics.heightPixels / 2 - ChatUtil.getActionBarHeight(activity, getResources());
        requestLayout();
    }

    private void needToReplace() {
        if (buttonListener == null) return;
        buttonListener.needToPlace();
    }

    //KEYBOARDS:
    public void showLetterKeyboard(){
        if (isLetterKeyboardShown || !(editInterface instanceof View)) return;
        log("showLetterKeyboard()");
        hideEmojiKeyboard();
        final View view = (View) editInterface;
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imm.showSoftInput(view, 0, null);
        isLetterKeyboardShown = true;
        emojiIcon.setImageResource(R.drawable.ic_emojicon);
        needToReplace();
    }


    public void showEmojiKeyboard(){
        if (isEmojiKeyboardShown) return;
        log("showEmojiKeyboard");
        hideLetterKeyboard();
        setVisibility(VISIBLE);
        isEmojiKeyboardShown = true;
        emojiIcon.setImageResource(R.drawable.ic_keyboard_white);
        if (editInterface instanceof View){
            final View view = (View) editInterface;
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        }

        needToReplace();
    }

    public void hideBothKeyboard(Activity activity){
        if (activity == null) return;
        log("hideBothKeyboard()");
        hideEmojiKeyboard();
        hideLetterKeyboard();
        emojiIcon.setImageResource(R.drawable.ic_emojicon);
    }

    public void hideLetterKeyboard() {
        if (!isLetterKeyboardShown || !(editInterface instanceof View)) return;
        log("hideLetterKeyboard() ");
        final View view = (View) editInterface;
        view.clearFocus();
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0, null);
        isLetterKeyboardShown = false;
        needToReplace();
    }

    public void hideEmojiKeyboard(){
        if(!isEmojiKeyboardShown) return;
        log("hideEmojiKeyboard() ");
        recentEmoji.persist();
        variantEmoji.persist();
        setVisibility(GONE);

        isEmojiKeyboardShown = false;
        if (editInterface instanceof View) {
            final View view = (View) editInterface;
            view.clearFocus();
        }

        needToReplace();

    }

    public void setListenerActivated(boolean listenerActivated) {
        isListenerActivated = listenerActivated;
    }

    public boolean getLetterKeyboardShown() {
        return isLetterKeyboardShown;
    }

    public boolean getEmojiKeyboardShown() {
        return isEmojiKeyboardShown;
    }

    public static void log(String message) {
        Util.log("EmojiKeyboard", message);
    }
}

