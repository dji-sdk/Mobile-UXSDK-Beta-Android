/*
 * Copyright (c) 2018-2021 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.dji.ux.beta.sample.showcase.core

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import butterknife.*
import com.dji.ux.beta.sample.R
import com.dji.ux.beta.sample.view.ColorPickerTileView
import com.dji.ux.beta.sample.view.SettingsDrawerView
import com.flask.colorpicker.OnColorSelectedListener
import dji.common.airlink.PhysicalSource
import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.SettingsDefinitions
import dji.common.product.Model
import dji.keysdk.AirLinkKey
import dji.keysdk.CameraKey
import dji.keysdk.DJIKey
import dji.keysdk.KeyManager
import dji.sdk.camera.Camera
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.communication.OnStateChangeCallback
import dji.ux.beta.core.extension.hide
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget
import dji.ux.beta.core.ui.CenterPointView
import dji.ux.beta.core.ui.GridLineView
import dji.ux.beta.core.ui.SeekBarView
import dji.ux.beta.core.util.ProductUtil
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import dji.ux.beta.core.util.SettingDefinitions.GimbalIndex
import dji.ux.beta.core.widget.compass.CompassWidget
import dji.ux.beta.core.widget.fpv.FPVWidget
import dji.ux.beta.core.widget.fpv.FPVWidget.ModelState.CameraSideUpdated
import dji.ux.beta.core.widget.radar.RadarWidget
import java.util.*

/**
 * Displays the widgets in the core module and controls to customize the look of each of the widgets.
 */
class CoreWidgetsActivity : AppCompatActivity() {

    //region Fields
    @BindView(R.id.widget_radar)
    lateinit var radarWidget: RadarWidget

    @BindView(R.id.widget_fpv)
    lateinit var fpvWidget: FPVWidget

    @BindView(R.id.widget_panel_system_status_list)
    lateinit var systemStatusListPanelWidget: SystemStatusListPanelWidget

    @BindView(R.id.widget_compass)
    lateinit var compassWidget: CompassWidget

    @BindView(R.id.settings_scroll_view)
    lateinit var scrollView: SettingsDrawerView

    @BindView(R.id.btn_settings)
    lateinit var btnPanel: ImageView

    @BindView(R.id.btn_fpv_grid_lines_color)
    lateinit var gridLinesColorView: ColorPickerTileView

    @BindView(R.id.seekbar_fpv_grid_lines_size)
    lateinit var gridLinesWidthSeekBarView: SeekBarView

    @BindView(R.id.spinner_fpv_center_point_icon)
    lateinit var centerPointIconSpinner: Spinner

    @BindView(R.id.btn_fpv_center_point_color)
    lateinit var centerPointColorView: ColorPickerTileView

    @BindView(R.id.textview_fpv_stream_source)
    lateinit var fpvStreamSourceTextView: TextView

    @BindView(R.id.spinner_fpv_stream_source)
    lateinit var fpvStreamSourceSpinner: Spinner

    @BindView(R.id.textview_assign_source)
    lateinit var assignSourceTextView: TextView

    @BindView(R.id.spinner_assign_source)
    lateinit var assignSourceSpinner: Spinner

    @BindView(R.id.textview_fpv_orientation)
    lateinit var fpvOrientationTextView: View

    @BindView(R.id.spinner_fpv_orientation)
    lateinit var fpvOrientationSpinner: Spinner

    private var compositeDisposable: CompositeDisposable? = null

