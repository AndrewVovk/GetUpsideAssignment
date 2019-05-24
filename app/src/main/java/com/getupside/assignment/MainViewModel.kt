package com.getupside.assignment

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import io.realm.Realm
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

    fun onMapReady(bounds: LatLngBounds, location: Location) {
        realm.where(Place::class.java).findAllAsync().addChangeListener { results ->
            if (results.none {
                    bounds.contains(
                        LatLng(
                            it.latitude ?: throw IllegalStateException(),
                            it.longitude ?: throw IllegalStateException()
                        )
                    )
                }) {
                fetchPlaces(location)
            } else {
                places.value = results
            }
        }
    }

    private fun fetchPlaces(location: Location) {
        val task = {
            params.preferredSearchLocation = Point(location.longitude, location.latitude)
            locatorTask.geocodeAsync("", params).let { listenableFuture ->
                listenableFuture.addDoneListener {

                    realm.executeTransactionAsync(
                        Realm.Transaction { realm ->
                            realm.deleteAll()

                            realm.copyToRealm(
                                listenableFuture.get().map { result ->
                                    val attrs = result.attributes
                                    Place().apply {
                                        latitude = attrs["Y"] as Double
                                        longitude = attrs["X"] as Double
                                        name = attrs["PlaceName"] as String
                                        address = attrs["Place_addr"] as String
                                        url = attrs["URL"] as String
                                        phone = attrs["Phone"] as String
                                        type = attrs["Type"] as String
                                    }
                                }
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