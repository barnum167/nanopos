package com.nanodatacenter.ndppos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.elixirpay.elixirpaycat.SerialPrinter
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "NDP_PRINTER"
    }
    
    private lateinit var printer: SerialPrinter
    private lateinit var printerHelper: PrinterHelper
    private lateinit var koreanHelper: KoreanPrinterHelper
    
    // 자동 인쇄 시스템 (SSL 검증 우회 포함)
    private lateinit var serverPollingService: ServerPollingServiceV2
    private lateinit var autoPrintManager: AutoPrintManager
    
    // UI 요소
    private lateinit var etPrintContent: EditText
    private lateinit var btnPrint: Button
    private lateinit var btnTestPrint: Button
    private lateinit var btnKoreanTest: Button
    private lateinit var tvStatus: TextView
    private lateinit var rgEncoding: RadioGroup
    private lateinit var rbUtf8: RadioButton
    private lateinit var rbEucKr: RadioButton
    private lateinit var rbCp949: RadioButton
    private lateinit var switchSimpleMode: Switch
    
    // 자동 인쇄 UI 요소
    private lateinit var tvServerStatus: TextView
    private lateinit var viewServerIndicator: View
    private lateinit var btnStartAutoPrint: Button
    private lateinit var btnStopAutoPrint: Button
    private lateinit var tvAutoPrintStatus: TextView
    
    private val printerPort = "/dev/ttyS4"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "NDP 프린터 앱 시작")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        initializeComponents()
        initializeViews()
        initializePrinter()
        setupClickListeners()
        checkServerConnection()
    }
    
    private fun initializeComponents() {
        printerHelper = PrinterHelper()
        koreanHelper = KoreanPrinterHelper()
        
        // 자동 인쇄 시스템 초기화 (SSL 검증 우회 포함)
        serverPollingService = ServerPollingServiceV2()
        autoPrintManager = AutoPrintManager()
        
        Log.d(TAG, "프린터 헬퍼 초기화 완료")
        Log.d(TAG, "자동 인쇄 시스템 초기화 완료")
    }
    
    private fun initializeViews() {
        // 기본 UI 요소
        etPrintContent = findViewById(R.id.et_print_content)
        btnPrint = findViewById(R.id.btn_print)
        btnTestPrint = findViewById(R.id.btn_test_print)
        btnKoreanTest = findViewById(R.id.btn_korean_test)
        tvStatus = findViewById(R.id.tv_status)
        rgEncoding = findViewById(R.id.rg_encoding)
        rbUtf8 = findViewById(R.id.rb_utf8)
        rbEucKr = findViewById(R.id.rb_euc_kr)
        rbCp949 = findViewById(R.id.rb_cp949)
        switchSimpleMode = findViewById(R.id.switch_simple_mode)
        
        // 자동 인쇄 UI 요소
        tvServerStatus = findViewById(R.id.tv_server_status)
        viewServerIndicator = findViewById(R.id.view_server_indicator)
        btnStartAutoPrint = findViewById(R.id.btn_start_auto_print)
        btnStopAutoPrint = findViewById(R.id.btn_stop_auto_print)
        tvAutoPrintStatus = findViewById(R.id.tv_auto_print_status)
        
        // 초기 상태 설정
        tvStatus.text = "프린터 준비 중..."
        rbUtf8.isChecked = true
        switchSimpleMode.isChecked = true // 기본적으로 간단 모드 활성화
        
        // 자동 인쇄 초기 상태
        tvServerStatus.text = "서버 연결 확인 중..."
        tvAutoPrintStatus.text = "자동 인쇄 시스템 대기 중"
        updateServerConnectionIndicator(false)
        
        Log.d(TAG, "UI 요소 초기화 완료")
    }
    
    private fun initializePrinter() {
        try {
            printer = SerialPrinter.Builder()
                .tty(printerPort)
                .baudRate(115200)
                .build()
                
            tvStatus.text = "프린터 준비 완료\n포트: $printerPort"
            Log.i(TAG, "프린터 초기화 완료 - 포트: $printerPort, 115200bps")
        } catch (e: Exception) {
            Log.e(TAG, "프린터 초기화 실패: ${e.message}")
            tvStatus.text = "프린터 초기화 실패\n${e.message}"
        }
    }
    
    private fun setupClickListeners() {
        // 일반 프린트 버튼
        btnPrint.setOnClickListener {
            val content = etPrintContent.text.toString().trim()
            if (content.isNotEmpty()) {
                performPrint(content)
            } else {
                Toast.makeText(this, "인쇄할 내용을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 테스트 프린트 버튼
        btnTestPrint.setOnClickListener {
            performTestPrint()
        }
        
        // 한국어 테스트 프린트 버튼
        btnKoreanTest.setOnClickListener {
            performKoreanTestPrint()
        }
        
        // 자동 인쇄 시작 버튼
        btnStartAutoPrint.setOnClickListener {
            startAutoPrintSystem()
        }
        
        // 자동 인쇄 중지 버튼
        btnStopAutoPrint.setOnClickListener {
            stopAutoPrintSystem()
        }
    }
    
    /**
     * 인쇄 실행
     */
    private fun performPrint(content: String) {
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "프린트 시작")
        Log.i(TAG, "내용: $content")
        Log.i(TAG, "간단 모드: ${switchSimpleMode.isChecked}")
        Log.i(TAG, "포트: $printerPort")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // UI 업데이트
        tvStatus.text = "인쇄 중...\n내용: ${content.take(30)}${if(content.length > 30) "..." else ""}"
        btnPrint.isEnabled = false
        
        // 선택된 인코딩 확인
        val selectedEncoding = when (rgEncoding.checkedRadioButtonId) {
            R.id.rb_utf8 -> "UTF-8"
            R.id.rb_euc_kr -> "EUC-KR"
            R.id.rb_cp949 -> "CP949"
            else -> "UTF-8"
        }
        
        Log.i(TAG, "선택된 인코딩: $selectedEncoding")
        
        // 백그라운드에서 인쇄 실행
        Thread {
            try {
                // 간단 모드 확인
                val printData = if (switchSimpleMode.isChecked) {
                    // 순수 텍스트만 출력 (헤더, 푸터, 부가정보 없음)
                    printerHelper.createCleanTextData(content, selectedEncoding)
                } else {
                    // 기존 방식 (헤더, 푸터 포함)
                    when (selectedEncoding) {
                        "UTF-8" -> printerHelper.createEncodedPrintData(content, "UTF-8")
                        "EUC-KR" -> printerHelper.createEncodedPrintData(content, "EUC-KR")
                        "CP949" -> printerHelper.createEncodedPrintData(content, "CP949")
                        else -> printerHelper.createEncodedPrintData(content, "UTF-8")
                    }
                }
                
                Log.d(TAG, "인쇄 데이터 생성 완료: ${printData.size} bytes")
                
                // 프린터로 데이터 전송
                printer.setBuffer(printData)
                
                val startTime = System.currentTimeMillis()
                printer.print()
                val endTime = System.currentTimeMillis()
                
                Log.i(TAG, "인쇄 완료!")
                Log.i(TAG, "  ✓ 전송 시간: ${endTime - startTime}ms")
                Log.i(TAG, "  ✓ 전송 바이트: ${printData.size}")
                Log.i(TAG, "  ✓ 모드: ${if(switchSimpleMode.isChecked) "간단 출력" else "상세 출력"}")
                
                // 프린터 처리 대기
                Thread.sleep(2000)
                
                runOnUiThread {
                    val modeText = if(switchSimpleMode.isChecked) "간단 모드" else "상세 모드"
                    tvStatus.text = "인쇄 완료!\n시간: ${getCurrentTime()}\n전송: ${printData.size} bytes\n인코딩: $selectedEncoding\n모드: $modeText"
                    btnPrint.isEnabled = true
                    Toast.makeText(this, "✓ 인쇄 완료! ($modeText)", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "인쇄 실패: ${e.message}", e)
                
                runOnUiThread {
                    tvStatus.text = "인쇄 실패\n오류: ${e.message}"
                    btnPrint.isEnabled = true
                    Toast.makeText(this, "✗ 인쇄 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    /**
     * 테스트 인쇄 실행
     */
    private fun performTestPrint() {
        Log.i(TAG, "테스트 인쇄 시작 - 간단 모드: ${switchSimpleMode.isChecked}")
        
        tvStatus.text = "테스트 인쇄 중..."
        btnTestPrint.isEnabled = false
        
        Thread {
            try {
                val printData = if (switchSimpleMode.isChecked) {
                    // 간단한 테스트 데이터 (부가 정보 없음)
                    printerHelper.createCleanTestData()
                } else {
                    // 기존 상세 테스트 데이터
                    printerHelper.createTestPrintData()
                }
                
                printer.setBuffer(printData)
                printer.print()
                
                Thread.sleep(2000)
                
                runOnUiThread {
                    val modeText = if(switchSimpleMode.isChecked) "간단 모드" else "상세 모드"
                    tvStatus.text = "테스트 인쇄 완료!\n시간: ${getCurrentTime()}\n모드: $modeText"
                    btnTestPrint.isEnabled = true
                    Toast.makeText(this, "✓ 테스트 인쇄 완료! ($modeText)", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "테스트 인쇄 실패: ${e.message}", e)
                
                runOnUiThread {
                    tvStatus.text = "테스트 인쇄 실패\n오류: ${e.message}"
                    btnTestPrint.isEnabled = true
                    Toast.makeText(this, "✗ 테스트 인쇄 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    /**
     * 한국어 테스트 인쇄 실행
     */
    private fun performKoreanTestPrint() {
        Log.i(TAG, "한국어 테스트 인쇄 시작 - 간단 모드: ${switchSimpleMode.isChecked}")
        
        tvStatus.text = "한국어 테스트 인쇄 중..."
        btnKoreanTest.isEnabled = false
        
        Thread {
            try {
                if (switchSimpleMode.isChecked) {
                    // 간단한 한국어 테스트
                    koreanHelper.printSimpleKoreanTest()
                    Thread.sleep(2000)
                } else {
                    // 기존 상세 한국어 테스트 (여러 인코딩)
                    koreanHelper.printKoreanTestReceipt()
                    Thread.sleep(3000)
                }
                
                runOnUiThread {
                    val modeText = if(switchSimpleMode.isChecked) "간단 모드" else "상세 모드"
                    tvStatus.text = "한국어 테스트 인쇄 완료!\n모드: $modeText"
                    btnKoreanTest.isEnabled = true
                    Toast.makeText(this, "✓ 한국어 테스트 인쇄 완료! ($modeText)", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "한국어 테스트 인쇄 실패: ${e.message}", e)
                
                runOnUiThread {
                    tvStatus.text = "한국어 테스트 인쇄 실패\n오류: ${e.message}"
                    btnKoreanTest.isEnabled = true
                    Toast.makeText(this, "✗ 한국어 테스트 인쇄 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun getCurrentTime(): String {
        val dateFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }
    
    /**
     * 서버 연결 상태 확인
     */
    private fun checkServerConnection() {
        Log.d(TAG, "서버 연결 상태 확인 시작")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isConnected = serverPollingService.testServerConnection()
                
                runOnUiThread {
                    if (isConnected) {
                        tvServerStatus.text = "서버 연결 성공"
                        updateServerConnectionIndicator(true)
                        btnStartAutoPrint.isEnabled = true
                        Log.i(TAG, "서버 연결 성공")
                    } else {
                        tvServerStatus.text = "서버 연결 실패"
                        updateServerConnectionIndicator(false)
                        btnStartAutoPrint.isEnabled = false
                        Log.w(TAG, "서버 연결 실패")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "서버 연결 확인 오류: ${e.message}")
                
                runOnUiThread {
                    tvServerStatus.text = "연결 확인 오류: ${e.message}"
                    updateServerConnectionIndicator(false)
                    btnStartAutoPrint.isEnabled = false
                }
            }
        }
    }
    
    /**
     * 자동 인쇄 시스템 시작
     */
    private fun startAutoPrintSystem() {
        try {
            Log.i(TAG, "자동 인쇄 시스템 시작")
            
            serverPollingService.startPolling(autoPrintManager)
            
            // UI 업데이트
            btnStartAutoPrint.isEnabled = false
            btnStopAutoPrint.isEnabled = true
            tvAutoPrintStatus.text = "자동 인쇄 시스템 실행 중 (폴링 간격: 3초)"
            
            Toast.makeText(this, "자동 인쇄 시스템이 시작되었습니다", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "자동 인쇄 시스템 시작 실패: ${e.message}")
            Toast.makeText(this, "자동 인쇄 시스템 시작 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 자동 인쇄 시스템 중지
     */
    private fun stopAutoPrintSystem() {
        try {
            Log.i(TAG, "자동 인쇄 시스템 중지")
            
            serverPollingService.stopPolling()
            
            // UI 업데이트
            btnStartAutoPrint.isEnabled = true
            btnStopAutoPrint.isEnabled = false
            tvAutoPrintStatus.text = "자동 인쇄 시스템 중지됨"
            
            Toast.makeText(this, "자동 인쇄 시스템이 중지되었습니다", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "자동 인쇄 시스템 중지 오류: ${e.message}")
            Toast.makeText(this, "자동 인쇄 시스템 중지 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 서버 연결 상태 표시기 업데이트
     */
    private fun updateServerConnectionIndicator(isConnected: Boolean) {
        runOnUiThread {
            if (isConnected) {
                viewServerIndicator.setBackgroundColor(getColor(android.R.color.holo_green_light))
            } else {
                viewServerIndicator.setBackgroundColor(getColor(android.R.color.holo_orange_light))
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "액티비티 재개됨")
        
        // 서버 연결 상태 재확인
        checkServerConnection()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 자동 인쇄 시스템 중지
        if (serverPollingService.isPollingActive()) {
            serverPollingService.stopPolling()
            Log.i(TAG, "앱 종료 시 자동 인쇄 시스템 중지")
        }
        
        Log.i(TAG, "NDP 프린터 앱 종료")
    }
}