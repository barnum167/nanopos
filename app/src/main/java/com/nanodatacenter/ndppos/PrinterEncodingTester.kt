package com.nanodatacenter.ndppos

import android.util.Log
import com.elixirpay.elixirpaycat.SerialPrinter
import java.nio.charset.Charset

/**
 * 프린터 인코딩 문제 해결을 위한 테스터
 * 다양한 코드페이지와 인코딩 조합을 테스트합니다.
 */
class PrinterEncodingTester {
    
    companion object {
        private const val TAG = "PrinterEncodingTester"
        private const val PRINTER_PORT = "/dev/ttyS4"
        private const val BAUD_RATE = 115200
    }
    
    private val printer: SerialPrinter by lazy {
        SerialPrinter.Builder()
            .tty(PRINTER_PORT)
            .baudRate(BAUD_RATE)
            .build()
    }
    
    /**
     * 모든 코드페이지 테스트
     */
    fun testAllCodePages() {
        Log.i(TAG, "=====================================")
        Log.i(TAG, "코드페이지 테스트 시작")
        Log.i(TAG, "=====================================")
        
        val testText = """
한글 테스트 (Korean Test)
가나다라마바사
아자차카타파하
ABC 123 !@#
=====================================
상품명          수량    금액
김치찌개         1    8,000원
된장찌개         1    7,500원
공기밥           2    2,000원
=====================================
합계                 17,500원

감사합니다!
"""
        
        // 테스트할 코드페이지들
        val codePages = mapOf(
            "PC437 (USA)" to 0x00.toByte(),
            "PC850 (Multilingual)" to 0x02.toByte(),
            "PC852 (Latin 2)" to 0x12.toByte(),
            "PC858 (Euro)" to 0x13.toByte(),
            "PC860 (Portuguese)" to 0x03.toByte(),
            "PC863 (Canadian-French)" to 0x04.toByte(),
            "PC865 (Nordic)" to 0x05.toByte(),
            "PC866 (Cyrillic #2)" to 0x11.toByte(),
            "PC1252 (Latin 1)" to 0x10.toByte(),
            "WPC1250 (Central Europe)" to 0x2D.toByte(),
            "WPC1251 (Cyrillic)" to 0x2E.toByte(),
            "WPC1253 (Greek)" to 0x2F.toByte(),
            "WPC1254 (Turkish)" to 0x30.toByte(),
            "WPC1255 (Hebrew)" to 0x31.toByte(),
            "WPC1256 (Arabic)" to 0x32.toByte(),
            "WPC1257 (Baltic)" to 0x33.toByte(),
            "WPC1258 (Vietnam)" to 0x34.toByte(),
            "ISO8859-2 (Latin 2)" to 0x06.toByte(),
            "ISO8859-7 (Greek)" to 0x07.toByte(),
            "ISO8859-15 (Latin 9)" to 0x0D.toByte(),
            "Thai Code 11" to 0x14.toByte(),
            "Thai Code 18" to 0x1A.toByte(),
            "TCVN-3 (Vietnamese)" to 0x1E.toByte(),
            "PC720 (Arabic)" to 0x29.toByte(),
            "PC775 (Baltic)" to 0x2A.toByte(),
            "PC855 (Cyrillic)" to 0x2B.toByte(),
            "PC857 (Turkish)" to 0x2C.toByte(),
            "PC862 (Hebrew)" to 0x0F.toByte(),
            "PC864 (Arabic)" to 0x1C.toByte(),
            "PC869 (Greek)" to 0x1D.toByte(),
            "ISO8859-1 (Latin 1)" to 0x01.toByte(),
            "ISO8859-3 (Latin 3)" to 0x04.toByte(),
            "ISO8859-4 (Baltic)" to 0x05.toByte(),
            "ISO8859-5 (Cyrillic)" to 0x08.toByte(),
            "ISO8859-6 (Arabic)" to 0x09.toByte(),
            "ISO8859-8 (Hebrew)" to 0x0A.toByte(),
            "ISO8859-9 (Turkish)" to 0x0B.toByte(),
            "KS X 1001 (Korean)" to 0x25.toByte(),
            "BIG5 (Traditional Chinese)" to 0xFF.toByte(),
            "GB18030 (Simplified Chinese)" to 0xFF.toByte()
        )
        
        // 인코딩 목록
        val encodings = listOf("EUC-KR", "CP949", "MS949", "UTF-8")
        
        for ((cpName, cpCode) in codePages) {
            for (encoding in encodings) {
                try {
                    printWithCodePageAndEncoding(cpName, cpCode, encoding, testText)
                    Thread.sleep(500) // 각 테스트 사이 잠시 대기
                } catch (e: Exception) {
                    Log.e(TAG, "테스트 실패 - CP: $cpName, Encoding: $encoding - ${e.message}")
                }
            }
        }
        
        Log.i(TAG, "=====================================")
        Log.i(TAG, "모든 코드페이지 테스트 완료")
        Log.i(TAG, "=====================================")
    }
    
