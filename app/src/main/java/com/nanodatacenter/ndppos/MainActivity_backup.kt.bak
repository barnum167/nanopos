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
        
        // ✅ 추가: 앱 시작 시 인코딩 테스트
        runEncodingTests()
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
    
    /**
     * ✅ 추가: 앱 시작 시 인코딩 및 시스템 테스트
     */
    private fun runEncodingTests() {
        Thread {
            try {
                performComprehensiveTest()
            } catch (e: Exception) {
                Log.e(TAG, "전체 테스트 실행 오류: ${e.message}")
            }
        }.start()
    }
    
    /**
     * 전체 인코딩 및 프린터 출력 테스트
     */
    private fun performComprehensiveTest() {
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "종합 테스트 시작")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // 1. 인코딩 호환성 테스트
        testEncodingCompatibility()
        
        // 2. 이모지 제거 테스트
        testEmojiRemoval()
        
        // 3. 프린터 출력 테스트
        testPrinterOutput()
        
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "종합 테스트 완료")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    /**
     * 1. 인코딩 호환성 테스트
     */
    private fun testEncodingCompatibility() {
        Log.i(TAG, "=== 인코딩 호환성 테스트 ===")
        
        // EncodingHelper 정보 출력
        EncodingHelper.logEncodingInfo()
        
        // 호환성 테스트 실행
        val success = EncodingHelper.testEncodingCompatibility()
        Log.i(TAG, "인코딩 호환성 테스트: ${if (success) "✅ 성공" else "❌ 실패"}")
        
        // 추가 테스트 케이스
        val testCases = listOf(
            "결제가 완료되었습니다",
            "김치찌개 8,000원",
            "거래 해시: 0x1234...5678",
            "보내는 주소: 0xabcd...efgh"
        )
        
        Log.i(TAG, "--- 추가 테스트 케이스 ---")
        for ((index, testCase) in testCases.withIndex()) {
            try {
                val encoded = EncodingHelper.stringToBytes(testCase)
                val decoded = EncodingHelper.bytesToString(encoded)
                val match = testCase == decoded
                
                Log.i(TAG, "케이스 ${index + 1}: ${if (match) "✅" else "❌"} '$testCase'")
                if (!match) {
                    Log.w(TAG, "  원본: '$testCase'")
                    Log.w(TAG, "  복원: '$decoded'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "케이스 ${index + 1} 오류: ${e.message}")
            }
        }
    }
    
    /**
     * 2. 이모지 제거 테스트
     */
    private fun testEmojiRemoval() {
        Log.i(TAG, "=== 이모지 제거 테스트 ===")
        
        val emojiTestCases = listOf(
            "결제 영수증",
            "거래 정보",
            "주소 정보",
            "처리 시간",
            "결제가 완료되었습니다",
            "프린터 테스트",
            "감사합니다!"
        )
        
        var allSuccess = true
        for ((index, testCase) in emojiTestCases.withIndex()) {
            val sanitized = EncodingHelper.sanitizeForPrinter(testCase)
            val hasEmoji = testCase != sanitized
            
            Log.i(TAG, "이모지 테스트 ${index + 1}: ${if (hasEmoji) "제거됨" else "⚠원본유지"}")
            Log.i(TAG, "  입력: '$testCase'")
            Log.i(TAG, "  출력: '$sanitized'")
        }
        
        Log.i(TAG, "이모지 제거 테스트 완료")
    }
    
    /**
     * 3. 프린터 출력 테스트
     */
    private fun testPrinterOutput() {
        Log.i(TAG, "=== 프린터 출력 테스트 ===")
        
        try {
            // 1. EUC-KR 인코딩 테스트
            Log.i(TAG, "1. EUC-KR 인코딩 테스트")
            val testContent = """
                인코딩 테스트 (EUC-KR)
                
                한글 출력 테스트
                가나다라마바사
                아자차카타파하
                
                결제 정보:
                - 상품: 아메리카노
                - 금액: 4,500원
                - 상태: 완료
                
                감사합니다!
            """.trimIndent()
            
            val printData = printerHelper.createCleanTextData(testContent, "EUC-KR")
            Log.i(TAG, "프린터 테스트 데이터 생성 완료: ${printData.size} bytes")
            
            // 2. 영수증 형태 테스트
            Log.i(TAG, "2. 영수증 형태 테스트")
            val receiptItems = listOf(
                "아메리카노" to "4,500원",
                "카페라떼" to "5,500원", 
                "크로와상" to "3,000원"
            )
            
            val receiptData = printerHelper.createReceiptData(
                title = "*** 테스트 영수증 ***", // 이모지 제거됨
                items = receiptItems,
                totalAmount = "13,000원"
            )
            
            Log.i(TAG, "영수증 테스트 데이터 생성 완료: ${receiptData.size} bytes")
            
            // 3. 자동 인쇄 매니저 테스트
            Log.i(TAG, "3. 자동 인쇄 매니저 테스트")
            testAutoPrintManager()
            
            Log.i(TAG, "✅ 프린터 출력 테스트 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 프린터 출력 테스트 실패: ${e.message}")
        }
    }
    
    /**
     * 자동 인쇄 매니저 테스트
     */
    private fun testAutoPrintManager() {
        Log.i(TAG, "=== 자동 인쇄 매니저 테스트 ===")
        
        try {
            // 테스트용 영수증 데이터 생성
            val testReceiptData = ReceiptData(
                printId = "test-001",
                transactionHash = "0x1234567890abcdef1234567890abcdef12345678",
                amount = "10000",
                token = "USDT",
                fromAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd",
                toAddress = "0x1234567890123456789012345678901234567890",
                timestamp = "2024-08-29T10:30:00.000Z"
            )
            
            Log.i(TAG, "테스트 영수증 데이터 생성 완료")
            Log.i(TAG, "  - ID: ${testReceiptData.printId}")
            Log.i(TAG, "  - 금액: ${testReceiptData.amount} ${testReceiptData.token}")
            Log.i(TAG, "  - 타임스탬프: ${testReceiptData.timestamp}")
            
            // 실제 인쇄는 하지 않고 데이터 생성만 테스트
            // val printResult = autoPrintManager.printReceipt(testReceiptData)
            // Log.i(TAG, "자동 영수증 인쇄 테스트: ${if (printResult) "✅ 성공" else "❌ 실패"}")
            
            Log.i(TAG, "자동 인쇄 매니저 데이터 생성 테스트 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "자동 인쇄 매니저 테스트 오류: ${e.message}")
        }
    }
}