package com.example.foodmemory.feature.nearby

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.foodmemory.domain.model.NearbyRestaurant
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val RESTAURANT_SOURCE_ID = "food-memory-restaurants"
private const val USER_SOURCE_ID = "food-memory-user-location"
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val TAG = "FoodMemoryMap"
private const val MAP_CONTENT_BOTTOM_PADDING_DP = 250

private class LifecycleMapView(context: Context, options: MapLibreMapOptions) : MapView(context, options) {
    var hostLifecycle: Lifecycle? = null
    var hostObserver: LifecycleEventObserver? = null
}

@Composable
fun NearbyMap(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    restaurants: List<NearbyRestaurant>
) {
    val hostLifecycle = (LocalContext.current.findActivity() as? LifecycleOwner)?.lifecycle
    val mapReference = remember { mutableStateOf<MapLibreMap?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            MapLibre.getInstance(viewContext.applicationContext)
            // TextureView composes correctly below Compose screens during background map warm-up.
            val options = MapLibreMapOptions.createFromAttributes(viewContext).textureMode(true)
            LifecycleMapView(viewContext, options).also { mapView ->
                mapView.addOnDidFailLoadingMapListener { error ->
                    Log.e(TAG, "MapLibre failed to load map/style: $error")
                }
                mapView.onCreate(null)
                mapView.hostLifecycle = hostLifecycle
                mapView.hostObserver = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapView.onStop()
                        else -> Unit
                    }
                }
                mapView.hostObserver?.let { mapView.hostLifecycle?.addObserver(it) }
                mapView.getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                        Log.d(TAG, "Map style loaded")
                        style.addSource(
                            GeoJsonSource(
                                RESTAURANT_SOURCE_ID,
                                FeatureCollection.fromFeatures(emptyArray()),
                                GeoJsonOptions().withCluster(true).withClusterRadius(48).withClusterMaxZoom(15)
                            )
                        )
                        style.addSource(GeoJsonSource(USER_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
                        style.addLayer(
                            CircleLayer("food-memory-restaurant-clusters", RESTAURANT_SOURCE_ID)
                                .withFilter(Expression.has("point_count"))
                                .withProperties(
                                    circleColor(Color.rgb(37, 91, 67)),
                                    circleRadius(
                                        Expression.step(
                                            Expression.get("point_count"), 9,
                                            Expression.stop(20, 13),
                                            Expression.stop(80, 17)
                                        )
                                    ),
                                    circleStrokeColor(Color.WHITE), circleStrokeWidth(2.5f)
                                )
                        )
                        style.addLayer(
                            SymbolLayer("food-memory-restaurant-cluster-count", RESTAURANT_SOURCE_ID)
                                .withFilter(Expression.has("point_count"))
                                .withProperties(
                                    textField(Expression.toString(Expression.get("point_count"))),
                                    textFont(arrayOf("Noto Sans Regular")),
                                    textSize(12f), textColor(Color.WHITE),
                                    textAllowOverlap(true), textIgnorePlacement(true)
                                )
                        )
                        style.addLayer(
                            CircleLayer("food-memory-restaurant-pins", RESTAURANT_SOURCE_ID)
                                .withFilter(Expression.not(Expression.has("point_count")))
                                .withProperties(
                                    circleColor(Color.rgb(37, 91, 67)), circleRadius(7f),
                                    circleStrokeColor(Color.WHITE), circleStrokeWidth(2.5f)
                                )
                        )
                        style.addLayer(
                            CircleLayer("food-memory-user-location-pin", USER_SOURCE_ID).withProperties(
                                circleColor(Color.rgb(40, 120, 220)), circleRadius(9f),
                                circleStrokeColor(Color.WHITE), circleStrokeWidth(3f)
                            )
                        )
                        mapReference.value = map
                        val bottomSheetPadding = (viewContext.resources.displayMetrics.density * MAP_CONTENT_BOTTOM_PADDING_DP).toInt()
                        val cameraPosition = CameraPosition.Builder()
                            .target(LatLng(latitude, longitude))
                            .zoom(14.0)
                            .padding(0.0, 0.0, 0.0, bottomSheetPadding.toDouble())
                            .build()
                        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    }
                }
            }
        },
        update = {},
        onRelease = { view ->
            val mapView = view as LifecycleMapView
            mapView.hostObserver?.let { mapView.hostLifecycle?.removeObserver(it) }
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
            mapReference.value = null
        }
    )

    LaunchedEffect(mapReference.value, restaurants) {
        val map = mapReference.value ?: return@LaunchedEffect
        map.getStyle { style ->
            val features = restaurants.map { restaurant ->
                Feature.fromGeometry(Point.fromLngLat(restaurant.longitude, restaurant.latitude)).apply {
                    addStringProperty("name", restaurant.name)
                }
            }
            style.getSourceAs<GeoJsonSource>(RESTAURANT_SOURCE_ID)
                ?.setGeoJson(FeatureCollection.fromFeatures(features))
        }
    }

    LaunchedEffect(mapReference.value, latitude, longitude) {
        val map = mapReference.value ?: return@LaunchedEffect
        map.getStyle { style ->
            val userFeature = Feature.fromGeometry(Point.fromLngLat(longitude, latitude))
            style.getSourceAs<GeoJsonSource>(USER_SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(arrayOf(userFeature)))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 14.0))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
