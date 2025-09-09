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
        
        // 테스트 인쇄를 위한 변수
        private var testPrintTouchCount = 0
        private var lastTestTouchTime = 0L
        private const val TEST_TOUCH_RESET_TIME = 3000L // 3초 후 카운트 리셋
    }
    
    // MainActivity 참조 (결제 완료 콜백용)
    private var mainActivity: MainActivity? = null
    
    /**
     * MainActivity 참조 설정
     */
    fun setMainActivity(activity: MainActivity) {
        mainActivity = activity
        Log.d(TAG, "MainActivity 참조 설정됨")
    }
    
    private val printer: SerialPrinter by lazy {
        SerialPrinter.Builder()
            .tty(PRINTER_PORT)
            .baudRate(BAUD_RATE)
            .build()
    }
    
    private val printerHelper = PrinterHelper()
    private val printerHelperEnglish = PrinterHelperEnglish()  // English version helper
    
    /**
     * 프린터 상태 터치 이벤트 처리 (10번 터치시 테스트 인쇄)
     */
    fun onPrinterStatusTouch() {
        val currentTime = System.currentTimeMillis()
        
        // 3초 이상 지났으면 카운트 리셋
        if (currentTime - lastTestTouchTime > TEST_TOUCH_RESET_TIME) {
            testPrintTouchCount = 0
        }
        
        testPrintTouchCount++
        lastTestTouchTime = currentTime
        
        Log.d(TAG, "프린터 상태 터치: $testPrintTouchCount/10")
        
        if (testPrintTouchCount >= 10) {
            Log.i(TAG, "🎯 테스트 인쇄 트리거 - 10번 터치 완료!")
            testPrintTouchCount = 0 // 리셋
            
            // 테스트 인쇄 실행
            Thread {
                printTestReceipt()
            }.start()
        }
    }
    
    /**
     * Test receipt printing (English version)
     */
    private fun printTestReceipt() {
        Log.i(TAG, "═══════════════════════════════════════════")
        Log.i(TAG, "🧪 Test Printing Started - English Receipt")
        Log.i(TAG, "═══════════════════════════════════════════")
        
        try {
            // 단일 테스트 금액 (4.5 USDT)
            val testAmount = "4500000000000000000" // 4.5 USDT in Wei
            
            Log.i(TAG, "테스트 영수증 - Wei 금액: $testAmount")
            
            val testReceiptData = ReceiptData(
                printId = "TEST-${System.currentTimeMillis()}",
                transactionHash = "0x1234567890abcdef1234567890abcdef12345678",
                amount = testAmount,
                token = "USDT",
                fromAddress = "0xabc123def456789012345678901234567890abcd",
                toAddress = "0xdef456789012345678901234567890abcdef1234",
                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
                productName = "CUBE COFFEE"
            )
            
            // Log the formatted amount for verification
            val formattedAmount = formatAmount(testReceiptData.amount, testReceiptData.token)
            Log.i(TAG, "Wei 금액 변환 결과: $testAmount wei -> $formattedAmount")
            
            // Generate English receipt
            val testData = printerHelperEnglish.createTransactionReceipt(testReceiptData)
            
            // Send to printer
            printer.setBuffer(testData)
            printer.print()
            
            // 테스트 영수증 출력 시작 즉시 감사 이미지 표시
            mainActivity?.showThankYouImage()
            Log.i(TAG, "🎉 테스트 영수증 출력 시작 - 즉시 감사 이미지 표시")
            
            Log.i(TAG, "✅ Test printing completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Test printing failed: ${e.message}")
        }
    }

    /**
     * Receipt auto printing (English version)
     */
    fun printReceipt(receiptData: ReceiptData): Boolean {
        return try {
            Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.i(TAG, "Auto Receipt Printing Started (English)")
            Log.i(TAG, "Print ID: ${receiptData.printId}")
            Log.i(TAG, "Transaction Hash: ${receiptData.transactionHash}")
            val normalizedToken = normalizeTokenSymbol(receiptData.token)
            Log.i(TAG, "Amount: ${receiptData.amount} ${receiptData.token} -> $normalizedToken")
            
            // Set English locale
            val originalLocale = Locale.getDefault()
            Locale.setDefault(Locale.US)
            Log.i(TAG, "Locale setting: ${originalLocale} -> ${Locale.getDefault()}")
            
            Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            // Generate receipt data using English helper
            val printData = printerHelperEnglish.createTransactionReceipt(receiptData)
            
            // Send to printer
            printer.setBuffer(printData)
            printer.print()
            
            // 영수증 출력 시작 즉시 감사 이미지 표시
            mainActivity?.showThankYouImage()
            Log.i(TAG, "🎉 영수증 출력 시작 - 즉시 감사 이미지 표시")
            
            // Wait for printing completion
            Thread.sleep(3000)
            
            Log.i(TAG, "✅ Auto receipt printing completed - ID: ${receiptData.printId}")
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Auto receipt printing failed - ID: ${receiptData.printId}, Error: ${e.message}")
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
        val productName = receiptData.productName.takeIf { !it.isNullOrBlank() } ?: "CUBE COFFEE"
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
    private fun normalizeTokenSymbol(@Suppress("UNUSED_PARAMETER") token: String): String {
        // 현재는 USDT만 지원하지만, 향후 다른 토큰 지원을 위해 매개변수 유지
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
            
            // 소수점 처리: 매우 작은 값도 적절히 표시
            val formatted = if (tokenAmount.compareTo(java.math.BigDecimal.ZERO) == 0) {
                "0"
            } else if (tokenAmount.scale() <= 0 || tokenAmount.remainder(java.math.BigDecimal.ONE).compareTo(java.math.BigDecimal.ZERO) == 0) {
                tokenAmount.toBigInteger().toString()
            } else {
                // 매우 작은 값들을 위해 더 많은 소수점 자리 지원
                val scaledAmount = if (tokenAmount.compareTo(java.math.BigDecimal("0.000001")) < 0) {
                    // 0.000001보다 작은 경우 최대 18자리까지 표시 (과학적 표기법 회피)
                    tokenAmount.setScale(18, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                } else {
                    // 일반적인 경우 최대 6자리까지 표시
                    tokenAmount.setScale(6, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                }
                scaledAmount.toPlainString()
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
    
    /**
     * 테스트 인쇄용 데이터 생성
     */
    private fun createTestPrintData(): ByteArray {
        Log.d(TAG, "🧪 테스트 인쇄 데이터 생성 시작")
        
        val commands = mutableListOf<Byte>()
        
        // 프린터 초기화 및 다양한 인코딩 테스트
        commands.addAll(getInitCommands())
        
        // 테스트 헤더
        commands.addAll(getAlignCenter())
        commands.addAll(getBoldLargeFont())
        commands.addAll(convertStringToBytes("한국어 테스트"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        commands.addAll(getNormalFont())
        commands.addAll(getAlignLeft())
        commands.addAll(createSeparatorLine())
        
        // 다양한 한국어 텍스트 테스트
        val testTexts = listOf(
            "가나다라마바사",
            "한글 인쇄 테스트",
            "안녕하세요",
            "결제가 완료되었습니다",
            "감사합니다",
            "상품명: 아메리카노",
            "금액: 4,500원",
            "시간: 2025년 01월 01일"
        )
        
        testTexts.forEach { text ->
            commands.addAll(convertStringToBytes(text))
            commands.addAll(getLineFeed())
        }
        
        commands.addAll(createSeparatorLine())
        commands.addAll(getLineFeed())
        commands.addAll(getAlignCenter())
        commands.addAll(convertStringToBytes("테스트 완료"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // 용지 자르기
        commands.addAll(getPaperCutCommand())
        
        val result = commands.toByteArray()
        Log.d(TAG, "🧪 테스트 인쇄 데이터 생성 완료: ${result.size} bytes")
        
        return result
    }

    private fun getInitCommands(): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        Log.d(TAG, "🔧 프린터 초기화 시작")
        
        // 프린터 초기화
        val initCmd = listOf(0x1B.toByte(), 0x40.toByte()) // ESC @
        commands.addAll(initCmd)
        Log.d(TAG, "   초기화 명령어 (ESC @): ${initCmd.joinToString(" ") { String.format("%02X", it) }}")
        
        // 한국어 코드페이지 설정 시도 1: KS X 1001
        val cp37 = listOf(0x1B.toByte(), 0x74.toByte(), 0x25.toByte()) // ESC t 37
        commands.addAll(cp37)
        Log.d(TAG, "   코드페이지 37 (KS X 1001): ${cp37.joinToString(" ") { String.format("%02X", it) }}")
        
        // 한국어 코드페이지 설정 시도 2: CP949
        val cp21 = listOf(0x1B.toByte(), 0x74.toByte(), 0x15.toByte()) // ESC t 21
        commands.addAll(cp21)
        Log.d(TAG, "   코드페이지 21 (CP949): ${cp21.joinToString(" ") { String.format("%02X", it) }}")
        
        // 추가 시도 3: CP932 (일본어지만 한자 지원으로 한국어도 가능할 수 있음)
        val cp932 = listOf(0x1B.toByte(), 0x74.toByte(), 0x01.toByte()) // ESC t 1
        commands.addAll(cp932)
        Log.d(TAG, "   코드페이지 1 (CP932): ${cp932.joinToString(" ") { String.format("%02X", it) }}")
        
        // 추가 시도 4: 국제 문자 세트 설정
        val intlSet = listOf(0x1B.toByte(), 0x52.toByte(), 0x08.toByte()) // ESC R 8 (한국)
        commands.addAll(intlSet)
        Log.d(TAG, "   국제 문자 세트 (한국): ${intlSet.joinToString(" ") { String.format("%02X", it) }}")
        
        Log.d(TAG, "🔧 초기화 명령 생성 완료: ${commands.size} bytes")
        logCommandBytes("전체 초기화", commands)
        
        return commands
    }
    
    private fun convertStringToBytes(text: String): List<Byte> {
        Log.d(TAG, "🔤 텍스트 변환 시작: '$text'")
        
        // 다양한 인코딩 방식으로 시도하고 결과 비교
        val encodingResults = mutableMapOf<String, ByteArray?>()
        
        // 1. EUC-KR 시도
        try {
            val eucKrBytes = text.toByteArray(Charset.forName("EUC-KR"))
            encodingResults["EUC-KR"] = eucKrBytes
            Log.d(TAG, "   ✅ EUC-KR 변환 성공: '$text' -> ${eucKrBytes.size} bytes")
            logTextBytes("EUC-KR", text, eucKrBytes)
        } catch (e: Exception) {
            encodingResults["EUC-KR"] = null
            Log.w(TAG, "   ❌ EUC-KR 변환 실패: ${e.message}")
        }
        
        // 2. CP949 시도
        try {
            val cp949Bytes = text.toByteArray(Charset.forName("CP949"))
            encodingResults["CP949"] = cp949Bytes
            Log.d(TAG, "   ✅ CP949 변환 성공: '$text' -> ${cp949Bytes.size} bytes")
            logTextBytes("CP949", text, cp949Bytes)
        } catch (e: Exception) {
            encodingResults["CP949"] = null
            Log.w(TAG, "   ❌ CP949 변환 실패: ${e.message}")
        }
        
        // 3. ISO-8859-1 시도 (라틴 문자)
        try {
            val isoBytes = text.toByteArray(Charset.forName("ISO-8859-1"))
            encodingResults["ISO-8859-1"] = isoBytes
            Log.d(TAG, "   ✅ ISO-8859-1 변환 성공: '$text' -> ${isoBytes.size} bytes")
            logTextBytes("ISO-8859-1", text, isoBytes)
        } catch (e: Exception) {
            encodingResults["ISO-8859-1"] = null
            Log.w(TAG, "   ❌ ISO-8859-1 변환 실패: ${e.message}")
        }
        
        // 4. UTF-8 (폴백)
        val utf8Bytes = text.toByteArray(Charsets.UTF_8)
        encodingResults["UTF-8"] = utf8Bytes
        Log.d(TAG, "   ✅ UTF-8 변환 (폴백): '$text' -> ${utf8Bytes.size} bytes")
        logTextBytes("UTF-8", text, utf8Bytes)
        
        // 우선순위: EUC-KR > CP949 > ISO-8859-1 > UTF-8
        val finalBytes = encodingResults["EUC-KR"] 
            ?: encodingResults["CP949"] 
            ?: encodingResults["ISO-8859-1"] 
            ?: utf8Bytes
            
        val usedEncoding = when (finalBytes) {
            encodingResults["EUC-KR"] -> "EUC-KR"
            encodingResults["CP949"] -> "CP949"
            encodingResults["ISO-8859-1"] -> "ISO-8859-1"
            else -> "UTF-8"
        }
        
        Log.d(TAG, "🎯 최종 선택된 인코딩: $usedEncoding (${finalBytes.size} bytes)")
        
        return finalBytes.toList()
    }
    
    /**
     * 명령어 바이트 로깅
     */
    private fun logCommandBytes(description: String, commands: List<Byte>) {
        val hexString = commands.take(30).joinToString(" ") { String.format("%02X", it) }
        Log.d(TAG, "🔧 [$description] 명령어 바이트: $hexString${if (commands.size > 30) "... (총 ${commands.size} bytes)" else ""}")
        
        // 주요 ESC/POS 명령어 해석
        var i = 0
        while (i < commands.size - 1) {
            val cmd = commands[i]
            val next = commands.getOrNull(i + 1)
            
            when {
                cmd == 0x1B.toByte() && next == 0x40.toByte() -> {
                    Log.d(TAG, "   위치 $i: ESC @ (프린터 초기화)")
                    i += 2
                }
                cmd == 0x1B.toByte() && next == 0x74.toByte() -> {
                    val param = commands.getOrNull(i + 2)
                    Log.d(TAG, "   위치 $i: ESC t $param (코드페이지 설정)")
                    i += 3
                }
                cmd == 0x1B.toByte() && next == 0x52.toByte() -> {
                    val param = commands.getOrNull(i + 2)
                    Log.d(TAG, "   위치 $i: ESC R $param (국제 문자 세트)")
                    i += 3
                }
                cmd == 0x1B.toByte() && next == 0x61.toByte() -> {
                    val align = commands.getOrNull(i + 2)
                    val alignText = when (align?.toInt()) {
                        0 -> "왼쪽"
                        1 -> "가운데"
                        2 -> "오른쪽"
                        else -> "알 수 없음"
                    }
                    Log.d(TAG, "   위치 $i: ESC a $align (정렬: $alignText)")
                    i += 3
                }
                cmd == 0x1B.toByte() && next == 0x21.toByte() -> {
                    val style = commands.getOrNull(i + 2)
                    Log.d(TAG, "   위치 $i: ESC ! $style (폰트 스타일)")
                    i += 3
                }
                cmd == 0x0A.toByte() -> {
                    Log.d(TAG, "   위치 $i: LF (줄바꿈)")
                    i += 1
                }
                else -> i += 1
            }
        }
    }

    /**
     * 텍스트 바이트 변환 로깅 (개선된 버전)
     */
    private fun logTextBytes(encoding: String, text: String, bytes: ByteArray) {
        val hexString = bytes.take(20).joinToString(" ") { String.format("%02X", it) }
        Log.d(TAG, "      [$encoding] '$text' -> $hexString${if (bytes.size > 20) "..." else ""}")
        
        // 문자별 바이트 분석 (한글의 경우)
        if (text.any { it.code > 127 }) {
            Log.d(TAG, "      [$encoding] 다바이트 문자 포함 - 한글/특수문자 ${text.count { it.code > 127 }}개")
        }
        
        // 역변환 테스트
        try {
            val decoded = String(bytes, Charset.forName(encoding))
            val isCorrect = decoded == text
            Log.d(TAG, "      [$encoding] 역변환 테스트: '$decoded' (${if (isCorrect) "✅ 성공" else "❌ 실패"})")
            
            if (!isCorrect) {
                Log.w(TAG, "      [$encoding] 원본과 다름 - 인코딩 문제 가능성!")
            }
        } catch (e: Exception) {
            Log.w(TAG, "      [$encoding] 역변환 실패: ${e.message}")
        }
    }
    
    private fun getLineFeed(): List<Byte> = listOf(0x0A.toByte())
    
    private fun getAlignLeft(): List<Byte> = listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte())
    
    private fun getAlignCenter(): List<Byte> = listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())
    
    private fun getNormalFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x00.toByte())
    
    private fun getBoldFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x08.toByte())
    
    private fun getBoldLargeFont(): List<Byte> = listOf(0x1B.toByte(), 0x21.toByte(), 0x38.toByte())
    
    private fun getPaperCutCommand(): List<Byte> = listOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x01.toByte())
}