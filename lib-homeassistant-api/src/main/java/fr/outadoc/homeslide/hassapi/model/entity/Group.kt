package fr.outadoc.homeslide.hassapi.model.entity

import fr.outadoc.homeslide.hassapi.model.EntityState
import fr.outadoc.homeslide.hassapi.model.entity.base.ToggleableEntity
import fr.outadoc.mdi.toIcon

class Group(state: EntityState) : ToggleableEntity(state, "google-circles-communities".toIcon()) {

    companion object {
        const val DOMAIN = "group"
    }
}
