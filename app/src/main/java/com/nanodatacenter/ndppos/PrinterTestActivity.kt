package com.nanodatacenter.ndppos

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elixirpay.elixirpaycat.SerialPrinter
import kotlinx.coroutines.*
import java.nio.charset.Charset

/**
 * 프린터 인코딩 테스트를 위한 독립 액티비티
 */
class PrinterTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PrinterTestActivity"
        private const val PRINTER_PORT = "/dev/ttyS4"
        private const val BAUD_RATE = 115200
    }
    
    private lateinit var etTestText: EditText
    private lateinit var rgCodePage: RadioGroup
    private lateinit var rgEncoding: RadioGroup
    private lateinit var btnPrint: Button
    private lateinit var tvStatus: TextView
    
    private val printer: SerialPrinter by lazy {
        SerialPrinter.Builder()
            .tty(PRINTER_PORT)
            .baudRate(BAUD_RATE)
            .build()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_test)
        
        initializeViews()
        setupListeners()
    }
    
    private fun initializeViews() {
        etTestText = findViewById(R.id.et_test_text)
        rgCodePage = findViewById(R.id.rg_code_page)
        rgEncoding = findViewById(R.id.rg_encoding)
        btnPrint = findViewById(R.id.btn_print)
        tvStatus = findViewById(R.id.tv_status)
        
        // 기본 테스트 텍스트 설정
        etTestText.setText("""
한글 테스트
가나다라마바사
아자차카타파하

상품명          가격
김치찌개        8,000원
된장찌개        7,500원

합계: 15,500원
        """.trimIndent())
    }
    
    private fun setupListeners() {
        btnPrint.setOnClickListener {
            printTest()
        }
    }
    
    private fun printTest() {
        val text = etTestText.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "텍스트를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 선택된 코드페이지 가져오기
        val codePage = when (rgCodePage.checkedRadioButtonId) {
            R.id.rb_cp_25 -> 0x25.toByte()  // CP949/KS5601
            R.id.rb_cp_12 -> 0x12.toByte()  // Korean (0x12)
            R.id.rb_cp_0d -> 0x0D.toByte()  // Korean (0x0D)
            R.id.rb_cp_ff -> 0xFF.toByte()  // User Defined
            else -> 0x25.toByte()
        }
        
        // 선택된 인코딩 가져오기
        val encoding = when (rgEncoding.checkedRadioButtonId) {
            R.id.rb_enc_euckr -> "EUC-KR"
            R.id.rb_enc_cp949 -> "CP949"
            R.id.rb_enc_ms949 -> "MS949"
            R.id.rb_enc_utf8 -> "UTF-8"
            else -> "EUC-KR"
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "인쇄 중..."
                    btnPrint.isEnabled = false
                }
                
                // 인쇄 실행
                printWithSettings(text, codePage, encoding)
                
                withContext(Dispatchers.Main) {
                    tvStatus.text = "인쇄 완료\n코드페이지: 0x${String.format("%02X", codePage)}\n인코딩: $encoding"
                    btnPrint.isEnabled = true
                }
                
                Log.i(TAG, "인쇄 성공 - CP: 0x${String.format("%02X", codePage)}, Enc: $encoding")
                
            } catch (e: Exception) {
                Log.e(TAG, "인쇄 실패: ${e.message}")
                
                withContext(Dispatchers.Main) {
                    tvStatus.text = "인쇄 실패: ${e.message}"
                    btnPrint.isEnabled = true
                }
            }
        }
    }
    
    private fun printWithSettings(text: String, codePage: Byte, encoding: String) {
        val commands = mutableListOf<Byte>()
        
        // 헤더 추가
        val header = """
========================================
코드페이지: 0x${String.format("%02X", codePage)}
인코딩: $encoding
========================================

""".trimIndent()
        
        val fullText = header + text + "\n\n\n"
        
        // 프린터 초기화
        commands.addAll(byteArrayOf(0x1B, 0x40).toList())
        
        // 코드페이지 설정
        commands.addAll(byteArrayOf(0x1B, 0x74, codePage).toList())
        
        // 텍스트 인코딩
        val textBytes = when(encoding) {
            "EUC-KR" -> fullText.toByteArray(Charset.forName("EUC-KR"))
            "CP949" -> fullText.toByteArray(Charset.forName("CP949"))
            "MS949" -> fullText.toByteArray(Charset.forName("MS949"))
            "UTF-8" -> fullText.toByteArray(Charsets.UTF_8)
            else -> fullText.toByteArray(Charset.forName(encoding))
        }
        
        commands.addAll(textBytes.toList())
        
        // 용지 자르기
        commands.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())
        
        // 프린터로 전송
        printer.setBuffer(commands.toByteArray())
        printer.print()
        
        Log.d(TAG, "전송 완료: ${commands.size} bytes")
    }
}
