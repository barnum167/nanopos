package com.nanodatacenter.ndppos

import android.util.Log
import java.nio.charset.Charset

/**
 * 인코딩 통일 관리 헬퍼 클래스
 * 서버 통신과 프린터 출력에서 일관된 인코딩을 사용하기 위한 헬퍼
 */
object EncodingHelper {
    
    private const val TAG = "EncodingHelper"
    
    // ✅ 프로젝트 전체에서 사용할 통일된 인코딩
    const val DEFAULT_CHARSET_NAME = "EUC-KR"
    val DEFAULT_CHARSET: Charset = Charset.forName(DEFAULT_CHARSET_NAME)
    
    // 백업 인코딩들
    const val FALLBACK_CHARSET_NAME = "CP949"
    val FALLBACK_CHARSET: Charset = Charset.forName(FALLBACK_CHARSET_NAME)
    
    const val UTF8_CHARSET_NAME = "UTF-8"
    val UTF8_CHARSET: Charset = Charsets.UTF_8
    
    /**
     * 문자열을 기본 인코딩(EUC-KR)으로 바이트 배열로 변환
     */
    fun stringToBytes(text: String): ByteArray {
        return try {
            val bytes = text.toByteArray(DEFAULT_CHARSET)
            Log.d(TAG, "문자열을 ${DEFAULT_CHARSET_NAME}로 인코딩: '${text}' -> ${bytes.size} bytes")
            bytes
        } catch (e: Exception) {
            Log.w(TAG, "기본 인코딩(${DEFAULT_CHARSET_NAME}) 실패, 백업 인코딩 사용: ${e.message}")
            try {
                val bytes = text.toByteArray(FALLBACK_CHARSET)
                Log.d(TAG, "백업 인코딩(${FALLBACK_CHARSET_NAME}) 사용: '${text}' -> ${bytes.size} bytes")
                bytes
            } catch (e2: Exception) {
                Log.w(TAG, "백업 인코딩도 실패, UTF-8 사용: ${e2.message}")
                text.toByteArray(UTF8_CHARSET)
            }
        }
    }
    
    /**
     * 바이트 배열을 기본 인코딩(EUC-KR)으로 문자열로 변환
     */
    fun bytesToString(bytes: ByteArray): String {
        return try {
            val text = String(bytes, DEFAULT_CHARSET)
            Log.d(TAG, "${DEFAULT_CHARSET_NAME}에서 문자열로 디코딩: ${bytes.size} bytes -> '${text}'")
            text
        } catch (e: Exception) {
            Log.w(TAG, "기본 디코딩(${DEFAULT_CHARSET_NAME}) 실패, 백업 인코딩 사용: ${e.message}")
            try {
                val text = String(bytes, FALLBACK_CHARSET)
                Log.d(TAG, "백업 디코딩(${FALLBACK_CHARSET_NAME}) 사용: ${bytes.size} bytes -> '${text}'")
                text
            } catch (e2: Exception) {
                Log.w(TAG, "백업 디코딩도 실패, UTF-8 사용: ${e2.message}")
                String(bytes, UTF8_CHARSET)
            }
        }
    }
    
    /**
     * 프린터용 ESC/POS 초기화 명령어 생성 (EUC-KR 코드페이지 포함)
     */
    fun getPrinterInitCommands(): ByteArray {
        return byteArrayOf(
            0x1B.toByte(), 0x40.toByte(),        // ESC @ (프린터 초기화)
            0x1B.toByte(), 0x74.toByte(), 0x12.toByte()  // ESC t 18 (한국어 코드페이지 CP949/EUC-KR)
        )
    }
    
    /**
     * 서버 통신용 Content-Type 헤더 반환
     */
    fun getContentTypeHeader(): String {
        return "application/json; charset=${DEFAULT_CHARSET_NAME}"
    }
    
    /**
     * 서버 통신용 Accept 헤더 반환
     */
    fun getAcceptHeader(): String {
        return "application/json; charset=${DEFAULT_CHARSET_NAME}"
    }
    
