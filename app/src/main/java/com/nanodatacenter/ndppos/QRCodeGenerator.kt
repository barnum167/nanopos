package com.nanodatacenter.ndppos

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.util.*

/**
 * QR 코드 생성 유틸리티 클래스
 * Android 6.0 호환성을 고려하여 작성됨
 */
class QRCodeGenerator {

    companion object {
        private const val TAG = "QRCodeGenerator"
        
        // QR 코드 기본 설정
        private const val DEFAULT_WIDTH = 512
        private const val DEFAULT_HEIGHT = 512
        private const val DEFAULT_MARGIN = 2
    }

    /**
     * 결제 정보를 포함한 QR 코드 데이터 생성
     */
    data class PaymentInfo(
        val amount: String,           // 결제 금액
        val merchantId: String,       // 가맹점 ID
        val merchantName: String,     // 가맹점 이름
        val orderId: String,          // 주문 ID
        val description: String = "", // 결제 설명
        val currency: String = "KRW"  // 통화
    )

    /**
     * 결제 정보로부터 QR 코드용 문자열 생성
     */
    fun createPaymentQRString(paymentInfo: PaymentInfo): String {
        // JSON 형식으로 결제 정보 구성
        return buildString {
            append("{")
            append("\"type\":\"payment\",")
            append("\"amount\":\"${paymentInfo.amount}\",")
            append("\"merchant_id\":\"${paymentInfo.merchantId}\",")
            append("\"merchant_name\":\"${paymentInfo.merchantName}\",")
            append("\"order_id\":\"${paymentInfo.orderId}\",")
            append("\"description\":\"${paymentInfo.description}\",")
            append("\"currency\":\"${paymentInfo.currency}\",")
            append("\"timestamp\":\"${System.currentTimeMillis()}\",")
            append("\"version\":\"1.0\"")
            append("}")
        }
    }

    /**
     * 간단한 결제 정보 문자열 생성 (호환성용)
     */
    fun createSimplePaymentString(amount: String, merchantName: String, orderId: String): String {
        return "PAY:$amount:$merchantName:$orderId:${System.currentTimeMillis()}"
    }

    /**
     * 텍스트로부터 QR 코드 비트맵 생성
     */
    fun generateQRCodeBitmap(
        text: String,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT
    ): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, DEFAULT_MARGIN)
                put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M)
            }

            val writer = MultiFormatWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            
            convertBitMatrixToBitmap(bitMatrix)
            
        } catch (e: WriterException) {
            android.util.Log.e(TAG, "QR 코드 생성 실패: ${e.message}")
            null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "비트맵 변환 실패: ${e.message}")
            null
        }
    }

    /**
     * BitMatrix를 Bitmap으로 변환
     */
    private fun convertBitMatrixToBitmap(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }

    /**
     * 결제 정보로부터 직접 QR 코드 비트맵 생성
     */
    fun generatePaymentQRBitmap(
        paymentInfo: PaymentInfo,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT
    ): Bitmap? {
        val qrString = createPaymentQRString(paymentInfo)
        return generateQRCodeBitmap(qrString, width, height)
    }

    /**
     * 간단한 결제 QR 코드 비트맵 생성
     */
    fun generateSimplePaymentQRBitmap(
        amount: String,
        merchantName: String,
        orderId: String,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT
    ): Bitmap? {
        val qrString = createSimplePaymentString(amount, merchantName, orderId)
        return generateQRCodeBitmap(qrString, width, height)
    }

    /**
     * QR 코드 데이터 검증
     */
    fun validateQRData(data: String): Boolean {
        return data.isNotBlank() && data.length <= 4296 // QR 코드 최대 용량
    }

    /**
     * 주문 ID 자동 생성
     */
    fun generateOrderId(prefix: String = "NDP"): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "${prefix}_${timestamp}_${random}"
    }
}
