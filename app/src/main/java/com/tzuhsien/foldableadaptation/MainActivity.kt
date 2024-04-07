package com.tzuhsien.foldableadaptation

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculator
import com.tzuhsien.foldableadaptation.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var windowInfoTracker: WindowInfoTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        windowInfoTracker = WindowInfoTracker.getOrCreate(this@MainActivity)

        obtainWindowMetrics()

        onWindowLayoutInfoChange()
    }

    private fun obtainWindowMetrics() {
        val wmc = WindowMetricsCalculator.getOrCreate()
        val currentWM = wmc.computeCurrentWindowMetrics(this).bounds.flattenToString()
        val maximumWM = wmc.computeMaximumWindowMetrics(this).bounds.flattenToString()
        binding.windowMetrics.text =
            "Current Window Metrics: ${currentWM}\nMaximum Window Metrics: ${maximumWM}"
    }

    private fun onWindowLayoutInfoChange() {
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowInfoTracker.windowLayoutInfo(this@MainActivity)
                    .collect { value ->
                        updateUI(value)
                    }
            }
        }
    }

    private fun updateUI(newLayoutInfo: WindowLayoutInfo) {
        binding.layoutChange.text = newLayoutInfo.toString()

        /**
         * Check states with displayFeatures to update UI for each state (spanned/half-opened/unspanned)
         * */
//        if (newLayoutInfo.displayFeatures.isNotEmpty()) {
//
//            // Add a vertical divider on the flat/half-opened screen for demo
//            alignViewToFoldingFeatureBounds(newLayoutInfo = newLayoutInfo)
//
//            (newLayoutInfo.displayFeatures[0] as? FoldingFeature)?.let { foldingFeature ->
//                when (foldingFeature.state) {
//                    FoldingFeature.State.FLAT -> {
//                        binding.configurationChanged.text = "Spanned across displays"
//                        setUpPicture(isCentered = false)
//                    }
//                    FoldingFeature.State.HALF_OPENED -> {
//                        binding.configurationChanged.text = "Half opened"
//                        setUpPicture(isCentered = false)
//                    }
//                    else -> FoldableState.UNKNOWN
//                }
//            }
//
//        } else {
//            binding.configurationChanged.text = "Unspanned"
//            setUpPicture(isCentered = true)
//        }


        /**
         *  Write an extension function to get foldable state with less code from each view
         */
        when(newLayoutInfo.getFoldableState()) {
            FoldableState.FLAT -> {
                // Add a vertical divider on the flat/half-opened screen for demo
                alignViewToFoldingFeatureBounds(newLayoutInfo = newLayoutInfo)

                binding.configurationChanged.text = "Spanned across displays"
                setUpPicture(isCentered = false)
            }
            FoldableState.HALF_OPENED -> {
                // Add a vertical divider on the flat/half-opened screen for demo
                alignViewToFoldingFeatureBounds(newLayoutInfo = newLayoutInfo)

                binding.configurationChanged.text = "Half opened"
                setUpPicture(isCentered = false)
            }
            FoldableState.UNSPANNED -> {
                binding.configurationChanged.text = "Unspanned"
                setUpPicture(isCentered = true)
            }

            FoldableState.UNKNOWN -> {
                binding.configurationChanged.text = "Unknown state"
                setUpPicture(isCentered = true)
            }
        }
    }

    private fun setUpPicture(isCentered: Boolean) {
        val constraintLayout = binding.constraintLayout
        val set = ConstraintSet()
        set.clone(constraintLayout)

        binding.root.apply {
            if (isCentered) {
                set.connect(
                    R.id.image_profile, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, 48
                )
            } else {
                set.clear(
                    R.id.image_profile, ConstraintSet.END
                )
            }

            set.connect(
                R.id.image_profile, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START, 48
            )

            set.connect(
                R.id.image_profile, ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.TOP, 48
            )
        }
        set.applyTo(constraintLayout)
    }

    private fun alignViewToFoldingFeatureBounds(newLayoutInfo: WindowLayoutInfo) {
        val constraintLayout = binding.constraintLayout
        val set = ConstraintSet()
        set.clone(constraintLayout)

        // Get and translate the feature bounds to the View's coordinate space and current
        // position in the window.
        val foldingFeature = newLayoutInfo.displayFeatures[0] as FoldingFeature
        val bounds = getFeatureBoundsInWindow(foldingFeature, binding.root)


        bounds?.let { rect ->
            // Some devices have a 0px width folding feature. We set a minimum of 1px so we
            // can show the view that mirrors the folding feature in the UI and use it as reference.
            val horizontalFoldingFeatureHeight = (rect.bottom - rect.top).coerceAtLeast(5)
            val verticalFoldingFeatureWidth = (rect.right - rect.left).coerceAtLeast(5)

            // Sets the view to match the height and width of the folding feature
            set.constrainHeight(
                R.id.folding_feature,
                horizontalFoldingFeatureHeight
            )
            set.constrainWidth(
                R.id.folding_feature,
                verticalFoldingFeatureWidth
            )

            set.connect(
                R.id.folding_feature, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START, 0
            )
            set.connect(
                R.id.folding_feature, ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0
            )

            if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) {
                set.setMargin(R.id.folding_feature, ConstraintSet.START, rect.left)
                set.connect(
                    R.id.layout_change, ConstraintSet.END,
                    R.id.folding_feature, ConstraintSet.START, 0
                )
            } else {
                // FoldingFeature is Horizontal
                set.setMargin(
                    R.id.folding_feature, ConstraintSet.TOP,
                    rect.top
                )
                set.connect(
                    R.id.layout_change, ConstraintSet.TOP,
                    R.id.folding_feature, ConstraintSet.BOTTOM, 0
                )
            }

            // Set the view to visible and apply constraints
            set.setVisibility(R.id.folding_feature, View.VISIBLE)
            set.applyTo(constraintLayout)
        }
    }

    private fun getFeatureBoundsInWindow(
        displayFeature: DisplayFeature,
        view: View,
        includePadding: Boolean = true
    ): Rect? {
        // Adjust the location of the view in the window to be in the same coordinate space as the feature.
        val viewLocationInWindow = IntArray(2)
        view.getLocationInWindow(viewLocationInWindow)

        // Intersect the feature rectangle in window with view rectangle to clip the bounds.
        val viewRect = Rect(
            viewLocationInWindow[0], viewLocationInWindow[1],
            viewLocationInWindow[0] + view.width, viewLocationInWindow[1] + view.height
        )

        // Include padding if needed
        if (includePadding) {
            viewRect.left += view.paddingLeft
            viewRect.top += view.paddingTop
            viewRect.right -= view.paddingRight
            viewRect.bottom -= view.paddingBottom
        }

        val featureRectInView = Rect(displayFeature.bounds)
        val intersects = featureRectInView.intersect(viewRect)

        // Checks to see if the display feature overlaps with our view at all
        if ((featureRectInView.width() == 0 && featureRectInView.height() == 0) ||
            !intersects
        ) {
            return null
        }

        // Offset the feature coordinates to view coordinate space start point
        featureRectInView.offset(-viewLocationInWindow[0], -viewLocationInWindow[1])

        return featureRectInView
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}