    /**
     * 한국어에 특화된 코드페이지만 테스트
     */
    fun testKoreanCodePages() {
        Log.i(TAG, "=====================================")
        Log.i(TAG, "한국어 코드페이지 테스트 시작")
        Log.i(TAG, "=====================================")
        
        val testText = """
[한글 인쇄 테스트]

가나다라마바사
아자차카타파하
한글 English 123

상품명          가격
김치찌개        8,000원
된장찌개        7,500원

합계: 15,500원
"""
        
        // 한국어 관련 코드페이지들만 테스트
        val koreanCodePages = mapOf(
            "CP949/KS5601 (0x25)" to 0x25.toByte(),
            "Korean (0x0D)" to 0x0D.toByte(),
            "Korean (0x12)" to 0x12.toByte(),
            "Korean (0x15)" to 0x15.toByte(),
            "Korean (0x20)" to 0x20.toByte(),
            "User Defined (0xFF)" to 0xFF.toByte()
        )
        
        val encodings = listOf("EUC-KR", "CP949", "MS949")
        
        for ((cpName, cpCode) in koreanCodePages) {
            for (encoding in encodings) {
                try {
                    Log.d(TAG, "테스트: $cpName with $encoding")
                    printWithCodePageAndEncoding(cpName, cpCode, encoding, testText)
                    Thread.sleep(1000) // 각 테스트 사이 1초 대기
                } catch (e: Exception) {
                    Log.e(TAG, "테스트 실패 - CP: $cpName, Encoding: $encoding - ${e.message}")
                }
            }
        }
    }
    
    /**
     * 특정 코드페이지와 인코딩으로 출력
     */
    private fun printWithCodePageAndEncoding(
        codepageName: String,
        codepageValue: Byte,
        encoding: String,
        text: String
    ) {
        Log.d(TAG, "출력 시도: CP=$codepageName(0x${String.format("%02X", codepageValue)}), Encoding=$encoding")
        
        val header = """
=====================================
코드페이지: $codepageName
값: 0x${String.format("%02X", codepageValue)}
인코딩: $encoding
=====================================

"""
        
        val fullText = header + text
        
        // 인코딩에 따라 바이트 변환
        val textBytes = when(encoding) {
            "EUC-KR" -> fullText.toByteArray(Charset.forName("EUC-KR"))
            "CP949" -> fullText.toByteArray(Charset.forName("CP949"))
            "MS949" -> fullText.toByteArray(Charset.forName("MS949"))
            "UTF-8" -> fullText.toByteArray(Charsets.UTF_8)
            else -> fullText.toByteArray(Charset.forName(encoding))
        }
        
        val commands = mutableListOf<Byte>()
        
        // 프린터 초기화
        commands.addAll(byteArrayOf(0x1B, 0x40).toList())
        
        // 코드페이지 설정
        commands.addAll(byteArrayOf(0x1B, 0x74, codepageValue).toList())
        
        // 텍스트 추가
        commands.addAll(textBytes.toList())
        
        // 줄바꿈
        commands.addAll(byteArrayOf(0x0A, 0x0A, 0x0A).toList())
        
        // 용지 자르기
        commands.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())
        
