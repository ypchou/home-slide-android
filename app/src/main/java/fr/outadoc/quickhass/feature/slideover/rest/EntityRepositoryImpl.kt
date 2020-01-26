package fr.outadoc.quickhass.feature.slideover.rest

import com.chuckerteam.chucker.api.ChuckerInterceptor
import fr.outadoc.quickhass.feature.slideover.TileFactory
import fr.outadoc.quickhass.feature.slideover.model.Tile
import fr.outadoc.quickhass.model.Action
import fr.outadoc.quickhass.model.EntityState
import fr.outadoc.quickhass.model.Service
import fr.outadoc.quickhass.model.entity.CoverEntity
import fr.outadoc.quickhass.model.entity.Entity
import fr.outadoc.quickhass.model.entity.EntityFactory
import fr.outadoc.quickhass.model.entity.LightEntity
import fr.outadoc.quickhass.model.entity.WeatherEntity
import fr.outadoc.quickhass.persistence.EntityDatabase
import fr.outadoc.quickhass.preferences.PreferenceRepository
import fr.outadoc.quickhass.rest.AccessTokenProvider
import okhttp3.logging.HttpLoggingInterceptor

class EntityRepositoryImpl(
    private val db: EntityDatabase,
    private val tileFactory: TileFactory,
    loggingInterceptor: HttpLoggingInterceptor,
    chuckerInterceptor: ChuckerInterceptor,
    accessTokenProvider: AccessTokenProvider,
    prefs: PreferenceRepository
) : EntityRepository {

    private val client: HomeAssistantApi by lazy {
        RestClient.create<HomeAssistantApi>(
            loggingInterceptor,
            chuckerInterceptor,
            accessTokenProvider,
            prefs
        )
    }

    override suspend fun getEntityTiles(): Result<List<Tile<Entity>>> {
        val persistedEntities = db.entityDao()
            .getPersistedEntities()
            .map { it.entityId to it }
            .toMap()

        return wrapResponse { client.getStates() }.map { states ->
            states.asSequence()
                .map { EntityFactory.create(it) }
                .map { entity ->
                    val persistedEntity = persistedEntities[entity.entityId]
                    tileFactory
                        .create(entity)
                        .copy(
                            isHidden = persistedEntity?.hidden
                                ?: !entity.isVisible || INITIAL_DOMAIN_BLACKLIST.contains(entity.domain)
                        )
                }
                .sortedWith(
                    compareBy(
                        // If the user has already ordered the item manually, use that order
                        // Otherwise put it at the end of the list initially
                        { tile -> persistedEntities[tile.source.entityId]?.order ?: Int.MAX_VALUE },

                        // Shove hidden tiles to the end of the list initially
                        { tile -> tile.isHidden },

                        // Order by domain priority (put lights and covers first for example)
                        { tile ->
                            getPriorityForDomain(tile.source.domain) ?: Int.MAX_VALUE
                        },

                        // Order by domain so that the items are somewhat sorted
                        { tile -> tile.source.domain },

                        // Order by label within a domain
                        { tile -> tile.source.friendlyName }
                    )
                )
                .toList()
        }
    }

    private fun getPriorityForDomain(domain: String): Int? {
        return when (domain) {
            LightEntity.DOMAIN -> 0
            CoverEntity.DOMAIN -> 1
            WeatherEntity.DOMAIN -> 2
            else -> null
        }
    }

    override suspend fun getServices(): Result<List<Service>> =
        wrapResponse { client.getServices() }

    override suspend fun callService(action: Action): Result<List<EntityState>> =
        wrapResponse { client.callService(action.domain, action.service, action.allParams) }

    companion object {
        val INITIAL_DOMAIN_BLACKLIST = listOf(
            "automation",
            "device_tracker",
            "updater",
            "camera",
            "persistent_notification"
        )
    }
}