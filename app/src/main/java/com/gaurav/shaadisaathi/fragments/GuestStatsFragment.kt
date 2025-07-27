package com.gaurav.shaadisaathi.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.FragmentGuestStatsBinding
import com.gaurav.shaadisaathi.repository.GuestRepository
import kotlinx.coroutines.launch

class GuestStatsFragment : Fragment() {

    private var _binding: FragmentGuestStatsBinding? = null
    private val binding get() = _binding!!

    private val guestRepository = GuestRepository()
    private val TAG = "GuestStatsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        loadGuestStatistics()
    }

    private fun setupCharts() {
        setupRSVPChart()
        setupCategoryChart()
        setupMealChart()
    }

    private fun setupRSVPChart() {
        binding.chartRsvp.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            setTransparentCircleColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "RSVP\nStatus"
            setCenterTextSize(16f)
            setRotationAngle(0f)
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
        }
    }

    private fun setupCategoryChart() {
        binding.chartCategory.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Guest\nCategories"
            setCenterTextSize(16f)
        }
    }

    private fun setupMealChart() {
        binding.chartMeal.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Meal\nPreferences"
            setCenterTextSize(16f)
        }
    }

    private fun loadGuestStatistics() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = guestRepository.getGuestStatistics()
                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    stats?.let { updateUI(it) }
                } else {
                    Log.e(TAG, "Error loading guest statistics: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading guest statistics", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateUI(stats: com.gaurav.shaadisaathi.repository.GuestStatistics) {
        updateSummaryCards(stats)
        updateRSVPChart(stats)
        updateCategoryChart(stats)
        updateMealChart(stats)
    }

    private fun updateSummaryCards(stats: com.gaurav.shaadisaathi.repository.GuestStatistics) {
        binding.tvTotalGuests.text = stats.totalGuests.toString()
        binding.tvConfirmedGuests.text = stats.confirmedGuests.toString()
        binding.tvPendingGuests.text = stats.pendingGuests.toString()
        binding.tvDeclinedGuests.text = stats.declinedGuests.toString()
        binding.tvTotalAttending.text = stats.totalAttending.toString()

        // Update percentages
        val total = stats.totalGuests.toFloat()
        if (total > 0) {
            val confirmedPct = (stats.confirmedGuests / total * 100).toInt()
            val pendingPct = (stats.pendingGuests / total * 100).toInt()
            val declinedPct = (stats.declinedGuests / total * 100).toInt()

            binding.tvConfirmedPercentage.text = "$confirmedPct%"
            binding.tvPendingPercentage.text = "$pendingPct%"
            binding.tvDeclinedPercentage.text = "$declinedPct%"
        }
    }

    private fun updateRSVPChart(stats: com.gaurav.shaadisaathi.repository.GuestStatistics) {
        val entries = mutableListOf<PieEntry>()

        if (stats.confirmedGuests > 0) {
            entries.add(PieEntry(stats.confirmedGuests.toFloat(), "Confirmed"))
        }
        if (stats.pendingGuests > 0) {
            entries.add(PieEntry(stats.pendingGuests.toFloat(), "Pending"))
        }
        if (stats.declinedGuests > 0) {
            entries.add(PieEntry(stats.declinedGuests.toFloat(), "Declined"))
        }

        if (entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "RSVP Status")
            dataSet.colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.status_confirmed),
                ContextCompat.getColor(requireContext(), R.color.status_pending),
                ContextCompat.getColor(requireContext(), R.color.status_declined)
            )
            dataSet.setDrawValues(true)
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = ContextCompat.getColor(requireContext(), android.R.color.white)

            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter())

            binding.chartRsvp.data = data
            binding.chartRsvp.invalidate()
        }
    }

    private fun updateCategoryChart(stats: com.gaurav.shaadisaathi.repository.GuestStatistics) {
        val entries = mutableListOf<PieEntry>()

        stats.categoryBreakdown.forEach { (category, count) ->
            if (count > 0) {
                val displayName = when (category) {
                    "family" -> "Family"
                    "friends" -> "Friends"
                    "colleagues" -> "Colleagues"
                    "others" -> "Others"
                    else -> category.replaceFirstChar { it.uppercase() }
                }
                entries.add(PieEntry(count.toFloat(), displayName))
            }
        }

        if (entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "Guest Categories")
            dataSet.colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.category_family),
                ContextCompat.getColor(requireContext(), R.color.category_friends),
                ContextCompat.getColor(requireContext(), R.color.category_colleagues),
                ContextCompat.getColor(requireContext(), R.color.colorSecondary)
            )
            dataSet.setDrawValues(true)
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = ContextCompat.getColor(requireContext(), android.R.color.white)

            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter())

            binding.chartCategory.data = data
            binding.chartCategory.invalidate()
        }
    }

    private fun updateMealChart(stats: com.gaurav.shaadisaathi.repository.GuestStatistics) {
        val entries = mutableListOf<PieEntry>()

        stats.mealPreferenceBreakdown.forEach { (meal, count) ->
            if (count > 0) {
                val displayName = when (meal) {
                    "vegetarian" -> "Vegetarian"
                    "nonvegetarian" -> "Non-Veg"
                    "jain" -> "Jain"
                    "vegan" -> "Vegan"
                    else -> meal.replaceFirstChar { it.uppercase() }
                }
                entries.add(PieEntry(count.toFloat(), displayName))
            }
        }

        if (entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "Meal Preferences")
            dataSet.colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.meal_veg),
                ContextCompat.getColor(requireContext(), R.color.meal_nonveg),
                ContextCompat.getColor(requireContext(), R.color.colorAccent),
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
            dataSet.setDrawValues(true)
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = ContextCompat.getColor(requireContext(), android.R.color.white)

            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter())

            binding.chartMeal.data = data
            binding.chartMeal.invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = GuestStatsFragment()
    }
}
