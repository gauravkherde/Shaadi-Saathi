package com.gaurav.shaadisaathi.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gaurav.shaadisaathi.databinding.FragmentCountdownBinding
import com.gaurav.shaadisaathi.utils.DateTimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CountdownFragment : Fragment() {

    private var _binding: FragmentCountdownBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null
    private var weddingDate: Long = 0L
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCountdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = requireContext().getSharedPreferences("wedding_prefs", 0)

        setupClickListeners()
        loadWeddingDate()
        startCountdown()
    }

    private fun setupClickListeners() {
        binding.btnSetWeddingDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnShareCountdown.setOnClickListener {
            shareCountdown()
        }

        binding.btnAddToCalendar.setOnClickListener {
            addToCalendar()
        }
    }

    private fun loadWeddingDate() {
        weddingDate = sharedPrefs.getLong("wedding_date", 0L)

        if (weddingDate > 0) {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            binding.tvWeddingDate.text = dateFormat.format(Date(weddingDate))
            binding.layoutCountdown.visibility = View.VISIBLE
            binding.layoutSetDate.visibility = View.GONE
        } else {
            binding.layoutCountdown.visibility = View.GONE
            binding.layoutSetDate.visibility = View.VISIBLE
        }
    }

    private fun startCountdown() {
        if (weddingDate <= 0) return

        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(weddingDate - System.currentTimeMillis(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateCountdownDisplay(millisUntilFinished)
            }

            override fun onFinish() {
                showWeddingCompleteMessage()
            }
        }.start()
    }

    private fun updateCountdownDisplay(millisUntilFinished: Long) {
        val days = millisUntilFinished / (1000 * 60 * 60 * 24)
        val hours = (millisUntilFinished % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        val minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millisUntilFinished % (1000 * 60)) / 1000

        binding.tvDays.text = days.toString()
        binding.tvHours.text = hours.toString()
        binding.tvMinutes.text = minutes.toString()
        binding.tvSeconds.text = seconds.toString()

        // Update progress bars
        val totalDaysInYear = 365f
        val progressDays = ((totalDaysInYear - days) / totalDaysInYear * 100).coerceIn(0f, 100f)
        binding.progressDays.progress = progressDays.toInt()

        val progressHours = ((24 - hours) / 24f * 100).coerceIn(0f, 100f)
        binding.progressHours.progress = progressHours.toInt()

        val progressMinutes = ((60 - minutes) / 60f * 100).coerceIn(0f, 100f)
        binding.progressMinutes.progress = progressMinutes.toInt()

        // Update milestone messages
        updateMilestoneMessage(days)
    }

    private fun updateMilestoneMessage(days: Long) {
        val message = when {
            days <= 0 -> "🎉 Wedding Day is Here! 🎉"
            days == 1L -> "💒 Tomorrow is the Big Day! 💒"
            days <= 7 -> "📅 Final Week Countdown! 📅"
            days <= 30 -> "🗓️ One Month to Go! 🗓️"
            days <= 60 -> "⏰ Two Months Left! ⏰"
            days <= 100 -> "💍 100 Days or Less! 💍"
            else -> "💕 Planning in Progress 💕"
        }

        binding.tvMilestoneMessage.text = message
    }

    private fun showWeddingCompleteMessage() {
        binding.layoutCountdown.visibility = View.GONE
        binding.layoutWeddingComplete.visibility = View.VISIBLE

        binding.tvCongratulations.text = "🎉 Congratulations! 🎉"
        binding.tvWeddingCompleteMessage.text = "Your special day has arrived!\nWishing you a lifetime of happiness together! 💕"
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 12, 0, 0)

                weddingDate = selectedCalendar.timeInMillis
                saveWeddingDate()
                loadWeddingDate()
                startCountdown()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun saveWeddingDate() {
        sharedPrefs.edit()
            .putLong("wedding_date", weddingDate)
            .apply()
    }

    private fun shareCountdown() {
        if (weddingDate <= 0) return

        val currentTime = System.currentTimeMillis()
        val timeDifference = weddingDate - currentTime
        val days = timeDifference / (1000 * 60 * 60 * 24)

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val weddingDateStr = dateFormat.format(Date(weddingDate))

        val shareText = """
            🎉 Wedding Countdown! 🎉
            
            📅 Wedding Date: $weddingDateStr
            ⏰ Days Remaining: $days days
            
            The big day is getting closer! 💍
            
            Shared via ShaadiSaathi Wedding App
        """.trimIndent()

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Wedding Countdown")
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Countdown"))
    }

    private fun addToCalendar() {
        if (weddingDate <= 0) return

        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_INSERT)
            intent.data = android.provider.CalendarContract.Events.CONTENT_URI
            intent.putExtra(android.provider.CalendarContract.Events.TITLE, "Wedding Day")
            intent.putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Our special wedding day!")
            intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, weddingDate)
            intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, weddingDate + (4 * 60 * 60 * 1000)) // 4 hours
            intent.putExtra(android.provider.CalendarContract.Events.ALL_DAY, false)

            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "Unable to add to calendar", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }

    companion object {
        fun newInstance() = CountdownFragment()
    }
}
