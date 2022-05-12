package unist.pjs.hometraining

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import unist.pjs.hometraining.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val permission = 1
    private lateinit var i: Intent
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var listener: RecognitionListener
    private lateinit var countDownTimer: CountDownTimer

    private val defaultRemainedAuthTime: Long = 60000L
    private var finalTime: Long = defaultRemainedAuthTime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Glide.with(this).load(R.raw.incline_pushup).into(binding.ivGif)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO
            ), permission
        )

        i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, applicationContext.packageName) // 여분의 키
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // 언어 설정

        listener = object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                // 말하기 시작할 준비가 되면 호출
            }

            override fun onBeginningOfSpeech() {
                // 말하기 시작했을 때 호출
            }

            override fun onRmsChanged(p0: Float) {
                // 입력받는 소리의 크기를 알려줌
            }

            override fun onBufferReceived(p0: ByteArray?) {
                // 말을 시작하고 인식이 된 단어를 buffer 에 담음
            }

            override fun onEndOfSpeech() {
                // 말하기를 중지하면 호출
            }

            override fun onError(p0: Int) {
                // 네트워크 또는 인식 오류가 발생했을 때 호출
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

                Toast.makeText(applicationContext, "에러 발생: $message", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(p0: Bundle?) {
                // 인식 결과가 준비되면 호출
                // 말을 하면 ArrayList 에 단어를 넣고 textView 에 단어를 이어줌
                val matches = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                for (i in matches.indices) {
                    Log.e("TEST", "TEST: ${matches[i]}")
                    val number = matches[i].replace("[^0-9]".toRegex(), "")
                    Log.e("TEST", "TEST: $number")
                    if (number.isNotEmpty()) {
                        finalTime += 1000 * number.toLong()
                        when(binding.tvPause.text){
                            "RESUME" -> startCountDown(finalTime)

                            else -> long2Time(finalTime)
                        }
                    }
                }
            }

            override fun onPartialResults(p0: Bundle?) {
                // 부분 인식 결과를 사용할 수 있을 때 호출
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                // 향후 이벤트를 추가하기 위해 예약
            }
        }
        countDownTimer = object : CountDownTimer(finalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                finalTime = millisUntilFinished
                long2Time(finalTime)
            }

            override fun onFinish() {
                binding.tvTime.text = "00:00"
            }

        }

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
            startCountDown(finalTime)
        }
        binding.tvPrev.setOnClickListener {
            countDownTimer.cancel()
        }
        binding.tvNext.setOnClickListener {
            Toast.makeText(applicationContext, "Next", Toast.LENGTH_SHORT).show()
        }
        binding.tbAppbar.apply {
            setOnMenuItemClickListener {
                stt()
                true
            }
        }

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
            }

        }
        countDownTimer.start()
    }

    private fun stt() {
        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this) // 새 SpeechRecognizer 를 만드는 팩토리 메서드
        speechRecognizer.setRecognitionListener(listener) // 리스너 설정
        speechRecognizer.startListening(intent) // 듣기 시작
    }

    private fun long2Time(finalTime: Long){
        val minute = finalTime / 60000
        val second = finalTime % 60000 / 1000
        val remainTime = if (second < 10) "0$minute:0$second" else "0$minute:$second"

        binding.tvTime.text = remainTime
    }
}