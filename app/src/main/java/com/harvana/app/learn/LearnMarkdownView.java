package com.harvana.app.learn;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.harvana.app.R;

/** Paints {@link LearnMarkdown} onto text: bold, italic, headings, lists, quotes, links, code and dividers. */
public final class LearnMarkdownView {
    private LearnMarkdownView() {}

    public static CharSequence render(Context c, String source) {
        LearnMarkdown.Result r = LearnMarkdown.parse(source);
        SpannableStringBuilder sb = new SpannableStringBuilder(r.text);
        float d = c.getResources().getDisplayMetrics().density;
        int accent = ContextCompat.getColor(c, R.color.colorAccent);
        int muted = ContextCompat.getColor(c, R.color.colorTextTertiary);
        int primary = ContextCompat.getColor(c, R.color.colorTextPrimary);
        int flag = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;
        for (LearnMarkdown.Span s : r.spans) {
            int a = s.start, b = Math.min(s.end, sb.length());
            if (a >= b) continue;
            // A paragraph style (indent, quote bar) must end at a line break or at the end of the text.
            int pb = b < sb.length() && sb.charAt(b) == '\n' ? b + 1 : b;
            switch (s.kind) {
                case H1: sb.setSpan(new RelativeSizeSpan(1.4f), a, b, flag); sb.setSpan(new StyleSpan(Typeface.BOLD), a, b, flag); sb.setSpan(new ForegroundColorSpan(primary), a, b, flag); break;
                case H2: sb.setSpan(new RelativeSizeSpan(1.25f), a, b, flag); sb.setSpan(new StyleSpan(Typeface.BOLD), a, b, flag); sb.setSpan(new ForegroundColorSpan(primary), a, b, flag); break;
                case H3: sb.setSpan(new RelativeSizeSpan(1.1f), a, b, flag); sb.setSpan(new StyleSpan(Typeface.BOLD), a, b, flag); sb.setSpan(new ForegroundColorSpan(primary), a, b, flag); break;
                case BOLD: sb.setSpan(new StyleSpan(Typeface.BOLD), a, b, flag); break;
                case ITALIC: sb.setSpan(new StyleSpan(Typeface.ITALIC), a, b, flag); break;
                case STRIKE: sb.setSpan(new StrikethroughSpan(), a, b, flag); break;
                case CODE:
                    sb.setSpan(new TypefaceSpan("monospace"), a, b, flag);
                    sb.setSpan(new BackgroundColorSpan(0x22808080), a, b, flag);
                    break;
                case LINK: sb.setSpan(new URLSpan(s.url), a, b, flag); break;
                case QUOTE:
                    sb.setSpan(new QuoteSpan(accent), a, pb, flag);
                    sb.setSpan(new StyleSpan(Typeface.ITALIC), a, b, flag);
                    break;
                case BULLET: {
                    int first = Math.round(s.level * 18 * d), rest = first + Math.round(16 * d);
                    sb.setSpan(new LeadingMarginSpan.Standard(first, rest), a, pb, flag);
                    break;
                }
                case RULE: sb.setSpan(new ForegroundColorSpan(muted), a, b, flag); break;
            }
        }
        return sb;
    }

    /** Sets the text. In the reader links are tappable; in the feed card a tap opens the post, so links stay plain. */
    public static void bind(TextView tv, String source, boolean tappableLinks) {
        tv.setText(render(tv.getContext(), source));
        if (tappableLinks) {
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            tv.setLinkTextColor(ContextCompat.getColor(tv.getContext(), R.color.colorAccent));
        } else {
            tv.setMovementMethod(null);
        }
    }
}
