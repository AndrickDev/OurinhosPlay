package com.ourinhosplay.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourinhosplay.app.data.Court
import com.ourinhosplay.app.data.CourtRepository
import com.ourinhosplay.app.data.MatchCard
import com.ourinhosplay.app.data.MatchRepository
import com.ourinhosplay.app.data.Sport
import com.ourinhosplay.app.data.Venue
import com.ourinhosplay.app.data.VenueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de uma lista remota: carregando, erro com retry, ou dados (vazia = estado vazio). */
data class Remote<T>(
    val loading: Boolean = true,
    val error: String? = null,
    val data: List<T> = emptyList(),
)

data class FeedUi(
    val sport: Sport? = null,
    val matches: Remote<MatchCard> = Remote(),
)

class HomeViewModel : ViewModel() {
    private val _feed = MutableStateFlow(FeedUi())
    val feed: StateFlow<FeedUi> = _feed.asStateFlow()

    private val _venues = MutableStateFlow(Remote<Venue>())
    val venues: StateFlow<Remote<Venue>> = _venues.asStateFlow()

    private val _courts = MutableStateFlow(Remote<Court>())
    val courts: StateFlow<Remote<Court>> = _courts.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _mine = MutableStateFlow(Remote<MatchCard>())
    val mine: StateFlow<Remote<MatchCard>> = _mine.asStateFlow()

    init {
        refreshFeed()
        refreshVenues()
        viewModelScope.launch {
            _isAdmin.value = runCatching { com.ourinhosplay.app.data.MatchDetailRepository.isAdmin() }.getOrDefault(false)
        }
    }

    fun refreshMine() {
        _mine.update { it.copy(loading = it.data.isEmpty(), error = null) }
        viewModelScope.launch {
            _mine.value = runCatching { com.ourinhosplay.app.data.MineRepository.matches() }.fold(
                onSuccess = { Remote(loading = false, data = it) },
                onFailure = { Remote(loading = false, error = friendlyError(it)) },
            )
        }
    }

    fun selectSport(sport: Sport?) {
        _feed.update { it.copy(sport = sport) }
        refreshFeed()
    }

    fun refreshFeed() {
        val sport = _feed.value.sport
        _feed.update { it.copy(matches = it.matches.copy(loading = true, error = null)) }
        viewModelScope.launch {
            val result = runCatching { MatchRepository.openMatches(sport) }
            _feed.update {
                // Resposta de um filtro antigo não sobrescreve o filtro atual.
                if (it.sport != sport) it else it.copy(
                    matches = result.fold(
                        onSuccess = { list -> Remote(loading = false, data = list) },
                        onFailure = { e -> Remote(loading = false, error = friendlyError(e)) },
                    ),
                )
            }
        }
    }

    fun refreshVenues() {
        _venues.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            _courts.value = runCatching { CourtRepository.all() }.fold(
                onSuccess = { Remote(loading = false, data = it) },
                onFailure = { Remote(loading = false, error = friendlyError(it)) },
            )
            _venues.value = runCatching { VenueRepository.all() }.fold(
                onSuccess = { Remote(loading = false, data = it) },
                onFailure = { Remote(loading = false, error = friendlyError(it)) },
            )
        }
    }
}

/** Nunca expõe a mensagem crua: ela pode conter URL e cabeçalhos (inclusive o token). */
internal fun friendlyError(e: Throwable): String = when (e) {
    is java.io.IOException -> "Sem conexão. Confira a internet e tente de novo."
    else -> "Erro ao falar com o servidor. Tente de novo em instantes."
}
