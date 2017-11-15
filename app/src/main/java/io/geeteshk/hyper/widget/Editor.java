/*
 * Copyright 2016 Geetesh Kalakoti <kalakotig@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.geeteshk.hyper.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.geeteshk.hyper.R;
import io.geeteshk.hyper.helper.Prefs;
import io.geeteshk.hyper.helper.ResourceHelper;

/**
 * Editor class to handle code highlighting etc
 * Derived from: https://github.com/markusfisch/ShaderEditor/blob/master/app/src/main/java/de/markusfisch/android/shadereditor/widget/ShaderEditor.java
 */
public class Editor extends AppCompatMultiAutoCompleteTextView {

    private final String TAG = Editor.class.getSimpleName();

    /**
     * Handler used to update colours when code is changed
     */
    private final Handler updateHandler = new Handler();
    /**
     * Custom listener
     */
    public OnTextChangedListener onTextChangedListener = null;
    /**
     * Delay used to update code
     */
    public int updateDelay = 2000;

    int currentLine = 0;
    int lineDiff = 0;
    /**
     * Type of code set
     */
    private CodeType codeType;
    /**
     * Checks if code has been changed
     */
    private boolean fileModified = true;
    /**
     * Context used to get preferences
     */
    private Context context;
    /**
     * Rect to represent each line
     */
    private Rect lineRect;
    /**
     * Paint to draw line numbers
     */
    private Paint numberPaint, lineShadowPaint;
    private boolean isHighlighting;
    /**
     * Runnable used to update colours when code is changed
     */
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isHighlighting) {
                Editable e = getText();
                if (onTextChangedListener != null)
                    onTextChangedListener.onTextChanged(e.toString());
                highlightWithoutChange(e);
            }
        }
    };

    private Colors colors;
    private Patterns patterns;

    private boolean hasLineNumbers;

    /**
     * Public constructor
     *
     * @param context used to get preferences
     */
    public Editor(Context context) {
        this(context, null);
    }

    /**
     * Public constructor
     *
     * @param context used to get preferences
     * @param attrs   not used
     */
    public Editor(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    /**
     * Highlights given code and sets it as text
     *
     * @param text to be highlighted
     */
    public void setTextHighlighted(CharSequence text) {
        cancelUpdate();

        fileModified = false;
        setText(highlight(new SpannableStringBuilder(text)));
        fileModified = true;

        if (onTextChangedListener != null) onTextChangedListener.onTextChanged(text.toString());
    }

    /**
     * Code used to initialise editor
     */
    private void init() {
        colors = new Colors(!Prefs.get(context, "dark_theme_editor", false));
        patterns = new Patterns();
        lineRect = new Rect();
        hasLineNumbers = Prefs.get(context, "show_line_numbers", true);

        lineShadowPaint = new Paint();
        lineShadowPaint.setStyle(Paint.Style.FILL);
        lineShadowPaint.setColor(colors.getColorLineShadow());

        if (hasLineNumbers) {
            numberPaint = new Paint();
            numberPaint.setStyle(Paint.Style.FILL);
            numberPaint.setAntiAlias(true);
            numberPaint.setTextSize(ResourceHelper.dpToPx(context, 14));
            numberPaint.setTextAlign(Paint.Align.RIGHT);
            numberPaint.setColor(colors.getColorNumber());
        } else {
            int padding = ResourceHelper.dpToPx(context, 8);
            if (Build.VERSION.SDK_INT > 15) {
                setPaddingRelative(padding, padding, padding, 0);
            } else {
                setPadding(padding, padding, padding, 0);
            }
        }

        setLineSpacing(0, 1.2f);
        setBackgroundColor(colors.getColorBackground());
        setTextColor(colors.getColorText());
        setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Inconsolata-Regular.ttf"));
        setHorizontallyScrolling(true);
        setCustomSelectionActionModeCallback(new EditorCallback());
        setHorizontallyScrolling(false);
        setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (fileModified && end - start == 1 && start < source.length() && dstart < dest.length()) {
                    char c = source.charAt(start);
                    if (c == '\n') return autoIndent(source, dest, dstart, dend);
                }

                return source;
            }
        }});

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setupAutoComplete();
            }
        });
    }

    /**
     * Prevent code from updating
     */
    private void cancelUpdate() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    /**
     * Used in main runnable
     *
     * @param e text to be highlighted
     */
    private void highlightWithoutChange(Editable e) {
        fileModified = false;
        highlight(e);
        fileModified = true;
    }

    /**
     * Main method used for highlighting i.e. this is where the magic happens
     *
     * @param e text to be highlighted
     * @return highlighted text
     */
    private Editable highlight(Editable e) {
        isHighlighting = true;

        try {
            if (e.length() == 0) return e;
            if (hasSpans(e)) clearSpans(e);

            Matcher m;
            switch (codeType) {
                case HTML:
                    for (m = patterns.getPatternKeywords().matcher(e); m.find(); ) {
                        if (e.toString().charAt(m.start() - 1) == '<' || e.toString().charAt(m.start() - 1) == '/') {
                            e.setSpan(new ForegroundColorSpan(colors.getColorKeyword()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    for (m = patterns.getPatternBuiltins().matcher(e); m.find(); ) {
                        if (e.toString().charAt(m.start() - 1) == ' ' && e.toString().charAt(m.end()) == '=') {
                            e.setSpan(new ForegroundColorSpan(colors.getColorBuiltin()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    for (m = patterns.getPatternStrings().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorStrings()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternComments().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorComment()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    break;
                case CSS:
                    for (m = patterns.getPatternKeywords().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorKeyword()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternParams().matcher(e); m.find(); ) {
                        if (e.toString().charAt(m.end()) == ':') {
                            e.setSpan(new ForegroundColorSpan(colors.getColorParams()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    for (int index = e.toString().indexOf(":"); index >= 0; index = e.toString().indexOf(":", index + 1)) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorEnding()), index + 1, e.toString().indexOf(";", index + 1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (int index = e.toString().indexOf("."); index >= 0; index = e.toString().indexOf(".", index + 1)) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorBuiltin()), index + 1, e.toString().indexOf("{", index + 1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (int index = e.toString().indexOf("#"); index >= 0; index = e.toString().indexOf("#", index + 1)) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorBuiltin()), index + 1, e.toString().indexOf("{", index + 1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternEndings().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorEnding()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternStrings().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorStrings()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternCommentsOther().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorComment()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    break;
                case JS:
                    for (m = patterns.getPatternDatatypes().matcher(e); m.find(); ) {
                        if (e.toString().charAt(m.end()) == ' ') {
                            e.setSpan(new ForegroundColorSpan(colors.getColorParams()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    for (m = patterns.getPatternFunctions().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorFunctions()), m.start() + 2, m.end() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternSymbols().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorKeyword()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (int index = e.toString().indexOf("null"); index >= 0; index = e.toString().indexOf("null", index + 1)) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorEnding()), index, index + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternNumbers().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorBuiltin()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternBooleans().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorBuiltin()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternStrings().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorStrings()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    for (m = patterns.getPatternCommentsOther().matcher(e); m.find(); ) {
                        e.setSpan(new ForegroundColorSpan(colors.getColorComment()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    break;
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

        isHighlighting = false;
        return e;
    }

    /**
     * Method to set the code type
     *
     * @param type of code
     */
    public void setType(CodeType type) {
        codeType = type;
    }

    /**
     * Removes all spans
     *
     * @param e text to be cleared
     */
    private void clearSpans(Editable e) {
        {
            ForegroundColorSpan spans[] = e.getSpans(0, e.length(), ForegroundColorSpan.class);
            for (int n = spans.length; n-- > 0; ) e.removeSpan(spans[n]);
        }
    }

    private boolean hasSpans(Editable e) {
        return e.getSpans(0, e.length(), ForegroundColorSpan.class).length > 0;
    }

    /**
     * Method used to draw line numbers onto code
     *
     * @param canvas used for drawing
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (hasLineNumbers) {
            int cursorLine = getCurrentCursorLine();
            int lineBounds;
            int lineHeight = getLineHeight();
            int lineCount = getLineCount();
            List<CharSequence> lines = getLines();

            for (int i = 0; i < lineCount; i++) {
                lineBounds = getLineBounds(i - lineDiff, lineRect);
                if (lines.get(i).toString().endsWith("\n") || i == lineCount - 1) {
                    if (hasLineNumbers) canvas.drawText(String.valueOf(currentLine + 1), 64, lineBounds, numberPaint);
                    currentLine += 1;
                    lineDiff = 0;
                } else {
                    lineDiff += 1;
                }

                if (i == cursorLine) {
                    if (hasLineNumbers) {
                        canvas.drawRect(0, 10 + lineBounds - lineHeight, 72, lineBounds + 8, lineShadowPaint);
                    }
                }

                if (i == lineCount - 1) {
                    currentLine = 0;
                    lineDiff = 0;
                }
            }
        } else {
            int cursorLine = getCurrentCursorLine();
            int lineBounds;
            int lineHeight = getLineHeight();

            lineBounds = getLineBounds(cursorLine - lineDiff, lineRect);
            canvas.drawRect(0, 8 + lineBounds - lineHeight, getWidth(), lineBounds + 12, lineShadowPaint);
        }

        super.onDraw(canvas);
    }

    public List<CharSequence> getLines() {
        final List<CharSequence> lines = new ArrayList<>();
        final Layout layout = getLayout();

        if (layout != null) {
            final int lineCount = layout.getLineCount();
            final CharSequence text = layout.getText();

            for (int i = 0, startIndex = 0; i < lineCount; i++) {
                final int endIndex = layout.getLineEnd(i);
                lines.add(text.subSequence(startIndex, endIndex));
                startIndex = endIndex;
            }
        }
        return lines;
    }

    public int getCurrentCursorLine() {
        int selectionStart = Selection.getSelectionStart(getText());
        Layout layout = getLayout();

        if (!(selectionStart == -1)) {
            return layout.getLineForOffset(selectionStart);
        }

        return -1;
    }

    /**
     * Method used for indenting code automatically
     *
     * @param source the main code
     * @param dest   the new code
     * @param dstart start of the code
     * @param dend   end of the code
     * @return indented code
     */
    private CharSequence autoIndent(CharSequence source, Spanned dest, int dstart, int dend) {
        String indent = "";
        int istart = dstart - 1;
        int iend;

        boolean dataBefore = false;
        int pt = 0;

        for (; istart > -1; --istart) {
            char c = dest.charAt(istart);

            if (c == '\n')
                break;

            if (c != ' ' &&
                    c != '\t') {
                if (!dataBefore) {
                    if (c == '{' ||
                            c == '+' ||
                            c == '-' ||
                            c == '*' ||
                            c == '/' ||
                            c == '%' ||
                            c == '^' ||
                            c == '=')
                        --pt;

                    dataBefore = true;
                }

                if (c == '(')
                    --pt;
                else if (c == ')')
                    ++pt;
            }
        }

        if (istart > -1) {
            char charAtCursor = dest.charAt(dstart);

            for (iend = ++istart;
                 iend < dend;
                 ++iend) {
                char c = dest.charAt(iend);

                if (charAtCursor != '\n' &&
                        c == '/' &&
                        iend + 1 < dend &&
                        dest.charAt(iend) == c) {
                    iend += 2;
                    break;
                }

                if (c != ' ' &&
                        c != '\t')
                    break;
            }

            indent += dest.subSequence(istart, iend);
        }

        if (pt < 0)
            indent += "\t";

        return source + indent;
    }

    private void setupAutoComplete() {
        String[] items = patterns.getPatternKeywords().pattern().replace("(", "").replace(")", "").substring(2, patterns.getPatternKeywords().pattern().length() - 2).split("\\|");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, items);
        setAdapter(adapter);

        setThreshold(1);
        setTokenizer(new Tokenizer() {
            @Override
            public int findTokenStart(CharSequence text, int cursor) {
                int i = cursor;

                while (i > 0 && text.charAt(i - 1) != '<') {
                    i--;
                }

                if (i < 1 || text.charAt(i - 1) != '<') {
                    return cursor;
                }

                return i;
            }

            @Override
            public int findTokenEnd(CharSequence text, int cursor) {
                int i = cursor;
                int len = text.length();

                while (i < len) {
                    if (text.charAt(i) == ' ' || text.charAt(i) == '>' || text.charAt(i) == '/') {
                        return i;
                    } else {
                        i++;
                    }
                }

                return i;
            }

            @Override
            public CharSequence terminateToken(CharSequence text) {
                int i = text.length();

                while (i > 0 && text.charAt(i - 1) == ' ') {
                    i--;
                }

                if (i > 0 && text.charAt(i - 1) == ' ') {
                    return text;
                } else {
                    if (text instanceof Spanned) {
                        SpannableString sp = new SpannableString(text);
                        TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
                        return sp;
                    } else {
                        return text + "></" + text + ">";
                    }
                }
            }
        });

        addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Layout layout = getLayout();
                int position = getSelectionStart();
                int line = layout.getLineForOffset(position);
                int baseline = layout.getLineBaseline(line);
                int bottom = getHeight();
                int x = (int) layout.getPrimaryHorizontal(position);

                if (x + (getWidth() / 2) > getWidth()) {
                    x = getWidth() / 2;
                }

                setDropDownVerticalOffset(baseline - bottom);
                setDropDownHorizontalOffset(x);

                setDropDownHeight(getHeight() / 3);
                setDropDownWidth(getWidth() / 2);
            }

            @Override
            public void afterTextChanged(Editable e) {
                cancelUpdate();

                if (!fileModified) return;

                updateHandler.postDelayed(updateRunnable, updateDelay);
            }
        });
    }

    /**
     * Listens to when text is changed
     */
    public interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    private class EditorCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(0, 1, 3, R.string.refactor);
            menu.add(0, 2, 3, R.string.comment);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case 1:
                    String selected = getSelectedString();
                    View layout = View.inflate(context, R.layout.dialog_refactor, null);

                    final EditText replaceFrom = layout.findViewById(R.id.replace_from);
                    final EditText replaceTo = layout.findViewById(R.id.replace_to);
                    replaceFrom.setText(selected);

                    AlertDialog.Builder builder;
                    if (Prefs.get(context, "dark_theme", false)) {
                        builder = new AlertDialog.Builder(context, R.style.Hyper_Dark);
                    } else {
                        builder = new AlertDialog.Builder(context);
                    }

                    builder.setView(layout);
                    builder.setPositiveButton(R.string.replace, null);

                    final AppCompatDialog dialog = builder.create();
                    dialog.show();

                    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String replaceFromStr = replaceFrom.getText().toString();
                            String replaceToStr = replaceTo.getText().toString();

                            if (replaceFromStr.isEmpty()) {
                                replaceFrom.setError(context.getString(R.string.empty_field_no_no));
                            } else if (replaceToStr.isEmpty()) {
                                replaceTo.setError(context.getString(R.string.empty_field_no_no));
                            } else {
                                setText(getText().toString().replace(replaceFromStr, replaceToStr));
                                dialog.dismiss();
                            }
                        }
                    });

                    return true;
                case 2:
                    String startComment = "", endComment = "";
                    switch (codeType) {
                        case HTML:
                            startComment = "<!-- ";
                            endComment = " -->";
                            break;
                        case CSS:
                            startComment = "/* ";
                            endComment = " */";
                            break;
                        case JS:
                            startComment = "/** ";
                            endComment = " */";
                            break;
                    }

                    setText(getText().insert(getSelectionStart(), startComment).insert(getSelectionEnd(), endComment));
                    return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }

        private String getSelectedString() {
            return getText().toString().substring(getSelectionStart(), getSelectionEnd());
        }
    }

    public enum CodeType {
        HTML, CSS, JS
    }

    class Colors {
        private final int colorKeyword = 0xfff92672;
        private final int colorParams = 0xff64cbf4;
        private final int colorEnding = 0xff9a79dd;
        private final int colorFunctions = 0xffed5c00;

        private final int colorBuiltin = 0xff72b000;
        private final int colorComment = 0xffa0a0a0;
        private final int colorStrings = 0xffed5c00;

        private final int colorBuiltinDark = 0xffa6e22e;
        private final int colorCommentDark = 0xff75715e;
        private final int colorStringsDark = 0xffe6db74;

        private final int colorLineShadow = 0x10000000;
        private final int colorNumber = 0xffa0a0a0;
        private final int colorBackground = 0xfff8f8f8;
        private final int colorText = 0xff222222;

        private final int colorLineShadowDark = 0x10FFFFFF;
        private final int colorNumberDark = 0xffd3d3d3;
        private final int colorBackgroundDark = 0xff222222;
        private final int colorTextDark = 0xfff8f8f8;

        private boolean darkTheme;

        Colors(boolean darkTheme) {
            this.darkTheme = darkTheme;
        }

        int getColorKeyword() {
            return colorKeyword;
        }

        int getColorParams() {
            return colorParams;
        }

        int getColorEnding() {
            return colorEnding;
        }

        int getColorFunctions() {
            return colorFunctions;
        }

        int getColorBuiltin() {
            return darkTheme ? colorBuiltin : colorBuiltinDark;
        }

        int getColorComment() {
            return darkTheme ? colorComment : colorCommentDark;
        }

        int getColorStrings() {
            return darkTheme ? colorStrings : colorStringsDark;
        }

        int getColorLineShadow() {
            return darkTheme ? colorLineShadow : colorLineShadowDark;
        }

        int getColorNumber() {
            return darkTheme ? colorNumber : colorNumberDark;
        }

        int getColorBackground() {
            return darkTheme ? colorBackground : colorBackgroundDark;
        }

        int getColorText() {
            return darkTheme ? colorText : colorTextDark;
        }
    }

    class Patterns {
        private final Pattern patternKeywords = Pattern.compile("\\b(a|address|app|applet|area|b|base|basefont|bgsound|big|blink|blockquote|body|br|button|caption|center|cite|code|col|colgroup|comment|dd|dfn|dir|div|dl|dt|em|embed|fieldset|font|form|frame|frameset|h1|h2|h3|h4|h5|h6|head|hr|html|htmlplus|hype|i|iframe|img|input|ins|del|isindex|kbd|label|legend|li|link|listing|map|marquee|menu|meta|multicol|nobr|noembed|noframes|noscript|ol|option|p|param|plaintext|pre|s|samp|script|select|small|sound|spacer|span|strike|strong|style|sub|sup|table|tbody|td|textarea|tfoot|th|thead|title|tr|tt|u|var|wbr|xmp|import)\\b");
        private final Pattern patternBuiltins = Pattern.compile("\\b(charset|lang|href|onclick|onmouseover|onmouseout|code|codebase|width|height|align|vspace|hspace|name|archive|mayscript|alt|shape|coords|target|nohref|size|color|face|src|loop|bgcolor|background|text|vlink|alink|bgproperties|topmargin|leftmargin|marginheight|marginwidth|onload|onunload|onfocus|onblur|stylesrc|scroll|clear|type|value|valign|span|compact|pluginspage|pluginurl|hidden|autostart|playcount|volume|controller|mastersound|starttime|endtime|point-size|weight|action|method|enctype|onsubmit|onreset|scrolling|noresize|frameborder|bordercolor|cols|rows|framespacing|border|noshade|longdesc|ismap|usemap|lowsrc|naturalsizeflag|nosave|dynsrc|controls|start|suppress|maxlength|checked|language|onchange|onkeypress|onkeyup|onkeydown|autocomplete|prompt|for|rel|rev|media|direction|behaviour|scrolldelay|scrollamount|http-equiv|content|gutter|defer|event|multiple|readonly|cellpadding|cellspacing|rules|bordercolorlight|bordercolordark|summary|colspan|rowspan|nowrap|halign|disabled|accesskey|tabindex|id|class)\\b");
        private final Pattern patternParams = Pattern.compile("\\b(azimuth|background-attachment|background-color|background-image|background-position|background-repeat|background|border-collapse|border-color|border-spacing|border-style|border-top|border-right|border-bottom|border-left|border-top-color|border-right-color|border-left-color|border-bottom-color|border-top-style|border-right-style|border-bottom-style|border-left-style|border-top-width|border-right-width|border-bottom-width|border-left-width|border-width|border|bottom|caption-side|clear|clip|color|content|counter-increment|counter-reset|cue-after|cue-before|cue|cursor|direction|display|elevation|empty-cells|float|font-family|font-size|font-style|font-variant|font-weight|font|height|left|letter-spacing|line-height|list-style-image|list-style-position|list-style-type|list-style|margin-left|margin-right|margin-top|margin-bottom|margin|max-height|max-width|min-height|min-width|orphans|outline-color|outline-style|outline-width|outline|overflow|padding-top|padding-right|padding-bottom|padding-left|padding|page-break-after|page-break-before|page-break-inside|pause-after|pause-before|pause|pitch-range|pitch|play-during|position|quotes|richness|right|speak-header|speak-numeral|speak-punctuation|speak|speech-rate|stress|table-layout|text-align|text-decoration|text-indent|text-transform|top|unicode-bidi|vertical-align|visibility|voice-family|volume|white-space|widows|width|word-spacing|z-index)\\b");
        private final Pattern patternComments = Pattern.compile("/\\**?\\*/|<!--.*");
        private final Pattern patternCommentsOther = Pattern.compile("/\\*(?:.|[\\n\\r])*?\\*/|//.*");
        private final Pattern patternEndings = Pattern.compile("(em|rem|px|pt|%)");
        private final Pattern patternDatatypes = Pattern.compile("\\b(abstract|arguments|boolean|byte|char|class|const|double|enum|final|float|function|int|interface|long|native|package|private|protected|public|short|static|synchronized|transient|var|void|volatile)\\b");
        private final Pattern patternSymbols = Pattern.compile("(&|=|throw|new|for|if|else|>|<|^|\\+|-|\\s\\|\\s|break|try|catch|do|!|finally|default|case|switch|native|let|super|throws|return)");
        private final Pattern patternFunctions = Pattern.compile("n\\((.*?)\\)");
        private final Pattern patternNumbers = Pattern.compile("\\b(\\d*[.]?\\d+)\\b");
        private final Pattern patternBooleans = Pattern.compile("\\b(true|false)\\b");
        private final Pattern patternStrings = Pattern.compile("([\"'])(?:(?=(\\\\?))\\2.)*?\\1");

        Pattern getPatternKeywords() {
            return patternKeywords;
        }

        Pattern getPatternBuiltins() {
            return patternBuiltins;
        }

        Pattern getPatternParams() {
            return patternParams;
        }

        Pattern getPatternComments() {
            return patternComments;
        }

        Pattern getPatternCommentsOther() {
            return patternCommentsOther;
        }

        Pattern getPatternEndings() {
            return patternEndings;
        }

        Pattern getPatternDatatypes() {
            return patternDatatypes;
        }

        Pattern getPatternSymbols() {
            return patternSymbols;
        }

        Pattern getPatternFunctions() {
            return patternFunctions;
        }

        Pattern getPatternNumbers() {
            return patternNumbers;
        }

        Pattern getPatternBooleans() {
            return patternBooleans;
        }

        Pattern getPatternStrings() {
            return patternStrings;
        }
    }
}