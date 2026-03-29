package com.satya.factory

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.satya.factory.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val DEFAULT_MESSAGE = "Progress Button Clicked"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBtnStart.setOnButtonClick {
            showDefaultToast()
        }

        binding.progressBtnMiddle.setOnButtonClick {
            showDefaultToast()
        }

        binding.progressBtnEnd.setOnButtonClick {
            showDefaultToast()
        }

        binding.btnSearch.setOnClickListener {
            showToast("Search button clicked!")
        }

        binding.btnSave.setOnClickListener {
            showToast("Save button clicked!")
        }

        binding.btnMore.setOnClickListener {
            showToast("More btn clicked!")
        }
    }

    private fun showToast(p0: String) {
        Toast.makeText(this, p0, Toast.LENGTH_SHORT).show()
    }

    private fun showDefaultToast() {
        Toast.makeText(this, DEFAULT_MESSAGE, Toast.LENGTH_SHORT).show()
    }
}