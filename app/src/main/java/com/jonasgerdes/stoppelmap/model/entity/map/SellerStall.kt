package com.jonasgerdes.stoppelmap.model.entity.map

import com.jonasgerdes.stoppelmap.model.entity.Price
import io.realm.RealmList
import io.realm.RealmObject

/**
 * @author Jonas Gerdes <dev@jonasgerdes.com>
 * @since 16.06.2017
 */
open class SellerStall : RealmObject() {
    var prices: RealmList<Price> = RealmList()

    companion object {
        val TYPE = "seller-stall"
    }
}