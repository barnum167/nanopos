package com.nanodatacenter.ndppos

import android.util.Log
import com.elixirpay.elixirpaycat.helper.EpsonPrinterHelper
import com.elixirpay.elixirpaycat.helper.command.Align

/**
 * 제조사에서 제공한 EpsonPrinterHelper를 코틀린으로 감싸는 헬퍼 클래스
 * 제조사 파일은 수정하지 않고 이 클래스에서 편의 기능 제공
 */
class PrinterHelper {
    
    companion object {
        private const val TAG = "NDP_PRINTER_HELPER"
    }
    
    private val epsonHelper = EpsonPrinterHelper.getInstance().setPageWidth(42)
    
    /**
     * 테스트 프린트용 데이터 생성
     */
    fun createTestPrintData(): ByteArray {
        Log.d(TAG, "═══════════════ 테스트 데이터 생성 시작 ═══════════════")
        
        val commands = mutableListOf<Byte>()
        var commandCount = 0
        
        // 초기화 및 언어 설정
        Log.d(TAG, "명령어 ${++commandCount}: 초기화 및 언어 설정")
        val initCommands = getInitCommands()
        commands.addAll(initCommands)
        logHexData("초기화 명령", initCommands)
        
        // 헤더
        Log.d(TAG, "명령어 ${++commandCount}: 헤더 생성")
        val headerCommands = createHeader("🖨️ 프린터 테스트 🖨️")
        commands.addAll(headerCommands)
        logHexData("헤더 명령", headerCommands)
        
        // 테스트 내용
        Log.d(TAG, "명령어 ${++commandCount}: 테스트 내용 생성")
        val contentCommands = createTestContent()
        commands.addAll(contentCommands)
        logHexData("내용 명령", contentCommands.take(50)) // 처음 50바이트만
        
        // 푸터
        Log.d(TAG, "명령어 ${++commandCount}: 푸터 생성")
        val footerCommands = createFooter("✅ 테스트 완료 ✅")
        commands.addAll(footerCommands)
        logHexData("푸터 명령", footerCommands)
        
        // 용지 자르기
        Log.d(TAG, "명령어 ${++commandCount}: 용지 자르기")
        val cutCommands = getPaperCutCommand()
        commands.addAll(cutCommands)
        logHexData("용지 자르기 명령", cutCommands)
        
        val result = commands.toByteArray()
        
        Log.i(TAG, "═══════════════ 데이터 생성 완료 ═══════════════")
        Log.i(TAG, "총 명령어 수: $commandCount")
        Log.i(TAG, "총 데이터 크기: ${result.size} bytes")
        Log.i(TAG, "예상 출력 라인 수: ~15라인")
        
        // 전체 데이터의 체크섬 계산
        val checksum = result.fold(0) { acc, byte -> acc + byte.toInt() and 0xFF } and 0xFF
        Log.d(TAG, "데이터 체크섬: 0x${String.format("%02X", checksum)}")
        
        // 최종 데이터 미리보기 (처음과 끝 각 20바이트)
        if (result.size > 40) {
            val startHex = result.take(20).joinToString(" ") { String.format("%02X", it) }
            val endHex = result.takeLast(20).joinToString(" ") { String.format("%02X", it) }
            Log.d(TAG, "시작 데이터: $startHex")
            Log.d(TAG, "종료 데이터: $endHex")
        }
        
        Log.d(TAG, "═══════════════════════════════════════════════════")
        
        return result
    }
    
    /**
     * 간단한 텍스트 프린트용 데이터 생성
     */
    fun createSimpleTextData(text: String): ByteArray {
        Log.d(TAG, "간단한 텍스트 데이터 생성: '$text'")
        
        val commands = mutableListOf<Byte>()
        
        commands.addAll(getInitCommands())
        commands.addAll(convertStringToBytes(text))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        commands.addAll(getPaperCutCommand())
        
        val result = commands.toByteArray()
        Log.i(TAG, "간단한 텍스트 데이터 생성 완료 - ${result.size} bytes")
        
        return result
    }
    
