# kotlinx.serialization 은 라이브러리에 R8 규칙이 내장되어 있어 별도 keep 이 필요 없다.
# 저장 데이터(GameState)의 enum 이름이 난독화되면 기존 세이브를 읽지 못하므로 이름만 보존한다.
-keepclassmembers enum kr.paranos.stolenlife.data.** {
    <fields>;
}
