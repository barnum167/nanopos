package com.nanodatacenter.ndppos

import android.util.Log
import com.elixirpay.elixirpaycat.helper.EpsonPrinterHelper
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 * English version of Printer Helper with improved design
 * Creates beautiful receipts in English without emojis or markdown
 */
class PrinterHelperEnglish {
    
    companion object {
        private const val TAG = "NDP_PRINTER_HELPER_EN"
        private const val RECEIPT_WIDTH = 42
    }
    
    private val epsonHelper = EpsonPrinterHelper.getInstance().setPageWidth(RECEIPT_WIDTH)
    
    /**
     * Create beautifully formatted transaction receipt in English
     */
    fun createTransactionReceipt(receiptData: ReceiptData): ByteArray {
        Log.d(TAG, "Creating English transaction receipt")
        
        val commands = mutableListOf<Byte>()
        
        // Initialize printer
        commands.addAll(getInitCommands())
        
        // ========== HEADER SECTION ==========
        commands.addAll(createWelcomeHeader())
        
        // ========== PAYMENT DETAILS SECTION ==========
        commands.addAll(createPaymentSection(receiptData))
        
        // ========== BLOCKCHAIN INFO SECTION ==========
        // commands.addAll(createBlockchainSection(receiptData)) // 주석처리: 블록체인 섹션 비활성화
        
        // ========== FOOTER SECTION ==========
        commands.addAll(createThankYouFooter())
        
        // Cut paper
        commands.addAll(getPaperCutCommand())
        
        val result = commands.toByteArray()
        Log.i(TAG, "Receipt created successfully: ${result.size} bytes")
        
        return result
    }
    
    /**
     * Create welcome header with improved design
     */
    private fun createWelcomeHeader(): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // Add some spacing at the top
        commands.addAll(getLineFeed())
        
        // Top border
        commands.addAll(getAlignCenter())
        commands.addAll(convertStringToBytes("********************************"))
        commands.addAll(getLineFeed())
        
        // Large welcome message
        commands.addAll(getBoldLargeFont())
        commands.addAll(convertStringToBytes("Welcome!!"))
        commands.addAll(getLineFeed())
        
        // Bottom border
        commands.addAll(getNormalFont())
        commands.addAll(convertStringToBytes("********************************"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        return commands
    }
    
    /**
     * Create payment details section
     */
    private fun createPaymentSection(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // Section title
        commands.addAll(createDashedLine())
        commands.addAll(getAlignCenter())
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("PAYMENT INFORMATION"))
        commands.addAll(getLineFeed())
        commands.addAll(getNormalFont())
        commands.addAll(createDashedLine())
        
        // Payment details
        commands.addAll(getAlignLeft())
        
        // Product name (품명)
        commands.addAll(createDetailLine("Item (Description)", "CUBE COFFEE * 1"))
        
        // Amount (highlighted) - using formatAmount for Wei conversion
        commands.addAll(getBoldFont())
        val formattedAmount = formatAmount(receiptData.amount, receiptData.token)
        commands.addAll(createDetailLine("Amount", formattedAmount))
        commands.addAll(getNormalFont())
        //commands.addAll(getLineFeed())
        
        // Currency/Token
        //commands.addAll(createDetailLine("Currency", receiptData.token))
//        commands.addAll(createDetailLine("Network", "MAINNET"))
        //commands.addAll(getLineFeed())
        
        // From address (shortened)
        val shortFrom = "${receiptData.fromAddress.take(6)}...${receiptData.fromAddress.takeLast(4)}"
        commands.addAll(createDetailLine("From", shortFrom))
        
        // To address (shortened)
        val shortTo = "${receiptData.toAddress.take(6)}...${receiptData.toAddress.takeLast(4)}"
        commands.addAll(createDetailLine("To", shortTo))

        commands.addAll(convertStringToBytes("Transaction Hash: ${receiptData.transactionHash}"))
        commands.addAll(getLineFeed())

        commands.addAll(createDashedLine())
        
        return commands
    }
    
    /*
     * Create blockchain information section
     * 주석처리: 블록체인 섹션 비활성화
     */
    /*
    private fun createBlockchainSection(receiptData: ReceiptData): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // Section title
        commands.addAll(getAlignCenter())
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("BLOCKCHAIN VERIFICATION"))
        commands.addAll(getLineFeed())
        commands.addAll(getNormalFont())
        commands.addAll(createDashedLine())
        
        // Blockchain details
        commands.addAll(getAlignLeft())
        
        // Full transaction hash (split into two lines for readability)
        commands.addAll(convertStringToBytes("Transaction Hash:"))
        commands.addAll(getLineFeed())
        
        val hashPart1 = receiptData.transactionHash.take(21)
        val hashPart2 = receiptData.transactionHash.drop(21)
        
        commands.addAll(convertStringToBytes("  $hashPart1"))
        commands.addAll(getLineFeed())
        commands.addAll(convertStringToBytes("  $hashPart2"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // Confirmation status
//        commands.addAll(createDetailLine("Status", "CONFIRMED"))
//        commands.addAll(createDetailLine("Block", "VERIFIED"))
        
        commands.addAll(createDashedLine())
        
        return commands
    }
    */
    
