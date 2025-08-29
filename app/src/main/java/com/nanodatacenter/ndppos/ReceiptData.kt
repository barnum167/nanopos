package com.nanodatacenter.ndppos

/**
 * 영수증 데이터 클래스
 * 서버에서 받아온 인쇄 작업 정보를 담는 데이터 클래스입니다.
 */
data class ReceiptData(
    val printId: String,           // 인쇄 작업 고유 ID
    val transactionHash: String,   // 트랜잭션 해시
    val amount: String,            // 거래 금액
    val token: String,             // 토큰 종류
    val fromAddress: String,       // 보내는 주소
    val toAddress: String,         // 받는 주소
    val timestamp: String,         // 타임스탬프
    val productName: String? = "아메리카노"  // 상품명 (기본값: 아메리카노)
)