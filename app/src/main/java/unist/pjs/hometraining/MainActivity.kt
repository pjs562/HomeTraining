package unist.pjs.hometraining

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import unist.pjs.hometraining.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val permission = 1
    private lateinit var i: Intent
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var listener: RecognitionListener
    private lateinit var countDownTimer: CountDownTimer

    private var tts: TextToSpeech? = null

    private val defaultRemainedAuthTime: Long = 60000L
    private var finalTime: Long = defaultRemainedAuthTime
    private var currentPage: Int = 1
    private var currentString: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO
            ), permission
        )

        countDownTimer = object : CountDownTimer(finalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                finalTime = millisUntilFinished
                long2Time(finalTime)
            }

            override fun onFinish() {
                binding.tvTime.text = "00:00"
            }

        }
        switchPage(currentPage)
        i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, applicationContext.packageName) // 여분의 키
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // 언어 설정

        listener = object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                // 말하기 시작할 준비가 되면 호출
                Log.e("TEST", "onReadyForSpeech: $p0")
            }

            override fun onBeginningOfSpeech() {
                // 말하기 시작했을 때 호출
                Log.e("TEST", "onBeginningOfSpeech")
            }

            override fun onRmsChanged(p0: Float) {
                // 입력받는 소리의 크기를 알려줌
                if (p0 > 5)
                    Log.e("TEST", "onRmsChanged: $p0")
            }

            override fun onBufferReceived(p0: ByteArray?) {
                // 말을 시작하고 인식이 된 단어를 buffer 에 담음
            }

            override fun onEndOfSpeech() {
                // 말하기를 중지하면 호출
                Log.e("TEST", "onEndOfSpeech")
            }

            override fun onError(p0: Int) {
                // 네트워크 또는 인식 오류가 발생했을 때 호출
                Log.e("TEST", "onError")
                var message = ""

                message = when (p0) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "퍼미션 없음"
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                    SpeechRecognizer.ERROR_NO_MATCH -> "찾을 수 없음"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER 가 바쁨"
                    SpeechRecognizer.ERROR_SERVER -> "서버가 이상함"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말하는 시간초과"
                    else -> "알 수 없는 오류"
                }

                if (message != "찾을 수 없음")
                    Toast.makeText(applicationContext, "에러 발생: $message", Toast.LENGTH_SHORT).show()
                stt()
            }

            override fun onResults(p0: Bundle?) {
                Log.e("TEST", "onResults: $p0")
                // 인식 결과가 준비되면 호출
                // 말을 하면 ArrayList 에 단어를 넣고 textView 에 단어를 이어줌
                val matches = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                var sign = "+"
                for (i in matches.indices) {
                    Log.e("TEST", "matches[$i]: ${matches[i]}")
                    if (matches[i].contains("-")) {
                        sign = "-"
                    } else if (matches[i].contains("+")) {
                        sign = "+"
                    } else if (matches[i].contains("explain")) {
                        ttsSpeak(currentString)
                    } else if (matches[i].contains("start") || matches[i].contains("resume")) {
                        binding.tvPause.text = "PAUSE"
                        startCountDown(finalTime)
                    } else if (matches[i].contains("pause") || matches[i].contains("stop")) {
                        binding.tvPause.text = "RESUME"
                        countDownTimer.cancel()
                    } else if (matches[i].contains("next")) {
                        binding.tvNext.performClick()
                    } else if (matches[i].contains("previous")|| matches[i].contains("back")) {
                        binding.tvPrev.performClick()
                    }
                    val number = matches[i].replace("[^0-9]".toRegex(), "")
                    Log.e("TEST", "number: $number")
                    if (number.isNotEmpty()) {
                        if (sign == "-") finalTime -= 1000 * number.toLong() else finalTime += 1000 * number.toLong()

                        when (binding.tvPause.text) {
                            "PAUSE" -> startCountDown(finalTime)

                            else -> long2Time(finalTime)
                        }
                    }
                }
                stt()
            }

            override fun onPartialResults(p0: Bundle?) {
                // 부분 인식 결과를 사용할 수 있을 때 호출
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                // 향후 이벤트를 추가하기 위해 예약
            }
        }

        initTextToSpeech()

        binding.tvPause.setOnClickListener {
            when (binding.tvPause.text) {
                "START" -> {
                    binding.tvPause.text = "PAUSE"
                    startCountDown(finalTime)
                }
                "PAUSE" -> {
                    binding.tvPause.text = "RESUME"
                    countDownTimer.cancel()
                }
                else -> {
                    binding.tvPause.text = "PAUSE"
                    startCountDown(finalTime)
                }
            }
        }
        binding.tvAddTime.setOnClickListener {
            finalTime += 20000L
            when (binding.tvPause.text) {
                "PAUSE" -> startCountDown(finalTime)

                else -> {
                    long2Time(finalTime)
                }
            }
        }
        binding.tvPrev.setOnClickListener {
            currentPage = if (currentPage == 1) 7 else currentPage - 1
            switchPage(currentPage)
        }
        binding.tvNext.setOnClickListener {
            currentPage = if (currentPage == 7) 1 else currentPage + 1
            switchPage(currentPage)
        }
        binding.tbAppbar.apply {
            setOnMenuItemClickListener {
                ttsSpeak(currentString)
                true
            }
            setNavigationOnClickListener {
                currentPage = if (currentPage == 1) 7 else currentPage - 1
                switchPage(currentPage)
            }
        }
        stt()
    }

    private fun startCountDown(time: Long) {
        countDownTimer.cancel()
        countDownTimer = object : CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                finalTime = millisUntilFinished
                long2Time(finalTime)
            }

            override fun onFinish() {
                binding.tvTime.text = "00:00"
                binding.tvNext.performClick()
            }
        }
        countDownTimer.start()
    }

    private fun stt() {
        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this) // 새 SpeechRecognizer 를 만드는 팩토리 메서드
        speechRecognizer.setRecognitionListener(listener) // 리스너 설정
        speechRecognizer.startListening(i) // 듣기 시작
    }

    private fun long2Time(finalTime: Long) {
        val minute = finalTime / 60000
        val second = finalTime % 60000 / 1000
        val remainTime = if (second < 10) "0$minute:0$second" else "0$minute:$second"

        binding.tvTime.text = remainTime
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
                    return@TextToSpeech
                }
            } else {
                Toast.makeText(this, "TTS init failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ttsSpeak(strTTS: String) {
        tts?.setSpeechRate(0.8f);    // 읽는 속도는 기본 설정
        tts?.speak(strTTS, TextToSpeech.QUEUE_ADD, null, null)
    }

    private fun switchPage(number: Int) {
        when (number) {
            1 -> {
                binding.tvTitle.text = "INCLINE PUSH-UPS"
                Glide.with(this).load(R.raw.incline_pushup).into(binding.ivGif)
                binding.tvCount.visibility = View.VISIBLE
                binding.ivGif.visibility = View.VISIBLE
                binding.tvPause.text = "PAUSE"
                finalTime = 60000L
                currentString = resources.getString(R.string.incline_push_up)
            }
            3 -> {
                binding.tvTitle.text = "KNEE PUSH-UPS"
                Glide.with(this).load(R.raw.knee_pushup).into(binding.ivGif)
                binding.tvCount.visibility = View.VISIBLE
                binding.ivGif.visibility = View.VISIBLE
                binding.tvPause.text = "PAUSE"
                finalTime = 60000L
                currentString = resources.getString(R.string.knee_push_up)
            }
            5 -> {
                binding.tvTitle.text = "PUSH-UPS"
                Glide.with(this).load(R.raw.pushup).into(binding.ivGif)
                binding.tvCount.visibility = View.VISIBLE
                binding.ivGif.visibility = View.VISIBLE
                binding.tvPause.text = "PAUSE"
                finalTime = 60000L
                currentString = resources.getString(R.string.push_up)
            }
            7 -> {
                binding.tvTitle.text = "WIDE ARM PUSH-UPS"
                Glide.with(this).load(R.raw.wide_arm_pushup).into(binding.ivGif)
                binding.tvCount.visibility = View.VISIBLE
                binding.ivGif.visibility = View.VISIBLE
                binding.tvPause.text = "PAUSE"
                finalTime = 60000L
                currentString = resources.getString(R.string.wide_arm_push_up)
            }
            else -> {
                binding.tvTitle.text = "REST"
                binding.tvCount.visibility = View.GONE
                binding.ivGif.visibility = View.GONE
                binding.tvPause.text = "PAUSE"
                finalTime = 20000L
                currentString = resources.getString(R.string.rest)
            }
        }
        startCountDown(finalTime)
    }

    override fun onStop() {
        tts?.let {
            it.stop()
            it.shutdown()
        }
        speechRecognizer.destroy()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}