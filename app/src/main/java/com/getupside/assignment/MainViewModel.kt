package com.getupside.assignment

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import java.util.*

class MainViewModel : ViewModel() {

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

    fun onMapInteractionsStopped(location: Location) {
        with({
            params.preferredSearchLocation = Point(location.longitude, location.latitude)
            locatorTask.geocodeAsync("", params).let { listenableFuture ->
                listenableFuture.addDoneListener {

                    Log.d("vovk", listenableFuture.get().map { result ->
                        val attrs = result.attributes
                        return@map Place(
                            attrs["PlaceName"].toString(),
                            attrs["Place_addr"].toString(),
                            attrs["URL"].toString(),
                            attrs["Phone"].toString(),
                            attrs["Type"].toString()
                        )
                    }.toString())

                }
            }
        }) {
            if (locatorTask.loadStatus == LoadStatus.LOADED) this()
            else pendingTasks.add(this)
        }
    }
}