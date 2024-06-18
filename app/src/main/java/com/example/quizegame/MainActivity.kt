package com.example.quizegame

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.quizegame.databinding.ActivityMainBinding
import com.example.quizegame.databinding.DialogResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {
    companion object {
        const val PREFS_NAME = "GamePreferences"
        const val HIGH_SCORE_KEY = "HighScore"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var isPlayed = false
    private var firstRandomNumber: Int? = null
    private var secondRandomNumber: Int? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        binding.cardQuestion.visibility = View.GONE
        binding.cardScore.visibility = View.GONE
    }

    private fun setupListeners() {
        binding.btnStartOrNext.setOnClickListener {
            if (isPlayed) {
                getRandomNumbers()
                binding.tvScore.text = (binding.tvScore.text.toString().toInt() - 1).toString()
            } else {
                isPlayed = true
                binding.btnStartOrNext.text = "Next!"
                binding.cardQuestion.visibility = View.VISIBLE
                binding.cardScore.visibility = View.VISIBLE
                getRandomNumbers()
                runTimer()
            }
        }

        binding.etAnswer.addTextChangedListener { editable ->
            editable?.toString()?.toIntOrNull()?.let { inputNumber ->
                val answer = (firstRandomNumber ?: 0) + (secondRandomNumber ?: 0)
                if (inputNumber == answer) {
                    val newScore = binding.tvScore.text.toString().toInt() + 1
                    binding.tvScore.text = newScore.toString()
                    updateHighScore(newScore)
                    binding.etAnswer.setText("")
                    getRandomNumbers()
                }
            }
        }
    }

    private fun runTimer() {
        lifecycleScope.launch(Dispatchers.Main) {
            (1..30).asFlow().onStart {
                binding.constraintLayout.transitionToEnd()
            }.onCompletion {
                runOnUiThread {
                    updateHighScore(binding.tvScore.text.toString().toInt())
                    binding.cardQuestion.visibility = View.GONE
                    showEndGameDialog()
                }
            }.collect {
                delay(1000)
            }
        }
    }

    private fun showEndGameDialog() {
        val dialogBinding = DialogResultBinding.inflate(layoutInflater)
        val dialog = Dialog(this@MainActivity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setCancelable(false)
            show()
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialogBinding.apply {
            val currentScore = binding.tvScore.text.toString().toInt()
            val highScore = sharedPreferences.getInt(HIGH_SCORE_KEY, 0)
            tvDialogScore.text = "Score: $currentScore\nHigh Score: $highScore"
            btnClose.setOnClickListener {
                dialog.dismiss()
                finish()
            }
            btnTryAgain.setOnClickListener {
                dialog.dismiss()
                resetGame()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getRandomNumbers() {
        firstRandomNumber = Random.nextInt(2, 99)
        secondRandomNumber = Random.nextInt(2, 99)
        binding.tvQuestionNumber.text = "$firstRandomNumber + $secondRandomNumber"
    }

    private fun updateHighScore(currentScore: Int) {
        val highScore = sharedPreferences.getInt(HIGH_SCORE_KEY, 0)
        if (currentScore > highScore) {
            sharedPreferences.edit().putInt(HIGH_SCORE_KEY, currentScore).apply()
        }
    }

    private fun resetGame() {
        binding.apply {
            btnStartOrNext.text = getString(R.string.start_game)
            cardQuestion.visibility = View.GONE
            cardScore.visibility = View.GONE
            isPlayed = false
            constraintLayout.setTransition(R.id.start, R.id.end)
            constraintLayout.transitionToEnd()
            tvScore.text = "0"
        }
    }
}
