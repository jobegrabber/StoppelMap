package com.jonasgerdes.stoppelmap.usecase.map.presenter

import co.com.parsoniisolutions.custombottomsheetbehavior.lib.BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN
import com.jonasgerdes.stoppelmap.App
import com.jonasgerdes.stoppelmap.R
import com.jonasgerdes.stoppelmap.model.entity.Picture
import com.jonasgerdes.stoppelmap.model.entity.map.*
import com.jonasgerdes.stoppelmap.model.entity.map.detail.EntityDescriptionCard
import com.jonasgerdes.stoppelmap.model.entity.map.detail.EntityDetailCard
import com.jonasgerdes.stoppelmap.model.entity.map.detail.EntityPhoneNumberCard
import com.jonasgerdes.stoppelmap.model.entity.map.detail.EntityProductCard
import com.jonasgerdes.stoppelmap.usecase.map.viewmodel.MapInteractor
import com.jonasgerdes.stoppelmap.usecase.map.viewmodel.MapViewState
import com.jonasgerdes.stoppelmap.util.asset.Assets
import com.jonasgerdes.stoppelmap.util.asset.StringResourceHelper
import com.jonasgerdes.stoppelmap.util.plusAssign
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * @author Jonas Gerdes <dev@jonasgerdes.com>
 * @since 18.06.2017
 */
class MapPresenter(
        private val view: MapView,
        private val interactor: MapInteractor) : Disposable {

    @Inject
    lateinit var stringResHelper: StringResourceHelper

    private val disposables = CompositeDisposable()
    private var isFirstMapUpdate = true //don't animate first map update
    private var visibleEntitySubject = BehaviorSubject.create<List<MapEntity>>()

    init {
        App.graph.inject(this)
    }

    fun bind() {
        disposables += interactor.state
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::render)

        disposables += view.getIntents()
                .subscribe(interactor::onIntentReceived)
        disposables += view.getMapMoveEvents()
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinct()
                .doOnNext(interactor::onMapMoved)
                .subscribe()
        disposables += view.getUserLocationEvents()
                .subscribe(interactor::onUserMoved)
        disposables += view.getMapClicks()
                .subscribe(interactor::onMapClicked)
        disposables += view.getSearchEvents()
                .subscribeOn(AndroidSchedulers.mainThread())
                .map { it.toString().trim() }
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(interactor::onSearchChanged)
        disposables += view.getSearchResultSelectionEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { updateSearchView(it.title) }
                .subscribe(interactor::onSearchResultSelected)
        disposables += view.getBottomSheetStateEvents()
                .filter { it == STATE_HIDDEN }
                .subscribe(interactor::onBottomSheetClosed)
        disposables += view.getShareBottomClicks()
                .subscribe(interactor::onShare)

        disposables += visibleEntitySubject
                .distinctUntilChanged { first, second ->
                    first.size == second.size && first.size != 1
                }
                .map {
                    it.map {
                        MapMarker(
                                it.center.latLng,
                                stringResHelper.getTitleFor(it),
                                Assets.getTypeIconFor(it)
                        )
                    }
                }
                .subscribe(view::setMarkers)

    }

    private fun render(state: MapViewState) {
        updateMap(state)
        when (state) {
            is MapViewState.Searching -> renderSearch(state)
            is MapViewState.EntityDetail -> renderDetail(state)
            is MapViewState.EntityGroupDetail -> renderGroupDetail(state)
            is MapViewState.Exploring -> renderExploring(state)
            else -> renderDefault(state)
        }
    }

    private fun renderExploring(state: MapViewState.Exploring) {
        view.setSearchField("")
        renderDefault(state)
        view.toggleMyLocationButton(true)
        if (state.message != Assets.NONE) {
            view.showMessage(state.message)
        }
    }

    private fun renderDefault(state: MapViewState) {
        view.toggleSearchResults(false)
        view.toggleBottomSheet(false)
    }

    private fun updateMap(state: MapViewState) {
        view.setMapLimits(state.mapState.limits)
        if (state.mapState.bounds != null) {
            view.setMapCamera(state.mapState.bounds, !isFirstMapUpdate)
        } else {
            view.setMapCamera(state.mapState.center!!, state.mapState.zoom!!, !isFirstMapUpdate)
        }
        isFirstMapUpdate = false
        visibleEntitySubject.onNext(state.visibleEntities)
    }

    private fun renderSearch(state: MapViewState.Searching) {
        view.toggleMyLocationButton(true)
        view.toggleBottomSheet(false)
        disposables += state.results.subscribe {
            view.toggleSearchResults(!it.isEmpty())
            view.setSearchResults(it)
        }
    }

    private fun renderDetail(state: MapViewState.EntityDetail) {
        view.toggleSearchResults(false)
        view.toggleSearchFieldFocus(false)

        view.setBottomSheetTitle(stringResHelper.getTitleFor(state.entity))
        val headers = state.entity.getPictures(Picture.TYPE_HEADER)
        view.setBottomSheetImage(
                Assets.getHeadersFor(state.entity)[0],
                headers.firstOrNull()?.source
        )
        view.setBottomSheetIcons(Assets.getIconsFor(state.entity)
                .filter { i -> i != Assets.NONE })
        view.setBottomSheetCards(getCardsFor(state.entity))

        //workaround: wait 500ms so keyboard is actually closed before showing bottom sheet
        Completable.complete()
                .delay(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view.toggleBottomSheet(true)
                    view.toggleMyLocationButton(false)
                }

        if (state.share) {
            with(stringResHelper) {
                val entityName = getTitleFor(state.entity)
                view.shareLink(
                        get(R.string.url_share_entity, state.entity.slug!!),
                        get(R.string.entity_detail_share_text, entityName),
                        get(R.string.entity_detail_share_chooser_text, entityName)
                )
            }
        }
    }

    private fun getCardsFor(entity: MapEntity): List<EntityDetailCard> {
        val cards = ArrayList<EntityDetailCard>()
        if (entity.description != null) {
            cards.add(EntityDescriptionCard(entity.description!!.description!!,
                    entity.description!!.source))
        }
        if (!entity.phoneNumbers.isEmpty()) {
            cards.add(EntityPhoneNumberCard(entity.phoneNumbers))
        }
        when (entity.type) {
            Bar.TYPE -> cards.add(EntityProductCard(entity.bar!!.drinks))
            FoodStall.TYPE -> cards.add(EntityProductCard(entity.foodStall!!.dishes))
            CandyStall.TYPE -> cards.add(EntityProductCard(entity.candyStall!!.products))
        }
        return cards
    }

    private fun renderGroupDetail(state: MapViewState.EntityGroupDetail) {
        view.toggleSearchResults(false)
        view.toggleSearchFieldFocus(false)
    }

    private fun updateSearchView(title: String) {
        view.setSearchField(stringResHelper.getNameFor(title))
    }

    override fun isDisposed(): Boolean {
        return disposables.isDisposed
    }

    override fun dispose() {
        disposables.dispose()
    }
}