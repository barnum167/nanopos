package com.nanodatacenter.ndppos

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.elixirpay.elixirpaycat.SerialPrinter
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "NDP_PRINTER"
        private const val PRINTER_PORT = "/dev/ttyS4"
        private const val BAUD_RATE = 115200
        private const val STATUS_CHECK_INTERVAL = 5000L // 5초마다 상태 체크
    }
    
    // UI 요소
    private lateinit var ivPaymentQr: ImageView
    private lateinit var tvPrinterStatus: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvDateTime: TextView
    
    // 상태 관리
    private var networkStatus = "연결 확인 중"
    private var printerStatusText = "초기화 중"
    
    // 프린터 관련
    private var printer: SerialPrinter? = null
    private var printerHelper: PrinterHelper? = null
    private var isPrinterReady = false
    
    // 자동 인쇄 시스템 (필요시 사용)
    private var serverPollingService: ServerPollingServiceV2? = null
    private var autoPrintManager: AutoPrintManager? = null
    
    // 상태 체크 핸들러
    private val statusHandler = Handler(Looper.getMainLooper())
    private var statusCheckRunnable: Runnable? = null
    
    // 시간 업데이트 핸들러  
    private val timeHandler = Handler(Looper.getMainLooper())
    private var timeUpdateRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 전체화면 설정 (상태바 숨김)
        setFullScreen()
        
        setContentView(R.layout.activity_main)
        
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "NDP POS 시스템 시작")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        initializeViews()
        initializeComponents()
        initializePrinter()
        startStatusCheck()
        startTimeUpdate()
        
        // 자동 인쇄 시스템 자동 시작 (5초 후)
        Handler(Looper.getMainLooper()).postDelayed({
            startAutoPrintSystemWithConnectionTest()
        }, 5000)
    }
    
    /**
     * 전체화면 설정 (상태바 숨김)
     */
    private fun setFullScreen() {
        // Android 6.0 (API 23) 이상에서 동작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            
            // 상태바 숨김
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
        // 액션바 숨김
        supportActionBar?.hide()
    }
    
    /**
     * UI 요소 초기화
     */
    private fun initializeViews() {
        ivPaymentQr = findViewById(R.id.iv_payment_qr)
        tvPrinterStatus = findViewById(R.id.tv_printer_status)
        tvStatusMessage = findViewById(R.id.tv_status_message)
        tvDateTime = findViewById(R.id.tv_date_time)
        
        // QR 이미지 설정
        ivPaymentQr.setImageResource(R.drawable.payment_qr)
        
        // 초기 상태 설정
        updatePrinterStatus(false, "프린터 초기화 중...")
        tvStatusMessage.text = "시스템 준비 중..."
        
        // 프린터 상태 터치 이벤트 추가 (10번 터치시 테스트 인쇄)
        tvPrinterStatus.setOnClickListener {
            Log.d(TAG, "프린터 상태 영역 터치됨")
            autoPrintManager?.onPrinterStatusTouch()
        }
        
        Log.d(TAG, "UI 요소 초기화 완료")
    }
    
    /**
     * 컴포넌트 초기화
     */
    private fun initializeComponents() {
        try {
            printerHelper = PrinterHelper()
            
            // 자동 인쇄 시스템 초기화 (필요시)
            serverPollingService = ServerPollingServiceV2()
            autoPrintManager = AutoPrintManager()
            
            Log.d(TAG, "컴포넌트 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "컴포넌트 초기화 실패: ${e.message}")
        }
    }
    
    /**
     * 프린터 초기화
     */
    private fun initializePrinter() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "프린터 초기화 시작 - 포트: $PRINTER_PORT")
                
                printer = SerialPrinter.Builder()
                    .tty(PRINTER_PORT)
                    .baudRate(BAUD_RATE)
                    .build()
                
                // 프린터 연결 테스트
                testPrinterConnection()
                
            } catch (e: Exception) {
                Log.e(TAG, "프린터 초기화 실패: ${e.message}")
                isPrinterReady = false
                
                withContext(Dispatchers.Main) {
                    updatePrinterStatus(false, "프린터 연결 실패: ${e.message}")
                    tvStatusMessage.text = "프린터를 확인해주세요"
                }
            }
        }
    }
    
    /**
     * 프린터 연결 테스트
     */
    private suspend fun testPrinterConnection() {
        try {
            // 간단한 테스트 커맨드 전송 (초기화 커맨드)
            val testCommand = byteArrayOf(0x1B, 0x40) // ESC @ (프린터 초기화)
            printer?.setBuffer(testCommand)
            printer?.print()
            
            isPrinterReady = true
            
            withContext(Dispatchers.Main) {
                updatePrinterStatus(true, "영수증 출력 가능")
                tvStatusMessage.text = "시스템 준비 완료"
            }
            
            Log.i(TAG, "프린터 연결 성공 - 준비 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "프린터 연결 테스트 실패: ${e.message}")
            isPrinterReady = false
            
            withContext(Dispatchers.Main) {
                updatePrinterStatus(false, "프린터 오류")
                tvStatusMessage.text = "프린터 점검 필요"
            }
        }
    }
    
    /**
     * 프린터 상태 업데이트 UI
     */
    private fun updatePrinterStatus(isReady: Boolean, statusText: String) {
        printerStatusText = statusText
        runOnUiThread {
            tvPrinterStatus.text = statusText
            
            if (isReady) {
                // 준비 상태: 초록색
                tvPrinterStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                tvPrinterStatus.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_check_circle, 0, 0, 0
                )
            } else {
                // 오류 상태: 빨간색
                tvPrinterStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                tvPrinterStatus.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_error, 0, 0, 0
                )
            }
            
            // 종합 상태 메시지 업데이트
            updateOverallStatus()
        }
    }
    
    /**
     * 네트워크 상태 업데이트
     */
    private fun updateNetworkStatus(status: String) {
        networkStatus = status
        runOnUiThread {
            updateOverallStatus()
        }
    }
    
    /**
     * 종합 상태 메시지 업데이트
     */
    private fun updateOverallStatus() {
        val overallStatus = "프린터: $printerStatusText | 네트워크: $networkStatus"
        tvStatusMessage.text = overallStatus
    }
    
    /**
     * 주기적인 상태 체크 시작
     */
    private fun startStatusCheck() {
        statusCheckRunnable = object : Runnable {
            override fun run() {
                checkPrinterStatus()
                statusHandler.postDelayed(this, STATUS_CHECK_INTERVAL)
            }
        }
        statusHandler.postDelayed(statusCheckRunnable!!, STATUS_CHECK_INTERVAL)
    }
    
    /**
     * 프린터 상태 체크
     */
    private fun checkPrinterStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 프린터가 초기화되지 않았으면 다시 시도
                if (printer == null) {
                    initializePrinter()
                    return@launch
                }
                
                // 간단한 상태 체크 커맨드
                val statusCommand = byteArrayOf(0x10, 0x04, 0x01) // DLE EOT 1 (프린터 상태 요청)
                printer?.setBuffer(statusCommand)
                printer?.print()
                
                // 응답 대기 시간
                delay(100)
                
                // 프린터가 응답하면 정상
                isPrinterReady = true
                
                withContext(Dispatchers.Main) {
                    updatePrinterStatus(true, "영수증 출력 가능")
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "프린터 상태 체크 실패: ${e.message}")
                isPrinterReady = false
                
                withContext(Dispatchers.Main) {
                    updatePrinterStatus(false, "프린터 연결 확인 필요")
                }
            }
        }
    }
    
    /**
     * 시간 업데이트 시작
     */
    private fun startTimeUpdate() {
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateDateTime()
                timeHandler.postDelayed(this, 1000) // 1초마다 업데이트
            }
        }
        timeHandler.post(timeUpdateRunnable!!)
    }
    
    /**
     * 날짜 및 시간 업데이트 (한국시간 기준)
     */
    private fun updateDateTime() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
        val currentDateTime = dateFormat.format(Date())
        tvDateTime.text = currentDateTime
    }
    
    /**
     * 테스트 영수증 출력 (디버깅용)
     */
    fun printTestReceipt() {
        if (!isPrinterReady) {
            Log.w(TAG, "프린터가 준비되지 않음")
            tvStatusMessage.text = "프린터를 확인해주세요"
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 한국어 로케일 강제 설정
                Locale.setDefault(Locale.KOREA)
                Log.i(TAG, "테스트 영수증 - 로케일 설정: ${Locale.getDefault()}")
                
                val testContent = """
                    ================================
                    NDP POS SYSTEM
                    ================================
                    날짜: ${SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())}
                    시간: ${SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date())}
                    --------------------------------
                    테스트 영수증
                    프린터 상태: 정상
                    ================================
                """.trimIndent()
                
                Log.i(TAG, "테스트 영수증 내용:")
                testContent.lines().forEach { line ->
                    Log.i(TAG, "  '$line'")
                }
                
                val printData = printerHelper?.createCleanTextData(testContent, "EUC-KR")
                if (printData != null) {
                    printer?.setBuffer(printData)
                    printer?.print()
                    
                    withContext(Dispatchers.Main) {
                        tvStatusMessage.text = "테스트 영수증 출력 완료"
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "테스트 영수증 출력 실패: ${e.message}")
                
                withContext(Dispatchers.Main) {
                    tvStatusMessage.text = "출력 오류: ${e.message}"
                }
            }
        }
    }
    
    /**
     * 자동 인쇄 시스템 시작 (필요시 사용)
     */
    fun startAutoPrintSystem() {
        try {
            serverPollingService?.startPolling(autoPrintManager!!)
            updateNetworkStatus("폴링 활성화")
            Log.i(TAG, "자동 인쇄 시스템 시작")
        } catch (e: Exception) {
            updateNetworkStatus("시작 실패")
            Log.e(TAG, "자동 인쇄 시스템 시작 실패: ${e.message}")
        }
    }
    
    /**
     * 서버 연결 테스트와 함께 자동 인쇄 시스템 시작
     */
    private fun startAutoPrintSystemWithConnectionTest() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "서버 연결 테스트 및 자동 인쇄 시스템 시작 중...")
                
                updateNetworkStatus("연결 테스트 중")
                
                // 서버 연결 테스트
                val isConnected = serverPollingService?.testServerConnection() ?: false
                
                withContext(Dispatchers.Main) {
                    if (isConnected) {
                        // 연결 성공 시 자동 인쇄 시스템 시작
                        updateNetworkStatus("연결됨")
                        startAutoPrintSystem()
                        Log.i(TAG, "서버 연결 성공, 자동 인쇄 시스템 활성화")
                    } else {
                        // 연결 실패 시 재시도 예약
                        updateNetworkStatus("연결 실패 (재시도 중)")
                        Log.w(TAG, "서버 연결 실패, 30초 후 재시도 예약")
                        
                        // 30초 후 재시도
                        Handler(Looper.getMainLooper()).postDelayed({
                            startAutoPrintSystemWithConnectionTest()
                        }, 30000)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "자동 인쇄 시스템 시작 중 오류: ${e.message}")
                
                withContext(Dispatchers.Main) {
                    updateNetworkStatus("오류 (재시도 예정)")
                    
                    // 60초 후 재시도
                    Handler(Looper.getMainLooper()).postDelayed({
                        startAutoPrintSystemWithConnectionTest()
                    }, 60000)
                }
            }
        }
    }
    
    /**
     * 자동 인쇄 시스템 중지
     */
    fun stopAutoPrintSystem() {
        try {
            serverPollingService?.stopPolling()
            tvStatusMessage.text = "자동 인쇄 시스템 중지됨"
            Log.i(TAG, "자동 인쇄 시스템 중지")
        } catch (e: Exception) {
            Log.e(TAG, "자동 인쇄 시스템 중지 오류: ${e.message}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        setFullScreen() // 다시 전체화면 설정
        
        // 프린터 상태 재확인
        checkPrinterStatus()
        
        Log.d(TAG, "액티비티 재개됨")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 핸들러 정리
        statusCheckRunnable?.let { statusHandler.removeCallbacks(it) }
        timeUpdateRunnable?.let { timeHandler.removeCallbacks(it) }
        
        // 자동 인쇄 시스템 중지
        serverPollingService?.let {
            if (it.isPollingActive()) {
                it.stopPolling()
            }
        }
        
        // 프린터 리소스 해제
        printer = null
        
        Log.i(TAG, "NDP POS 시스템 종료")
    }
}