    /**
     * 영수증 형태의 프린트 데이터 생성
     */
    fun createReceiptData(
        title: String,
        items: List<Pair<String, String>>, // 상품명, 가격
        totalAmount: String
    ): ByteArray {
        Log.d(TAG, "영수증 데이터 생성 시작")
        Log.d(TAG, "제목: '$title', 상품 수: ${items.size}, 총액: '$totalAmount'")
        
        val commands = mutableListOf<Byte>()
        
        commands.addAll(getInitCommands())
        commands.addAll(createHeader(title))
        
        // 구분선
        commands.addAll(createSeparatorLine())
        
        // 상품 목록
        Log.d(TAG, "상품 목록 추가:")
        for ((index, item) in items.withIndex()) {
            Log.d(TAG, "  ${index + 1}. ${item.first} - ${item.second}")
            commands.addAll(createItemLine(item.first, item.second))
        }
        
        // 구분선
        commands.addAll(createSeparatorLine())
        
        // 합계
        Log.d(TAG, "합계 추가: $totalAmount")
        commands.addAll(createItemLine("합계", totalAmount))
        commands.addAll(getLineFeed())
        
        commands.addAll(createFooter("감사합니다!"))
        commands.addAll(getPaperCutCommand())
        
        val result = commands.toByteArray()
        Log.i(TAG, "영수증 데이터 생성 완료 - ${result.size} bytes")
        
        return result
    }
    
    /**
     * 헥사 데이터 로깅 헬퍼
     */
    private fun logHexData(description: String, data: List<Byte>) {
        if (data.isNotEmpty()) {
            val hex = data.joinToString(" ") { String.format("%02X", it) }
            Log.d(TAG, "  $description (${data.size} bytes): $hex")
            
            // ASCII 표현도 출력 (출력 가능한 문자만)
            val ascii = data.map { byte ->
                val char = byte.toInt() and 0xFF
                if (char in 32..126) char.toChar() else '.'
            }.joinToString("")
            if (ascii.isNotBlank()) {
                Log.d(TAG, "  ASCII: '$ascii'")
            }
        }
    }
    
    // === Private 메서드들 ===
    
    private fun getInitCommands(): List<Byte> {
        Log.d(TAG, "초기화 명령어 생성")
        return listOf(0x1B.toByte(), 0x40.toByte()) + // ESC @ (초기화)
               listOf(0x1B.toByte(), 0x59.toByte(), 0x48.toByte(), 0x43.toByte(), 0x01.toByte()) // 언어 설정
    }
    
    private fun createHeader(title: String): List<Byte> {
        Log.d(TAG, "헤더 생성: '$title'")
        val commands = mutableListOf<Byte>()
        
        // 가운데 정렬
        commands.addAll(getAlignCenter())
        // 굵게, 큰 글씨
        commands.addAll(getBoldLargeFont())
        // 제목
        commands.addAll(convertStringToBytes(title))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        // 폰트 리셋
        commands.addAll(getNormalFont())
        commands.addAll(getAlignLeft()) // 왼쪽 정렬로 복원
        
        return commands
    }
    
    private fun createFooter(message: String): List<Byte> {
        Log.d(TAG, "푸터 생성: '$message'")
        val commands = mutableListOf<Byte>()
        
        commands.addAll(getLineFeed())
        commands.addAll(createSeparatorLine())
        commands.addAll(getAlignCenter())
        commands.addAll(convertStringToBytes(message))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        return commands
    }
    
    private fun createTestContent(): List<Byte> {
        Log.d(TAG, "테스트 내용 생성")
        val commands = mutableListOf<Byte>()
        
        // 구분선
        commands.addAll(createSeparatorLine())
        
        val testLines = listOf(
            "상품명                    수량    가격",
            "테스트 상품1                1   10,000원",
            "테스트 상품2                2    5,000원", 
            "아메리카노                  3    4,500원",
            "카페라떼                    1    5,500원"
        )
        
        Log.d(TAG, "테스트 내용 라인 수: ${testLines.size}")
        for ((index, line) in testLines.withIndex()) {
            Log.d(TAG, "  라인 ${index + 1}: '$line'")
            commands.addAll(convertStringToBytes(line))
            commands.addAll(getLineFeed())
        }
        
        commands.addAll(createSeparatorLine())
        
        // 합계 라인
        commands.addAll(createItemLine("합계", "25,000원"))
        commands.addAll(createItemLine("부가세", "2,500원"))
        commands.addAll(createItemLine("총 결제금액", "27,500원"))
        
        return commands
    }
    
