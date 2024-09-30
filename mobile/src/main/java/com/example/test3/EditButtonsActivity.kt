package com.example.test3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class EditButtonsActivity : AppCompatActivity() {

    private lateinit var editTextButton1: EditText
    private lateinit var editTextButton2: EditText
    private lateinit var editTextButton3: EditText
    private lateinit var editTextButton4: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_buttons)

        // Initialize EditTexts
        editTextButton1 = findViewById(R.id.editTextButton1)
        editTextButton2 = findViewById(R.id.editTextButton2)
        editTextButton3 = findViewById(R.id.editTextButton3)
        editTextButton4 = findViewById(R.id.editTextButton4)
        buttonSave = findViewById(R.id.buttonSave)
        buttonCancel = findViewById(R.id.buttonCancel)

        // Get current button values from Intent
        val button1Text = intent.getStringExtra("button1Text")
        val button2Text = intent.getStringExtra("button2Text")
        val button3Text = intent.getStringExtra("button3Text")
        val button4Text = intent.getStringExtra("button4Text")

        // Set the EditText fields with current values
        editTextButton1.setText(button1Text)
        editTextButton2.setText(button2Text)
        editTextButton3.setText(button3Text)
        editTextButton4.setText(button4Text)

        // Set up Save button
        buttonSave.setOnClickListener {
            // Pass the updated button texts back to the MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("button1Text", editTextButton1.text.toString())
            resultIntent.putExtra("button2Text", editTextButton2.text.toString())
            resultIntent.putExtra("button3Text", editTextButton3.text.toString())
            resultIntent.putExtra("button4Text", editTextButton4.text.toString())

            setResult(RESULT_OK, resultIntent)
            finish()  // Close the EditButtonsActivity
        }

        // Set up Cancel button
        buttonCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()  // Close the activity without saving
        }
    }
}