package fr.haran.example

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setClickListeners()
    }

    private fun setClickListeners() {
        findViewById<Button>(R.id.play).setOnClickListener {
            startActivity(Intent(this, PlayActivity::class.java)) }
        findViewById<Button>(R.id.rec).setOnClickListener {
            startActivity(Intent(this, RecActivity::class.java)) }
    }
}