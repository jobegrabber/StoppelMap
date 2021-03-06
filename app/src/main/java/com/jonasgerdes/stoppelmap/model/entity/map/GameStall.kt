package com.jonasgerdes.stoppelmap.model.entity.map

import com.jonasgerdes.stoppelmap.model.entity.Product
import io.realm.RealmList
import io.realm.RealmObject


/**
 * @author Jonas Gerdes <dev@jonasgerdes.com>
 * @since 16.06.2017
 */
open class GameStall : RealmObject() {
    var games: RealmList<Product> = RealmList()

    companion object {
        val TYPE = "game-stall"
    }
}