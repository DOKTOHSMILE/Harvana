package com.harvana.app.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.harvana.app.R;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Capital letters where people expect them, without anyone typing them.
 *
 * Every plain text field in the app starts sentences with a capital (and names, cities and places with a capital on every
 * word) because the keyboard is told so. Fields that must stay exactly as typed are left alone: email, passwords, user
 * names, wallet addresses and other codes, URLs, search boxes, recovery phrases, amounts, phone and ID numbers.
 *
 * If someone forgets and types a whole entry in lower case, the first letter of each sentence is capitalised when they leave
 * the field. Text that already has any capital letter in it is never touched, so "iPhone" and "eBay" stay as written.
 *
 * It works from the window, so it covers every screen, sheet, dialog and field built in code without editing each one.
 */
public final class SmartCaps {
    private SmartCaps() {}

    private static final int NONE = 0, SENTENCES = 1, WORDS = 2;

    /** Fields that must stay exactly as typed. Matched against the hint, the id name and the description. */
    private static final Pattern KEEP = Pattern.compile(
            "e-?mail|mail|passw|passcode|\\bpin\\b|otp|verification|code|user ?name|handle|@|wallet|recipient|receiver|to address"
                    + "|hash|txid|phrase|recovery|backup|restore|\\bwords\\b|seed|mnemonic|private|secret|\\bkey\\b|url|link|http|www|search|find|filter|amount|price|\\brate\\b"
                    + "|quantity|number|phone|mobile|bvn|\\bnin\\b|(^|[ _])id($|[ _])|iban|swift|referral|invite|token|coupon"
                    + "|slug|json|hex|base64|paste|scan|hv0x|0x|enter group|group id|loan id|genloan|decimals|percent|%|\\bage\\b|\\byear\\b|\\bdate\\b|\\btime\\b");

    /** Fields whose every word starts with a capital. */
    private static final Pattern WORD_FIELDS = Pattern.compile(
            "name|surname|first|last|city|town|state|country|location|occupation|company|bank|merchant|street|address|village|lga|region");

    public static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) { attach(a.getWindow()); }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    /** Watches one window (an activity, a bottom sheet, a dialog). Safe to call again. */
    public static void attach(Window w) {
        if (w == null) return;
        final View decor = w.getDecorView();
        if (decor.getTag(R.id.smart_caps_attached) != null) { sweep(decor); return; }
        decor.setTag(R.id.smart_caps_attached, Boolean.TRUE);
        decor.getViewTreeObserver().addOnGlobalFocusChangeListener(new ViewTreeObserver.OnGlobalFocusChangeListener() {
            @Override public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                if (oldFocus instanceof EditText) fixEntry((EditText) oldFocus);
                if (newFocus instanceof EditText) prepare((EditText) newFocus, true);
            }
        });
        sweep(decor);
        decor.postDelayed(new Runnable() { @Override public void run() { sweep(decor); } }, 700);
    }

    private static void sweep(View v) {
        if (v instanceof EditText) { prepare((EditText) v, false); return; }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) sweep(g.getChildAt(i));
        }
    }

    /** Tells the keyboard to capitalise, when this field is the kind that should be. */
    private static void prepare(EditText e, boolean restart) {
        int mode = modeOf(e);
        if (mode == NONE) return;
        int type = e.getInputType();
        int flag = mode == WORDS ? InputType.TYPE_TEXT_FLAG_CAP_WORDS : InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        if ((type & (InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)) != 0) return;
        int selStart = e.getSelectionStart(), selEnd = e.getSelectionEnd();
        e.setInputType(type | flag);
        if (selStart >= 0 && selEnd >= 0 && selStart <= e.length() && selEnd <= e.length()) e.setSelection(selStart, selEnd);
        if (restart && e.hasFocus()) {
            InputMethodManager imm = (InputMethodManager) e.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.restartInput(e);
        }
    }

    /** Which kind of capitals this field wants: NONE, SENTENCES or WORDS. */
    private static int modeOf(EditText e) {
        int type = e.getInputType();
        if ((type & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return NONE;
        int variation = type & InputType.TYPE_MASK_VARIATION;
        switch (variation) {
            case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
            case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
            case InputType.TYPE_TEXT_VARIATION_PASSWORD:
            case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
            case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
            case InputType.TYPE_TEXT_VARIATION_URI:
            case InputType.TYPE_TEXT_VARIATION_FILTER:
            case InputType.TYPE_TEXT_VARIATION_PHONETIC:
                return NONE;
            default:
        }
        if (!e.isEnabled() || e.getKeyListener() == null) return NONE;
        String id = "";
        try { if (e.getId() != View.NO_ID) id = e.getResources().getResourceEntryName(e.getId()); } catch (Exception ignored) { }
        CharSequence hint = e.getHint();
        CharSequence desc = e.getContentDescription();
        String hay = ((hint == null ? "" : hint) + " " + id + " " + (desc == null ? "" : desc) + " " + (e.getTag() instanceof String ? e.getTag() : ""))
                .toLowerCase(Locale.US);
        if (KEEP.matcher(hay).find()) return NONE;
        if (variation == InputType.TYPE_TEXT_VARIATION_PERSON_NAME || variation == InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS) return WORDS;
        return WORD_FIELDS.matcher(hay).find() ? WORDS : SENTENCES;
    }

    /** When someone leaves a field typed entirely in lower case, capitalises the start of each sentence (or word). */
    private static void fixEntry(EditText e) {
        int mode = modeOf(e);
        if (mode == NONE) return;
        CharSequence cs = e.getText();
        if (cs == null || cs.length() == 0) return;
        String text = cs.toString();
        boolean hasLetter = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isUpperCase(c)) return;      // somebody already chose their capitals
            if (Character.isLetter(c)) hasLetter = true;
        }
        if (!hasLetter) return;
        StringBuilder out = new StringBuilder(text.length());
        boolean start = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (start && Character.isLetter(c)) { out.append(Character.toUpperCase(c)); start = false; }
            else { out.append(c); if (Character.isLetterOrDigit(c)) start = false; }
            if (mode == WORDS) { if (Character.isWhitespace(c) || c == '-') start = true; }
            else if (c == '.' || c == '!' || c == '?' || c == '\n') start = true;
        }
        String fixed = out.toString();
        if (!fixed.equals(text)) {
            int sel = e.getSelectionStart();
            e.setText(fixed);
            if (sel >= 0 && sel <= fixed.length()) e.setSelection(sel);
        }
    }
}
