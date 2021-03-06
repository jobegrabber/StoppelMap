package com.jonasgerdes.stoppelmap.model.entity.map

import com.jonasgerdes.stoppelmap.model.entity.Product
import io.realm.RealmList
import io.realm.RealmObject


/**
 * @author Jonas Gerdes <dev@jonasgerdes.com>
 * @since 16.06.2017
 */
open class FoodStall : RealmObject() {
    var isBar: Boolean = false
    var isTent: Boolean = false
    var dishes: RealmList<Product> = RealmList()
    var drinks: RealmList<Product> = RealmList()

    companion object {
        val TYPE = "food-stall"
    }
}