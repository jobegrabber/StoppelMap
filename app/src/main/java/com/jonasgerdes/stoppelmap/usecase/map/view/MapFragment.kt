package com.jonasgerdes.stoppelmap.usecase.map.view

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.com.parsoniisolutions.custombottomsheetbehavior.lib.BottomSheetBehaviorGoogleMapsLike
import co.com.parsoniisolutions.custombottomsheetbehavior.lib.MergedAppBarLayoutBehavior
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChanges
import com.jakewharton.rxbinding2.view.clicks
import com.jonasgerdes.stoppelmap.App
import com.jonasgerdes.stoppelmap.R
import com.jonasgerdes.stoppelmap.model.entity.map.MapMarker
import com.jonasgerdes.stoppelmap.model.entity.map.detail.EntityDetailCard
import com.jonasgerdes.stoppelmap.model.entity.map.search.MapSearchResult
import com.jonasgerdes.stoppelmap.usecase.main.view.MainActivity
import com.jonasgerdes.stoppelmap.usecase.map.presenter.MapPresenter
import com.jonasgerdes.stoppelmap.usecase.map.presenter.MapView
import com.jonasgerdes.stoppelmap.usecase.map.view.search.EntityDetailCardAdapter
import com.jonasgerdes.stoppelmap.usecase.map.view.search.SearchResultAdapter
import com.jonasgerdes.stoppelmap.usecase.map.viewmodel.MapBounds
import com.jonasgerdes.stoppelmap.usecase.map.viewmodel.MapInteractor
import com.jonasgerdes.stoppelmap.util.asset.MarkerIconFactory
import com.jonasgerdes.stoppelmap.util.map.clicks
import com.jonasgerdes.stoppelmap.util.map.idles
import com.jonasgerdes.stoppelmap.util.map.markerClicks
import com.jonasgerdes.stoppelmap.util.map.stateChanges
import io.reactivex.Observable
import kotlinx.android.synthetic.main.map_entity_bottom_sheet.*
import kotlinx.android.synthetic.main.map_fragment.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import javax.inject.Inject
import kotlin.properties.Delegates


/**
 * @author Jonas Gerdes <dev@jonasgerdes.com>
 * @since 16.06.2017
 */
class MapFragment : LifecycleFragment(), MapView {
    companion object {
        val ARG_DETAIL_ENTITY_SLUG = "ARG_DETAIL_ENTITY_SLUG"
    }

    @Inject
    lateinit var markerFactory: MarkerIconFactory

    private var map by Delegates.notNull<GoogleMap>()
    private val mapMarkers: ArrayList<Marker> = ArrayList()

    private lateinit var presenter: MapPresenter
    private lateinit var bottomSheetbehavior: BottomSheetBehaviorGoogleMapsLike<View>
    private lateinit var mergedAppBarLayoutBehavior: MergedAppBarLayoutBehavior

    private var isUpdatingMap = false //prevent map animations triggering observables
    private var isUpdatingSearch = false //prevent retriggering of search observables

    private val searchAdapter = SearchResultAdapter()
    private val cardAdapter = EntityDetailCardAdapter()

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.map_fragment, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.graph.inject(this)

        val interactor = ViewModelProviders.of(activity).get(MapInteractor::class.java)
        presenter = MapPresenter(this, interactor)

        //todo: check if google play services are installed
        MapsInitializer.initialize(context)
        initMap(presenter)

        searchResultList.adapter = searchAdapter
        search.setIconifiedByDefault(false)

