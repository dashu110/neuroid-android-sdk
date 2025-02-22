package com.neuroid.tracker.events

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.ToggleButton
import android.widget.Switch
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.RatingBar
import android.widget.Button
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.children
import com.neuroid.tracker.callbacks.NIDSensorHelper
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.neuroid.tracker.utils.getIdOrTag

class NIDTouchEventManager(
    private val viewParent: ViewGroup
) {
    private var lastView: View? = null
    private var lastViewName = ""
    private var lastTypeOfView = 0

    fun detectView(motionEvent: MotionEvent?, timeMills: Long): View? {
        return motionEvent?.let {
            val currentView = getView(viewParent, motionEvent.x, motionEvent.y)
            val nameView = currentView?.getIdOrTag() ?: "main_view"
            val gyroData = NIDSensorHelper.getGyroscopeInfo()
            val accelData = NIDSensorHelper.getAccelerometerInfo()

            detectChangesOnView(currentView, timeMills, motionEvent.action)

            val typeOfView = when (currentView) {
                is EditText,
                is CheckBox,
                is RadioButton,
                is ToggleButton,
                is Switch,
                is SwitchCompat,
                is ImageButton,
                is SeekBar,
                is Spinner,
                is RatingBar
                -> 1
                is Button -> 2
                else -> 0
            }

            when (it.action) {
                ACTION_DOWN -> {
                    if (typeOfView > 0) {
                        lastViewName = nameView
                        lastTypeOfView = typeOfView
                        getDataStoreInstance()
                            .saveEvent(
                                NIDEventModel(
                                    type = TOUCH_START,
                                    ts = timeMills,
                                    tgs = nameView,
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                    ),
                                    tg = hashMapOf(
                                        "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                        "tgs" to nameView,
                                        "sender" to currentView?.javaClass?.simpleName.orEmpty()
                                    ),
                                    gyro = gyroData,
                                    accel = accelData
                                )
                            )

                        if (typeOfView == 2) {
                            getDataStoreInstance()
                                .saveEvent(
                                    NIDEventModel(
                                        type = FOCUS,
                                        ts = timeMills,
                                        tgs = lastViewName,
                                        gyro = gyroData,
                                        accel = accelData
                                    )
                                )
                        }
                    }
                }
                ACTION_MOVE -> {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = TOUCH_MOVE,
                                ts = timeMills,
                                tgs = nameView,
                                tg = hashMapOf(
                                    "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                    "tgs" to nameView,
                                    "sender" to currentView?.javaClass?.simpleName.orEmpty()
                                ),
                                touches = listOf(
                                    "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                ),
                                gyro = gyroData,
                                accel = accelData
                            )
                        )
                }
                ACTION_UP -> {
                    if (lastTypeOfView > 0) {

                        if (lastTypeOfView == 2) {
                            getDataStoreInstance()
                                .saveEvent(
                                    NIDEventModel(
                                        type = BLUR,
                                        ts = timeMills,
                                        tgs = lastViewName,
                                        gyro = gyroData,
                                        accel = accelData
                                    )
                                )
                        }

                        lastTypeOfView = 0
                        lastViewName = ""

                        getDataStoreInstance()
                            .saveEvent(
                                NIDEventModel(
                                    type = TOUCH_END,
                                    ts = timeMills,
                                    tgs = nameView,
                                    tg = hashMapOf(
                                        "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                        "tgs" to nameView,
                                        "sender" to currentView?.javaClass?.simpleName.orEmpty()
                                    ),
                                    touches = listOf(
                                        "{\"tid\":0, \"x\":${it.x},\"y\":${it.y}}"
                                    ),
                                    gyro = gyroData,
                                    accel = accelData
                                )
                            )
                    }
                }
            }
            currentView
        }
    }

    private fun getView(subView: ViewGroup, x: Float, y: Float): View? {
        val view = subView.children.firstOrNull {
            val location = IntArray(2)
            it.getLocationInWindow(location)
            (x >= location[0] && x <= location[0] + it.width && y >= location[1] && y <= location[1] + it.height)
        }

        return when (view) {
            is Spinner -> view
            is ViewGroup -> getView(view, x, y)
            else -> view
        }
    }

    private fun detectChangesOnView(currentView: View?, timeMills: Long, action: Int) {
        var type = ""
        val nameView = currentView?.getIdOrTag().orEmpty()
        val gyroData = NIDSensorHelper.getGyroscopeInfo()
        val accelData = NIDSensorHelper.getAccelerometerInfo()

        if (action == ACTION_UP) {
            if (lastView == currentView) {
                when (currentView) {
                    is CheckBox, is AppCompatCheckBox -> {
                        type = CHECKBOX_CHANGE
                        Log.i(
                            NIDLog.CHECK_BOX_CHANGE_TAG,
                            NIDLog.CHECK_BOX_ID + currentView.getIdOrTag()
                        )
                    }
                    is RadioButton -> {
                        type = RADIO_CHANGE
                        Log.i(
                            NIDLog.RADIO_BUTTON_CHANGE_TAG,
                            NIDLog.RADIO_BUTTON_ID + currentView.getIdOrTag()
                        )
                    }
                    is Switch, is SwitchCompat -> {
                        type = SWITCH_CHANGE
                    }
                    is ToggleButton -> {
                        type = TOGGLE_BUTTON_CHANGE
                    }
                    is RatingBar -> {
                        type = RATING_BAR_CHANGE
                    }
                    is SeekBar -> {
                        type = SLIDER_CHANGE
                    }
                    else -> {
                        // Null
                    }
                }

               /* if (type.isNotEmpty()) {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = type,
                                tg = hashMapOf(
                                    "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                    "tgs" to nameView,
                                    "sender" to currentView?.javaClass?.simpleName.orEmpty()
                                ),
                                tgs = nameView,
                                ts = timeMills,
                                gyro = gyroData,
                                accel = accelData
                            )
                        )
                }*/
            } else {
                /*if (lastView is SeekBar) {
                    getDataStoreInstance()
                        .saveEvent(
                            NIDEventModel(
                                type = SLIDER_CHANGE,
                                tg = hashMapOf(
                                    "etn" to currentView?.javaClass?.simpleName.orEmpty(),
                                    "tgs" to nameView,
                                    "sender" to currentView?.javaClass?.simpleName.orEmpty()
                                ),
                                tgs = nameView,
                                v = ((lastView as SeekBar).progress).toString(),
                                ts = System.currentTimeMillis(),
                                gyro = gyroData,
                                accel = accelData
                            )
                        )
                }*/
            }
            lastView = null
        } else if (action == ACTION_DOWN) {
            lastView = currentView
        }
    }
}