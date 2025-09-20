package com.samsung.android.heartauth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samsung.android.heartauth.EcgMeasurementController

class RootViewModelFactory(
    private val controller: EcgMeasurementController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RootViewModel(controller) as T
    }
}
