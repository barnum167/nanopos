package com.nanodatacenter.ndppos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.elixirpay.elixirpaycat.SerialPrinter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "NDP_QR_GENERATOR"
        private const val CAMERA_PERMISSION_REQUEST = 1001
    }
    
    private lateinit var printer: SerialPrinter
    private lateinit var qrGenerator: QRCodeGenerator
    
    // QR 생성 관련 UI
    private lateinit var etPaymentAmount: EditText
    private lateinit var etMerchantName: EditText
    private lateinit var etPaymentDescription: EditText
    private lateinit var btnGenerateQr: Button
    private lateinit var tvQrTitle: TextView
    private lateinit var layoutQrDisplay: LinearLayout
    private lateinit var ivGeneratedQr: ImageView
    private lateinit var tvQrInfo: TextView
    private lateinit var btnPrintGeneratedQr: Button
    
    // QR 스캔 관련 UI
    private lateinit var btnScanQR: Button
    private lateinit var btnManualInput: Button
    private lateinit var btnPrint: Button
    private lateinit var etQrContent: EditText
    private lateinit var tvStatus: TextView
    private lateinit var tvLastScan: TextView
    
    private val printerPort = "/dev/ttyS4"
    private var qrContent = ""
    private var generatedQrBitmap: Bitmap? = null
    private var lastGeneratedQrString = ""
    
    // QR 스캔 결과를 받는 launcher
    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        handleQrScanResult(result)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "NDP QR 생성기 앱 시작")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        initializeComponents()
        initializeViews()
        initializePrinter()
        setupClickListeners()
        checkCameraPermission()
    }
    
    private fun initializeComponents() {
        qrGenerator = QRCodeGenerator()
        Log.d(TAG, "QR 생성기 초기화 완료")
    }
    
    private fun initializeViews() {
        // QR 생성 관련 UI
        etPaymentAmount = findViewById(R.id.et_payment_amount)
        etMerchantName = findViewById(R.id.et_merchant_name)
        etPaymentDescription = findViewById(R.id.et_payment_description)
        btnGenerateQr = findViewById(R.id.btn_generate_qr)
        tvQrTitle = findViewById(R.id.tv_qr_title)
        layoutQrDisplay = findViewById(R.id.layout_qr_display)
        ivGeneratedQr = findViewById(R.id.iv_generated_qr)
        tvQrInfo = findViewById(R.id.tv_qr_info)
        btnPrintGeneratedQr = findViewById(R.id.btn_print_generated_qr)
        
        // QR 스캔 관련 UI
        btnScanQR = findViewById(R.id.btn_scan_qr)
        btnManualInput = findViewById(R.id.btn_manual_input)
        btnPrint = findViewById(R.id.btn_print)
        etQrContent = findViewById(R.id.et_qr_content)
        tvStatus = findViewById(R.id.tv_status)
        tvLastScan = findViewById(R.id.tv_last_scan)
        
        // 초기 상태 설정
        btnPrint.isEnabled = false
        tvStatus.text = "프린터 준비 중..."
        tvLastScan.text = "QR 코드를 생성, 스캔하거나 직접 입력하세요"
        
        // 기본값 설정
        etMerchantName.setText("나노데이터센터")
        etPaymentDescription.setText("상품 구매")
        
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
        // QR 생성 버튼
        btnGenerateQr.setOnClickListener {
            generatePaymentQR()
        }
        
        // 생성된 QR 인쇄 버튼
        btnPrintGeneratedQr.setOnClickListener {
            printGeneratedQR()
        }
        
        // QR 스캔 버튼
        btnScanQR.setOnClickListener {
            if (hasCameraPermission()) {
                startQrScan()
            } else {
                requestCameraPermission()
            }
        }
        
        // 수동 입력 버튼
        btnManualInput.setOnClickListener {
            val content = etQrContent.text.toString().trim()
            if (content.isNotEmpty()) {
                handleManualInput(content)
            } else {
                Toast.makeText(this, "내용을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 일반 프린트 버튼
        btnPrint.setOnClickListener {
            if (qrContent.isNotEmpty()) {
                performPrint(qrContent)
            } else {
                Toast.makeText(this, "인쇄할 내용이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 결제용 QR 코드 생성
     */
    private fun generatePaymentQR() {
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "결제용 QR 코드 생성 시작")
        
        val amount = etPaymentAmount.text.toString().trim()
        val merchantName = etMerchantName.text.toString().trim()
        val description = etPaymentDescription.text.toString().trim()
        
        // 입력 검증
        if (amount.isEmpty()) {
            Toast.makeText(this, "결제 금액을 입력하세요", Toast.LENGTH_SHORT).show()
            etPaymentAmount.requestFocus()
            return
        }
        
        if (merchantName.isEmpty()) {
            Toast.makeText(this, "가맹점명을 입력하세요", Toast.LENGTH_SHORT).show()
            etMerchantName.requestFocus()
            return
        }
        
        // 금액 유효성 검사
        val amountValue = try {
            amount.toLong()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "올바른 금액을 입력하세요", Toast.LENGTH_SHORT).show()
            etPaymentAmount.requestFocus()
            return
        }
        
        if (amountValue <= 0) {
            Toast.makeText(this, "결제 금액은 0보다 커야 합니다", Toast.LENGTH_SHORT).show()
            etPaymentAmount.requestFocus()
            return
        }
        
        Log.i(TAG, "결제 정보:")
        Log.i(TAG, "  ✓ 금액: $amount 원")
        Log.i(TAG, "  ✓ 가맹점: $merchantName")
        Log.i(TAG, "  ✓ 설명: $description")
        
        // 주문 ID 생성
        val orderId = qrGenerator.generateOrderId("NDP")
        Log.i(TAG, "  ✓ 주문ID: $orderId")
        
        // QR 코드 생성
        Thread {
            try {
                val paymentInfo = QRCodeGenerator.PaymentInfo(
                    amount = amount,
                    merchantId = "NDP_001",
                    merchantName = merchantName,
                    orderId = orderId,
                    description = description
                )
                
                val qrString = qrGenerator.createPaymentQRString(paymentInfo)
                val qrBitmap = qrGenerator.generatePaymentQRBitmap(paymentInfo, 400, 400)
                
                Log.i(TAG, "QR 코드 생성 완료")
                Log.i(TAG, "  ✓ 데이터 길이: ${qrString.length}")
                Log.i(TAG, "  ✓ 비트맵 크기: 400x400")
                
                runOnUiThread {
                    if (qrBitmap != null) {
                        // QR 코드 표시
                        ivGeneratedQr.setImageBitmap(qrBitmap)
                        tvQrInfo.text = "결제 금액: ${amount}원\n가맹점: ${merchantName}\n주문ID: ${orderId}"
                        
                        // UI 표시
                        tvQrTitle.visibility = View.VISIBLE
                        layoutQrDisplay.visibility = View.VISIBLE
                        
                        // 생성 정보 저장
                        generatedQrBitmap = qrBitmap
                        lastGeneratedQrString = qrString
                        
                        Toast.makeText(this, "✓ QR 코드 생성 완료!", Toast.LENGTH_SHORT).show()
                        Log.i(TAG, "QR 코드 UI 업데이트 완료")
                        
                    } else {
                        Toast.makeText(this, "✗ QR 코드 생성 실패", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "QR 코드 비트맵 생성 실패")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "QR 코드 생성 중 오류: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "QR 코드 생성 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    /**
     * 생성된 QR 코드 인쇄
     */
    private fun printGeneratedQR() {
        if (lastGeneratedQrString.isEmpty()) {
            Toast.makeText(this, "인쇄할 QR 코드가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "생성된 QR 코드 인쇄 시작")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        performPrint(lastGeneratedQrString, "생성된 QR 결제 코드")
    }
    
    /**
     * 카메라 권한 확인
     */
    private fun checkCameraPermission() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
        } else {
            Log.d(TAG, "카메라 권한 이미 승인됨")
        }
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "카메라 권한 승인됨")
                    Toast.makeText(this, "카메라 권한이 승인되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "카메라 권한 거부됨")
                    Toast.makeText(this, "QR 스캔을 위해 카메라 권한이 필요합니다", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * QR 스캔 시작
     */
    private fun startQrScan() {
        Log.d(TAG, "QR 스캔 시작")
        
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("QR 코드를 스캔하세요")
            setCameraId(0) // 후면 카메라 사용
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
        }
        
        qrScanLauncher.launch(options)
    }
    
    /**
     * QR 스캔 결과 처리
     */
    private fun handleQrScanResult(result: ScanIntentResult) {
        if (result.contents != null) {
            val scannedContent = result.contents
            Log.i(TAG, "QR 스캔 성공: $scannedContent")
            
            qrContent = scannedContent
            etQrContent.setText(qrContent)
            tvLastScan.text = "스캔 결과: $qrContent"
            btnPrint.isEnabled = true
            
            Toast.makeText(this, "QR 스캔 완료!", Toast.LENGTH_SHORT).show()
            
            // 자동으로 인쇄 여부 확인
            showPrintConfirmDialog(qrContent)
            
        } else {
            Log.w(TAG, "QR 스캔 취소됨")
            Toast.makeText(this, "QR 스캔이 취소되었습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 수동 입력 처리
     */
    private fun handleManualInput(content: String) {
        Log.i(TAG, "수동 입력: $content")
        
        qrContent = content
        tvLastScan.text = "수동 입력: $content"
        btnPrint.isEnabled = true
        
        Toast.makeText(this, "내용이 입력되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 인쇄 확인 대화상자
     */
    private fun showPrintConfirmDialog(content: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("인쇄 확인")
            .setMessage("다음 내용을 인쇄하시겠습니까?\n\n$content")
            .setPositiveButton("인쇄") { _, _ ->
                performPrint(content)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    /**
     * 인쇄 실행
     */
    private fun performPrint(content: String, printType: String = "스캔/입력 내용") {
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "$printType 인쇄 시작")
        Log.i(TAG, "내용: $content")
        Log.i(TAG, "포트: $printerPort")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // UI 업데이트
        tvStatus.text = "인쇄 중...\n내용: ${content.take(30)}${if(content.length > 30) "..." else ""}"
        
        // 해당 버튼 비활성화
        if (printType.contains("생성된")) {
            btnPrintGeneratedQr.isEnabled = false
        } else {
            btnPrint.isEnabled = false
        }
        
        // 백그라운드에서 인쇄 실행
        Thread {
            try {
                val helper = PrinterHelper()
                val printData = helper.createQrContentPrintData(content)
                
                Log.d(TAG, "인쇄 데이터 생성 완료: ${printData.size} bytes")
                
                // 프린터로 데이터 전송
                printer.setBuffer(printData)
                
                val startTime = System.currentTimeMillis()
                printer.print()
                val endTime = System.currentTimeMillis()
                
                Log.i(TAG, "인쇄 완료!")
                Log.i(TAG, "  ✓ 전송 시간: ${endTime - startTime}ms")
                Log.i(TAG, "  ✓ 전송 바이트: ${printData.size}")
                
                // 프린터 처리 대기
                Thread.sleep(2000)
                
                runOnUiThread {
                    tvStatus.text = "인쇄 완료!\n시간: ${getCurrentTime()}\n전송: ${printData.size} bytes"
                    
                    // 버튼 재활성화
                    if (printType.contains("생성된")) {
                        btnPrintGeneratedQr.isEnabled = true
                    } else {
                        btnPrint.isEnabled = true
                    }
                    
                    Toast.makeText(this, "✓ $printType 인쇄 완료!", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "인쇄 실패: ${e.message}", e)
                
                runOnUiThread {
                    tvStatus.text = "인쇄 실패\n오류: ${e.message}"
                    
                    // 버튼 재활성화
                    if (printType.contains("생성된")) {
                        btnPrintGeneratedQr.isEnabled = true
                    } else {
                        btnPrint.isEnabled = true
                    }
                    
                    Toast.makeText(this, "✗ 인쇄 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun getCurrentTime(): String {
        val dateFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "액티비티 재개됨")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "NDP QR 생성기 앱 종료")
    }
}
