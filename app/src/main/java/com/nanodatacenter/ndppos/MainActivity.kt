package com.nanodatacenter.ndppos

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elixirpay.elixirpaycat.SerialPrinter
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "NDP_PRINTER"
    }
    
    private lateinit var printer: SerialPrinter
    private lateinit var btnPrint: Button
    private lateinit var tvStatus: TextView
    
    // ttyS4 포트만 사용
    private val printerPort = "/dev/ttyS4"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "NDP 프린터 앱 시작 (ttyS4 전용)")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        initializeViews()
        initializePrinter()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        btnPrint = findViewById(R.id.btn_print)
        tvStatus = findViewById(R.id.tv_status)
        
        // 프린트 버튼 활성화
        btnPrint.isEnabled = true
        
        Log.d(TAG, "UI 요소 초기화 완료")
    }
    
    private fun initializePrinter() {
        // ttyS4 포트로 프린터 초기화
        printer = SerialPrinter.Builder()
            .tty(printerPort)
            .baudRate(115200)
            .build()
            
        tvStatus.text = "프린터 준비 완료\n포트: $printerPort\n바로 프린트 가능!"
        Log.i(TAG, "프린터 초기화 완료 - 포트: $printerPort, 115200bps")
    }
    
    private fun setupClickListeners() {
        btnPrint.setOnClickListener {
            Log.d(TAG, "프린트 버튼 클릭")
            performPrint()
        }
    }
    
    /**
     * ttyS4 포트에서 프린트 실행
     */
    private fun performPrint() {
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "프린트 시작")
        Log.i(TAG, "사용 포트: $printerPort")
        Log.i(TAG, "보드율: 115200bps")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // 백그라운드에서 실행
        Thread {
            try {
                // 1. 포트 상태 확인
                Log.d(TAG, "1단계: 포트 상태 확인")
                val portFile = File(printerPort)
                Log.d(TAG, "   - 파일 존재: ${portFile.exists()}")
                Log.d(TAG, "   - 읽기 가능: ${portFile.canRead()}")
                Log.d(TAG, "   - 쓰기 가능: ${portFile.canWrite()}")
                
                if (!portFile.exists()) {
                    throw IOException("포트 파일이 존재하지 않음: $printerPort")
                }
                
                // 2. 프린트 데이터 생성
                Log.d(TAG, "2단계: 프린트 데이터 생성")
                val helper = PrinterHelper()
                val printData = helper.createTestPrintData()
                Log.i(TAG, "   - 생성된 데이터 크기: ${printData.size} bytes")
                
                // UI 업데이트
                runOnUiThread {
                    tvStatus.text = "프린트 실행 중...\n포트: $printerPort\n데이터: ${printData.size} bytes"
                }
                
                // 3. 프린터 연결 확인
                Log.d(TAG, "3단계: 프린터 연결 확인")
                Log.d(TAG, "   - 연결 상태: ${printer.isConnected}")
                
                // 4. 데이터 전송
                Log.d(TAG, "4단계: 데이터 전송 시작")
                
                // 전송 전 잠깐 대기
                Thread.sleep(100)
                
                printer.setBuffer(printData)
                Log.d(TAG, "   - 버퍼 설정 완료")
                
                // 실제 전송
                val startTime = System.currentTimeMillis()
                printer.print()
                val endTime = System.currentTimeMillis()
                
                Log.i(TAG, "5단계: 전송 완료!")
                Log.i(TAG, "   ✓ 포트: $printerPort")
                Log.i(TAG, "   ✓ 전송 시간: ${endTime - startTime}ms")
                Log.i(TAG, "   ✓ 전송 바이트: ${printData.size}")
                
                // 프린터 처리 대기
                Log.d(TAG, "6단계: 프린터 처리 대기 (2초)")
                Thread.sleep(2000)
                
                Log.i(TAG, "✓ 프린트 프로세스 완료!")
                Log.i(TAG, "  → 프린터에서 출력 확인")
                
                // UI 업데이트
                runOnUiThread {
                    val statusText = "프린트 완료!\n" +
                            "포트: $printerPort\n" +
                            "시간: ${getCurrentTime()}\n" +
                            "데이터: ${printData.size} bytes\n" +
                            "전송시간: ${endTime - startTime}ms"
                    tvStatus.text = statusText
                    
                    Toast.makeText(this, "✓ 프린트 완료!", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: IOException) {
                Log.e(TAG, "✗ 프린트 실패 (IO 오류)")
                Log.e(TAG, "  - 오류 메시지: ${e.message}")
                Log.e(TAG, "스택 트레이스:", e)
                
                runOnUiThread {
                    tvStatus.text = "프린트 실패\nIO 오류: ${e.message}"
                    Toast.makeText(this, "✗ 프린트 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "✗ 프린트 실패 (예상치 못한 오류)")
                Log.e(TAG, "  - 오류 메시지: ${e.message}")
                Log.e(TAG, "  - 오류 유형: ${e.javaClass.simpleName}")
                Log.e(TAG, "스택 트레이스:", e)
                
                runOnUiThread {
                    tvStatus.text = "예상치 못한 오류\n${e.message}"
                    Toast.makeText(this, "✗ 예상치 못한 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
        
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    private fun getCurrentTime(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "액티비티 재개됨")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "액티비티 일시정지됨")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "NDP 프린터 앱 종료")
    }
}
