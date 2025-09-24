package knemognition.heartauth.mobile.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import knemognition.heartauth.mobile.api.EcgSender
import knemognition.heartauth.mobile.core.EcgMeasurementController
import knemognition.heartauth.mobile.core.RootViewModel

class RootViewModelFactory(
    private val controller: EcgMeasurementController,
    private val ecgSender: EcgSender
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RootViewModel(controller, ecgSender) as T
    }
}
