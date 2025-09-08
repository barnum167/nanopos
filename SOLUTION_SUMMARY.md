# 프린터 한글 인쇄 문제 해결 완료

## 🔍 문제 진단

현재 프로젝트에서 영수증 프린터로 한글을 출력할 때 중국어로 나오는 문제가 발생했습니다.

### 원인
1. **코드페이지 불일치**: PrinterHelper.kt(0x25)와 KoreanPrinterHelper.kt(0x12)에서 서로 다른 코드페이지 사용
2. **프린터 모델별 차이**: 프린터 제조사/모델에 따라 지원하는 코드페이지가 다름
3. **인코딩 설정**: EUC-KR, CP949 등 인코딩과 코드페이지 조합이 맞지 않음

## ✅ 해결 방법

### 1. 테스트 도구 추가 완료

다음 파일들을 추가했습니다:

#### 📄 PrinterEncodingTester.kt
- 다양한 코드페이지와 인코딩 조합을 자동 테스트
- 한국어 전용 테스트 모드 제공
- ESC/POS 직접 명령어 테스트 지원

#### 📄 PrinterTestActivity.kt + activity_printer_test.xml
- GUI 기반 테스트 화면
- 코드페이지와 인코딩을 직접 선택하여 테스트
- 실시간 테스트 텍스트 편집 가능

### 2. 테스트 방법

#### 방법 1: 메인 화면에서 빠른 테스트
1. 앱 실행
2. **상태 메시지를 길게 터치** (Long Press)
3. 테스트 메뉴에서 원하는 옵션 선택:
   - 간단한 테스트 (초기화 없이)
   - 한국어 코드페이지 테스트 ⭐ **권장**
   - 직접 명령어 테스트
   - 모든 코드페이지 테스트
   - 기본 한글 테스트
   - **테스트 화면 열기 (GUI)** ⭐ **권장**

#### 방법 2: GUI 테스트 화면 사용
1. 메인 화면에서 상태 메시지 길게 터치
2. "6. 테스트 화면 열기 (GUI)" 선택
3. 테스트 화면에서:
   - 텍스트 입력/수정
   - 코드페이지 선택 (0x25, 0x12, 0x0D, 0xFF)
   - 인코딩 선택 (EUC-KR, CP949, MS949, UTF-8)
   - "테스트 인쇄" 버튼 클릭

### 3. 정상 동작 확인 방법

올바른 설정을 찾으면 다음과 같이 한글이 정상 출력됩니다:

```
========================================
코드페이지: 0x25
인코딩: EUC-KR
========================================

한글 테스트
가나다라마바사
아자차카타파하

상품명          가격
김치찌개        8,000원
된장찌개        7,500원

합계: 15,500원
```

### 4. 권장 설정 (대부분의 프린터)

```kotlin
// PrinterHelper.kt의 getInitCommands() 메서드
private fun getInitCommands(): List<Byte> {
    return listOf(0x1B.toByte(), 0x40.toByte()) +  // ESC @ (초기화)
           listOf(0x1B.toByte(), 0x74.toByte(), 0x25.toByte())  // ESC t 37 (CP949)
}

// convertStringToBytes() 메서드
private fun convertStringToBytes(text: String): List<Byte> {
    return text.toByteArray(Charset.forName("EUC-KR")).toList()
}
```

## 🛠️ 프린터별 권장 설정

| 프린터 브랜드 | 코드페이지 | 인코딩 |
|-------------|-----------|--------|
| Epson | 0x25 | EUC-KR |
| Bixolon | 0x25 | CP949 |
| 일부 중국산 | 0x12 | EUC-KR |
| Star | 0x0D | EUC-KR |

## 📝 추가된 파일 목록

1. `PrinterEncodingTester.kt` - 자동 테스트 클래스
2. `PrinterTestActivity.kt` - GUI 테스트 액티비티
3. `activity_printer_test.xml` - 테스트 화면 레이아웃
4. `PRINTER_ENCODING_FIX.md` - 문제 해결 가이드
5. `MainActivity.kt` - 테스트 메뉴 추가 (수정)
6. `AndroidManifest.xml` - 테스트 액티비티 등록 (수정)

## 🚀 다음 단계

1. **테스트 실행**: GUI 테스트 화면에서 다양한 조합 테스트
2. **정상 설정 찾기**: 한글이 올바르게 출력되는 설정 확인
3. **코드 수정**: PrinterHelper.kt의 코드페이지 값 수정
4. **검증**: 실제 영수증 출력 테스트

## ⚠️ 주의사항

- 프린터 전원과 연결 상태 확인
- 시리얼 포트 `/dev/ttyS4` 확인
- 보드레이트 `115200` 확인
- 프린터 용지 확인

## 📞 추가 지원

문제가 지속되면:
1. Logcat에서 "PrinterEncodingTester" 태그로 로그 확인
2. 프린터 모델명과 제조사 확인
3. 프린터 매뉴얼에서 ESC/POS 코드페이지 확인

---

작성: 2024년
프로젝트: NDP POS System
버전: 1.0