    /**
     * Create thank you footer with improved design
     */
    private fun createThankYouFooter(): List<Byte> {
        val commands = mutableListOf<Byte>()
        
        // Success status box
//        commands.addAll(getAlignCenter())
//        commands.addAll(convertStringToBytes("================================"))
//        commands.addAll(getLineFeed())
//        commands.addAll(getBoldFont())
//        commands.addAll(convertStringToBytes("PAYMENT SUCCESSFUL"))
//        commands.addAll(getLineFeed())
//        commands.addAll(getNormalFont())
//        commands.addAll(convertStringToBytes("================================"))
//        commands.addAll(getLineFeed())
//        commands.addAll(getLineFeed())
        
        // Thank you message with frame
        commands.addAll(getAlignCenter())
        commands.addAll(convertStringToBytes("********************************"))
        commands.addAll(getLineFeed())
        commands.addAll(getBoldMediumFont())
        commands.addAll(convertStringToBytes("Thank You!"))
        commands.addAll(getLineFeed())
        commands.addAll(getNormalFont())
        commands.addAll(convertStringToBytes("********************************"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        // Additional messages
        commands.addAll(convertStringToBytes("Have a Wonderful Day!"))
        commands.addAll(getLineFeed())
        commands.addAll(convertStringToBytes("We Appreciate Your Trust"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
//        // Customer service info
//        commands.addAll(convertStringToBytes("----------------------------------------"))
//        commands.addAll(getLineFeed())
//        commands.addAll(getBoldFont())
//        commands.addAll(convertStringToBytes("Customer Service"))
//        commands.addAll(getLineFeed())
//        commands.addAll(getNormalFont())
//        commands.addAll(convertStringToBytes("support@cubecoffee.com"))
//        commands.addAll(getLineFeed())
//        commands.addAll(convertStringToBytes("Tel: 1-800-COFFEE"))
//        commands.addAll(getLineFeed())
//        commands.addAll(convertStringToBytes("Available 24/7"))
//        commands.addAll(getLineFeed())
//        commands.addAll(convertStringToBytes("----------------------------------------"))
//        commands.addAll(getLineFeed())
//        commands.addAll(getLineFeed())
        
        // Store info
//        commands.addAll(convertStringToBytes("Store Location"))
//        commands.addAll(getLineFeed())
//        commands.addAll(convertStringToBytes("123 Digital Street"))
//        commands.addAll(getLineFeed())
//        commands.addAll(convertStringToBytes("Seoul, Korea"))
//        commands.addAll(getLineFeed())
//        commands.addAll(getLineFeed())
        
        // Final decorative border
        commands.addAll(convertStringToBytes("********************************"))
        commands.addAll(getLineFeed())
        commands.addAll(getBoldFont())
        commands.addAll(convertStringToBytes("KEEP THIS RECEIPT"))
        commands.addAll(getLineFeed())
        commands.addAll(convertStringToBytes("A Token of Our First Day"))
        commands.addAll(getLineFeed())
        commands.addAll(convertStringToBytes("********************************"))
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        commands.addAll(getLineFeed())
        
        return commands
    }
    
    /**
     * Create a detail line with label and value
     */
    private fun createDetailLine(label: String, value: String): List<Byte> {
        val labelWidth = 12
        val paddedLabel = label.padEnd(labelWidth)
        val line = "$paddedLabel: $value"
        
        return convertStringToBytes(line) + getLineFeed()
    }
    
    /**
     * Create dashed separator line
     */
    private fun createDashedLine(): List<Byte> {
        val line = "-" * (RECEIPT_WIDTH - 10)
        return convertStringToBytes(line) + getLineFeed()
    }
    
    /**
     * Format timestamp to readable date and time
     */
    private fun formatTimestamp(timestamp: String): Pair<String, String> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = dateFormat.parse(timestamp) ?: Date()
            
            val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val displayTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
            
            Pair(displayDateFormat.format(date), displayTimeFormat.format(date))
        } catch (e: Exception) {
            Pair(timestamp.split(" ")[0], timestamp.split(" ").getOrNull(1) ?: "")
        }
    }
    
    // ========== PRINTER COMMAND METHODS ==========
    
    private fun getInitCommands(): List<Byte> {
        return listOf(0x1B.toByte(), 0x40.toByte()) // ESC @ (Initialize)
    }
    
    private fun convertStringToBytes(text: String): List<Byte> {
        return text.toByteArray(Charsets.US_ASCII).toList()
    }
    
    private fun getLineFeed(): List<Byte> = listOf(0x0A.toByte())
    
    private fun getAlignLeft(): List<Byte> {
        return listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte())
    }
    
    private fun getAlignCenter(): List<Byte> {
        return listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())
    }
    
    private fun getAlignRight(): List<Byte> {
        return listOf(0x1B.toByte(), 0x61.toByte(), 0x02.toByte())
    }
    
    private fun getNormalFont(): List<Byte> {
        return listOf(0x1B.toByte(), 0x21.toByte(), 0x00.toByte())
    }
    
    private fun getBoldFont(): List<Byte> {
        return listOf(0x1B.toByte(), 0x21.toByte(), 0x08.toByte())
    }
    
    private fun getBoldMediumFont(): List<Byte> {
        return listOf(0x1B.toByte(), 0x21.toByte(), 0x28.toByte())
    }
    
    private fun getBoldLargeFont(): List<Byte> {
        return listOf(0x1B.toByte(), 0x21.toByte(), 0x38.toByte())
    }
    
    private fun getPaperCutCommand(): List<Byte> {
        return listOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x01.toByte())
    }
    
    // Extension function for string repetition
    private operator fun String.times(count: Int): String = this.repeat(count)
    
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
}
