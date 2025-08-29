package com.nanodatacenter.ndppos

import android.util.Log
import com.elixirpay.elixirpaycat.SerialPrinter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 자동 영수증 인쇄 관리자
 * 서버에서 받은 결제 데이터를 영수증 형태로 자동 인쇄합니다
 */
class AutoPrintManager {
    
    companion object {
        private const val TAG = "AutoPrintManager"
        private const val PRINTER_PORT = "/dev/ttyS4"
        private const val BAUD_RATE = 115200
    }
    
    private val printer: SerialPrinter by lazy {
        SerialPrinter.Builder()
            .tty(PRINTER_PORT)
            .baudRate(BAUD_RATE)
            .build()
    }
    
    private val printerHelper = PrinterHelper()
    
    /**
     * 영수증 자동 인쇄
     */
    fun printReceipt(receiptData: ReceiptData): Boolean {
        return try {
            Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.i(TAG, "자동 영수증 인쇄 시작")
            Log.i(TAG, "인쇄 ID: ${receiptData.printId}")
            Log.i(TAG, "거래 해시: ${receiptData.transactionHash}")
            Log.i(TAG, "금액: ${receiptData.amount} ${receiptData.token}")
            Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            // 영수증 데이터 생성
            val printData = createReceiptPrintData(receiptData)
            
            // 프린터로 전송
            printer.setBuffer(printData)
            printer.print()
            
            // 인쇄 완료 대기
            Thread.sleep(3000)
            
            Log.i(TAG, "✅ 자동 영수증 인쇄 완료 - ID: ${receiptData.printId}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 자동 영수증 인쇄 실패 - ID: ${receiptData.printId}, 오류: ${e.message}")
            false
        }
    }
    
    /**
     * 영수증 인쇄 데이터 생성
     */
    private fun createReceiptPrintData(receiptData: ReceiptData): ByteArray {
        Log.d(TAG, "영수증 인쇄 데이터 생성 시작")
        
        val commands = mutableListOf<Byte>()
        
        // 초기화
        commands.addAll(getInitCommands())
        
        // 헤더
        commands.addAll(createReceiptHeader())
        
        // 거래 정보
        commands.addAll(createTransactionInfo(receiptData))
        
        // 주소 정보
        commands.addAll(createAddressInfo(receiptData))
        
        // 타임스탬프
        commands.addAll(createTimestampInfo(receiptData))
        
        // 푸터
        commands.addAll(createReceiptFooter())
        
        // 용지 자르기
        commands.addAll(getPaperCutCommand())
        
        val result = commands.toByteArray()
        Log.d(TAG, "영수증 데이터 생성 완료: ${result.size} bytes")
        
        return result
    }
    
    /**
     * 영수증 헤더 생성
     */
    private fun createReceiptHeader(): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 가운데 정렬
        commands.addAll(getAlignCenter())
        
        // 굵게, 큰 글씨
        commands.addAll(getBoldLargeFont())
        
        // 제목
        commands.addAll(convertStringToBytes("🧾 결제 영수증 🧾"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // 폰트 리셋
        commands.addAll(getNormalFont())
        commands.addAll(getAlignLeft())
        
        // 구분선
        commands.addAll(createSeparatorLine())
        
        return commands
    }
    
    /**
     * 거래 정보 섹션 생성
     */
    private fun createTransactionInfo(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 거래 정보 헤더
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("📋 거래 정보"))
        commands.addAll(getNormalFont())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // 거래 해시
        commands.addAll(createInfoLine("거래 해시", shortenHash(receiptData.transactionHash)))
        commands.addAll(getLineFeed())
        
        // 결제 금액
        commands.addAll(createInfoLine("결제 금액", "${receiptData.amount} ${receiptData.token}"))
        commands.addAll(getLineFeed())
        
        // 토큰 정보
        commands.addAll(createInfoLine("토큰", receiptData.token))
        commands.addAll(getLineFeed())
        
        commands.addAll(createSeparatorLine())
        
        return commands
    }
    
    /**
     * 주소 정보 섹션 생성
     */
    private fun createAddressInfo(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 주소 정보 헤더
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("📍 주소 정보"))
        commands.addAll(getNormalFont())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // 보내는 주소
        commands.addAll(createInfoLine("보내는 주소", shortenAddress(receiptData.fromAddress)))
        commands.addAll(getLineFeed())
        
