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
package dji.ux.beta.core.widget.fpv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import dji.common.airlink.PhysicalSource
import dji.common.airlink.VideoFeedPriority
import dji.common.camera.CameraVideoStreamSource
import dji.common.camera.ResolutionAndFrameRate
import dji.common.camera.SettingsDefinitions
import dji.common.camera.SettingsDefinitions.CameraMode
import dji.common.camera.SettingsDefinitions.FlatCameraMode
import dji.common.camera.SettingsDefinitions.PhotoAspectRatio
import dji.common.error.DJIError
import dji.common.product.Model
import dji.common.util.CommonCallbacks
import dji.keysdk.AirLinkKey
import dji.keysdk.CameraKey
import dji.keysdk.ProductKey
import dji.keysdk.RemoteControllerKey
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.camera.VideoFeeder.*
import dji.sdk.codec.DJICodecManager
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.observers.TestObserver
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.base.UXSDKError
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.module.FlatCameraModule
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.util.SettingDefinitions.CameraIndex
import dji.ux.beta.core.util.SettingDefinitions.CameraSide
import io.mockk.every
import io.mockk.spyk
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [FPVWidgetModel]
 * 1. [FPVWidgetModelTest.fpvWidgetModel_videoViewAndOrientation_changesWithOrientation]
 * Test that the orientation and the video view values change as expected with a change in orientation.
 *
 * 2. [FPVWidgetModelTest.fpvWidgetModel_videoView_changesWithPhotoAspectRatio]
 * Test that the video view values change as expected with a change in the photo aspect ratio.
 *
 * 3. [FPVWidgetModelTest.fpvWidgetModel_videoView_changesWithCameraMode]
 * Test that the video view values change as expected with a change in the camera mode.
 *
 * 4. [FPVWidgetModelTest.fpvWidgetModel_videoView_changesWithFlatCameraMode]
 * Test that the video view values change as expected with a change in the flat camera mode.
 *
 * 5. [FPVWidgetModelTest.fpvWidgetModel_videoView_changesWithResolutionAndFrameRate]
 * Test that the video view values change as expected with a change in the resolution and frame rate.
 *
 * 6. [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_changesWithModelName]
 * Test that the camera display name, camera index and model name changes as expected with a change in model name.
 *
 * 7. [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_changesWithUpdateState]
 * Test that the camera display name and camera index changes as expected when the updateStates function is called.
 *
 * 8. - 9.
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forInspire2MainCam_isCorrect]
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forInspire2FPVCam_isCorrect]
 * Test that the camera display name and camera index changes as expected when the for an Inspire 2's main and FPV
 * cameras.
 *
 * 10. - 13.
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forExtSupportedMainCam_isCorrect]
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forExtSupportedHDMICam_isCorrect]
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forExtSupportedExtCam_isCorrect]
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forExtSupportedAVCam_isCorrect]
 * Test that the camera display name and index changes as expected when the physical source is an external source.
 *
 * 14. - 17.
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameSideAndIndex_forM210LeftCam_isCorrect]
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameSideAndIndex_forM210RightCam_isCorrect]
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forM210FPVCam_isCorrect]
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forM210MainCam_isCorrect]
 * Test that the camera display name, index and side changes as expected when the for an M210's camera for
 * both the gimbals and the FPV.
 *
 * 18.
 * [FPVWidgetModelTest.fpvWidgetModel_cameraDisplayNameAndIndex_forUnknownPhysicalSource_isCorrect]
 * Test that the camera display name and index changes as expected when the camera's physical source is unknown.
 *
 * 19. - 22.
 * [FPVWidgetModelTest.fpvWidgetModel_videoFeedSource_changesWithVideoSourceSetToSecondary]
 * [FPVWidgetModelTest.fpvWidgetModel_videoFeedSource_changesWithVideoSourceSetToAutoWithoutExtSupportedProduct]
 * [FPVWidgetModelTest.fpvWidgetModel_videoFeedSource_changesWithVideoSourceSetToAutoWithExtSupportedProduct]
 * [FPVWidgetModelTest.fpvWidgetModel_videoFeedSource_changesWithVideoSourceSetToAutoWithExtSupportedProductAndEnableExt]
 * Test that the video feed changes with video source as expected and also changes the isPrimaryVideoFeed processor.
 *
 * 23. [FPVWidgetModelTest.fpvWidgetModel_setStreamSource_success]
 * Test that the stream source is updated successfully
 *
 * 24. [FPVWidgetModelTest.fpvWidgetModel_setStreamSource_error]
 * Test that updating the stream source fails
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class FPVWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var widgetModel: FPVWidgetModel
    private lateinit var testScheduler: TestScheduler
    private lateinit var testVideoDataListener: VideoDataListener
    private lateinit var testVideoFeed: VideoFeed

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        testVideoDataListener = VideoDataListener { _: ByteArray?, _: Int -> }
        val testTranscodedVideoDataListener = VideoDataListener { _: ByteArray?, _: Int -> }
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = spyk(FPVWidgetModel(djiSdkModel, keyedStore, testVideoDataListener,
                testTranscodedVideoDataListener, spyk(FlatCameraModule())), recordPrivateCalls = true)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)
    }

    @Test
    fun fpvWidgetModel_videoViewAndOrientation_changesWithOrientation() {
        setEmptyValues()
        val testOrientationValue = SettingsDefinitions.Orientation.PORTRAIT
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.ORIENTATION),
                testOrientationValue,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoView = widgetModel.hasVideoViewChanged.test()
        val testSubscriberOrientation = widgetModel.orientation.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true)
        testSubscriberOrientation.assertValue(SettingsDefinitions.Orientation.UNKNOWN) //Initialization value
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true, true)
        testSubscriberOrientation.assertValues(SettingsDefinitions.Orientation.UNKNOWN, testOrientationValue)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoView)
        compositeDisposable.add(testSubscriberOrientation)
    }

    @Test
    fun fpvWidgetModel_videoView_changesWithPhotoAspectRatio() {
        setEmptyValues()
        val testPhotoAspectRatio = PhotoAspectRatio.RATIO_16_9
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.PHOTO_ASPECT_RATIO, CameraIndex.CAMERA_INDEX_UNKNOWN.index),
                testPhotoAspectRatio,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoView = widgetModel.hasVideoViewChanged.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoView)
    }

    @Test
    fun fpvWidgetModel_videoView_changesWithCameraMode() {
        setEmptyValues()
        val testCameraMode = CameraMode.SHOOT_PHOTO
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.MODE),
                testCameraMode,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoView = widgetModel.hasVideoViewChanged.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoView)
    }

    @Test
    fun fpvWidgetModel_videoView_changesWithFlatCameraMode() {
        setEmptyValues()
        val testCameraFlatMode = FlatCameraMode.PHOTO_SINGLE
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.IS_FLAT_CAMERA_MODE_SUPPORTED),
                true,
                10,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.FLAT_CAMERA_MODE),
                testCameraFlatMode,
                20,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoView = widgetModel.hasVideoViewChanged.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true, true)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true, true, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoView)
    }

    @Test
    fun fpvWidgetModel_videoView_changesWithResolutionAndFrameRate() {
        setEmptyValues()
        val testResolutionAndFrameRate = ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.RESOLUTION_640x480,
                SettingsDefinitions.VideoFrameRate.FRAME_RATE_24_FPS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, CameraIndex.CAMERA_INDEX_UNKNOWN.index),
                testResolutionAndFrameRate,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoView = widgetModel.hasVideoViewChanged.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoView.assertValues(false, true, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoView)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_changesWithModelName() {
        setEmptyValues()
        mockVideoFeederFunctions()
        val testModel = Model.INSPIRE_1
        val testCamera0Name = Camera.DisplayNameX3
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                testModel,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        val testSubscriberModel = widgetModel.model.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        testSubscriberModel.assertValue(Model.UNKNOWN_AIRCRAFT) //Initialization value
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        testSubscriberModel.assertValues(Model.UNKNOWN_AIRCRAFT, testModel)
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_0.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
        compositeDisposable.add(testSubscriberModel)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_changesWithUpdateState() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.INSPIRE_1,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        val testCamera0Name = Camera.DisplayNameX3
        val testCamera1Name = Camera.DisplayNameX5
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_0.index)
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera1Name)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera1Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_0.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forInspire2MainCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.INSPIRE_2,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        val testCamera0Name = Camera.DisplayNameX4S
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_0.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forInspire2FPVCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.INSPIRE_2,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        Mockito.doReturn(PhysicalSource.FPV_CAM).`when`(testVideoFeed).videoSource
        val testCamera0Name = PhysicalSource.FPV_CAM.toString()
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forExtSupportedMainCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_600,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        every { widgetModel["isExtPortSupportedProduct"]() } returns true
        val testCamera0Name = Camera.DisplayNameX4S
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forExtSupportedExtCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_600,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        Mockito.doReturn(PhysicalSource.EXT).`when`(testVideoFeed).videoSource
        every { widgetModel["isExtPortSupportedProduct"]() } returns true
        val testCamera0Name = PhysicalSource.EXT.toString()
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_2.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forExtSupportedHDMICam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_600,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        Mockito.doReturn(PhysicalSource.HDMI).`when`(testVideoFeed).videoSource
        every { widgetModel["isExtPortSupportedProduct"]() } returns true
        val testCamera0Name = PhysicalSource.HDMI.toString()
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_2.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forExtSupportedAVCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_600,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        Mockito.doReturn(PhysicalSource.AV).`when`(testVideoFeed).videoSource
        every { widgetModel["isExtPortSupportedProduct"]() } returns true
        val testCamera0Name = PhysicalSource.AV.toString()
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_2.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameSideAndIndex_forM210LeftCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_210,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        Mockito.doReturn(PhysicalSource.LEFT_CAM).`when`(testVideoFeed).videoSource
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        val testCamera0Name = Camera.DisplayNameX4S
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        val testSubscriberCameraSide = widgetModel.cameraSide.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        testSubscriberCameraSide.assertValue(CameraSide.UNKNOWN)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_0.index)
        testSubscriberCameraSide.assertValueAt(testSubscriberCameraSide.valueCount() - 1
        ) { testCameraSide: CameraSide -> testCameraSide == CameraSide.PORT }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
        compositeDisposable.add(testSubscriberCameraSide)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameSideAndIndex_forM210RightCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_210,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        Mockito.doReturn(PhysicalSource.RIGHT_CAM).`when`(testVideoFeed).videoSource
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        val testCamera1Name = Camera.DisplayNameZ30
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 1)))
                .thenReturn(testCamera1Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        val testSubscriberCameraSide = widgetModel.cameraSide.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        testSubscriberCameraSide.assertValue(CameraSide.UNKNOWN)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera1Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_2.index)
        testSubscriberCameraSide.assertValueAt(testSubscriberCameraSide.valueCount() - 1
        ) { testCameraSide: CameraSide ->
            (testCameraSide
                    == CameraSide.STARBOARD)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
        compositeDisposable.add(testSubscriberCameraSide)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forM210FPVCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_210,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        Mockito.doReturn(PhysicalSource.FPV_CAM).`when`(testVideoFeed).videoSource
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        val testCamera0Name = PhysicalSource.FPV_CAM.toString()
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forM210MainCam_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_210,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        val testCamera0Name = Camera.DisplayNameX4S
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_0.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_cameraDisplayNameAndIndex_forUnknownPhysicalSource_isCorrect() {
        setEmptyValues()
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.INSPIRE_2,
                10,
                TimeUnit.SECONDS) //Required to update videoFeed value
        mockVideoFeederFunctions()
        Mockito.doReturn(PhysicalSource.UNKNOWN).`when`(testVideoFeed).videoSource
        val testCamera0Name = PhysicalSource.UNKNOWN.toString()
        Mockito.`when`(djiSdkModel.getCacheValue(CameraKey.create(CameraKey.DISPLAY_NAME, 0)))
                .thenReturn(testCamera0Name)
        widgetModel.setup()
        val testSubscriberCameraName = widgetModel.cameraName.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValue("")
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.updateStates()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberCameraName.assertValueAt(testSubscriberCameraName.valueCount() - 1
        ) { testCameraName: String -> testCameraName == testCamera0Name }
        Assert.assertEquals(widgetModel.currentCameraIndex.index, CameraIndex.CAMERA_INDEX_UNKNOWN.index)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberCameraName)
    }

    @Test
    fun fpvWidgetModel_videoFeedSource_changesWithVideoSourceSetToSecondary() {
        setEmptyValues()
        val videoFeeder = Mockito.mock(VideoFeeder::class.java, Mockito.RETURNS_DEEP_STUBS)
        testVideoFeed = Mockito.spy(MockVideoFeed())
        Mockito.`when`(videoFeeder.secondaryVideoFeed).thenReturn(testVideoFeed)
        every { widgetModel["getVideoFeeder"]() } returns videoFeeder
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_210,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoFeedSource = widgetModel.videoFeedSource.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoFeedSource.assertValue(DJICodecManager.VideoSource.UNKNOWN)
        widgetModel.videoSource = SettingDefinitions.VideoSource.SECONDARY
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoFeedSource.assertValues(DJICodecManager.VideoSource.UNKNOWN, DJICodecManager.VideoSource.FPV)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoFeedSource)
    }

    @Test
    fun fpvWidgetModel_videoFeedSource_changesWithVideoSourceSetToAutoWithoutExtSupportedProduct() {
        setEmptyValues()
        val videoFeeder = Mockito.mock(VideoFeeder::class.java, Mockito.RETURNS_DEEP_STUBS)
        testVideoFeed = Mockito.spy(MockVideoFeed())
        Mockito.`when`(videoFeeder.primaryVideoFeed).thenReturn(testVideoFeed)
        every { widgetModel["getVideoFeeder"]() } returns videoFeeder
        every { widgetModel["isExtPortSupportedProduct"]() } returns false
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_210,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoFeedSource = widgetModel.videoFeedSource.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoFeedSource.assertValue(DJICodecManager.VideoSource.UNKNOWN)
        widgetModel.videoSource = SettingDefinitions.VideoSource.AUTO
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoFeedSource.assertValues(DJICodecManager.VideoSource.UNKNOWN, DJICodecManager.VideoSource.CAMERA)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoFeedSource)
    }

    @Test
    fun fpvWidgetModel_videoFeedSource_changesWithVideoSourceSetToAutoWithExtSupportedProduct() {
        setEmptyValues()
        val videoFeeder = Mockito.mock(VideoFeeder::class.java, Mockito.RETURNS_DEEP_STUBS)
        testVideoFeed = Mockito.spy(MockVideoFeed())
        Mockito.`when`(videoFeeder.secondaryVideoFeed).thenReturn(testVideoFeed)
        every { widgetModel["getVideoFeeder"]() } returns videoFeeder
        every { widgetModel["isExtPortSupportedProduct"]() } returns true
        Mockito.`when`(djiSdkModel.getCacheValue(AirLinkKey.createLightbridgeLinkKey(AirLinkKey.IS_EXT_VIDEO_INPUT_PORT_ENABLED)))
                .thenReturn(true)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT),
                0.0f,
                null)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_600,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoFeedSource = widgetModel.videoFeedSource.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoFeedSource.assertValue(DJICodecManager.VideoSource.UNKNOWN)
        widgetModel.videoSource = SettingDefinitions.VideoSource.AUTO
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoFeedSource.assertValues(DJICodecManager.VideoSource.UNKNOWN, DJICodecManager.VideoSource.FPV)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoFeedSource)
    }

    @Test
    fun fpvWidgetModel_videoFeedSource_changesWithVideoSourceSetToAutoWithExtSupportedProductAndEnableExt() {
        setEmptyValues()
        val videoFeeder = Mockito.mock(VideoFeeder::class.java, Mockito.RETURNS_DEEP_STUBS)
        testVideoFeed = Mockito.spy(MockVideoFeed())
        Mockito.`when`(videoFeeder.secondaryVideoFeed).thenReturn(testVideoFeed)
        every { widgetModel["getVideoFeeder"]() } returns videoFeeder
        every { widgetModel["isExtPortSupportedProduct"]() } returns true
        Mockito.`when`(djiSdkModel.getCacheValue(AirLinkKey.createLightbridgeLinkKey(AirLinkKey.IS_EXT_VIDEO_INPUT_PORT_ENABLED)))
                .thenReturn(false)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.IS_EXT_VIDEO_INPUT_PORT_ENABLED),
                true,
                null)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT),
                0.0f,
                null)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_600,
                10,
                TimeUnit.SECONDS)
        widgetModel.setup()
        val testSubscriberVideoFeedSource = widgetModel.videoFeedSource.test()
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVideoFeedSource.assertValue(DJICodecManager.VideoSource.UNKNOWN)
        widgetModel.videoSource = SettingDefinitions.VideoSource.AUTO
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriberVideoFeedSource.assertValues(DJICodecManager.VideoSource.UNKNOWN, DJICodecManager.VideoSource.FPV)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVideoFeedSource)
    }

    @Test
    fun fpvWidgetModel_setStreamSource_success() {
        setEmptyValues()
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_VIDEO_STREAM_SOURCE, CameraIndex.CAMERA_INDEX_UNKNOWN.index),
                CameraVideoStreamSource.INFRARED_THERMAL, null)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel
                .setCameraVideoStreamSource(CameraVideoStreamSource.INFRARED_THERMAL).test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertComplete()
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @Test
    fun fpvWidgetModel_setStreamSource_error() {
        setEmptyValues()
        val uxsdkError = UXSDKError(DJIError.COMMON_EXECUTION_FAILED)
        WidgetTestUtil.setEmittedSetValue(djiSdkModel,
                CameraKey.create(CameraKey.CAMERA_VIDEO_STREAM_SOURCE, CameraIndex.CAMERA_INDEX_UNKNOWN.index),
                CameraVideoStreamSource.INFRARED_THERMAL, uxsdkError)
        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        val observer: TestObserver<*> = widgetModel
                .setCameraVideoStreamSource(CameraVideoStreamSource.INFRARED_THERMAL).test()
        testScheduler.advanceTimeBy(20, TimeUnit.SECONDS)
        observer.assertError(uxsdkError)
        widgetModel.cleanup()
        compositeDisposable.add(observer)
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    //Helper functions
    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, ProductKey.create(ProductKey.MODEL_NAME))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.ORIENTATION))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.PHOTO_ASPECT_RATIO, CameraIndex.CAMERA_INDEX_UNKNOWN.index))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.MODE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, CameraKey.create(CameraKey.RESOLUTION_FRAME_RATE, CameraIndex.CAMERA_INDEX_UNKNOWN.index))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, AirLinkKey.createOcuSyncLinkKey(AirLinkKey.PRIMARY_VIDEO_FEED_PHYSICAL_SOURCE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, AirLinkKey.createOcuSyncLinkKey(AirLinkKey.SECONDARY_VIDEO_FEED_PHYSICAL_SOURCE))
        WidgetTestUtil.setEmptyValue(widgetModel, djiSdkModel, RemoteControllerKey.create(RemoteControllerKey.MODE))
        WidgetTestUtil.setEmptyFlatCameraModeValues(widgetModel, djiSdkModel, 0)
        WidgetTestUtil.setEmptyLensValues(widgetModel, djiSdkModel, 0)
    }

    private fun mockVideoFeederFunctions() {
        val videoFeeder = Mockito.mock(VideoFeeder::class.java, Mockito.RETURNS_DEEP_STUBS)
        testVideoFeed = Mockito.spy(MockVideoFeed())
        Mockito.`when`(videoFeeder.primaryVideoFeed).thenReturn(testVideoFeed)
        every { widgetModel["getVideoFeeder"]() } returns videoFeeder
    }

    private open inner class MockVideoFeed : VideoFeed {
        private val listenerSet: MutableSet<VideoDataListener> = HashSet()
        override fun getVideoSource(): PhysicalSource {
            return PhysicalSource.MAIN_CAM
        }

        override fun addVideoDataListener(listener: VideoDataListener): Boolean {
            return false
        }

        override fun addVideoDataListener(listener: VideoDataListener, needRawData: Boolean): Boolean {
            return listenerSet.add(testVideoDataListener)
        }

        override fun removeVideoDataListener(listener: VideoDataListener): Boolean {
            return false
        }

        override fun getListeners(): Set<VideoDataListener> {
            return listenerSet
        }

        override fun addVideoActiveStatusListener(listener: VideoActiveStatusListener): Boolean {
            return false
        }

        override fun removeVideoActiveStatusListener(listener: VideoActiveStatusListener): Boolean {
            return false
        }

        override fun setPriority(priority: VideoFeedPriority, callback: CommonCallbacks.CompletionCallback<*>?) {
            // Do nothing
        }

        override fun getPriority(callback: CommonCallbacks.CompletionCallbackWith<VideoFeedPriority>) {
            // Do nothing
        }

        override fun destroy() {
            // Do nothing
        }
    }
}