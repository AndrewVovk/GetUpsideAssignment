package com.getupside.assignment

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val GEOCODE_URL = "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer"
    }

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

    fun onMapInteractionsStopped(location: Location) {
        with({
            params.preferredSearchLocation = Point(location.longitude, location.latitude)
            locatorTask.geocodeAsync("", params).let { listenableFuture ->
                listenableFuture.addDoneListener {

                    realm.executeTransaction {
                        realm.deleteAll()

                        realm.copyToRealm(
                            listenableFuture.get().map { result ->
                                val attrs = result.attributes
                                Place().apply {
                                    name = attrs["PlaceName"].toString()
                                    address = attrs["Place_addr"].toString()
                                    url = attrs["URL"].toString()
                                    phone = attrs["Phone"].toString()
                                    type = attrs["Type"].toString()
                                }
                            }
                        )
                    }

                    Log.d("vovk", realm.where(Place::class.java).findAll().toString())

                }
            }
        }) {
            if (locatorTask.loadStatus == LoadStatus.LOADED) this()
            else pendingTasks.add(this)
        }
    }
}