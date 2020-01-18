package fr.outadoc.quickhass.feature.grid.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.outadoc.quickhass.feature.slideover.TileFactory
import fr.outadoc.quickhass.feature.slideover.model.Tile
import fr.outadoc.quickhass.feature.slideover.rest.EntityRepository
import fr.outadoc.quickhass.model.Action
import fr.outadoc.quickhass.model.entity.Entity
import fr.outadoc.quickhass.persistence.EntityDatabase
import fr.outadoc.quickhass.persistence.model.PersistedEntity
import fr.outadoc.quickhass.preferences.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class EntityGridViewModel(
    private val prefs: PreferenceRepository,
    private val repository: EntityRepository,
    private val db: EntityDatabase,
    private val tileFactory: TileFactory
) : ViewModel() {

    sealed class State {
        object Disabled : State()
        object Normal : State()
        object Editing : State()
    }

    private val _result = MutableLiveData<Result<Any>>()
    val result: LiveData<Result<Any>> = _result

    private val _tiles = MutableLiveData<List<Tile<Entity>>>()
    val tiles: LiveData<List<Tile<Entity>>> = _tiles

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _shouldAskForInitialValues = MutableLiveData<Boolean>()
    val shouldAskForInitialValues: LiveData<Boolean> = _shouldAskForInitialValues

    private val _editionState: MutableLiveData<State> = MutableLiveData(State.Disabled)
    val editionState: LiveData<State> = _editionState

    val refreshIntervalSeconds: Long
        get() = prefs.refreshIntervalSeconds

    fun loadShortcuts() {
        if (!prefs.isOnboardingDone) {
            _shouldAskForInitialValues.value = !prefs.isOnboardingDone
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            val res = repository.getEntities()
                .map { res -> res.map { entity -> tileFactory.create(entity) } }
                .onSuccess { tiles ->
                    if (tiles.isEmpty()) {
                        _editionState.postValue(State.Disabled)
                    } else {
                        _editionState.postValue(State.Normal)
                    }

                    _tiles.postValue(tiles)
                }

            _result.postValue(res)
            _isLoading.postValue(false)
        }
    }

    fun onEntityClick(item: Entity) {
        if (item.primaryAction == null) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            onEntityLoadStart(item)

            repository.callService(item.primaryAction as Action)
                .onSuccess {
                    loadShortcuts()
                }.onFailure {
                    _result.postValue(Result.failure(it))
                }

            onEntityLoadStop(item)
            _isLoading.postValue(false)
        }
    }

    fun onReorderedEntities(items: List<Entity>) {
        if (items.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val toBePersisted = items.mapIndexed { idx, item ->
                PersistedEntity(item.entityId, idx)
            }

            // Update database
            with(db.entityDao()) {
                replaceAll(toBePersisted)
            }
        }
    }

    fun onEntityLoadStart(entity: Entity) {
        _tiles.postValue(
            tiles.value?.map { tile ->
                when (tile.source) {
                    entity -> {
                        tile.copy(
                            isLoading = true,
                            isActivated = !tile.isActivated
                        )
                    }
                    else -> tile
                }
            }
        )
    }

    fun onEntityLoadStop(entity: Entity) {
        _tiles.postValue(
            tiles.value?.map { tile ->
                when (tile.source) {
                    entity -> tile.copy(isLoading = false)
                    else -> tile
                }
            }
        )
    }

    fun onEditClick() {
        _editionState.value = when (editionState.value!!) {
            State.Normal -> State.Editing
            State.Editing -> State.Normal
            State.Disabled -> State.Disabled
        }
    }
}