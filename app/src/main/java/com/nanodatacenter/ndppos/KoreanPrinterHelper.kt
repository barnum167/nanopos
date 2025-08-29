package com.nanodatacenter.ndppos

import android.util.Log
import com.elixirpay.elixirpaycat.SerialPrinter
import com.elixirpay.elixirpaycat.helper.EpsonPrinterHelper
import java.io.IOException
import java.nio.charset.Charset

/**
 * 한글 인쇄를 위한 프린터 헬퍼 클래스
 * 여러 인코딩 방식을 테스트하여 한글이 제대로 출력되는 방식을 찾습니다.
 */
class KoreanPrinterHelper {
    
    companion object {
        private const val TAG = "KoreanPrinterHelper"
        private const val PRINTER_PORT = "/dev/ttyS4" // 메인과 동일한 포트
        private const val BAUD_RATE = 115200
    }
    
    private val printer: SerialPrinter by lazy {
        SerialPrinter.Builder()
            .tty(PRINTER_PORT)
            .baudRate(BAUD_RATE)
            .build()
    }
    
    /**
     * 간단한 한글 테스트 (부가 정보 없음)
     */
    fun printSimpleKoreanTest() {
        Log.d(TAG, "간단한 한글 테스트 시작")
        
        val simpleKoreanText = """한글 테스트
가나다라마바사
아자차카타파하

상품명          가격
김치찌개        8,000원
된장찌개        7,500원
공기밥          2,000원

합계: 17,500원

감사합니다!"""

        try {
            // UTF-8로 간단하게 출력
            val bytes = simpleKoreanText.toByteArray(Charsets.UTF_8)
            
            val commands = mutableListOf<Byte>()
            
            // 초기화
            commands.addAll(byteArrayOf(0x1B, 0x40).toList())
            
            // UTF-8 코드페이지 설정
            commands.addAll(byteArrayOf(0x1B, 0x59, 0x48, 0x43, 0x01).toList())
            
            // 텍스트 추가
            commands.addAll(bytes.toList())
            
            // 줄바꿈
            commands.addAll(byteArrayOf(0x0A, 0x0A).toList())
            
            // 용지 자르기
            commands.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x01).toList())
            
            printer.setBuffer(commands.toByteArray())
            printer.print()
            
            Log.d(TAG, "간단한 한글 테스트 완료")
        } catch (e: Exception) {
            Log.e(TAG, "간단한 한글 테스트 실패: ${e.message}")
            throw e
        }
    }
    
    /**
     * 다양한 인코딩으로 한글 테스트 영수증 출력 (상세 모드)
     */
    fun printKoreanTestReceipt() {
        Log.d(TAG, "한글 테스트 영수증 출력 시작")
        
        val testEncodings = listOf(
            "EUC-KR",
            "CP949",
            "UTF-8",
            "MS949",
            "KSC5601"
        )
        
        testEncodings.forEach { encoding ->
            try {
                printWithEncoding(encoding)
            } catch (e: Exception) {
                Log.e(TAG, "$encoding 인코딩 출력 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 특정 인코딩으로 영수증 출력
     */
    private fun printWithEncoding(encoding: String) {
        Log.d(TAG, "$encoding 인코딩으로 출력 시도")
        
        val receipt = buildString {
            append("\n")
            append("=====================================\n")
            append("    인코딩 테스트: $encoding\n")
            append("=====================================\n")
            append("\n")
            append("한글 테스트 영수증\n")
            append("가나다라마바사\n")
            append("아자차카타파하\n")
            append("\n")
            append("상품명              수량     금액\n")
            append("-------------------------------------\n")
            append("김치찌개             1     8,000원\n")
            append("된장찌개             1     7,500원\n")
            append("공기밥               2     2,000원\n")
            append("-------------------------------------\n")
            append("합계:                     17,500원\n")
            append("\n")
            append("감사합니다!\n")
            append("\n")
            append("\n")
            append("\n")
        }
        
        try {
            val bytes = when(encoding) {
                "UTF-8" -> receipt.toByteArray(Charsets.UTF_8)
                "EUC-KR" -> receipt.toByteArray(Charset.forName("EUC-KR"))
                "CP949" -> receipt.toByteArray(Charset.forName("CP949"))
                "MS949" -> receipt.toByteArray(Charset.forName("MS949"))
                "KSC5601" -> receipt.toByteArray(Charset.forName("KSC5601"))
                else -> receipt.toByteArray(Charset.forName(encoding))
            }
            
            // ESC/POS 명령어 추가
            val commands = mutableListOf<Byte>()
            
            // 초기화
            commands.addAll(byteArrayOf(0x1B, 0x40).toList())
            
            // 한글 코드 페이지 설정 (CP949)
            commands.addAll(byteArrayOf(0x1B, 0x74, 0x12).toList())
            
            // 텍스트 추가
            commands.addAll(bytes.toList())
            
            // 용지 자르기 (부분 컷)
            commands.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())
            
            // 프린터로 전송
            printer.setBuffer(commands.toByteArray())
            printer.print()
            
            Log.d(TAG, "$encoding 인코딩 출력 성공")
        } catch (e: IOException) {
            Log.e(TAG, "$encoding 인코딩 출력 중 IO 오류: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$encoding 인코딩 출력 중 오류: ${e.message}")
            throw e
        }
    }
}