    private val cameraCombinations: MutableList<CameraCombination> = ArrayList()
    private val lbBandwidthKey: DJIKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT)
    private val leftCameraBandwidthKey: DJIKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LEFT_CAMERA)
    private val mainCameraBandwidthKey: DJIKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_PRIMARY_VIDEO)
    private val assignPrimarySourceKey: DJIKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.ASSIGN_SOURCE_TO_PRIMARY_CHANNEL)
    private var cameraIndex: CameraIndex = CameraIndex.CAMERA_INDEX_0
    private var gimbalIndex: GimbalIndex = GimbalIndex.PORT
    //endregion

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_core_widgets)
        ButterKnife.bind(this)
        setM200SeriesWarningLevelRanges()

        // Set up top bar state callbacks
        val topBarPanel = findViewById<TopBarPanelWidget>(R.id.panel_top_bar)
        val systemStatusWidget = topBarPanel.systemStatusWidget
        systemStatusWidget?.stateChangeCallback =
                    findViewById<SystemStatusListPanelWidget>(R.id.widget_panel_system_status_list)
                            as OnStateChangeCallback<Any>

        initColorPickerTileListeners()
        initCenterPointSpinners()
        initSettingsFromPreferences()

        updateOrientationVisibility()

        val seekBarChangeListener = object : SeekBarView.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBarView: SeekBarView, progress: Int, isFromUI: Boolean) {
                gridLinesWidthSeekBarView.text = progress.toString()
                fpvWidget.gridLineView.lineWidth = progress.toFloat()
            }

            override fun onMinusClicked(seekBarView: SeekBarView) {
                //No implementation
            }

            override fun onPlusClicked(seekBarView: SeekBarView) {
                //No implementation
            }

            override fun onStartTrackingTouch(seekBarView: SeekBarView, progress: Int) {
                //No implementation
            }

            override fun onStopTrackingTouch(seekBarView: SeekBarView, progress: Int) {
                //No implementation
            }
        }
        gridLinesWidthSeekBarView.text = gridLinesWidthSeekBarView.progress.toString()
        gridLinesWidthSeekBarView.addOnSeekBarChangeListener(seekBarChangeListener)
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable = CompositeDisposable()
        compositeDisposable?.add(systemStatusListPanelWidget.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { pressed: Boolean ->
                    if (pressed) {
                        systemStatusListPanelWidget.hide()
                    }
                })
        compositeDisposable?.add(fpvWidget.getWidgetStateUpdate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { modelState: FPVWidget.ModelState? ->
                    if (modelState is CameraSideUpdated) {
                        val (cameraSide) = modelState
                        val gimbalIndex: GimbalIndex
                        val cameraIndex: CameraIndex
                        when (cameraSide) {
                            SettingDefinitions.CameraSide.PORT -> {
                                gimbalIndex = GimbalIndex.PORT
                                cameraIndex = CameraIndex.CAMERA_INDEX_0
                            }
                            SettingDefinitions.CameraSide.STARBOARD -> {
                                gimbalIndex = GimbalIndex.STARBOARD
                                cameraIndex = CameraIndex.CAMERA_INDEX_2
                            }
                            else -> {
                                gimbalIndex = GimbalIndex.TOP
                                cameraIndex = CameraIndex.CAMERA_INDEX_4
                            }
                        }
                        if (gimbalIndex != gimbalIndex) {
                            setGimbalIndex(gimbalIndex)
                        }
                        if (this.cameraIndex != cameraIndex) {
                            setCameraIndex(cameraIndex)
                        }
                    }
                })
        compositeDisposable?.add(DJISDKModel.getInstance().addListener(CameraKey.create(CameraKey.CONNECTION), this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ initAssignSourceSpinner() }) { })
    }

    override fun onPause() {
        compositeDisposable?.dispose()
        compositeDisposable = null
        super.onPause()
    }
    //endregion

    //region Utils
    private fun setM200SeriesWarningLevelRanges() {
        val m200SeriesModels = arrayOf(
                Model.MATRICE_200,
                Model.MATRICE_210,
                Model.MATRICE_210_RTK,
                Model.MATRICE_200_V2,
                Model.MATRICE_210_V2,
                Model.MATRICE_210_RTK_V2
        )
        val ranges = floatArrayOf(70f, 30f, 20f, 12f, 6f, 3f)
        radarWidget.setWarningLevelRanges(m200SeriesModels, ranges)
    }

    @OnClick(R.id.btn_settings)
    fun onSettingsClick() {
        scrollView.movePanel(btnPanel)
    }

    @OnCheckedChanged(R.id.switch_fpv_grid_lines)
    fun onGridLinesCheckedChanged(checked: Boolean) {
        fpvWidget.isGridLinesEnabled = checked
    }

    @OnItemSelected(R.id.spinner_fpv_grid_lines)
    fun onGridLineTypeSelected(position: Int) {
        fpvWidget.gridLineView.type = GridLineView.GridLineType.find(position)
    }

    @OnCheckedChanged(R.id.switch_fpv_center_point)
    fun onCenterPointCheckedChanged(checked: Boolean) {
        fpvWidget.isCenterPointEnabled = checked
    }

    @OnItemSelected(R.id.spinner_fpv_center_point_icon)
    fun onCenterPointIconSelected(position: Int) {
        fpvWidget.centerPointView.type = CenterPointView.CenterPointType.find(position)
    }

    @OnItemSelected(R.id.spinner_fpv_source)
    fun onFPVSourceSelected(position: Int) {
        fpvWidget.videoSource = SettingDefinitions.VideoSource.find(position)
    }

    @OnItemSelected(R.id.spinner_fpv_stream_source)
    fun onFPVStreamSourceSelected(position: Int) {
        val streamSource = CameraVideoStreamSource.find(position + 1)
        fpvWidget.setCameraVideoStreamSource(streamSource)
    }

    @OnItemSelected(R.id.spinner_assign_source)
    fun onAssignSourceSelected(position: Int) {
        when (cameraCombinations[position]) {
            CameraCombination.PORT_FPV -> portAndFpv()
            CameraCombination.STARBOARD_FPV -> starboardAndFpv()
            CameraCombination.PORT_STARBOARD -> portAndStarboard()
            CameraCombination.TOP_FPV -> {
                KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                        PhysicalSource.TOP_CAM, PhysicalSource.FPV_CAM)
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 1.0f, null)
            }
            CameraCombination.PORT_TOP -> {
                KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                        PhysicalSource.LEFT_CAM, PhysicalSource.TOP_CAM)
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.5f, null)
            }
            CameraCombination.STARBOARD_TOP -> {
                KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                        PhysicalSource.RIGHT_CAM, PhysicalSource.TOP_CAM)
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.5f, null)
            }
        }
    }

    @OnItemSelected(R.id.spinner_fpv_orientation)
    fun onFPVOrientationSelected(position: Int) {
        val orientationKey: DJIKey = CameraKey.create(CameraKey.ORIENTATION, cameraIndex.index)
        val orientation = SettingsDefinitions.Orientation.find(position)
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(orientationKey, orientation, null)
        }
    }

    private fun initColorPickerTileListeners() {
        gridLinesColorView.onColorSelectedListener = OnColorSelectedListener { selectedColor ->
            fpvWidget.gridLineView.lineColor = selectedColor
        }
        centerPointColorView.onColorSelectedListener = OnColorSelectedListener { selectedColor ->
            fpvWidget.centerPointView.color = selectedColor
        }
    }

    private fun initCenterPointSpinners() {
        val adapter = ImageArrayAdapter(this, arrayOf(0, R.drawable.uxsdk_ic_centerpoint_standard,
                R.drawable.uxsdk_ic_centerpoint_cross,
                R.drawable.uxsdk_ic_centerpoint_narrow_cross,
                R.drawable.uxsdk_ic_centerpoint_frame,
                R.drawable.uxsdk_ic_centerpoint_frame_and_cross,
                R.drawable.uxsdk_ic_centerpoint_square,
                R.drawable.uxsdk_ic_centerpoint_square_and_cross))
        centerPointIconSpinner.adapter = adapter
    }

    private fun initSettingsFromPreferences() {
        val gridLineSwitch = findViewById<Switch>(R.id.switch_fpv_grid_lines)
        gridLineSwitch.isChecked = fpvWidget.isGridLinesEnabled
        val gridTypeSpinner = findViewById<Spinner>(R.id.spinner_fpv_grid_lines)
        gridTypeSpinner.setSelection(fpvWidget.gridLineView.type.value)
        val centerPointSwitch = findViewById<Switch>(R.id.switch_fpv_center_point)
        centerPointSwitch.isChecked = fpvWidget.isCenterPointEnabled
        val centerPointTypeSpinner = findViewById<Spinner>(R.id.spinner_fpv_center_point_icon)
        centerPointTypeSpinner.setSelection(fpvWidget.centerPointView.type.value)
        gridLinesColorView.tileColor = fpvWidget.gridLineView.lineColor
        gridLinesWidthSeekBarView.progress = fpvWidget.gridLineView.lineWidth.toInt()
        centerPointColorView.tileColor = fpvWidget.centerPointView.color
    }

    private fun updateOrientationVisibility() {
        if (!ProductUtil.isProductAvailable() ||
                Model.MAVIC_PRO != DJISDKManager.getInstance().product.model) {
            fpvOrientationTextView.visibility = View.GONE
            fpvOrientationSpinner.visibility = View.GONE
        }
    }

    private fun portAndFpv() {
        if (isM200V2OrM300()) {
            KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                    PhysicalSource.LEFT_CAM, PhysicalSource.FPV_CAM)
            KeyManager.getInstance().setValue(mainCameraBandwidthKey, 1.0f, null)
        } else {
            KeyManager.getInstance().setValue(lbBandwidthKey, 0.8f, null)
            KeyManager.getInstance().setValue(leftCameraBandwidthKey, 1.0f, null)
        }
    }

    private fun starboardAndFpv() {
        if (isM200V2OrM300()) {
            KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                    PhysicalSource.RIGHT_CAM, PhysicalSource.FPV_CAM)
            KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.0f, null)
        } else {
            KeyManager.getInstance().setValue(lbBandwidthKey, 0.8f, null)
            KeyManager.getInstance().setValue(leftCameraBandwidthKey, 0.0f, null)
        }
    }

    private fun portAndStarboard() {
        if (isM200V2OrM300()) {
            KeyManager.getInstance().performAction(assignPrimarySourceKey, null,
                    PhysicalSource.LEFT_CAM, PhysicalSource.RIGHT_CAM)
            KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.5f, null)
        } else {
            KeyManager.getInstance().setValue(lbBandwidthKey, 1.0f, null)
            KeyManager.getInstance().setValue(leftCameraBandwidthKey, 0.5f, null)
        }
    }

    private fun isM200V2OrM300(): Boolean {
        return if (ProductUtil.isProductAvailable()) {
            val model = DJISDKManager.getInstance().product.model
            ProductUtil.isM200V2OrM300(model)
        } else {
            false
        }
    }

    private fun setCameraIndex(cameraIndex: CameraIndex) {
        this.cameraIndex = cameraIndex;
        initFPVStreamSourceSpinner(cameraIndex);
    }

    private fun setGimbalIndex(gimbalIndex: GimbalIndex) {
        this.gimbalIndex = gimbalIndex
        compassWidget.setGimbalIndex(gimbalIndex)
    }

    private fun initFPVStreamSourceSpinner(cameraIndex: CameraIndex) {
        val cameraKey: DJIKey = CameraKey.create(CameraKey.DISPLAY_NAME, cameraIndex.index)
        val displayName = KeyManager.getInstance().getValue(cameraKey) as String?
        val list: Array<String>?
        when (displayName) {
            Camera.DisplayNameZenmuseH20 -> {
                list = resources.getStringArray(R.array.fpv_source_h20_array)
                fpvStreamSourceSpinner.visibility = View.VISIBLE
                fpvStreamSourceTextView.visibility = View.VISIBLE
            }
            Camera.DisplayNameZenmuseH20T -> {
                list = resources.getStringArray(R.array.fpv_source_h20t_array)
                fpvStreamSourceSpinner.visibility = View.VISIBLE
                fpvStreamSourceTextView.visibility = View.VISIBLE
            }
            else -> {
                list = null
                fpvStreamSourceSpinner.visibility = View.GONE
                fpvStreamSourceTextView.visibility = View.GONE
            }
        }
        if (list != null) {
            val fpvSourceAdapter = ArrayAdapter(this,
                    android.R.layout.simple_spinner_item, list)
            fpvSourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fpvStreamSourceSpinner.adapter = fpvSourceAdapter
        }
    }

    private fun initAssignSourceSpinner() {
        initCameraCombinations()
        if (cameraCombinations.size < 2) {
            assignSourceSpinner.visibility = View.GONE
            assignSourceTextView.visibility = View.GONE
        } else {
            assignSourceSpinner.visibility = View.VISIBLE
            assignSourceTextView.visibility = View.VISIBLE
            val strings: MutableList<String> = ArrayList()
            for (cameraCombination in cameraCombinations) {
                strings.add(getString(cameraCombination.displayName))
            }
            val assignSourceAdapter = ArrayAdapter(this,
                    android.R.layout.simple_spinner_item, strings)
            assignSourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            assignSourceSpinner.adapter = assignSourceAdapter
        }
    }

    private fun initCameraCombinations() {
        cameraCombinations.clear()
        val product = DJISDKManager.getInstance().product
        if (product is Aircraft) {
            val portCamera = product.getCameraWithComponentIndex(0)
            val starboardCamera = product.getCameraWithComponentIndex(1)
            val topCamera = product.getCameraWithComponentIndex(4)
            if (portCamera != null && portCamera.isConnected) {
                cameraCombinations.add(CameraCombination.PORT_FPV)
                if (starboardCamera != null && starboardCamera.isConnected) {
                    cameraCombinations.add(CameraCombination.PORT_STARBOARD)
                }
                if (topCamera != null && topCamera.isConnected) {
                    cameraCombinations.add(CameraCombination.PORT_TOP)
                }
            }
            if (starboardCamera != null && starboardCamera.isConnected) {
                cameraCombinations.add(CameraCombination.STARBOARD_FPV)
                if (topCamera != null && topCamera.isConnected) {
                    cameraCombinations.add(CameraCombination.STARBOARD_TOP)
                }
            }
            if (topCamera != null && topCamera.isConnected) {
                cameraCombinations.add(CameraCombination.TOP_FPV)
            }
        }
    }
    //endregion

    //region classes
    private enum class CameraCombination(@field:StringRes @param:StringRes val displayName: Int) {
        PORT_FPV(R.string.port_fpv),
        STARBOARD_FPV(R.string.starboard_fpv),
        PORT_STARBOARD(R.string.port_starboard),
        TOP_FPV(R.string.top_fpv),
        PORT_TOP(R.string.port_top),
        STARBOARD_TOP(R.string.starboard_top);
    }
    //endregion

    companion object {
        private const val TAG = "CoreWidgetsActivity"
    }
}