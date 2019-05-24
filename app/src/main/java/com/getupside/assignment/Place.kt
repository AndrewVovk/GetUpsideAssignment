package com.getupside.assignment

import io.realm.RealmObject

open class Place : RealmObject() {
    var name: String? = null
    var address: String? = null
    var url: String? = null
    var phone: String? = null
    var type: String? = null
}