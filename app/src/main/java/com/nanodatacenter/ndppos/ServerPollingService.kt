package com.nanodatacenter.ndppos

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * 서버 폴링 서비스
 * 주기적으로 서버의 인쇄 대기열을 확인하여 인쇄 요청을 처리합니다
 */
class ServerPollingService {
    
    companion object {
        private const val TAG = "ServerPollingService"
        private const val POLLING_INTERVAL = 3000L // 3초마다 폴링
        private const val SERVER_BASE_URL = "https://a7b4aeba3e88.ngrok-free.app" // 서버 IP 주소 (환경에 맞게 수정)
        // 실제 사용 시 PC의 IP 주소로 변경: 예) "http://192.168.1.100:3000"
    }
    
    private var isPolling = false
    private var pollingJob: Job? = null
    private var autoPrintManager: AutoPrintManager? = null
    
    /**
     * 폴링 서비스 시작
     */
    fun startPolling(autoPrintManager: AutoPrintManager) {
        if (isPolling) {
            Log.w(TAG, "이미 폴링이 실행 중입니다")
            return
        }
        
        this.autoPrintManager = autoPrintManager
        isPolling = true
        
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "서버 폴링 서비스 시작")
        Log.i(TAG, "서버 주소: $SERVER_BASE_URL")
        Log.i(TAG, "폴링 간격: ${POLLING_INTERVAL}ms")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isPolling) {
                try {
                    checkPrintQueue()
                    delay(POLLING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "폴링 중 오류 발생: ${e.message}")
                    delay(POLLING_INTERVAL * 2) // 오류 시 더 긴 대기
                }
            }
        }
    }
    
    /**
     * 폴링 서비스 중지
     */
    fun stopPolling() {
        if (!isPolling) {
            return
        }
        
        isPolling = false
        pollingJob?.cancel()
        pollingJob = null
        
        Log.i(TAG, "서버 폴링 서비스 중지됨")
    }
    
    /**
     * 서버의 인쇄 대기열 확인
     */
    private suspend fun checkPrintQueue() {
        try {
            Log.d(TAG, "인쇄 대기열 확인 중... URL: $SERVER_BASE_URL/api/receipt/queue")
            
            val url = URL("$SERVER_BASE_URL/api/receipt/queue")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10000 // 연결 타임아웃 증가
                readTimeout = 15000 // 읽기 타임아웃 증가
                
                // ngrok 요청에 필요한 헤더 추가
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Android-NDP-Printer/1.0")
                setRequestProperty("ngrok-skip-browser-warning", "true")
                
                // HTTPS 연결 설정
                useCaches = false
                doInput = true
            }
            
            Log.d(TAG, "서버 연결 시도 중... 호스트: ${url.host}")
            
            val responseCode = connection.responseCode
            Log.d(TAG, "서버 응답 코드: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { 
                    it.readText() 
                }
                
                Log.d(TAG, "서버 응답 성공: $response")
                processQueueResponse(response)
                
            } else {
                Log.w(TAG, "서버 요청 실패: HTTP $responseCode")
                
                // 오류 응답 내용 읽기
                val errorResponse = try {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream, StandardCharsets.UTF_8)).use { 
                        it.readText() 
                    }
                } catch (e: Exception) {
                    "Error stream not available"
                }
                
                Log.e(TAG, "서버 오류 응답: $errorResponse")
            }
            
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "서버 연결 실패: 서버에 연결할 수 없습니다 - ${e.message}")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "서버 연결 타임아웃: 서버 응답이 느립니다 - ${e.message}")
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "호스트를 찾을 수 없음: $SERVER_BASE_URL - ${e.message}")
        } catch (e: java.io.IOException) {
            Log.e(TAG, "네트워크 I/O 오류: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "대기열 확인 중 예상치 못한 오류: ${e.message}", e)
        }
    }
    
    /**
     * 대기열 응답 처리
     */
    private suspend fun processQueueResponse(response: String) {
        try {
            val jsonResponse = JSONObject(response)
            val status = jsonResponse.getString("status")
            
            if (status == "success") {
                val pendingItems = jsonResponse.getInt("pendingItems")
                Log.d(TAG, "대기 중인 인쇄 작업: ${pendingItems}개")
                
                if (pendingItems > 0) {
                    val items = jsonResponse.getJSONArray("items")
                    
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        processPrintItem(item)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "대기열 응답 처리 오류: ${e.message}")
        }
    }
    
    /**
     * 개별 인쇄 작업 처리
     */
    private suspend fun processPrintItem(item: JSONObject) {
        try {
            val printId = item.getString("id")
            val txHash = item.getString("transactionHash")
            val amount = item.getString("amount")
            val token = item.getString("token")
            val fromAddress = item.getString("fromAddress")
            val toAddress = item.getString("toAddress")
            val timestamp = item.getString("timestamp")
            
            // 상품명을 고정값으로 설정 (인코딩 문제 회피)
            val productName = "CUBE COFFEE"
            
            Log.i(TAG, "인쇄 작업 처리 시작 - ID: $printId, txHash: $txHash, 상품: $productName")
            
            // 인쇄 시작 상태로 업데이트
            updatePrintStatus(printId, "printing")
            
            // 영수증 데이터 생성
            val receiptData = ReceiptData(
                printId = printId,
                transactionHash = txHash,
                amount = amount,
                token = token,
                fromAddress = fromAddress,
                toAddress = toAddress,
                timestamp = timestamp,
                productName = productName
            )
            
            // 자동 인쇄 실행
            val printResult = autoPrintManager?.printReceipt(receiptData)
            
            // 인쇄 결과에 따라 상태 업데이트
            if (printResult == true) {
                updatePrintStatus(printId, "completed")
                Log.i(TAG, "인쇄 작업 완료 - ID: $printId")
            } else {
                updatePrintStatus(printId, "failed", "인쇄 실행 실패")
                Log.e(TAG, "인쇄 작업 실패 - ID: $printId")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "인쇄 작업 처리 오류: ${e.message}")
            val printId = item.optString("id", "unknown")
            updatePrintStatus(printId, "failed", e.message)
        }
    }
    
    /**
     * 서버에 인쇄 상태 업데이트
     */
    private suspend fun updatePrintStatus(printId: String, status: String, errorMessage: String? = null) {
        try {
            Log.d(TAG, "인쇄 상태 업데이트 시도: ID=$printId, 상태=$status")
            
            val url = URL("$SERVER_BASE_URL/api/receipt/status")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 15000
                
                // ngrok 요청에 필요한 헤더 추가
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Android-NDP-Printer/1.0")
                setRequestProperty("ngrok-skip-browser-warning", "true")
                
                doOutput = true
                useCaches = false
            }
            
            val requestBody = JSONObject().apply {
                put("printId", printId)
                put("status", status)
                if (errorMessage != null) {
                    put("errorMessage", errorMessage)
                }
            }
            
            Log.d(TAG, "전송 데이터: ${requestBody}")
            
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "상태 업데이트 응답: HTTP $responseCode (ID: $printId, 상태: $status)")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { 
                    it.readText() 
                }
                Log.d(TAG, "상태 업데이트 성공: $response")
            } else {
                val errorResponse = try {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream, StandardCharsets.UTF_8)).use { 
                        it.readText() 
                    }
                } catch (e: Exception) {
                    "Error stream not available"
                }
                Log.w(TAG, "상태 업데이트 실패: HTTP $responseCode, 오류: $errorResponse")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "상태 업데이트 오류: ${e.message}", e)
        }
    }
    
    /**
     * 폴링 상태 확인
     */
    fun isPollingActive(): Boolean = isPolling
    
    /**
     * 서버 연결 테스트
     */
    suspend fun testServerConnection(): Boolean {
        return try {
            Log.d(TAG, "서버 연결 테스트 시작: $SERVER_BASE_URL")
            
            val url = URL("$SERVER_BASE_URL/api/receipt/stats")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 8000 // 연결 타임아웃 증가
                readTimeout = 10000 // 읽기 타임아웃 증가
                
                // ngrok 요청에 필요한 헤더 추가
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Android-NDP-Printer/1.0")
                setRequestProperty("ngrok-skip-browser-warning", "true")
                
                useCaches = false
            }
            
            Log.d(TAG, "서버 연결 시도 중...")
            
            val responseCode = connection.responseCode
            Log.d(TAG, "서버 연결 테스트 응답: HTTP $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { 
                    it.readText() 
                }
                Log.d(TAG, "서버 연결 성공! 응답: $response")
                true
            } else {
                val errorResponse = try {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream, StandardCharsets.UTF_8)).use { 
                        it.readText() 
                    }
                } catch (e: Exception) {
                    "Error stream not available"
                }
                Log.w(TAG, "서버 연결 실패: HTTP $responseCode, 오류: $errorResponse")
                false
            }
            
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "서버 연결 실패: 서버에 연결할 수 없습니다 - ${e.message}")
            false
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "서버 연결 타임아웃: 서버 응답이 느립니다 - ${e.message}")
            false
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "호스트를 찾을 수 없음: $SERVER_BASE_URL - ${e.message}")
            false
        } catch (e: java.io.IOException) {
            Log.e(TAG, "네트워크 I/O 오류: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "서버 연결 테스트 예상치 못한 오류: ${e.message}", e)
            false
        }
    }
}