    /**
     * 서버 통신용 Accept-Charset 헤더 반환
     */
    fun getAcceptCharsetHeader(): String {
        return DEFAULT_CHARSET_NAME
    }
    
    /**
     * 현재 사용 중인 인코딩 정보 로깅
     */
    fun logEncodingInfo() {
        Log.i(TAG, "════════════════════════════════════════")
        Log.i(TAG, "인코딩 설정 정보")
        Log.i(TAG, "기본 인코딩: ${DEFAULT_CHARSET_NAME}")
        Log.i(TAG, "백업 인코딩: ${FALLBACK_CHARSET_NAME}")
        Log.i(TAG, "UTF-8 인코딩: ${UTF8_CHARSET_NAME}")
        Log.i(TAG, "Content-Type: ${getContentTypeHeader()}")
        Log.i(TAG, "Accept: ${getAcceptHeader()}")
        Log.i(TAG, "Accept-Charset: ${getAcceptCharsetHeader()}")
        Log.i(TAG, "════════════════════════════════════════")
    }
    
    /**
     * 인코딩 호환성 테스트
     */
    fun testEncodingCompatibility(): Boolean {
        val testTexts = listOf(
            "한글 테스트",
            "가나다라마바사",
            "아메리카노 4,500원",
            "결제가 완료되었습니다"
        )
        
        Log.i(TAG, "인코딩 호환성 테스트 시작")
        var allSuccess = true
        
        for ((index, testText) in testTexts.withIndex()) {
            try {
                // 인코딩 -> 디코딩 테스트
                val encoded = stringToBytes(testText)
                val decoded = bytesToString(encoded)
                
                val success = testText == decoded
                Log.d(TAG, "테스트 ${index + 1}: '$testText' -> ${encoded.size} bytes -> '$decoded' [${if (success) "성공" else "실패"}]")
                
                if (!success) {
                    allSuccess = false
                    Log.w(TAG, "인코딩 불일치 발견: 원본='$testText', 복원='$decoded'")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "테스트 ${index + 1} 실패: ${e.message}")
                allSuccess = false
            }
        }
        
        Log.i(TAG, "인코딩 호환성 테스트 ${if (allSuccess) "성공" else "실패"}")
        return allSuccess
    }
    
    /**
     * 문자열에서 이모지 제거 (Android 6.0 호환)
     */
    fun removeEmojis(text: String): String {
        // Android 6.0 호환 이모지 패턴 (서로게이트 쌍 사용)
        val emojiPattern = Regex(
            "[\\uD83D\\uDE00-\\uD83D\\uDE4F]|" +  // 얼굴 이모지
            "[\\uD83C\\uDF00-\\uD83C\\uDFFF]|" +  // 기호와 그림 문자
            "[\\uD83D\\uDC00-\\uD83D\\uDCFF]|" +  // 동물과 자연
            "[\\uD83D\\uDD00-\\uD83D\\uDDFF]|" +  // 기호
            "[\\uD83D\\uDE80-\\uD83D\\uDEFF]|" +  // 교통수단과 지도 기호
            "[\\uD83C\\uDDE0-\\uD83C\\uDDFF]|" +  // 국기
            "[\\u2600-\\u26FF]|" +               // 기타 기호
            "[\\u2700-\\u27BF]"                  // 딩뱃 문자
        )
        val cleaned = text.replace(emojiPattern, "")
        
        if (cleaned != text) {
            Log.d(TAG, "이모지 제거: '$text' -> '$cleaned'")
        }
        
        return cleaned
    }
    
    /**
     * 프린터 출력용 안전한 문자열 생성 (단순 이모지 제거만)
     */
    fun sanitizeForPrinter(text: String): String {
        val sanitized = removeEmojis(text)
        
        if (sanitized != text) {
            Log.d(TAG, "프린터용 문자열 정제: '$text' -> '$sanitized'")
        }
        
        return sanitized
    }
}
