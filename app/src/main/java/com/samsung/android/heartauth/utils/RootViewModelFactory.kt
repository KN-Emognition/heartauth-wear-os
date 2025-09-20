package com.samsung.android.heartauth.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samsung.android.heartauth.api.EcgSender
import com.samsung.android.heartauth.core.EcgMeasurementController
import com.samsung.android.heartauth.core.RootViewModel

class RootViewModelFactory(
    private val controller: EcgMeasurementController,
    private val ecgSender: EcgSender
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RootViewModel(controller, ecgSender) as T
    }
}
