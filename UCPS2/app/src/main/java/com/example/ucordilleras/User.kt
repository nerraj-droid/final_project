package com.example.ucordilleras

class User {
    var name: String? = null
    var imageUrl: String? = null

    constructor(name: String, imageUrl: String?) {
        var name = name
        if (name.trim { it <= ' ' } == "") {
            name = "No Name"
        }
        this.name = name
        this.imageUrl = imageUrl
    }
}
