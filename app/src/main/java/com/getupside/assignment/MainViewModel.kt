package com.getupside.assignment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import io.realm.Realm
import io.realm.RealmObject
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val GEOCODE_URL = "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer"
    }

    val places = MutableLiveData<List<Place>>()

    private val params = GeocodeParameters().apply {
        maxResults = 20
        categories.add("Food")
        resultAttributeNames.add("*")
    }

    private val locatorTask = LocatorTask(GEOCODE_URL).apply {
        addDoneLoadingListener {
            for (pendingTask in pendingTasks)
                pendingTask()
        }
        loadAsync()
    }

    private val pendingTasks: Queue<() -> Unit> = LinkedList<() -> Unit>()

    private val realm by lazy { getApplication<AssignmentApplication>().realm }

    val selectedPlace = MutableLiveData<Place>()

    fun onMarkerClick(latLng: LatLng) {
        selectedPlace.value =
            places.value?.let { list ->
                list.first {
                    it.latitude == latLng.latitude && it.longitude == latLng.longitude
                }
            }
    }

    fun onPlaceClick(place: Place) {
        selectedPlace.value = place
    }

    fun onMapReady(bounds: LatLngBounds, location: LatLng) {
        val results = realm.where(Place::class.java).findAll()
        if (results.none {
                bounds.contains(
                    LatLng(
                        it.latitude ?: throw IllegalStateException(),
                        it.longitude ?: throw IllegalStateException()
                    )
                )
            }) {
            fetchPlaces(location.latitude, location.longitude)
        } else {
            places.value = results
        }
    }

    fun onCameraIdle(latLng: LatLng) = fetchPlaces(latLng.latitude, latLng.longitude)

    private fun fetchPlaces(latitude: Double, longitude: Double) {
        val task = {
            params.preferredSearchLocation = Point(longitude, latitude)
            locatorTask.geocodeAsync("", params).let { listenableFuture ->
                listenableFuture.addDoneListener {

                    realm.executeTransactionAsync(
                        Realm.Transaction { realm ->
                            realm.where(Place::class.java).findAll()
                                .filter { it != selectedPlace.value }
                                .forEach(RealmObject::deleteFromRealm)

                            realm.copyToRealm(
                                listenableFuture.get()
                                    .map { result ->
                                        val attrs = result.attributes
                                        Place().apply {
                                            this.latitude = attrs["Y"] as Double
                                            this.longitude = attrs["X"] as Double
                                            name = attrs["PlaceName"] as String
                                            address = attrs["Place_addr"] as String
                                            url = attrs["URL"] as String
                                            phone = attrs["Phone"] as String
                                            type = attrs["Type"] as String
                                        }
                                    }
                                    .filter { it != selectedPlace.value }
                            )
                        },
                        Realm.Transaction.OnSuccess {
                            realm.where(Place::class.java).findAllAsync().addChangeListener { results ->
                                places.value = results
                            }
                        })
                }
            }
        }

        if (locatorTask.loadStatus == LoadStatus.LOADED) task()
        else pendingTasks.add(task)
    }
}