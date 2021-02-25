/*
 * Copyright (c) 2018-2020 DJI
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
package dji.ux.beta.core.widget.radar

import androidx.test.filters.SmallTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dji.common.flightcontroller.*
import dji.common.product.Model
import dji.keysdk.FlightControllerKey
import dji.keysdk.ProductKey
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.thirdparty.io.reactivex.plugins.RxJavaPlugins
import dji.thirdparty.io.reactivex.schedulers.TestScheduler
import dji.ux.beta.WidgetTestUtil
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.TestSchedulerProvider
import dji.ux.beta.core.communication.GlobalPreferenceKeys
import dji.ux.beta.core.communication.GlobalPreferencesInterface
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.communication.UXKeys
import dji.ux.beta.core.extension.toDistance
import dji.ux.beta.core.util.UnitConversionUtil
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

/**
 * Test:
 * This class tests the public methods in the [RadarWidgetModel]
 *
 * 1. [radarWidgetModel_unitType_isUpdated]
 * Test the initial value emitted by the unit type flowable is the value returned by the
 * mocked GlobalPreferencesInterface and it is updated with the given test value as expected.
 *
 * 2. [radarWidgetModel_distance_updatesWithMetricUnit]
 * Test the vision detection state flowable is initialized with a zero initial value for the
 * distance field as expected and is updated with the given test value in metric units as expected.
 *
 * 3. [radarWidgetModel_distance_updatesWithImperialUnit]
 * Test the vision detection state flowable is initialized with a zero initial value for the
 * distance field as expected and is updated with the given test value in imperial units as
 * expected.
 *
 * 4. [radarWidgetModel_isAscentLimitedByObstacle_isUpdated]
 * Test the ascentLimitedByObstacle flowable is initialized with a false initial value as expected
 * and is updated with the given test value as expected.
 *
 * 5. [radarWidgetModel_tailSectors_areReversed]
 * Test the vision detection state flowable is updated with reversed sectors for the tail position.
 *
 * 6. [radarWidgetModel_radar_disabledInSportMode]
 * Test the is radar enabled flowable is false when in sport mode.
 *
 * 7. [radarWidgetModel_radar_disabledWhenDisconnected]
 * Test the is radar enabled flowable is false when disconnected.
 *
 * 8. [radarWidgetModel_warningLevels_areAdjusted]
 * Test the warning level range customization assigns the correct warning level for the test value.
 *
 * 9. [radarWidgetModel_sectors_updatedForOmniPerception]
 * Test the sectors are updated for products with omni perception.
 *
 * 10. [radarWidgetModel_sectors_updatedInImperialUnitsForOmniPerception]
 * Test the sectors are updated for products with omni perception when the unit type is imperial.
 *
 * 11. [radarWidgetModel_sectors_updatedForOmniPerceptionM300]
 * Test the sectors are updated for Matrice 300.
 *
 * 12. [radarWidgetModel_obstacleAvoidanceLevel_isUpdatedByVisionDetectionState]
 * Test the obstacle avoidance level is updated when the vision detection state is updated.
 *
 * 13. [radarWidgetModel_obstacleAvoidanceLevel_isUpdatedByOmniPerception]
 * Test the obstacle avoidance level is updated when the omni perception values are updated.
 *
 * 14. [radarWidgetModel_obstacleAvoidanceLevel_isNotUpdatedWhenMotorsOff]
 * Test the obstacle avoidance level is not updated when the motors are off.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class RadarWidgetModelTest {
    @Mock
    private lateinit var djiSdkModel: DJISDKModel

    @Mock
    private lateinit var preferencesManager: GlobalPreferencesInterface

    @Mock
    private lateinit var keyedStore: ObservableInMemoryKeyedStore
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var widgetModel: RadarWidgetModel
    private lateinit var testScheduler: TestScheduler

    @Before
    fun beforeTest() {
        MockitoAnnotations.initMocks(this)
        RxJavaPlugins.reset()
        compositeDisposable = CompositeDisposable()
        Mockito.`when`(preferencesManager.unitType).thenReturn(UnitConversionUtil.UnitType.METRIC)
        val testSchedulerProvider = TestSchedulerProvider()
        testScheduler = testSchedulerProvider.testScheduler
        SchedulerProvider.scheduler = testSchedulerProvider
        widgetModel = RadarWidgetModel(djiSdkModel, keyedStore, preferencesManager)
        WidgetTestUtil.initialize(djiSdkModel, widgetModel, true)
    }

    @Test
    fun radarWidgetModel_unitType_isUpdated() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                20,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()

        // Initialize a test subscriber that subscribes to the unit type flowable from the model
        val testSubscriber = widgetModel.unitType.test()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testSubscriber.assertValue(UnitConversionUtil.UnitType.METRIC)
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)
        testSubscriber.assertValues(UnitConversionUtil.UnitType.METRIC, UnitConversionUtil.UnitType.IMPERIAL)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriber)
    }

    @Test
    fun radarWidgetModel_distance_updatesWithMetricUnit() {
        val testDistance = 25.4
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, testDistance)

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState,
                5,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberUnitType = widgetModel.unitType.test()
        val testSubscriberVisionDetectionState = widgetModel.visionDetectionState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberUnitType.assertValue(UnitConversionUtil.UnitType.METRIC)
        testSubscriberVisionDetectionState.assertValue { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 0.0
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == testDistance
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberUnitType)
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_distance_updatesWithImperialUnit() {
        val testDistance = 25.4
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, testDistance)

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState,
                5,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberUnitType = widgetModel.unitType.test()
        val testSubscriberVisionDetectionState = widgetModel.visionDetectionState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberUnitType.assertValue(UnitConversionUtil.UnitType.METRIC)
        testSubscriberVisionDetectionState.assertValue { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 0.0
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberUnitType.assertValues(UnitConversionUtil.UnitType.METRIC, UnitConversionUtil.UnitType.IMPERIAL)
        testSubscriberVisionDetectionState.assertValueAt(2) { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == UnitConversionUtil.convertMetersToFeet(testDistance)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberUnitType)
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_sectorDistance_updatesWithImperialUnit() {
        val testDistance = 25.4
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, testDistance)

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState,
                5,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberUnitType = widgetModel.unitType.test()
        val testSubscriberVisionDetectionState = widgetModel.visionDetectionState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberUnitType.assertValue(UnitConversionUtil.UnitType.METRIC)
        testSubscriberVisionDetectionState.assertValue { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 0.0
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberUnitType.assertValues(UnitConversionUtil.UnitType.METRIC, UnitConversionUtil.UnitType.IMPERIAL)
        testSubscriberVisionDetectionState.assertValueAt(2) { state: VisionDetectionState ->
            state.detectionSectors!![0].obstacleDistanceInMeters == UnitConversionUtil.convertMetersToFeet((testDistance).toFloat())
                    && state.detectionSectors!![1].obstacleDistanceInMeters == UnitConversionUtil.convertMetersToFeet((testDistance + 1).toFloat())
                    && state.detectionSectors!![2].obstacleDistanceInMeters == UnitConversionUtil.convertMetersToFeet((testDistance + 2).toFloat())
                    && state.detectionSectors!![3].obstacleDistanceInMeters == UnitConversionUtil.convertMetersToFeet((testDistance + 3).toFloat())
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberUnitType)
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_isAscentLimitedByObstacle_isUpdated() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ASCENT_LIMITED_BY_OBSTACLE),
                true,
                5,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberAscentLimitedByObstacle = widgetModel.ascentLimitedByObstacle.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberAscentLimitedByObstacle.assertValue(false)
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS)
        testSubscriberAscentLimitedByObstacle.assertValues(false, true)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberAscentLimitedByObstacle)
    }

    @Test
    fun radarWidgetModel_tailSectors_areReversed() {
        val testDistance = 25.4
        val normalDetectionState = getDetectionState(VisionSensorPosition.TAIL, testDistance)

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState,
                5,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberVisionDetectionState = widgetModel.visionDetectionState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValue { state: VisionDetectionState -> state.detectionSectors == null }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { state: VisionDetectionState ->
            state.detectionSectors!![0].obstacleDistanceInMeters == (testDistance + 3).toFloat()
                    && state.detectionSectors!![1].obstacleDistanceInMeters == (testDistance + 2).toFloat()
                    && state.detectionSectors!![2].obstacleDistanceInMeters == (testDistance + 1).toFloat()
                    && state.detectionSectors!![3].obstacleDistanceInMeters == (testDistance).toFloat()
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_radar_disabledInSportMode() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE),
                FlightMode.GPS_SPORT,
                6,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberVisionDetectionState = widgetModel.isRadarEnabled.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValue { isRadarEnabled: Boolean ->
            isRadarEnabled
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { isRadarEnabled: Boolean ->
            !isRadarEnabled
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_radar_disabledWhenDisconnected() {
        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValues(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.CONNECTION),
                mutableListOf(true, false),
                2,
                5,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberVisionDetectionState = widgetModel.isRadarEnabled.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { isRadarEnabled: Boolean ->
            isRadarEnabled
        }
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(2) { isRadarEnabled: Boolean ->
            !isRadarEnabled
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_warningLevels_areAdjusted() {
        val m200SeriesModels = arrayOf(
                Model.MATRICE_200
        )
        val ranges = floatArrayOf(70f, 30f, 20f, 12f, 6f, 3f)
        widgetModel.setWarningLevelRanges(m200SeriesModels, ranges)
        val testDistance = 25.4
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, testDistance)

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_200,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState,
                5,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberVisionDetectionState = widgetModel.visionDetectionState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValue { state: VisionDetectionState -> state.detectionSectors == null }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { state: VisionDetectionState ->
            state.detectionSectors!![0].warningLevel == ObstacleDetectionSectorWarning.LEVEL_2
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_sectors_updatedForOmniPerception() {
        val birdViewDistances = IntArray(360) {
            when (it) {
                in 0..17 -> 30000
                in 18..44 -> 18000
                in 45..134 -> 2000
                in 135..160 -> 14000
                in 161..179 -> 8000
                in 180..314 -> 4000
                else -> 2000
            }
        }

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_RADAR_DISTANCE),
                5,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_DISTANCE),
                1,
                6,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_RADAR_BIRD_VIEW_DISTANCE),
                birdViewDistances,
                7,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberVisionDetectionState = widgetModel.visionDetectionState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValue { state: VisionDetectionState ->
            state.detectionSectors == null
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { state: VisionDetectionState ->
            state.detectionSectors!![0].warningLevel == ObstacleDetectionSectorWarning.LEVEL_6
                    && state.detectionSectors!![1].warningLevel == ObstacleDetectionSectorWarning.LEVEL_6
                    && state.detectionSectors!![2].warningLevel == ObstacleDetectionSectorWarning.LEVEL_1
                    && state.detectionSectors!![3].warningLevel == ObstacleDetectionSectorWarning.LEVEL_2
        }
        testSubscriberVisionDetectionState.assertValueAt(2) { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 2.0
        }
        testSubscriberVisionDetectionState.assertValueAt(3) { state: VisionDetectionState ->
            state.detectionSectors!![0].warningLevel == ObstacleDetectionSectorWarning.LEVEL_5
                    && state.detectionSectors!![1].warningLevel == ObstacleDetectionSectorWarning.LEVEL_5
                    && state.detectionSectors!![2].warningLevel == ObstacleDetectionSectorWarning.LEVEL_4
                    && state.detectionSectors!![3].warningLevel == ObstacleDetectionSectorWarning.LEVEL_3
        }
        testSubscriberVisionDetectionState.assertValueAt(4) { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 4.0
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_sectors_updatedInImperialUnitsForOmniPerception() {
        val birdViewDistances = IntArray(360) {
            when (it) {
                in 0..17 -> 30000
                in 18..44 -> 18000
                in 45..134 -> 2000
                in 135..160 -> 14000
                in 161..179 -> 8000
                in 180..314 -> 4000
                else -> 2000
            }
        }

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(keyedStore,
                UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE),
                UnitConversionUtil.UnitType.IMPERIAL,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_RADAR_DISTANCE),
                5,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_DISTANCE),
                1,
                6,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_RADAR_BIRD_VIEW_DISTANCE),
                birdViewDistances,
                7,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberVisionDetectionState = widgetModel.visionDetectionState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValue { state: VisionDetectionState ->
            state.detectionSectors == null
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(2) { state: VisionDetectionState ->
            state.detectionSectors!![0].obstacleDistanceInMeters == 2f.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
                    && state.detectionSectors!![1].obstacleDistanceInMeters == 2f.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
                    && state.detectionSectors!![2].obstacleDistanceInMeters == 30f.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
                    && state.detectionSectors!![3].obstacleDistanceInMeters == 18f.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
        }
        testSubscriberVisionDetectionState.assertValueAt(3) { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 2.0.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
        }
        testSubscriberVisionDetectionState.assertValueAt(4) { state: VisionDetectionState ->
            state.detectionSectors!![0].obstacleDistanceInMeters == 4f.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
                    && state.detectionSectors!![1].obstacleDistanceInMeters == 4f.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
                    && state.detectionSectors!![2].obstacleDistanceInMeters == 8f.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
                    && state.detectionSectors!![3].obstacleDistanceInMeters == 14f.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
        }
        testSubscriberVisionDetectionState.assertValueAt(5) { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 4.0.toDistance(UnitConversionUtil.UnitType.IMPERIAL)
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_sectors_updatedForOmniPerceptionM300() {
        val birdViewDistances = IntArray(360) {
            when {
                it < 45 -> 60000
                it in 45..179 -> 2000
                it in 180..314 -> 4000
                else -> 6000
            }
        }

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_300_RTK,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_RADAR_DISTANCE),
                5,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_DISTANCE),
                1,
                6,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_RADAR_BIRD_VIEW_DISTANCE),
                birdViewDistances,
                7,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberVisionDetectionState = widgetModel.visionDetectionState.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValue { state: VisionDetectionState ->
            state.detectionSectors == null
        }
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { state: VisionDetectionState ->
            state.detectionSectors!![0].warningLevel == ObstacleDetectionSectorWarning.LEVEL_1
                    && state.detectionSectors!![1].warningLevel == ObstacleDetectionSectorWarning.LEVEL_1
                    && state.detectionSectors!![2].warningLevel == ObstacleDetectionSectorWarning.INVALID
                    && state.detectionSectors!![3].warningLevel == ObstacleDetectionSectorWarning.INVALID
        }
        testSubscriberVisionDetectionState.assertValueAt(2) { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 2.0
        }
        testSubscriberVisionDetectionState.assertValueAt(3) { state: VisionDetectionState ->
            state.detectionSectors!![0].warningLevel == ObstacleDetectionSectorWarning.LEVEL_4
                    && state.detectionSectors!![1].warningLevel == ObstacleDetectionSectorWarning.LEVEL_4
                    && state.detectionSectors!![2].warningLevel == ObstacleDetectionSectorWarning.LEVEL_6
                    && state.detectionSectors!![3].warningLevel == ObstacleDetectionSectorWarning.LEVEL_6
        }
        testSubscriberVisionDetectionState.assertValueAt(4) { state: VisionDetectionState ->
            state.obstacleDistanceInMeters == 4.0
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_obstacleAvoidanceLevel_isUpdatedByVisionDetectionState() {
        val testDistance = 8.0
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, testDistance)

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_210_RTK_V2,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState,
                6,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberUnitType = widgetModel.unitType.test()
        val testSubscriberVisionDetectionState = widgetModel.obstacleAvoidanceLevel.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberUnitType.assertValue(UnitConversionUtil.UnitType.METRIC)
        testSubscriberVisionDetectionState.assertValue(RadarWidgetModel.ObstacleAvoidanceLevel.NONE)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { level: RadarWidgetModel.ObstacleAvoidanceLevel ->
            level == RadarWidgetModel.ObstacleAvoidanceLevel.LEVEL_1
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberUnitType)
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_obstacleAvoidanceLevel_isUpdatedByOmniPerception() {
        val birdViewDistances = IntArray(360) {
            when (it) {
                in 0..44 -> 1000
                in 45..134 -> 2000
                in 135..179 -> 3000
                in 180..314 -> 6000
                else -> 1000
            }
        }

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_300_RTK,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON),
                true,
                5,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_RADAR_DISTANCE),
                5,
                6,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_DISTANCE),
                1,
                7,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_RADAR_BIRD_VIEW_DISTANCE),
                birdViewDistances,
                8,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberVisionDetectionState = widgetModel.obstacleAvoidanceLevel.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValue(RadarWidgetModel.ObstacleAvoidanceLevel.NONE)
        testScheduler.advanceTimeBy(7, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueAt(1) { level: RadarWidgetModel.ObstacleAvoidanceLevel ->
            level == RadarWidgetModel.ObstacleAvoidanceLevel.LEVEL_3
        }
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @Test
    fun radarWidgetModel_obstacleAvoidanceLevel_isNotUpdatedWhenMotorsOff() {
        val testDistance = 8.0
        val normalDetectionState = getDetectionState(VisionSensorPosition.NOSE, testDistance)

        setEmptyValues()
        // Use util method to set emitted value after given delay for given key
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME),
                Model.MATRICE_210_RTK_V2,
                4,
                TimeUnit.SECONDS)
        WidgetTestUtil.setEmittedValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE),
                normalDetectionState,
                6,
                TimeUnit.SECONDS)

        // Setup the widget model after emitted values have been initialized
        widgetModel.setup()
        val testSubscriberUnitType = widgetModel.unitType.test()
        val testSubscriberVisionDetectionState = widgetModel.obstacleAvoidanceLevel.test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        testSubscriberUnitType.assertValue(UnitConversionUtil.UnitType.METRIC)
        testSubscriberVisionDetectionState.assertValue(RadarWidgetModel.ObstacleAvoidanceLevel.NONE)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testSubscriberVisionDetectionState.assertValueCount(1)
        widgetModel.cleanup()
        compositeDisposable.add(testSubscriberUnitType)
        compositeDisposable.add(testSubscriberVisionDetectionState)
    }

    @After
    fun afterTest() {
        RxJavaPlugins.reset()
        compositeDisposable.dispose()
    }

    private fun setEmptyValues() {
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.VISION_DETECTION_STATE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.IS_ASCENT_LIMITED_BY_OBSTACLE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                ProductKey.create(ProductKey.MODEL_NAME))
        WidgetTestUtil.setEmptyValue(keyedStore, UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.ARE_MOTOR_ON))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.create(FlightControllerKey.FLIGHT_MODE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_PERCEPTION_RADAR_BIRD_VIEW_DISTANCE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_RADAR_DISTANCE))
        WidgetTestUtil.setEmptyValue(widgetModel,
                djiSdkModel,
                FlightControllerKey.createFlightAssistantKey(FlightControllerKey.OMNI_HORIZONTAL_AVOIDANCE_DISTANCE))
    }

    private fun getDetectionState(sensorPosition: VisionSensorPosition,
                                  distanceInMeters: Double): VisionDetectionState {
        val sectors = arrayOfNulls<ObstacleDetectionSector>(4)
        sectors[0] = ObstacleDetectionSector(ObstacleDetectionSectorWarning.LEVEL_1,
                distanceInMeters.toFloat())
        sectors[1] = ObstacleDetectionSector(ObstacleDetectionSectorWarning.LEVEL_1,
                (distanceInMeters + 1).toFloat())
        sectors[2] = ObstacleDetectionSector(ObstacleDetectionSectorWarning.LEVEL_1,
                (distanceInMeters + 2).toFloat())
        sectors[3] = ObstacleDetectionSector(ObstacleDetectionSectorWarning.LEVEL_1,
                (distanceInMeters + 3).toFloat())
        return VisionDetectionState.createInstance(false,
                distanceInMeters,
                VisionSystemWarning.UNKNOWN,
                sectors,
                sensorPosition,
                false,
                VisionSystemWarning.UNKNOWN.value())
    }
}