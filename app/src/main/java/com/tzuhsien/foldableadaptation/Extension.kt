package com.tzuhsien.foldableadaptation

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *  To reuse the method of tracking window info from fragments
 * */
fun Fragment.setupWindowInfoTracker(
    onWindowLayoutInfoChange: (WindowLayoutInfo) -> Unit
) {
    val windowInfoTracker = WindowInfoTracker.getOrCreate(requireContext())

    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            windowInfoTracker.windowLayoutInfo(requireActivity())
                .collect { value ->
                    onWindowLayoutInfoChange.invoke(value)
                }
        }
    }
}

/**
 * Determines the foldable state of the device.
 *
 * @return The [FoldableState] state.
 */
fun WindowLayoutInfo.getFoldableState(): FoldableState {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return FoldableState.UNKNOWN
    }

    return if (displayFeatures.isNotEmpty()) {
        // Get and translate the feature bounds to the View's coordinate space and current
        // position in the window.
        (displayFeatures[0] as? FoldingFeature)?.let { foldingFeature ->
            when (foldingFeature.state) {
                FoldingFeature.State.FLAT -> FoldableState.FLAT
                FoldingFeature.State.HALF_OPENED -> FoldableState.HALF_OPENED
                else -> FoldableState.UNKNOWN
            }
        } ?: FoldableState.UNKNOWN
    } else {
        FoldableState.UNSPANNED // Unspanned state (not folded)
    }
}