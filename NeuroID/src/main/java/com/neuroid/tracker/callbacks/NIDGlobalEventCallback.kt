package com.neuroid.tracker.callbacks

import android.os.Build
import android.view.Window
import android.view.View
import android.view.ViewTreeObserver
import android.view.KeyEvent
import android.view.MenuItem
import android.view.MotionEvent
import android.view.Menu
import android.view.WindowManager
import android.view.ActionMode
import android.view.SearchEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.neuroid.tracker.events.NIDTouchEventManager
import com.neuroid.tracker.events.FOCUS
import com.neuroid.tracker.events.WINDOW_RESIZE
import com.neuroid.tracker.events.TEXT_CHANGE
import com.neuroid.tracker.events.BLUR
import com.neuroid.tracker.extensions.getSHA256withSalt
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.JsonUtils.Companion.getAttrJson
import com.neuroid.tracker.utils.getIdOrTag

class NIDGlobalEventCallback(
    private val windowCallback: Window.Callback,
    private val eventManager: NIDTouchEventManager,
    private val viewMainContainer: View
) : ViewTreeObserver.OnGlobalFocusChangeListener,
    ViewTreeObserver.OnGlobalLayoutListener, Window.Callback {

    private var lastEditText: EditText? = null
    private var currentWidth = 0
    private var currentHeight = 0

    override fun onGlobalFocusChanged(oldView: View?, newView: View?) {
        val ts = System.currentTimeMillis()
        if (newView != null) {
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()
            val idName = newView.getIdOrTag()

            if (newView is EditText) {
                val text = newView.text.toString()
                getDataStoreInstance()
                    .saveEvent(
                        NIDEventModel(
                            type = FOCUS,
                            tg = hashMapOf(
                                "attr" to getAttrJson(text),
                            ),
                            ts = ts,
                            tgs = idName,
                            gyro = gyroData,
                            accel = accelData
                        )
                    )

                lastEditText = if (lastEditText == null) {
                    newView
                } else {
                    lastEditText?.let {
                        registerTextChangeEvent(it.text.toString())
                    }
                    null
                }
            }
        }
    }

    override fun onGlobalLayout() {
        if (currentWidth == 0 && currentHeight == 0) {
            currentWidth = viewMainContainer.width
            currentHeight = viewMainContainer.height
        }

        if (currentWidth != viewMainContainer.width || currentHeight != viewMainContainer.height) {
            currentWidth = viewMainContainer.width
            currentHeight = viewMainContainer.height

            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            getDataStoreInstance()
                .saveEvent(
                    NIDEventModel(
                        type = WINDOW_RESIZE,
                        w = currentWidth,
                        h = currentHeight,
                        ts = System.currentTimeMillis(),
                        gyro = gyroData,
                        accel = accelData
                    )
                )
        }
    }

    private fun registerTextChangeEvent(actualText: String) {
        val ts = System.currentTimeMillis()
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = TEXT_CHANGE,
                    tg = hashMapOf(
                        "attr" to getAttrJson(actualText),
                        "etn" to lastEditText?.getIdOrTag().orEmpty(),
                        "et" to "text"
                    ),
                    tgs = lastEditText?.getIdOrTag().orEmpty(),
                    ts = ts,
                    sm = 0,
                    pd = 0,
                    v = "S~C~~${actualText.length}",
                    hv = actualText.getSHA256withSalt().take(8),
                    gyro = gyroData,
                    accel = accelData
                )
            )

        getDataStoreInstance()
            .saveEvent(
                NIDEventModel(
                    type = BLUR,
                    tgs = lastEditText?.getIdOrTag().orEmpty(),
                    tg = hashMapOf(
                        "attr" to getAttrJson(actualText),
                    ),
                    ts = ts,
                    gyro = gyroData,
                    accel = accelData
                )
            )
    }

    //WindowCallback
    override fun dispatchKeyEvent(keyEvent: KeyEvent?): Boolean {
        return windowCallback.dispatchKeyEvent(keyEvent)
    }

    override fun dispatchKeyShortcutEvent(keyEvent: KeyEvent?): Boolean {
        return windowCallback.dispatchKeyShortcutEvent(keyEvent)
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent?): Boolean {
        val view = eventManager.detectView(motionEvent, System.currentTimeMillis())

        lastEditText?.let {
            if (lastEditText != view) {
                registerTextChangeEvent(lastEditText?.text.toString())
                lastEditText = null
            }
        }
        return windowCallback.dispatchTouchEvent(motionEvent)
    }

    override fun dispatchTrackballEvent(motionEvent: MotionEvent?): Boolean {
        return windowCallback.dispatchTrackballEvent(motionEvent)
    }

    override fun dispatchGenericMotionEvent(motionEvent: MotionEvent?): Boolean {
        return windowCallback.dispatchGenericMotionEvent(motionEvent)
    }

    override fun dispatchPopulateAccessibilityEvent(accessibilityEvent: AccessibilityEvent?): Boolean {
        return windowCallback.dispatchPopulateAccessibilityEvent(accessibilityEvent)
    }

    override fun onCreatePanelView(p0: Int): View? {
        return windowCallback.onCreatePanelView(p0)
    }

    override fun onCreatePanelMenu(p0: Int, menu: Menu): Boolean {
        return windowCallback.onCreatePanelMenu(p0, menu)
    }

    override fun onPreparePanel(p0: Int, view: View?, menu: Menu): Boolean {
        return windowCallback.onPreparePanel(p0, view, menu)
    }

    override fun onMenuOpened(p0: Int, menu: Menu): Boolean {
        return windowCallback.onMenuOpened(p0, menu)
    }

    override fun onMenuItemSelected(p0: Int, menuItem: MenuItem): Boolean {
        return windowCallback.onMenuItemSelected(p0, menuItem)
    }

    override fun onWindowAttributesChanged(layoutParams: WindowManager.LayoutParams?) {
        return windowCallback.onWindowAttributesChanged(layoutParams)
    }

    override fun onContentChanged() {
        return windowCallback.onContentChanged()
    }

    override fun onWindowFocusChanged(p0: Boolean) {
        return windowCallback.onWindowFocusChanged(p0)
    }

    override fun onAttachedToWindow() {
        return windowCallback.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        return windowCallback.onDetachedFromWindow()
    }

    override fun onPanelClosed(p0: Int, menu: Menu) {
        return windowCallback.onPanelClosed(p0, menu)
    }

    override fun onSearchRequested(): Boolean {
        return windowCallback.onSearchRequested()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return windowCallback.onSearchRequested(searchEvent)
    }

    override fun onWindowStartingActionMode(actionMode: ActionMode.Callback?): ActionMode? {
        return windowCallback.onWindowStartingActionMode(actionMode)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onWindowStartingActionMode(p0: ActionMode.Callback?, p1: Int): ActionMode? {
        return windowCallback.onWindowStartingActionMode(p0, p1)
    }

    override fun onActionModeStarted(p0: ActionMode?) {
        return windowCallback.onActionModeStarted(p0)
    }

    override fun onActionModeFinished(p0: ActionMode?) {
        return windowCallback.onActionModeFinished(p0)
    }

}