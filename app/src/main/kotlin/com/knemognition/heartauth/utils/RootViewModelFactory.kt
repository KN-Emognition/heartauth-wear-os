package com.knemognition.heartauth.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.knemognition.heartauth.api.EcgSender
import com.knemognition.heartauth.core.EcgMeasurementController
import com.knemognition.heartauth.core.RootViewModel

class RootViewModelFactory(
    private val controller: EcgMeasurementController,
    private val ecgSender: EcgSender
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RootViewModel(controller, ecgSender) as T
    }
}
