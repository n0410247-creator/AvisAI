package com.avis.ai

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var avisSubtext: TextView
    private lateinit var statusText: TextView
    private lateinit var listeningLabel: TextView

    private val micPermissionRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        avisSubtext = findViewById(R.id.avisSubtext)
        statusText = findViewById(R.id.statusText)
        listeningLabel = findViewById(R.id.listeningLabel)

        tts = TextToSpeech(this, this)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(recognitionListener)

        val micButton = findViewById<ImageButton>(R.id.micButton)
        micButton.setOnClickListener { startListening() }

        ensureMicPermission()
    }

    // ---- Text to Speech setup: force a male voice ----
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            val maleVoice = tts.voices?.firstOrNull { voice: Voice ->
                voice.name.contains("male", ignoreCase = true) &&
                    !voice.name.contains("female", ignoreCase = true) &&
                    voice.locale == Locale.US
            }
            if (maleVoice != null) {
                tts.voice = maleVoice
            } else {
                // Fallback: lower pitch slightly reads more male on the default voice
                tts.setPitch(0.85f)
            }
            speak("Hey Boss! I'm Avis. How can I assist you today?")
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "avis_utterance")
        avisSubtext.text = text
    }

    // ---- Speech to Text ----
    private fun ensureMicPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                micPermissionRequestCode
            )
        }
    }

    private fun startListening() {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        }
        statusText.text = "LISTENING"
        listeningLabel.text = "LISTENING..."
        speechRecognizer.startListening(intent)
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val heard = matches?.firstOrNull().orEmpty()
            statusText.text = "ACTIVE"
            listeningLabel.text = "TAP THE MIC TO SPEAK"
            if (heard.isNotBlank()) {
                handleCommand(heard)
            }
        }

        override fun onError(error: Int) {
            statusText.text = "ACTIVE"
            listeningLabel.text = "TAP THE MIC TO SPEAK"
            Toast.makeText(this@MainActivity, "Didn't catch that — try again", Toast.LENGTH_SHORT).show()
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // ---- The "brain": for now just a placeholder echo.
    // Step 2 replaces this with a real call to the Claude API. ----
    private fun handleCommand(heard: String) {
        avisSubtext.text = "You said: \"$heard\""
        speak("Got it boss. You said $heard. I'll be able to act on that once step 2 is wired up.")
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
        super.onDestroy()
    }
}