        // 프린터로 전송
        printer.setBuffer(commands.toByteArray())
        printer.print()
        
        Log.d(TAG, "출력 완료: ${commands.size} bytes")
    }
    
    /**
     * ESC/POS 직접 명령어 테스트
     */
    fun testDirectCommands() {
        Log.i(TAG, "직접 명령어 테스트 시작")
        
        val testText = "한글 테스트 가나다라"
        
        // 방법 1: 국제 문자셋 설정
        testWithInternationalCharset(testText)
        
        // 방법 2: 유니코드 모드
        testWithUnicodeMode(testText)
        
        // 방법 3: 2바이트 문자 모드
        testWith2ByteMode(testText)
    }
    
    private fun testWithInternationalCharset(text: String) {
        Log.d(TAG, "국제 문자셋 모드 테스트")
        
        val commands = mutableListOf<Byte>()
        
        // 초기화
        commands.addAll(byteArrayOf(0x1B, 0x40).toList())
        
        // 국제 문자셋 설정 (한국)
        commands.addAll(byteArrayOf(0x1B, 0x52, 0x0D).toList()) // ESC R n (n=13 for Korea)
        
        // 문자 코드 테이블 설정
        commands.addAll(byteArrayOf(0x1B, 0x74, 0x25).toList()) // ESC t n
        
        // 텍스트
        commands.addAll(text.toByteArray(Charset.forName("EUC-KR")).toList())
        commands.addAll(byteArrayOf(0x0A, 0x0A).toList())
        
        // 용지 자르기
        commands.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())
        
        printer.setBuffer(commands.toByteArray())
        printer.print()
    }
    
    private fun testWithUnicodeMode(text: String) {
        Log.d(TAG, "유니코드 모드 테스트")
        
        val commands = mutableListOf<Byte>()
        
        // 초기화
        commands.addAll(byteArrayOf(0x1B, 0x40).toList())
        
        // UTF-8 인코딩 설정 (일부 프린터 지원)
        commands.addAll(byteArrayOf(0x1C, 0x2E).toList()) // FS .
        
        // 텍스트 (UTF-8)
        commands.addAll(text.toByteArray(Charsets.UTF_8).toList())
        commands.addAll(byteArrayOf(0x0A, 0x0A).toList())
        
        // 용지 자르기
        commands.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())
        
        printer.setBuffer(commands.toByteArray())
        printer.print()
    }
    
    private fun testWith2ByteMode(text: String) {
        Log.d(TAG, "2바이트 문자 모드 테스트")
        
        val commands = mutableListOf<Byte>()
        
        // 초기화
        commands.addAll(byteArrayOf(0x1B, 0x40).toList())
        
        // 2바이트 문자 모드 시작
        commands.addAll(byteArrayOf(0x1C, 0x26).toList()) // FS &
        
        // 2바이트 문자 코드 시스템 선택 (한국어)
        commands.addAll(byteArrayOf(0x1C, 0x43, 0x01).toList()) // FS C n (n=1 for Korean)
        
        // 텍스트
        commands.addAll(text.toByteArray(Charset.forName("EUC-KR")).toList())
        
        // 2바이트 문자 모드 종료
        commands.addAll(byteArrayOf(0x1C, 0x2E).toList()) // FS .
        
        commands.addAll(byteArrayOf(0x0A, 0x0A).toList())
        
        // 용지 자르기
        commands.addAll(byteArrayOf(0x1D, 0x56, 0x42, 0x00).toList())
        
        printer.setBuffer(commands.toByteArray())
        printer.print()
    }
    
    /**
     * 가장 간단한 테스트 (초기화 없이)
     */
    fun testSimplePrint() {
        Log.i(TAG, "간단한 인쇄 테스트")
        
        val text = "Korean Test 한글 테스트\n가나다라마바사\n\n\n"
        
        // 단순히 EUC-KR로 변환해서 전송
        val bytes = text.toByteArray(Charset.forName("EUC-KR"))
        printer.setBuffer(bytes)
        printer.print()
        
        Log.d(TAG, "간단한 테스트 완료: ${bytes.size} bytes")
    }
}
