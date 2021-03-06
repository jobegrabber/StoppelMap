package com.jonasgerdes.stoppelmap.di

import com.jonasgerdes.stoppelmap.App
import com.jonasgerdes.stoppelmap.di.module.AppModule
import com.jonasgerdes.stoppelmap.di.module.DataModule
import com.jonasgerdes.stoppelmap.model.MapEntityRepository
import com.jonasgerdes.stoppelmap.model.QueryFactory
import com.jonasgerdes.stoppelmap.usecase.information.view.InformationFragment
import com.jonasgerdes.stoppelmap.usecase.main.view.MainActivity
import com.jonasgerdes.stoppelmap.usecase.main.viewmodel.MainInteractor
import com.jonasgerdes.stoppelmap.usecase.map.presenter.MapPresenter
import com.jonasgerdes.stoppelmap.usecase.map.view.MapFragment
import com.jonasgerdes.stoppelmap.usecase.map.view.search.PhoneNumberAdapter
import com.jonasgerdes.stoppelmap.usecase.map.view.search.ProductAdapter
import com.jonasgerdes.stoppelmap.usecase.map.view.search.ProductResultHolder
import com.jonasgerdes.stoppelmap.usecase.map.viewmodel.MapInteractor
import com.jonasgerdes.stoppelmap.usecase.transportation.overview.view.TransportOverviewFragment
import com.jonasgerdes.stoppelmap.usecase.transportation.route_detail.view.RouteDetailActivity
import com.jonasgerdes.stoppelmap.usecase.transportation.station_detail.view.DepartureDayFragmentAdapter
import com.jonasgerdes.stoppelmap.usecase.transportation.station_detail.view.StationDetailActivity
import com.jonasgerdes.stoppelmap.usecase.transportation.station_detail.view.departure_day.DepartureDayFragment
import dagger.Component
import javax.inject.Singleton

/**
 * @author Jonas Gerdes <dev@jonasgerdes.com>
 * @since 16.06.2017
 */
@Singleton
@Component(modules = arrayOf(AppModule::class, DataModule::class))
interface AppComponent {
    fun inject(app: App)
    fun inject(interactor: MainInteractor)
    fun inject(mainActivity: MainActivity)
    fun inject(interactor: MapInteractor)
    fun inject(mapFragment: MapFragment)
    fun inject(mapPresenter: MapPresenter)
    fun inject(productResultHolder: ProductResultHolder)
    fun inject(mapEntityRepository: MapEntityRepository)
    fun inject(queryFactory: QueryFactory)
    fun inject(productAdapter: ProductAdapter)
    fun inject(phoneNumberAdapter: PhoneNumberAdapter)
    fun inject(informationFragment: InformationFragment)
    fun inject(transportOverviewFragment: TransportOverviewFragment)
    fun inject(routeDetailActivity: RouteDetailActivity)
    fun inject(departureDayFragment: DepartureDayFragment)
    fun inject(stationDetailActivity: StationDetailActivity)
    fun inject(depatureDayFragmentAdapter: DepartureDayFragmentAdapter)
}