    private fun createSeparatorLine(): List<Byte> {
        val line = "━".repeat(42) // 42자 길이의 구분선 (유니코드)
        Log.d(TAG, "구분선 생성: 길이 ${line.length}")
        return convertStringToBytes(line) + getLineFeed()
    }
    
    private fun createItemLine(name: String, price: String): List<Byte> {
        val maxWidth = 42
        val priceLength = getDisplayLength(price)
        val nameMaxLength = maxWidth - priceLength - 1
        
        val trimmedName = if (getDisplayLength(name) > nameMaxLength) {
            truncateToDisplayLength(name, nameMaxLength)
        } else {
            name
        }
        
        val spaces = " ".repeat(maxWidth - getDisplayLength(trimmedName) - priceLength)
        val line = trimmedName + spaces + price
        
        Log.d(TAG, "상품 라인 생성: '$line' (길이: ${getDisplayLength(line)})")
        return convertStringToBytes(line) + getLineFeed()
    }
    
    /**
     * 한글을 고려한 표시 길이 계산
     */
    private fun getDisplayLength(text: String): Int {
        return text.fold(0) { acc, char ->
            acc + if (char.code > 127) 2 else 1 // 한글은 2, 영문은 1
        }
    }
    
    /**
     * 표시 길이에 맞춰 문자열 자르기
     */
    private fun truncateToDisplayLength(text: String, maxLength: Int): String {
        var length = 0
        val result = StringBuilder()
        
        for (char in text) {
            val charLength = if (char.code > 127) 2 else 1
            if (length + charLength > maxLength) break
            result.append(char)
            length += charLength
        }
        
        return result.toString()
    }
    
    private fun convertStringToBytes(text: String): List<Byte> {
        return try {
            val utf8Bytes = text.toByteArray(Charsets.UTF_8)
            Log.d(TAG, "문자열 변환: '$text' -> ${utf8Bytes.size} bytes (UTF-8)")
            utf8Bytes.toList()
        } catch (e: Exception) {
            Log.w(TAG, "UTF-8 변환 실패, 기본 인코딩 사용: ${e.message}")
            text.toByteArray().toList()
        }
    }
    
    private fun getLineFeed(): List<Byte> = listOf(0x0A.toByte()) // LF
    
    private fun getAlignLeft(): List<Byte> {
        Log.d(TAG, "왼쪽 정렬 명령 (ESC a 0)")
        return listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte())
    }
    
    private fun getAlignCenter(): List<Byte> {
        Log.d(TAG, "가운데 정렬 명령 (ESC a 1)")
        return listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())
    }
    
    private fun getAlignRight(): List<Byte> {
        Log.d(TAG, "오른쪽 정렬 명령 (ESC a 2)")
        return listOf(0x1B.toByte(), 0x61.toByte(), 0x02.toByte())
    }
    
    private fun getNormalFont(): List<Byte> {
        Log.d(TAG, "일반 폰트 명령 (ESC ! 0)")
        return listOf(0x1B.toByte(), 0x21.toByte(), 0x00.toByte())
    }
    
    private fun getBoldLargeFont(): List<Byte> {
        Log.d(TAG, "굵고 큰 폰트 명령 (ESC ! 56)")
        return listOf(0x1B.toByte(), 0x21.toByte(), 0x38.toByte()) // 굵게 + 큰 글씨
    }
    
    private fun getPaperCutCommand(): List<Byte> {
        Log.d(TAG, "용지 자르기 명령 (GS V B 1)")
        return listOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x01.toByte())
    }
}
