# 프린터 한글 인쇄 문제 해결 가이드

## 문제 상황
Android POS 시스템에서 영수증 프린터로 한글을 출력할 때 중국어로 출력되는 문제

## 원인 분석
1. **코드페이지 불일치**: 프린터의 코드페이지 설정이 잘못되어 있음
2. **인코딩 문제**: 문자열 인코딩과 프린터 설정이 일치하지 않음
3. **ESC/POS 명령어**: 한글 출력을 위한 올바른 명령어 사용 필요

## 해결 방법

### 1. 즉시 테스트 방법
앱에서 상태 메시지를 **길게 터치**하면 인코딩 테스트 메뉴가 나타납니다.

테스트 옵션:
1. **간단한 테스트**: 초기화 없이 바로 EUC-KR로 출력
2. **한국어 코드페이지 테스트**: 여러 한국어 코드페이지 조합 테스트
3. **직접 명령어 테스트**: ESC/POS 직접 명령어 테스트
4. **모든 코드페이지 테스트**: 모든 가능한 조합 (시간이 오래 걸림)
5. **기본 한글 테스트**: KoreanPrinterHelper 사용

### 2. 권장 설정

```kotlin
// 올바른 한글 출력 설정
val commands = mutableListOf<Byte>()

// 1. 프린터 초기화
commands.addAll(byteArrayOf(0x1B, 0x40).toList())  // ESC @

// 2. 코드페이지 설정 (중요!)
// 다음 중 하나를 사용:
commands.addAll(byteArrayOf(0x1B, 0x74, 0x25).toList())  // CP949/KS5601
// 또는
commands.addAll(byteArrayOf(0x1B, 0x74, 0x12).toList())  // 일부 프린터용

// 3. 문자열을 EUC-KR로 인코딩
val text = "한글 테스트"
val textBytes = text.toByteArray(Charset.forName("EUC-KR"))
commands.addAll(textBytes.toList())

// 4. 출력
printer.setBuffer(commands.toByteArray())
printer.print()
```

### 3. 문제 해결 체크리스트

✅ **코드페이지 확인**
- PrinterHelper.kt: `0x25` 사용 중
- KoreanPrinterHelper.kt: `0x12` 사용 중
- 프린터 모델에 따라 적절한 값 선택 필요

✅ **인코딩 확인**
- EUC-KR 인코딩 사용 권장
- CP949, MS949도 테스트 필요

✅ **프린터 모델별 차이**
- Epson: 보통 `0x25` (CP949)
- 일부 중국산: `0x12` 또는 `0x0D`

### 4. 테스트 결과 확인 방법

인코딩 테스트를 실행하면 각 설정마다 다음과 같은 내용이 출력됩니다:

```
=====================================
코드페이지: CP949/KS5601 (0x25)
값: 0x25
인코딩: EUC-KR
=====================================

[한글 인쇄 테스트]

가나다라마바사
아자차카타파하
한글 English 123

상품명          가격
김치찌개        8,000원
된장찌개        7,500원

합계: 15,500원
```

**정상적으로 한글이 출력되는 설정을 찾아서 사용하세요.**

### 5. 최종 수정 방법

정상 동작하는 설정을 찾으면:

1. `PrinterHelper.kt` 파일 열기
2. `getInitCommands()` 메서드에서 코드페이지 값 수정
3. `convertStringToBytes()` 메서드에서 인코딩 확인

예시:
```kotlin
private fun getInitCommands(): List<Byte> {
    return listOf(0x1B.toByte(), 0x40.toByte()) +  // 초기화
           listOf(0x1B.toByte(), 0x74.toByte(), 0x25.toByte())  // 올바른 코드페이지
}
```

## 추가 팁

1. **로그 확인**: Android Studio의 Logcat에서 "PrinterEncodingTester" 태그로 필터링
2. **프린터 매뉴얼**: 프린터 제조사의 ESC/POS 명령어 매뉴얼 확인
3. **시리얼 포트**: `/dev/ttyS4` 포트와 115200 보드레이트 확인

## 일반적인 코드페이지 값

| 코드페이지 | 값 (Hex) | 설명 |
|-----------|----------|------|
| CP949/KS5601 | 0x25 | 한국어 (권장) |
| Korean | 0x12 | 일부 프린터 |
| Korean | 0x0D | 대체 옵션 |
| User Defined | 0xFF | 사용자 정의 |

## 문제 지속 시

1. 프린터 펌웨어 버전 확인
2. 프린터 제조사에 한글 지원 문의
3. 프린터 드라이버/라이브러리 업데이트

---

작성일: 2024년
프로젝트: NDP POS System
