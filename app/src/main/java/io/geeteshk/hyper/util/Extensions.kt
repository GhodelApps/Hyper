package io.geeteshk.hyper.util

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.InputStream

fun File.copyInputStreamToFile(inputStream: InputStream) {
    inputStream.use { input ->
        this.outputStream().use { fileOut ->
            input.copyTo(fileOut)
        }
    }
}

fun ViewGroup.inflate(layoutId: Int): View =
        LayoutInflater.from(context).inflate(layoutId, this, false)

fun EditText.string() = text.toString()

fun TextInputEditText.string() = text.toString()

fun <T> MutableLiveData<T>.notify() {
    this.value = this.value
}

fun Spinner.onItemSelected(onItemSelected: (position: Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            onItemSelected.invoke(p2)
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {}
    }
}

fun DrawerLayout.onDrawerOpened(onDrawerOpened: () -> Unit) {
    addDrawerListener(object : DrawerLayout.DrawerListener {
        override fun onDrawerOpened(drawerView: View) {
            onDrawerOpened.invoke()
        }

        override fun onDrawerStateChanged(newState: Int) {}
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
        override fun onDrawerClosed(drawerView: View) {}
    })
}

fun ViewPropertyAnimator.onAnimationStop(onAnimationStop: () -> Unit) {
    setListener(object : Animator.AnimatorListener {
        override fun onAnimationEnd(p0: Animator?) {
            onAnimationStop.invoke()
        }

        override fun onAnimationCancel(p0: Animator?) {
            onAnimationStop.invoke()
        }

        override fun onAnimationStart(p0: Animator?) {}
        override fun onAnimationRepeat(p0: Animator?) {}
    })
}

inline fun View.snack(message: String, length: Int = Snackbar.LENGTH_LONG, f: Snackbar.() -> Unit = {}) {
    with (Snackbar.make(this, message, length)) {
        f()
        show()
    }
}

inline fun View.snack(@StringRes message: Int, length: Int = Snackbar.LENGTH_LONG, f: Snackbar.() -> Unit = {}) {
    with (Snackbar.make(this, message, length)) {
        f()
        show()
    }
}

fun Snackbar.action(action: String, color: Int? = null, listener: (View) -> Unit) {
    setAction(action, listener)
    color?.let { setActionTextColor(color) }
}

fun Snackbar.callback(callback: () -> Unit) {
    addCallback(object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            super.onDismissed(transientBottomBar, event)
            callback.invoke()
        }
    })
}

fun Context.compatColor(@ColorRes color: Int): Int {
    return ContextCompat.getColor(this, color)
}

fun Activity.startAndFinish(intent: Intent) {
    startActivity(intent)
    finish()
}