        // 받는 주소
        commands.addAll(createInfoLine("받는 주소", shortenAddress(receiptData.toAddress)))
        commands.addAll(getLineFeed())
        
        commands.addAll(createSeparatorLine())
        
        return commands
    }
    
    /**
     * 타임스탬프 정보 섹션 생성
     */
    private fun createTimestampInfo(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 시간 정보 헤더
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("⏰ 처리 시간"))
        commands.addAll(getNormalFont())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // 거래 시간
        val formattedTime = formatTimestamp(receiptData.timestamp)
        commands.addAll(createInfoLine("거래 시간", formattedTime))
        commands.addAll(getLineFeed())
        
        // 인쇄 시간
        val printTime = getCurrentTimeString()
        commands.addAll(createInfoLine("인쇄 시간", printTime))
        commands.addAll(getLineFeed())
        
        commands.addAll(createSeparatorLine())
        
        return commands
    }
    
    /**
     * 영수증 푸터 생성
     */
    private fun createReceiptFooter(): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        commands.addAll(getLineFeed())
        commands.addAll(getAlignCenter())
        commands.addAll(convertStringToBytes("✅ 결제가 완료되었습니다"))
        commands.addAll(getLineFeed())
        commands.addAll(convertStringToBytes("감사합니다!"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        return commands
    }
    
    /**
     * 정보 라인 생성 (라벨: 값 형태)
     */
    private fun createInfoLine(label: String, value: String): List<Byte> {
        val maxWidth = 40
        val colonSpace = ": "
        val totalLabelLength = getDisplayLength(label) + colonSpace.length
        
        // 값이 들어갈 공간 계산
        val valueMaxLength = maxWidth - totalLabelLength
        val truncatedValue = if (getDisplayLength(value) > valueMaxLength) {
            truncateToDisplayLength(value, valueMaxLength - 3) + "..."
        } else {
            value
        }
        
        val line = label + colonSpace + truncatedValue
        return convertStringToBytes(line) + getLineFeed()
    }
    
    /**
     * 구분선 생성
     */
    private fun createSeparatorLine(): List<Byte> {
        val line = "─".repeat(40)
        return convertStringToBytes(line) + getLineFeed()
    }
    
    /**
     * 해시 값 축약 (앞 8자리 + ... + 뒤 8자리)
     */
    private fun shortenHash(hash: String): String {
        return if (hash.length > 20) {
            "${hash.take(8)}...${hash.takeLast(8)}"
        } else {
            hash
        }
    }
    
    /**
     * 주소 축약 (앞 6자리 + ... + 뒤 6자리)
     */
    private fun shortenAddress(address: String): String {
        return if (address.length > 16) {
            "${address.take(6)}...${address.takeLast(6)}"
        } else {
            address
        }
    }
    
    /**
     * 타임스탬프 포맷팅
     */
    private fun formatTimestamp(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.w(TAG, "타임스탬프 파싱 실패: $timestamp")
            timestamp.take(19).replace('T', ' ')
        }
    }
    
    /**
     * 현재 시간 문자열 반환
     */
    private fun getCurrentTimeString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(Date())
    }
    
    /**
     * 한글을 고려한 표시 길이 계산
     */
    private fun getDisplayLength(text: String): Int {
        return text.fold(0) { acc, char ->
            acc + if (char.code > 127) 2 else 1
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
    
    // === ESC/POS 명령어 헬퍼 메서드들 ===
    
    private fun getInitCommands(): List<Byte> {
        return listOf(0x1B.toByte(), 0x40.toByte()) + // ESC @ (초기화)
               listOf(0x1B.toByte(), 0x59.toByte(), 0x48.toByte(), 0x43.toByte(), 0x01.toByte()) // UTF-8 설정
    }
    
    private fun convertStringToBytes(text: String): List<Byte> {
        return text.toByteArray(Charsets.UTF_8).toList()
    }
    
    private fun getLineFeed(): List<Byte> = listOf(0x0A.toByte())
    
    private fun getAlignLeft(): List<Byte> = listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte())
    
    private fun getAlignCenter(): List<Byte> = listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())
    
    private fun getNormalFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x00.toByte())
    
    private fun getBoldFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x08.toByte())
    
    private fun getBoldLargeFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x38.toByte())
    
    private fun getPaperCutCommand(): List<Byte> = listOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x01.toByte())
}