package com.nanodatacenter.ndppos

import android.util.Log
import com.elixirpay.elixirpaycat.SerialPrinter
import java.nio.charset.Charset
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
            val normalizedToken = normalizeTokenSymbol(receiptData.token)
            Log.i(TAG, "금액: ${receiptData.amount} ${receiptData.token} -> $normalizedToken")
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
     * 영수증 인쇄 데이터 생성 (간소화된 버전)
     */
    private fun createReceiptPrintData(receiptData: ReceiptData): ByteArray {
        Log.d(TAG, "간소화된 영수증 인쇄 데이터 생성 시작")
        
        val commands = mutableListOf<Byte>()
        
        // 초기화
        commands.addAll(getInitCommands())
        
        // 헤더
        commands.addAll(createSimpleReceiptHeader())
        
        // 상품 정보 (아메리카노)
        commands.addAll(createProductInfo(receiptData))
        
        // 거래 정보 (간소화)
        commands.addAll(createSimpleTransactionInfo(receiptData))
        
        // 푸터
        commands.addAll(createSimpleReceiptFooter())
        
        // 용지 자르기
        commands.addAll(getPaperCutCommand())
        
        val result = commands.toByteArray()
        Log.d(TAG, "간소화된 영수증 데이터 생성 완료: ${result.size} bytes")
        
        return result
    }
    
    /**
     * 간소화된 영수증 헤더 생성
     */
    private fun createSimpleReceiptHeader(): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 가운데 정렬
        commands.addAll(getAlignCenter())
        
        // 굵게, 큰 글씨
        commands.addAll(getBoldLargeFont())
        
        // 제목
        commands.addAll(convertStringToBytes("결제 영수증"))
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
     * 상품 정보 생성
     */
    private fun createProductInfo(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 상품 정보 헤더
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("[상품 정보]"))
        commands.addAll(getNormalFont())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // 상품명 (데이터에서 가져오거나 기본값 사용)
        val productName = receiptData.productName ?: "CUBE COFFEE"
        commands.addAll(createInfoLine("상품명", productName))
        commands.addAll(getLineFeed())
        
        commands.addAll(createSeparatorLine())
        
        return commands
    }
    
    /**
     * 간소화된 거래 정보 섹션 생성 (txHash, Amount, timestamp만)
     */
    private fun createSimpleTransactionInfo(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 거래 정보 헤더
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("[거래 정보]"))
        commands.addAll(getNormalFont())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // 거래 해시
        commands.addAll(createInfoLine("거래 해시", shortenHash(receiptData.transactionHash)))
        commands.addAll(getLineFeed())
        
        // 결제 금액 (포맷팅 적용)
        val formattedAmount = formatAmount(receiptData.amount, receiptData.token)
        commands.addAll(createInfoLine("결제 금액", formattedAmount))
        commands.addAll(getLineFeed())
        
        // 거래 시간
        val formattedTime = formatTimestamp(receiptData.timestamp)
        commands.addAll(createInfoLine("거래 시간", formattedTime))
        commands.addAll(getLineFeed())
        
        commands.addAll(createSeparatorLine())
        
        return commands
    }
    
    /**
     * 거래 정보 섹션 생성
     */
    private fun createTransactionInfo(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 거래 정보 헤더 - ✅ 이모지 제거
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("[거래 정보]"))
        commands.addAll(getNormalFont())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // 거래 해시
        commands.addAll(createInfoLine("거래 해시", shortenHash(receiptData.transactionHash)))
        commands.addAll(getLineFeed())
        
        // 결제 금액 (포맷팅 적용)
        val formattedAmount = formatAmount(receiptData.amount, receiptData.token)
        commands.addAll(createInfoLine("결제 금액", formattedAmount))
        //commands.addAll(createInfoLine("결제 금액", "1"))
        commands.addAll(getLineFeed())
        
        // 토큰 정보 (정규화된 심볼 사용)
        val normalizedToken = normalizeTokenSymbol(receiptData.token)
        commands.addAll(createInfoLine("토큰", normalizedToken))
        commands.addAll(getLineFeed())
        
        commands.addAll(createSeparatorLine())
        
        return commands
    }
    
    /**
     * 주소 정보 섹션 생성
     */
    private fun createAddressInfo(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // 주소 정보 헤더 - ✅ 이모지 제거
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("[주소 정보]"))
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
        
        // 시간 정보 헤더 - ✅ 이모지 제거
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("[처리 시간]"))
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
     * 간소화된 영수증 푸터 생성
     */
    private fun createSimpleReceiptFooter(): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        commands.addAll(getLineFeed())
        commands.addAll(getAlignCenter())
        commands.addAll(convertStringToBytes("결제가 완료되었습니다"))
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
     * 타임스탬프 포맷팅 (한국시간 기준)
     */
    private fun formatTimestamp(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            outputFormat.timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.w(TAG, "타임스탬프 파싱 실패: $timestamp")
            // 실패 시에도 한국시간으로 현재 시간 반환
            val fallbackFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            fallbackFormat.timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            fallbackFormat.format(Date())
        }
    }
    
    /**
     * 현재 시간 문자열 반환 (한국시간 기준)
     */
    private fun getCurrentTimeString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
        return format.format(Date())
    }
    
    /**
     * 토큰 심볼 정규화 (주소를 심볼로 변환)
     */
    private fun normalizeTokenSymbol(token: String): String {
        return "USDT"
    }

    /**
     * 결제 금액 포맷팅 (Wei 단위를 적절한 토큰 단위로 변환)
     */
    private fun formatAmount(amount: String, token: String): String {
        return try {
            val normalizedToken = normalizeTokenSymbol(token)
            Log.d(TAG, "금액 포맷팅 시작: amount=$amount, token=$token -> $normalizedToken")
            
            // Wei 단위를 토큰 단위로 변환 (18 decimals 기준)
            val weiAmount = when {
                amount.startsWith("0x", ignoreCase = true) -> {
                    // 16진수인 경우
                    java.math.BigInteger(amount.substring(2), 16)
                }
                amount.length > 15 && amount.all { it.isDigit() } -> {
                    // 매우 큰 숫자인 경우 (Wei 단위로 간주)
                    java.math.BigInteger(amount)
                }
                else -> {
                    // 일반 숫자인 경우도 Wei로 간주하여 변환
                    val numericAmount = amount.toLongOrNull() ?: 0L
                    java.math.BigInteger.valueOf(numericAmount)
                }
            }
            
            // Wei를 토큰 단위로 변환 (1 token = 10^18 wei)
            val divisor = java.math.BigDecimal("1000000000000000000") // 10^18
            val tokenAmount = weiAmount.toBigDecimal().divide(divisor)
            
            Log.d(TAG, "Wei -> 토큰 변환: $amount Wei -> $tokenAmount $normalizedToken")
            
            // 소수점 처리: 0이면 정수로, 아니면 최대 6자리까지 표시
            val formatted = if (tokenAmount.compareTo(java.math.BigDecimal.ZERO) == 0) {
                "0"
            } else if (tokenAmount.scale() <= 0 || tokenAmount.remainder(java.math.BigDecimal.ONE).compareTo(java.math.BigDecimal.ZERO) == 0) {
                tokenAmount.toBigInteger().toString()
            } else {
                // 소수점이 있는 경우 최대 6자리까지, 끝자리 0 제거
                tokenAmount.setScale(6, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
            }
            
            val result = "$formatted $normalizedToken"
            Log.d(TAG, "최종 포맷: $result")
            result
            
        } catch (e: Exception) {
            Log.w(TAG, "금액 포맷팅 실패: $amount, 오류: ${e.message}")
            val normalizedToken = normalizeTokenSymbol(token)
            // 실패 시 원본을 축약하여 표시
            val displayAmount = if (amount.length > 20) {
                "${amount.take(8)}...${amount.takeLast(4)}"
            } else {
                amount
            }
            "$displayAmount $normalizedToken"
        }
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
               listOf(0x1B.toByte(), 0x74.toByte(), 0x12.toByte()) // ESC t 18 (한국어 코드페이지 CP949/EUC-KR)
    }
    
    private fun convertStringToBytes(text: String): List<Byte> {
        return text.toByteArray(Charset.forName("EUC-KR")).toList()
    }
    
    private fun getLineFeed(): List<Byte> = listOf(0x0A.toByte())
    
    private fun getAlignLeft(): List<Byte> = listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte())
    
    private fun getAlignCenter(): List<Byte> = listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())
    
    private fun getNormalFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x00.toByte())
    
    private fun getBoldFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x08.toByte())
    
    private fun getBoldLargeFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x38.toByte())
    
    private fun getPaperCutCommand(): List<Byte> = listOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x01.toByte())
}