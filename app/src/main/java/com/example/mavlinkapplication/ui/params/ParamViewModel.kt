package com.example.mavlinkapplication.ui.params

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mavlinkapplication.domain.ParamRepository
import com.example.mavlinkapplication.domain.VehicleParam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Params aren't in the heartbeat — MAVLink confirms a PARAM_SET via a separate
 * PARAM_VALUE ack, which MAVSDK's setParamFloat/setParamInt already waits for
 * internally. So "confirmed" here means that suspend call returned success.
 */
sealed class ParamEditState {
    data object Idle : ParamEditState()
    data class Pending(val id: String) : ParamEditState()
    data class Failed(val id: String) : ParamEditState()
}

@HiltViewModel
class ParamViewModel @Inject constructor(
    private val paramRepository: ParamRepository,
) : ViewModel() {

    private val _params    = MutableStateFlow<List<VehicleParam>>(emptyList())
    private val _filter    = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _editState = MutableStateFlow<ParamEditState>(ParamEditState.Idle)

    val filter:    StateFlow<String>  = _filter.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val editState: StateFlow<ParamEditState> = _editState.asStateFlow()

    val filteredParams: StateFlow<List<VehicleParam>> = combine(_params, _filter) { params, f ->
        if (f.isBlank()) params else params.filter { it.id.contains(f, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadParams()
    }

    fun loadParams() {
        viewModelScope.launch {
            _isLoading.value = true
            _params.value = paramRepository.getAll()
            _isLoading.value = false
        }
    }

    fun setFilter(query: String) { _filter.value = query }

    fun setParam(id: String, value: Float, type: VehicleParam.ParamType) {
        _editState.value = ParamEditState.Pending(id)
        viewModelScope.launch {
            val result = paramRepository.set(id, value, type)
            if (result.isSuccess) {
                _editState.value = ParamEditState.Idle
                loadParams()
            } else {
                _editState.value = ParamEditState.Failed(id)
            }
        }
    }
}