        initBottomSheet()
    }


    private fun initBottomSheet() {
        bottomSheetbehavior = BottomSheetBehaviorGoogleMapsLike.from<View>(bottomSheet)

        mergedAppBarLayoutBehavior = MergedAppBarLayoutBehavior.from(mergedAppbarLayout)
        mergedAppBarLayoutBehavior.setToolbarTitle("Lorem Ipsum")
        mergedAppBarLayoutBehavior.setNavigationOnClickListener {
            bottomSheetbehavior.state = BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN
        }

        bottomSheetbehavior.state = BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN

        bottomSheetHeader.onClick {
            bottomSheetbehavior.state = BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT
        }

        detailCardList.adapter = cardAdapter
    }

    override fun onDestroyView() {
        presenter.dispose()
        super.onDestroyView()
    }

    @SuppressLint("MissingPermission")
    private fun initMap(presenter: MapPresenter) {
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager
                .beginTransaction()
                .replace(R.id.mapPlaceholder, mapFragment)
                .commitNowAllowingStateLoss()
        mapFragment.getMapAsync({
            map = it
            map.addTileOverlay(
                    TileOverlayOptions().tileProvider(CustomMapTileProvider(resources.assets))
            )
            map.mapType = GoogleMap.MAP_TYPE_NONE
            map.setPadding(0, resources.getDimensionPixelSize(R.dimen.map_padding_top), 0, 0)
            map.uiSettings.isMyLocationButtonEnabled = false
            presenter.bind()

            requestLocation().subscribe {
                map.isMyLocationEnabled = true
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun getUserLocationEvents(): Observable<Location> {
        return locationFab.clicks()
                .concatMap { requestLocation() }
                .doOnNext { map.isMyLocationEnabled = true }
                .filter { map.myLocation != null }
                .map { map.myLocation }
    }

    private fun requestLocation(): Observable<Boolean> {
        return (activity as MainActivity).permissions.request(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION).filter({ it })
    }

    override fun setMapLimits(bounds: MapBounds) {
        map.setLatLngBoundsForCameraTarget(bounds.bounds)
        map.setMaxZoomPreference(bounds.maxZoom)
        map.setMinZoomPreference(bounds.minZoom)
    }

    override fun setMapCamera(center: LatLng, zoom: Float, animate: Boolean) {
        val update = CameraUpdateFactory.newLatLngZoom(center, zoom)
        isUpdatingMap = true
        if (animate) {
            map.animateCamera(update)
        } else {
            map.moveCamera(update)
            isUpdatingMap = false
        }
    }

    override fun setMapCamera(bounds: LatLngBounds, animate: Boolean) {
        val padding = resources.getDimensionPixelSize(R.dimen.map_zoom_bound_padding)
        val update = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        isUpdatingMap = true
        if (animate) {
            map.animateCamera(update)
        } else {
            map.moveCamera(update)
            isUpdatingMap = false
        }
    }

    override fun setSearchField(term: String) {
        isUpdatingSearch = true
        search.setQuery(term, false)
        isUpdatingSearch = false
    }

    override fun toggleSearchFieldFocus(isFocused: Boolean) {
        if (isFocused) {
            search.requestFocus()
        } else {
            search.clearFocus()
        }
    }

    override fun setSearchResults(results: List<MapSearchResult>) {
        searchAdapter.results = results
    }

    override fun toggleSearchResults(show: Boolean) {
        if (show) {
            searchResult.visibility = View.VISIBLE
        } else {
            searchResult.visibility = View.GONE
        }
    }

    override fun toggleMyLocationButton(show: Boolean) {
        if (show) {
            locationFab.show()
        } else {
            locationFab.hide()
        }
    }

    override fun getIntents(): Observable<Uri> {
        val uri: Uri? = arguments?.getParcelable(ARG_DETAIL_ENTITY_SLUG)
        if (uri != null) {
            return Observable.just(uri)
        }
        return Observable.empty<Uri>()
    }

    override fun getMapMoveEvents(): Observable<CameraPosition> {
        return map.idles().filter {
            val propagate = !isUpdatingMap
            isUpdatingMap = false
            propagate
        }
    }

    override fun getMapClicks(): Observable<LatLng> {
        return map.clicks().mergeWith(map.markerClicks().map { it.position })
    }

    override fun getSearchEvents(): Observable<CharSequence> {
        return search.queryTextChanges().filter { !isUpdatingSearch }
    }

    override fun getSearchResultSelectionEvents(): Observable<MapSearchResult> {
        return searchAdapter.selections()
    }

    override fun getBottomSheetStateEvents(): Observable<Int> {
        return bottomSheetbehavior.stateChanges()
    }

    override fun getShareBottomClicks(): Observable<Unit> {
        return bottomSheetFab.clicks().map { }
    }

    override fun setMarkers(markers: List<MapMarker>) {
        mapMarkers.forEach {
            it.remove()
        }
        mapMarkers.clear()
        markers.map {
            MarkerOptions()
                    .icon(markerFactory.createMarker(it.title, it.iconResource))
                    .position(it.position)
                    .anchor(0.5f, 0.5f)

        }.map {
            map.addMarker(it)
        }.toCollection(mapMarkers)
    }

    override fun toggleBottomSheet(show: Boolean) {
        if (show) {
            if (bottomSheetbehavior.state == BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN) {
                bottomSheetbehavior.state = BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED
            }
        } else {
            bottomSheet.scrollY = 0
            bottomSheetbehavior.state = BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN
        }
    }

    override fun setBottomSheetTitle(title: String) {
        bottomSheetHeaderTitle.text = title
        mergedAppBarLayoutBehavior.setToolbarTitle(title)
    }

    override fun setBottomSheetImage(imageUri: Uri, source: String?) {
        Glide.with(context)
                .load(imageUri)
                .centerCrop()
                .into(bottomSheetImage)
        if (source != null) {
            bottomSheetImageLicense.visibility = View.VISIBLE
            bottomSheetImageLicense.text = "© $source"
        } else {
            bottomSheetImageLicense.visibility = View.GONE
        }
    }

    override fun setBottomSheetIcons(icons: List<Int>) {
        with(bottomSheetIconList) {
            iconTint = R.color.tint_icon_active_light
            iconSize = 20
            iconMargin = 4
            setIcons(icons)
        }

    }

    override fun setBottomSheetCards(cards: List<EntityDetailCard>) {
        cardAdapter.cards = cards
    }

    override fun showMessage(messageResource: Int) {
        Snackbar.make(view as View, messageResource, Snackbar.LENGTH_LONG).show()
    }

    override fun shareLink(url: String, subject: String, chooserText: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, url)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        startActivity(Intent.createChooser(intent, chooserText))
    }

    fun onBackPress(): Boolean {
        if (bottomSheetbehavior.state != BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN) {
            bottomSheetbehavior.state = BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN
            return true
        }
        return false
    }

}
