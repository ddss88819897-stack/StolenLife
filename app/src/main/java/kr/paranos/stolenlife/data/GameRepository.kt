package kr.paranos.stolenlife.data

/**
 * 이야기 데이터 전체.
 *
 * 웹 프로토타입(stolen_life_v2_pro.html)의 DOCS · PROFILES · SEARCH · SAYS · SPAM · LOCKS · PAIRS ·
 * story() 대본 · 챕터 대본 · 엔딩 대본 · hint() 를 한 글자도 줄이지 않고 옮겼다.
 * 상태를 바꾸는 로직은 없다. 여기 있는 것은 전부 불변 값이고, 판정은 GameState.meets 가 한다.
 *
 * 문서 묶음을 챕터별 private 함수로 나눈 것은 가독성 때문이기도 하지만,
 * 하나의 정적 초기화 블록이 JVM 메서드 크기 한도(64KB)에 걸리지 않게 하려는 이유도 있다.
 */
object GameRepository {

    // ── 문서와 챕터 대본이 함께 쓰는 독백 ───────────────────────────────────
    private val NEWS_PARK_THOUGHTS = listOf("포위됐다, 강민우. 드디어.", "…그런데 왜 내 심장이 이렇게 뛰지?")

    // ═════════════════════════════════════════════════════════════════════════
    //  문서 (38건)
    // ═════════════════════════════════════════════════════════════════════════

    val documents: Map<DocId, Document> = (
        chapterOneDocuments() +
            chapterTwoDocuments() +
            chapterThreeDocuments() +
            paranDocuments() +
            searchDummies()
        ).associateBy { it.id }

    fun document(id: DocId): Document = documents.getValue(id)

    private fun chapterOneDocuments(): List<Document> = listOf(
        Document(
            id = DocId.NOTICE,
            title = "[공고 제2009-412호] 의료법인 해송정신요양원 폐원 공고",
            source = "보건복지부 고시·공고 · 2009.06.30",
            snippet = "의료법 제40조에 따라 아래 의료기관의 폐원을 공고합니다. 소재지 고흥군 월포읍…",
            style = DocStyle.GOV,
            body = body {
                p("의료법 제40조에 따라 아래 의료기관의 폐원을 공고합니다.")
                table {
                    row("기관명", "의료법인 해송정신요양원")
                    row("소재지") { +"전라남도 고흥군 "; chip(Chip.WOLPO_EUP); +" 해안로 77" }
                    row("대표자") { +"원장 "; chip(Chip.OH_BYEONGCHEOL) }
                    row("법인 정보") { pin(PinId.CORP_NOTICE, "법인번호 486-82-00119") }
                    row("폐원 사유", "환자 인권침해 민원 누적에 따른 자진 폐원")
                    row("환자 처리") {
                        +"입원 환자 전원 타 기관 이송 완료"; br()
                        +"(단, 장기 입원 1명은 '보호자 인계' 처리)"
                    }
                }
                p(align = Align.CENTER) { +"2009년 6월 30일"; br(); +"보건복지부장관" }
            },
            monologue = lines("보호자 인계. 그 1명이 나다.", "그 보호자라는 놈이 날 20년 동안 꺼내 주지 않았다."),
        ),
        Document(
            id = DocId.POWER_CONTRACT,
            title = "전력수급 계약서 (사진)",
            source = "FaceBook 95 · 오병철 · 2026.10.28",
            style = DocStyle.GOV,
            body = body {
                figure(Picture.CONTRACT, "첨부 사진 · 화질 저하")
                table {
                    row("공급자", "한빛전력 고흥지사")
                    row("계약자", "월포수산(주) 대표 오병철")
                    row("사업자 정보") { pin(PinId.CORP_POWER, "사업자 법인번호 486-82-00119") }
                    row("설치 장소", "고흥군 월포읍 해안로 77 제3냉동창고")
                    row("증설 내역", "냉동설비 30kW / 음향설비(스피커) 24시간 상시전원")
                }
            },
            monologue = lines(
                "해안로 77. 냉동창고에 스피커 24시간 상시전원.",
                "…천장에서 쏟아지던 그 목소리의 전기세를 이 영감이 냈군.",
            ),
        ),
        Document(
            id = DocId.SETTLEMENT,
            title = "[0419호 장기 위탁 정산서] 2006.11 ~ 2026.10",
            source = "오병철 자료실 · 웹하드",
            style = DocStyle.LOG,
            logLine = "정산서 열람 (오병철 웹하드)",
            body = body {
                table {
                    row("피험자") { pin(PinId.SUBJECT_NO, "수감번호 0419호") }
                    row("실명") { redacted("강민우 본인 (760419-1******)") }
                    row("위탁 형태", "비공개 장기 격리 (의뢰인 요청 · 신원 비공개)")
                    row("위탁료") { +"월 12,000,000원 × 240개월"; br(); +"= 2,880,000,000원" }
                    row("입금자") { +"(재)"; chip(Chip.MUJEOK_FOUNDATION); +" 이사장 "; chip(Chip.KANG_MINWOO) }
                    row("처치 ①", "2006.11~2007.03 ECT 12회 (기존 기억 맥락 흐림)")
                    row("처치 ②") {
                        +"2007.04~현재 음성 자료 1일 18시간 반복 재생"; br()
                        +"재생 자료: "; redacted("2005년 본인 녹음 라디오 참회록 《무적》")
                    }
                    row("비고") { redacted("의뢰인 지시 — 피험자를 반드시 '한도윤'으로 호칭할 것") }
                }
            },
            monologue = linesWhen(
                Cond.ProxyOff,
                then = listOf("실명: 강민우 본인.", "나를 한도윤이라고 부르라는 지시까지. 20년 동안 나는 남의 이름으로 불렸다."),
                otherwise = listOf(
                    "28억 8천만 원. 날 가두는 데 쓴 돈.",
                    "강민우. 네가 훔친 내 인생으로 번 돈이 그거였나.",
                    "실명 칸만 까맣다. 프록시 손상… 하필 이름만.",
                ),
            ),
        ),
    )

    private fun chapterTwoDocuments(): List<Document> = listOf(
        Document(
            id = DocId.WIKI_KANG,
            title = "강민우 (작가) - 파란백과",
            source = "파란백과 · 최종 수정 2026.11.02",
            snippet = "대한민국의 소설가. 2005년 장편 《무적》으로 데뷔. 무적문화재단 이사장…",
            style = DocStyle.WIKI,
            body = body {
                table {
                    row("출생") { pin(PinId.KANG_BIRTH, "1976년 4월 19일 (50세)") }
                    row("데뷔") { +"2005년 장편 "; chip(Chip.MUJEOK_NOVEL); +"(霧笛) — 밀리언셀러" }
                    row("출신") {
                        +"1998년 천리안 문학 CUG "; chip(Chip.PARANNURI); +" (ID "; chip(Chip.MW419); +")"
                    }
                    row("배우자") { +"화가 "; chip(Chip.SEO_HARIN) }
                    row("직함", "(재)무적문화재단 이사장 (2010~)")
                }
                p {
                    bold("생애")
                    +" — '얼굴 없는 작가'로 불리며 2005년 심야 라디오 낭독으로 유명세를 얻었다. 2006년 11월 3일 "
                    chip(Chip.GAPYEONG_CRASH)
                    +"로 안면과 성대에 중화상을 입고 14개월간 투병했다. 복귀 후에는 낭독과 방송 출연을 일절 하지 않는다."
                }
                p {
                    bold("논란")
                    +" — 2005년 무명 작가 지망생 한모 씨가 표절을 주장했으나 법원은 기각했다. 한씨는 이후 정신요양원에 입원했다."
                }
            },
            monologue = lines("1976년 4월 19일생. …0419.", "내 수감번호가 이 자식 생일이었다고? 끝까지 날 조롱하는군."),
        ),
        Document(
            id = DocId.FOUNDATION,
            title = "(재)무적문화재단 — 한 사람의 고통이 모두의 위로가 되기를",
            source = "mujeok.or.kr · 재단 소개",
            snippet = "이사장 인사말, 등대 장학사업, 청년 작가 창작 지원…",
            style = DocStyle.NEWS,
            body = body {
                figure(Picture.PODIUM, "이사장 강민우 · 2026 창작지원 10주년 기념식")
                p { +"\"한 사람의 고통이 모두의 위로가 되기를 바랍니다.\" — 이사장 "; chip(Chip.KANG_MINWOO) }
                bullets {
                    item { chip(Chip.BAEKRODO); +" 등대 장학금 (도서 지역 청소년)" }
                    item("청년 작가 창작 지원금")
                    item("오디오북 《무적》 낭독본 복각 사업")
                }
                p { +"감사: 회계팀장 "; chip(Chip.PARK_TAESEONG); +" · 소재지: 서울 종로구" }
            },
            monologue = lines(
                "남의 고통을 팔아 재단을 세웠다.",
                "그것도 '등대' 장학금이라니. 뻔뻔함에도 재능이 필요한 법이다.",
            ),
        ),
        Document(
            id = DocId.GAPYEONG,
            title = "작가 강민우 차량, 가평 국도서 전소… 본인 중화상",
            source = "파란일보 · 2006.11.04",
            snippet = "3일 밤 11시 40분께 가평군 국도에서 작가 강민우(30)씨의 승용차가…",
            style = DocStyle.NEWS,
            body = body {
                p {
                    chip(Chip.DATE_2006_11_03, "3일")
                    +" 밤 11시 40분께 경기 가평군의 한 국도에서 작가 강민우(30)씨의 승용차가 중앙선을 넘어 화물차와 충돌한 뒤 불탔다."
                }
                p("강씨는 안면과 성대에 3도 화상을 입고 중환자실로 이송됐다. 동승자와 사망자는 없어 별도의 검시는 이뤄지지 않았다. 경찰은 졸음운전에 무게를 두고 있다.")
                p("한편 같은 날 밤, 전남 고흥의 한 요양원에서 장기 입원 환자 1명이 이송 도중 사라졌다가 이튿날 새벽 다시 수용됐다.")
            },
            monologue = lines("2006년 11월 3일. 내가 '다시 붙잡혀' 들어간 날이다.", "강민우가 불타던 바로 그 밤에."),
        ),
        Document(
            id = DocId.RADIO_2005,
            title = "얼굴 없는 작가 강민우, 심야 라디오서 《무적》 눈물의 낭독",
            source = "파란일보 문화면 · 2005.04.22",
            snippet = "MBS 라디오 '새벽 세 시'에서 장편 《무적》 전편을 직접 낭독… 방송 중 눈물",
            style = DocStyle.NEWS,
            body = body {
                p {
                    +"얼굴을 공개하지 않는 작가 "
                    chip(Chip.KANG_MINWOO)
                    +"가 MBS 라디오 '새벽 세 시'에서 장편 《무적》 전편을 직접 낭독했다. 그는 등대 화재 장면에서 목이 메어 낭독을 멈췄다."
                }
                p("방송사는 낭독본을 카세트 오디오북으로 한정 발매했다. \"이 이야기는 내 몸에 새겨진 기억\"이라는 그의 말이 화제가 됐다.")
            },
            monologue = lines("그 목소리를 안다. 스무 해 동안 천장에서 쏟아지던 목소리.", "내 이야기를 제 것처럼 읽던 도둑의 목소리."),
        ),
        Document(
            id = DocId.SOYUN_NEWS,
            title = "작가 지망생 여동생 투신… 오빠 \"베스트셀러 《무적》은 내 원고\"",
            source = "파란일보 사회면 · 2005.09.03",
            snippet = "한모(28)씨의 여동생 한소윤(22)씨가 2일 서울의 한 건물 옥상에서…",
            style = DocStyle.NEWS,
            body = body {
                p {
                    +"표절 소송에서 패소한 작가 지망생 한모(28)씨의 여동생 "
                    chip(Chip.HAN_SOYUN)
                    +"(22)씨가 2일 서울의 한 건물 옥상에서 투신해 숨졌다."
                }
                p("한씨는 \"《무적》은 백로도에서 자란 우리 남매의 이야기\"라고 주장해 왔다. 출판사는 \"근거 없는 주장\"이라고 일축했다.")
            },
            monologue = lines("소윤아.", "…아무도 우리 말을 믿지 않았다. 그래서 너는 옥상에 올라갔다."),
        ),
        Document(
            id = DocId.CY_SOYUN,
            title = "♡소윤이의 미니홈피♡ (휴면 백업)",
            source = "cyworld 휴면계정 백업 · 2005",
            snippet = "오빠 이야기가 서점에 있었다. 표지에는 다른 사람 이름…",
            style = DocStyle.CY,
            body = body {
                meta("TODAY 0 | TOTAL 4,817 · BGM ♪ 섬집 아기 (오르골)")
                guestbook("2005.03.15 다이어리") {
                    +"서점에 갔는데 오빠 이야기가 있었다. 표지에는 강민우라는 이름. "
                    chip(Chip.MW419)
                    +". 오빠가 제일 믿던 사람."
                }
                guestbook("2005.08.30 다이어리") {
                    +"오빠는 오늘도 출판사 앞에 서 있었다. 다들 오빠가 미쳤대. 오빠는 왼손으로 피켓을 들고 있었다."
                }
                guestbook("방명록 · 비밀글") { +"오빠, 미안해. 등대 불 끄고 갈게. (2005.09.02)" }
            },
            monologue = lines("등대 불 끄고 갈게…", "이 문장, 어디서 봤더라. 아주 최근에."),
        ),
        Document(
            id = DocId.DIARY,
            title = "8년째의 공범 (2014. 11. 03 작성)",
            source = "하린의 서랍 · 비공개 클라우드",
            style = DocStyle.DIARY,
            logLine = "서하린 비공개 클라우드 열람",
            body = body {
                p("2008년 겨울, 화상 붕대를 풀고 돌아온 남자가 왼손으로 커피잔을 쥐었을 때 나는 그 사람이 민우 씨가 아니라는 걸 단번에 알았다.")
                p("하지만 내가 경찰에 신고하려던 날 밤, 그 남자는 내 식탁 앞에 앉아 나직하게 물었다.")
                p {
                    italic("\"하린아, 2006년 11월 3일 밤에 네가 내 홍차에 갈아 넣은 졸피뎀이 몇 알이었지? 운전대 잡기 전에 네가 그 잔을 건네면서 무슨 말을 했더라?\"")
                }
                p("심장이 바닥으로 떨어졌다. 그날 밤 민우 씨의 폭언과 표절 히스테리가 너무 두려워 수면유도제 네 알을 넣었던 건 세상에 나와 민우 씨 단 둘만 아는 비밀이었다. 민우 씨가 가평 별장 수첩에 그걸 적어두었을 줄은 몰랐다.")
                p {
                    +"남자는 미소조차 짓지 않고 말했다. "
                    italic("\"당신 남편은 그날 밤 당신이 재운 거야. 살인자가 되기 싫으면 우린 끝까지 부부로 살아야지.\"")
                }
                p("나는 감옥에 가지 않기 위해, 내 남편의 이름을 뒤집어쓴 유령의 아내가 되기로 했다.")
                p(ruledAbove = true) {
                    bold("[관찰 기록]"); br()
                    +"· "; pin(PinId.DIARY_BEFORE, "사고 전 강민우: 오른손잡이, 커피 설탕 두 스푼"); br()
                    +"· 돌아온 남자: 왼손잡이, 블랙커피, 왼쪽 귀 뒤 오래된 화상 반흔"
                }
            },
            monologue = lines(
                "홍차에 수면제를 탔다고…? 잠깐만.",
                "그날 밤 가평 국도에서 쏟아지던 졸음, 혀끝에 감돌던 쓰디쓴 홍차 맛, 중앙선을 넘어 달려들던 트럭 헤드라이트…",
                "왜 한도윤이 아니라 '운전석에 앉아 있던 강민우'의 시야가 내 머릿속에서 떠오르는 거지?",
            ),
        ),
        Document(
            id = DocId.BODYLOG,
            title = "현재 상태 로그 #1 — 파란여관 203호",
            source = "ParanOS 98 · 생체 행동 기록",
            style = DocStyle.LOG,
            body = body {
                p { +"["; stamp(ClockStamp.COFFEE, "23:59"); +"] 탁자 위 믹스커피 1봉 개봉" }
                p {
                    +"["; stamp(ClockStamp.COFFEE, "23:59"); +"] 동작 기록: "
                    pin(PinId.BODYLOG_COFFEE, "오른손으로 설탕 두 스푼을 저음")
                }
                p { +"["; stamp(ClockStamp.COFFEE, "23:59"); +"] 커피잔 접촉 · 음용 (타액 잔류 가능)" }
            },
            monologue = lines(
                "손이 먼저 움직인다. 믹스커피 봉지를 뜯고, 설탕 두 스푼.",
                "오른손으로 젓는다. 20년 만의 단맛.",
                "…생각하기도 전에 손이 먼저 알았다.",
            ),
        ),
        Document(
            id = DocId.BINDER,
            title = "[특별감사 바인더 원본] 무적문화재단",
            source = "작성: 회계팀장 박태성 · 2026.10.30 · 203호 환풍구",
            style = DocStyle.BINDER,
            logLine = "감사 바인더 원본 열람 (전 페이지 접촉)",
            body = body {
                p {
                    bold("1. 비자금")
                    +" — 2019~2026년 총 184억 원. "
                    chip(Chip.WOLPO_SUSAN)
                    +"(주) 등 유령법인 3곳으로 분할 송금."
                }
                p {
                    bold("2. 위탁료")
                    +" — 월포수산 → '0419호' 위탁료 월 1,200만 원 (2006.11~)."
                }
                p {
                    bold("3. 이사장 신원 불일치")
                    +" — 2008년 이후 이사장 "
                    chip(Chip.KANG_MINWOO)
                    +"의 필적·치열이 2005년 이전 강민우 기록과 불일치."; br()
                    +"비교 결과: "
                    redacted("0419호(강민우 본인)의 치과 기록과 2005년 강민우의 사랑니 발치 기록 100% 일치. 현재 이사장은 사칭범(한도윤).")
                }
                p {
                    bold("4. 비자금 서버")
                    +" — "
                    chip(Chip.VAULT95)
                    +" (Vault 95). 접속 시 하드웨어 OTP 필수. 마스터키는 이사장이 '첫 원고 등록일'이라 부르는 8자리 숫자."
                }
                p {
                    bold("5. 보관")
                    +" — "
                    redacted {
                        +"본 바인더 원본은 파란여관 203호 환풍구에 은닉함. 이사장 개인 계정: "
                        chip(Chip.LIGHTHOUSE_1998)
                        +" (구 필명 등대지기)"
                    }
                }
                p(align = Align.END) { +"— "; chip(Chip.PARK_TAESEONG) }
            },
            monologue = linesWhen(
                Cond.ProxyOff,
                then = listOf("0419호의 치과 기록과 2005년 강민우의 사랑니 기록이 일치한다.", "…내 오른쪽 아래, 뽑힌 사랑니 자리."),
                otherwise = listOf("박태성이라는 회계쟁이가 모은 자료. 페이지마다 손때가 묻는다. 이제 내 손때도.", "이번에도 이름만 지워져 있다."),
            ),
        ),
        Document(
            id = DocId.VAULT_SUMMARY,
            title = "[감사 요약본 · 자동 백업] VAULT 95",
            source = "금고95 · 회계팀장 박태성 계정",
            style = DocStyle.VAULT,
            logLine = "금고95 접속 (비자금 서버)",
            body = body {
                p("비자금 총액: 18,412,000,000원")
                p("월포수산(주) → 0419호 위탁료: 2006.11~ 매월 12,000,000원")
                p("이사장 필적 대조: 불일치 (2005 vs 2026)")
                p { +"치과 기록 대조 대상: "; redacted("0419호 = 강민우 본인 (우측 하악 사랑니 X-ray 일치)") }
                p("마지막 메모 (10.31 21:40): \"이사장이 직접 만나자고 한다. 장소: 파란여관.\"")
            },
            monologue = lines("184억.", "…마지막 메모. 장소: 파란여관.", "여기다. 박태성은 사흘 전 이 여관으로 왔다."),
        ),
    )

    private fun chapterThreeDocuments(): List<Document> = listOf(
        Document(
            id = DocId.NEWS_PARK,
            title = "[속보] 월포항 변사체는 무적문화재단 회계팀장 박태성… 이사장 강민우 체포영장",
            source = "파란뉴스 속보 · 2026.11.03 23:58",
            snippet = "경찰, 이사장 강민우 긴급 체포영장 발부 및 은신처 위치 추적 중",
            style = DocStyle.NEWS,
            body = body {
                p("어제(2일) 월포항에서 인양된 변사체는 무적문화재단 회계팀장 박태성(45)씨로 확인됐다.")
                p("경찰청 중대범죄수사과는 박씨가 10월 31일 밤 월포읍 일대에서 마지막으로 목격된 점, 재단 비자금 184억 원 의혹을 조사하던 중이었던 점에 주목하고 있다.")
                p("경찰은 이사장 강민우(50)씨에 대해 살인 및 특정경제범죄 혐의로 긴급 체포영장을 발부받고 은신처 위치를 추적 중이다.")
            },
            monologue = Branch.Fixed(NEWS_PARK_THOUGHTS),
        ),
        Document(
            id = DocId.BURN_BLOG,
            title = "잊혀진 섬, 백로도 등대 화재 (1998.2.17)",
            source = "월포 향토사 연구회 블로그 · 2019.02.17",
            snippet = "첨부: 당시 생존자 화상 진료 기록 스캔",
            style = DocStyle.BLOG,
            body = body {
                p("1998년 2월 17일 새벽, 백로도 등대에 불이 났다. 등대지기 한씨가 숨졌고, 아들과 딸이 살아남았다.")
                figure(Picture.SCAN, "첨부 · 고흥의료원 화상센터 진료기록 (1998.02.18)")
                table {
                    row("환자", "한도윤 (남, 20세)")
                    row("보호자", "동생 한소윤 (15세)")
                    row("특이사항") { pin(PinId.BURN_RECORD, "한도윤: 선천적 왼손잡이, 좌측 귀 뒤 6cm 영구 화상 반흔") }
                }
            },
            monologue = lines("한도윤. 나다.", "…나여야 한다."),
        ),
        Document(
            id = DocId.MIRRORLOG,
            title = "현재 상태 로그 #2 — 거울",
            source = "ParanOS 98 · 생체 행동 기록",
            style = DocStyle.LOG,
            logLine = "거울 관찰 (신체 상태)",
            body = body {
                p { +"["; stamp(ClockStamp.MIRROR, "00:20"); +"] 대상: 0419호" }
                p { pin(PinId.MIRROR_LOG, "우세손: 오른손 / 좌측 귀 뒤: 흉터 전혀 없음 / 우측 하악: 사랑니 발치 흔적") }
            },
            monologue = lines("귀 뒤를 더듬는다. 매끈하다.", "등대 불길이 남겼어야 할 흉터가… 없다."),
        ),
    )

    private fun paranDocuments(): List<Document> = listOf(
        Document(
            id = DocId.PARAN_LH,
            title = "내 섬의 무적 (초고 1부)",
            source = "[창작마당] 등대지기 · 98/05/11 02:14",
            style = DocStyle.PARAN,
            body = body {
                meta {
                    +"글쓴이: "; chip(Chip.LIGHTHOUSE_KEEPER)
                    +"  등록: "; chip(Chip.DATE_1998_05_11, "98/05/11")
                    +"  조회: 41"
                }
                p {
                    chip(Chip.BAEKRODO)
                    +"에는 등대가 하나 있었다. 안개가 내리면 아버지는 무적을 울렸다. 소윤이는 그 소리를 고래 울음이라고 불렀다."
                }
                p("올해 2월, 등대에 불이 났다. 아버지는 나오지 못했다. 나는 소윤이를 업고 계단을 뛰어 내려왔다. 왼쪽 귀 뒤가 아직 붕대 아래서 뜨겁다.")
                p("이 글은 왼손으로 쓴다. 오른손은 원래 쓸 줄 모른다. 이건 소설이 아니라 참회록이다. 그날 밤 등대 불을 끄지 않은 건 나였으니까.")
                p("— 등대 불 끄고 갑니다.")
                meta("┌ 댓글 2", spacedAbove = true)
                p {
                    +"["; chip(Chip.MW419)
                    +"] 어쩌면, 아니 분명히 이 글은 책이 될 겁니다. 다음 편도 꼭 올려 주세요. 메일 남깁니다."
                }
                p("[달빛소나타] ㅠㅠ 새벽에 이거 읽고 울었어요")
            },
            monologue = lines(
                "1998년 5월 11일. 내 첫 원고.",
                "그 밑에 거머리처럼 붙은 댓글, MW419.",
                "…왼손으로 썼다고. 그래, 나는 왼손으로 썼다. 그랬을 거다.",
            ),
        ),
        Document(
            id = DocId.PARAN_MW,
            title = "습작 — 서울의 밤",
            source = "[창작마당] MW419 · 98/04/30",
            style = DocStyle.PARAN,
            body = body {
                p {
                    +"서울의 밤은 차갑다"; br()
                    +"나는 식은 커피에 설탕을 두 스푼 넣는다"; br()
                    +"그래도 쓰다"; br()
                    +"내 이야기가 없어서 쓰다"
                }
                meta("┌ 댓글 0")
            },
            monologue = lines("MW419의 습작. 형편없다.", "내 이야기가 없어서 쓰다… 그래서 남의 이야기를 훔쳤나."),
        ),
        Document(
            id = DocId.PARAN_JOKE,
            title = "[펌] 사오정 시리즈 98탄",
            source = "[유머방] 깡통로봇 · 98/05/09",
            style = DocStyle.PARAN,
            body = body {
                p {
                    +"선생님: 사오정, 넌 커서 뭐가 될래?"; br()
                    +"사오정: 네! 저는 커서가 깜빡이는 사람이 될래요!"; br()
                    +"선생님: ……"
                }
                p("ㅋㅋㅋ 퍼가실 때 출처 꼭 ^^;")
            },
            monologue = lines("1998년의 유머. 그때는 이게 웃겼다.", "…나는 웃었던 기억이 없다."),
        ),
        Document(
            id = DocId.PARAN_IMF,
            title = "[유머] IMF 시대 신(新) 직업 BEST 5",
            source = "[유머방] 명퇴아빠 · 98/03/02",
            style = DocStyle.PARAN,
            body = body {
                p {
                    +"5위 금 모으기 감별사"; br()
                    +"4위 명예퇴직 축사 대필"; br()
                    +"3위 PC방 야간 지킴이"; br()
                    +"2위 남의 이력서 대신 써 주기"; br()
                    +"1위 남의 인생 대신 살아 주기 (고수익 보장)"
                }
            },
            monologue = lines("남의 인생 대신 살아 주기. 고수익 보장.", "농담치고는 잘 맞았다."),
        ),
        Document(
            id = DocId.PARAN_NOTICE,
            title = "[공지] 5월 정모 안내 — 신촌 파랑새 다방",
            source = "[공지사항] 시삽 · 98/05/15",
            style = DocStyle.PARAN,
            body = body {
                p {
                    +"일시: 98년 5월 23일(토) 오후 3시"; br()
                    +"장소: 신촌 파랑새 다방 2층"; br()
                    +"참석: 등대지기, MW419, 달빛소나타 외"
                }
                p("※ 등대지기님 원고 낭독회 있습니다!")
            },
            monologue = lines(
                "파랑새 다방. 거기서 MW419를 처음 만났다.",
                "그 자식은 내 원고를 받아 적으며 커피에 설탕을 두 스푼 넣었다.",
            ),
        ),
    )

    private fun searchDummies(): List<Document> = listOf(
        Document(
            id = DocId.D_HOE,
            title = "해송횟집 ★4.1 · 월포항 맛집 리뷰",
            source = "파란지도 리뷰 · 2026.10",
            snippet = "사장님 손이 커요 ㅋㅋ 근데 뒤편 창고에서 밤마다…",
            style = DocStyle.NEWS,
            body = body {
                p("★★★★☆ 사장님 손이 커요 ㅋㅋ 광어 두께 실화?")
                p("★★★☆☆ 맛은 좋은데 뒤편 냉동창고에서 밤마다 라디오 소리가 나요. 낭독 같은 거. 좀 무서움;;")
                p("★★★★★ 월포 오면 무조건 여기!")
            },
            monologue = lines("밤마다 들리는 낭독 소리.", "…그건 라디오가 아니었다. 나였다. 나를 고문하던 소리였다."),
        ),
        Document(
            id = DocId.D_419,
            title = "4·19 혁명 - 파란백과",
            source = "파란백과",
            snippet = "1960년 4월 19일 학생과 시민이 중심이 된 민주주의 혁명…",
            style = DocStyle.WIKI,
            body = body { p("1960년 4월 19일, 학생과 시민이 중심이 되어 일어난 민주주의 혁명이다.") },
            monologue = lines("0419. 내 번호가 혁명 기념일이라니.", "혁명은커녕 20년 동안 화장실 갈 시간도 고르지 못했다."),
        ),
        Document(
            id = DocId.D_SPAM_SILVER,
            title = "★축★ 해송 실버타운 분양!! 평생 안 나가도 되는 집!!",
            source = "광고 · spam",
            snippet = "★선착순★ 입주 즉시 평생 관리 / 보호자 걱정 끝!",
            style = DocStyle.SPAM,
            body = body {
                p {
                    +"★★★ 평생 안 나가셔도 됩니다 ★★★"; br()
                    +"입주 즉시 24시간 관리 / 보호자 걱정 끝!"; br()
                    +"TEL 수신거부: 0419"
                }
            },
            monologue = lines("평생 안 나가도 되는 집. 웃기지도 않는다."),
        ),
        Document(
            id = DocId.D_OH_NEWS,
            title = "해송요양원 인권침해 의혹… 원장 \"사실무근\"",
            source = "남도일보 · 2009.05.11",
            snippet = "전직 직원 \"독방 환자에게 하루 종일 녹음테이프를 틀어\"…",
            style = DocStyle.NEWS,
            body = body {
                p("전직 직원 A씨는 \"독방 환자에게 하루 종일 녹음테이프를 틀어 놓았다\"고 주장했다. 오병철 원장은 \"치료 목적의 음악 요법\"이라고 해명했다.")
            },
            monologue = lines("음악 요법. 그 테이프에서 음악이 나온 적은 한 번도 없었다."),
        ),
        Document(
            id = DocId.D_WOLPO_TOUR,
            title = "[월포읍] 가을 전어 축제 & 해안로 산책길",
            source = "고흥군 관광 안내",
            snippet = "해안로 77번지 일대 옛 요양원 부지는 출입 제한…",
            style = DocStyle.NEWS,
            body = body {
                p("11월 전어 축제가 열립니다. 해안로 산책길을 따라 월포항 야경을 즐겨 보세요.")
                p("※ 해안로 77번지 일대(옛 요양원 부지, 현 수산 창고)는 사유지로 출입이 제한됩니다.")
            },
            monologue = lines("야경 좋네. 20년 동안 창문 한 번 못 봤는데."),
        ),
        Document(
            id = DocId.D_WOLPOSUSAN,
            title = "월포수산(주) - 기업정보",
            source = "파란 기업정보",
            snippet = "대표 오병철 · 냉동수산물 도매 · 직원 2명 · 매출 미공개",
            style = DocStyle.WIKI,
            body = body {
                table {
                    row("대표") { chip(Chip.OH_BYEONGCHEOL) }
                    row("업종", "냉동수산물 도매")
                    row("직원", "2명")
                    row("매출", "미공개")
                }
            },
            monologue = lines("직원 2명짜리 수산회사가 냉동창고에 스피커를 단다."),
        ),
        Document(
            id = DocId.D_FOUND_JOB,
            title = "[채용] 무적문화재단 회계팀 경력직 긴급 채용",
            source = "파란잡 · 2026.11.03 등록",
            snippet = "회계팀장 공석으로 인한 긴급 채용…",
            style = DocStyle.NEWS,
            body = body { p("회계팀장 공석으로 인한 긴급 채용. 즉시 출근 가능자 우대.") },
            monologue = lines("공석. 오늘 날짜로 올라왔다."),
        ),
        Document(
            id = DocId.D_DDUK,
            title = "부산 떡볶이 사장 강민우(31) \"저 그 작가 아닙니다ㅠ\"",
            source = "파란뉴스 생활 · 2026.11.03",
            snippet = "동명이인 작가 이름 때문에 하루 전화 300통…",
            style = DocStyle.NEWS,
            body = body { p("\"이름 때문에 오늘만 전화 300통 받았어요. 저는 마라로제 떡볶이밖에 몰라요ㅠㅠ\"") },
            monologue = lines("서른한 살 강민우. 마라로제가 뭔지는 모르겠지만,", "이 사람 말고. 내가 찾는 강민우."),
        ),
        Document(
            id = DocId.D_WRECK,
            title = "[사이다렉카TV] 국민작가 강민우 14개월 잠적의 진실?! 돌아온 사람은 대역?! (귀 뒤 흉터 비교)",
            source = "조회수 41만 · 2026.11.03",
            snippet = "ㄷㄷ 아내만 모르는 사람 끝에 나오는 상품 아님 착각 금지ㄷㄷ",
            style = DocStyle.SPAM,
            body = body {
                p {
                    +"ㄷㄷ 사고 전 사진엔 없던 귀 뒤 흉터가 복귀 후엔 있다?! 대역설 총정리!"; br()
                    +"끝에 나오는 상품 아님 착각 금지ㄷㄷ 구독 좋아요 알림설정★"
                }
            },
            monologue = linesWhen(
                Cond.Deduced(Deduction.NOT_DOYUN),
                then = listOf("귀 뒤 흉터.", "…렉카가 맞았다."),
                otherwise = listOf("끌어다 팔 거리는 안 줄었군.", "강민우, 네 유명세엔 이런 파리도 꼬이는구나."),
            ),
        ),
        Document(
            id = DocId.D_BOOK_REVIEW,
            title = "《무적》 20주년 리커버 — \"읽을 때마다 바다 냄새\"",
            source = "파란북스 독자 리뷰",
            snippet = "★★★★★ 작가님 목소리로 듣는 오디오북 강추",
            style = DocStyle.NEWS,
            body = body {
                p("★★★★★ 오디오북 강추. 작가님 목소리로 들으면 내가 그 섬에 있는 것 같아요.")
                p("★★★★☆ 근데 작가님 요즘은 왜 낭독 안 하세요?")
            },
            monologue = lines("내가 그 섬에 있는 것 같다고.", "…그 목소리를 20년 들으면, 정말 그 섬에 있게 된다."),
        ),
        Document(
            id = DocId.D_PARAN_INFO,
            title = "천리안 CUG '파란누리' (1996~2002) 아카이브 복원",
            source = "PC통신 보존회",
            snippet = "ParanOS 98 사용자는 '파란누리' 앱에서 go paran 으로 접속",
            style = DocStyle.WIKI,
            body = body {
                p {
                    +"문학 동호회 '파란누리'의 게시판 전체가 복원됐다. ParanOS 98 홈 화면의 "
                    bold("파란누리")
                    +" 앱에서 "
                    bold("go paran")
                    +" 으로 접속할 수 있다."
                }
            },
            monologue = lines("파란누리. 홈 화면에 앱이 하나 생겼다."),
        ),
        Document(
            id = DocId.D_HARIN_GALLERY,
            title = "화가 서하린 개인전 '안개 경보' — 열여덟 번째 등대",
            source = "파란일보 문화면 · 2026.10.20",
            snippet = "18년째 등대만 그리는 화가. \"그 사람은 등대를 싫어해요\"",
            style = DocStyle.NEWS,
            body = body {
                p("18년째 등대만 그려 온 화가 서하린. \"남편은 제가 등대를 그리는 걸 싫어해요. 그래서 계속 그려요.\" 작품 사진은 InstaPixel @harin_atelier 에서 볼 수 있다.")
            },
            monologue = lines("남편이 싫어하는 걸 18년째 그린다.", "이 여자, 남편과 싸우고 있다."),
        ),
        Document(
            id = DocId.D_LIGHTHOUSE_JOB,
            title = "등대지기 — 직업사전",
            source = "파란 직업사전",
            snippet = "항로표지관리원. 1990년대 이후 무인화로 대부분 사라짐",
            style = DocStyle.WIKI,
            body = body { p("정식 명칭은 항로표지관리원. 1990년대 이후 무인화로 대부분 사라졌다.") },
            monologue = lines("사라진 직업. 사라진 사람."),
        ),
        Document(
            id = DocId.D_BAEKRO_FISH,
            title = "백로도 감성돔 포인트 공유 (섬캉스 브이로그)",
            source = "InstaPixel 인기글",
            snippet = "#백로도 #섬캉스 #럭키비키잖아",
            style = DocStyle.NEWS,
            body = body { p("등대 터 옆 갯바위 폼 미쳤다ㄹㅇ 오늘 조황 럭키비키잖아 #섬캉스") },
            monologue = lines("폼이 미쳤다는 건 칭찬인가.", "20년 사이 한국어가 망가졌거나, 내가 망가졌거나."),
        ),
        Document(
            id = DocId.D_SCHOLAR,
            title = "무적문화재단 '등대 장학생' 모집",
            source = "고흥군청 공지",
            snippet = "백로도 및 도서 지역 청소년 대상",
            style = DocStyle.NEWS,
            body = body { p("백로도 및 도서 지역 청소년을 대상으로 장학생을 모집합니다. 후원: (재)무적문화재단") },
            monologue = lines("불탄 섬의 이름으로 장학금을 뿌린다. 속죄인가, 조롱인가."),
        ),
        Document(
            id = DocId.D_PARK_AWARD,
            title = "무적문화재단 박태성 팀장, 비영리 투명경영 대상",
            source = "파란경제 · 2023.12.11",
            snippet = "\"숫자는 거짓말을 하지 않습니다\"",
            style = DocStyle.NEWS,
            body = body { p("\"숫자는 거짓말을 하지 않습니다. 사람이 하죠.\" — 회계팀장 박태성") },
            monologue = lines("숫자는 거짓말을 하지 않는다. 사람이 한다.", "…이 사람, 끝까지 숫자를 믿었겠지."),
        ),
        Document(
            id = DocId.D_LH_WEB,
            title = "@lighthouse_1998 · InstaPixel",
            source = "instapixel",
            snippet = "비공개 계정입니다.",
            style = DocStyle.NEWS,
            body = body { p("이 계정은 비공개입니다. InstaPixel 앱에서 열람을 시도하세요.") },
            monologue = lines("마지막 문은 InstaPixel에 있다."),
        ),
    )

    // ═════════════════════════════════════════════════════════════════════════
    //  SNS 프로필 (11건)
    // ═════════════════════════════════════════════════════════════════════════

    val profiles: Map<ProfileId, Profile> = (facebookProfiles() + instaProfiles()).associateBy { it.id }

    fun profile(id: ProfileId): Profile = profiles.getValue(id)

    private const val MEMORY_BANNER = "[MEMORY] 10년 전 오늘"

    private fun facebookProfiles(): List<Profile> = listOf(
        Profile(
            id = ProfileId.FB_OH,
            app = AppId.FACEBOOK,
            name = "오병철",
            headline = "월포수산 대표 · 전 해송정신요양원 원장",
            avatar = Picture.FISH,
            body = body {
                post("2026.10.28 · 공개") {
                    +"월포수산 제3냉동창고 전력 증설 계약 완료~ 겨울 대비 끝!"
                    picture(Picture.CONTRACT)
                    link(DocId.POWER_CONTRACT, "[DOC] 계약서 사진 크게 보기")
                }
                post("2026.09.14 · 공개", reaction = Reaction.FB_FISH) {
                    +"월포항 감성돔 52cm!! ㅎㅎ"
                    picture(Picture.FISH)
                    comment("김씨", "형님 요즘 돈 어디서 그렇게 나요 ㅋㅋ")
                    comment("오병철", "매달 꼬박꼬박 들어오는 데가 있지^^")
                }
                post("2016.11.03", reaction = Reaction.FB_MEMORY, memory = MEMORY_BANNER) {
                    +"요양원 문 닫은 지 7년. 그래도 끝까지 남은 식구 하나는 내가 책임진다."
                }
                lock(LockId.WEBHARD, "오병철 자료실 (구형 웹하드)")
            },
            monologue = lines("오병철. 문틈으로 식판을 밀어 넣던 그 손.", "이제는 수산회사 사장이라고."),
        ),
        Profile(
            id = ProfileId.FB_KANG,
            app = AppId.FACEBOOK,
            name = "강민우 (작가)",
            headline = "페이지 · 팔로워 31만",
            avatar = Picture.KANG,
            body = body {
                post("2026.10.30") {
                    +"내일은 오래된 친구를 만나러 간다. 정리할 이야기가 있다."
                    comment("독자1", "신작 소식인가요?!")
                    comment("익명", "월포항 뉴스… 재단 직원이라던데 사실인가요? (11.02)")
                }
                post("2016.11.03", reaction = Reaction.FB_KANG_MEMORY, memory = MEMORY_BANNER) {
                    +"10년 전 오늘, 나는 한 번 죽었다. 어쩌면, 아니 분명히 그날 이후의 삶은 덤이다."
                    comment("서하린", "그날 얘기는 안 하기로 했잖아.")
                    comment("오병철", "덕분에 저도 노후가 편합니다 ㅎㅎ")
                }
            },
            monologue = lines("강민우. 20년 동안 내 인생 위에 앉아 있던 놈."),
        ),
        Profile(
            id = ProfileId.FB_YOUTH,
            app = AppId.FACEBOOK,
            name = "월포읍 청년회",
            headline = "그룹 · 멤버 38명",
            avatar = Picture.GROUP,
            body = body {
                post("2026.11.02", reaction = Reaction.SLANG) {
                    +"오늘 회식 폼 미쳤다 ㄹㅇ 이장님 노래 개킹받음ㅋㅋ AI한테 축사 써 달라 했는데 들킴"
                }
                post("2026.11.02", reaction = Reaction.POLICE) {
                    +"월포항에 경찰차 엄청 왔던데 무슨 일임?? 시체 나왔다는 소문…"
                }
            },
            monologue = lines("월포읍 사람들. 내가 갇혀 있던 창고 바로 옆에서 회식을 했다."),
        ),
        Profile(
            id = ProfileId.FB_HARIN,
            app = AppId.FACEBOOK,
            name = "서하린",
            headline = "화가 · 친구 공개 프로필",
            avatar = Picture.LIGHT,
            body = body {
                post("2008.01.20 · 공개", reaction = Reaction.HARIN_2008) {
                    +"다시 돌아와 줘서 고마워. 이제부터 다시 시작이야."
                }
                note("이 사용자의 다른 게시물은 친구에게만 공개됩니다.")
            },
            monologue = lines("2008년 1월. 강민우가 붕대를 풀고 돌아온 달."),
        ),
        Profile(
            id = ProfileId.FB_HAESONG,
            app = AppId.FACEBOOK,
            name = "해송 가족 동우회",
            headline = "비공개 그룹 · 멤버 23명",
            avatar = Picture.GROUP,
            body = body {
                post("2021.06.30", reaction = Reaction.HAESONG_GROUP) {
                    +"폐원 12주년ㅠ 우리 때 '조용한 0419호' 기억나는 사람? 원장님은 아직도 그 창고 관리하신다던데"
                    picture(Picture.GROUP)
                }
            },
            monologue = lines("조용한 0419호. 그들은 나를 그렇게 불렀다."),
        ),
    )

    private fun instaProfiles(): List<Profile> = listOf(
        Profile(
            id = ProfileId.IP_HARIN,
            app = AppId.INSTA,
            name = "harin_atelier",
            headline = "서하린 · 그림 그립니다. 등대만.",
            avatar = Picture.LIGHT,
            body = body {
                gallery(Picture.LIGHT, Picture.COFFEE, Picture.ISLAND)
                post("2026.10.29", reaction = Reaction.IP_HARIN_SERIES) {
                    +"등대 연작 #18. 그 사람은 내가 등대를 그리는 걸 싫어한다."
                }
                post("2025.12.20", reaction = Reaction.IP_HARIN_SUGAR) {
                    +"2008년 겨울부터 우리 집 설탕통은 나만 연다."
                }
                lock(LockId.CLOUD, "하린의 서랍 (비공개 클라우드)")
            },
            monologue = lines("서하린. 강민우의 아내.", "사진마다 등대다. 이 여자도 뭔가 알고 있다."),
        ),
        Profile(
            id = ProfileId.IP_KANG,
            app = AppId.INSTA,
            name = "kang.minwoo_official",
            headline = "작가 · (재)무적문화재단 이사장",
            avatar = Picture.KANG,
            body = body {
                figure(Picture.KANGCUP)
                post("2026.10.02", reaction = Reaction.IP_KANG_BLACK) { +"블랙. 쓴맛이 정직하다." }
                post("2026.09.18") {
                    +"창작 지원 10주년. 한 사람의 고통이 모두의 위로가 되기를."
                    picture(Picture.PODIUM)
                }
            },
            monologue = lines("놈의 얼굴. 화상 이식 자국이 희미하게 남아 있다."),
        ),
        Profile(
            id = ProfileId.IP_DDUK,
            app = AppId.INSTA,
            name = "kangminwoo_dduk",
            headline = "부산 · 마라로제 떡볶이 사장 강민우",
            avatar = Picture.FOOD,
            body = body {
                figure(Picture.FOOD)
                post("2026.11.03", reaction = Reaction.DDUK) {
                    +"네 그 작가 아닙니다ㅠㅠ 전화 그만… 오늘의 메뉴는 마라로제! #사장님이상해요"
                }
            },
            monologue = lines("동명이인. 이 사람 인생도 오늘 하루 엉망이 됐겠군."),
        ),
        Profile(
            id = ProfileId.IP_OH,
            app = AppId.INSTA,
            name = "oh_fishing61",
            headline = "낚시 · 월포",
            avatar = Picture.FISH,
            body = body {
                figure(Picture.FISH)
                post("2026.09.14", reaction = Reaction.FB_FISH) { +"#감성돔 #월포항 #인생어" }
            },
            monologue = lines("낚시 사진뿐이다. 이 영감의 진짜 얼굴은 FaceBook 95 쪽이다."),
        ),
        Profile(
            id = ProfileId.IP_BAEKRO,
            app = AppId.INSTA,
            name = "#백로도",
            headline = "게시물 1,204개",
            avatar = Picture.ISLAND,
            body = body {
                gallery(Picture.ISLAND, Picture.FISH, Picture.LIGHT)
                link(DocId.D_BAEKRO_FISH, "인기글: 백로도 감성돔 포인트 공유")
            },
            monologue = lines("관광객들의 섬. 불탄 등대 터에서 셀카를 찍는다."),
        ),
        Profile(
            id = ProfileId.IP_LH,
            app = AppId.INSTA,
            name = "lighthouse_1998",
            headline = "[LOCKED //] 비공개 계정",
            avatar = Picture.LIGHT,
            body = body {
                note("이 계정은 비공개입니다.")
                lock(LockId.LIGHTHOUSE, "비공개 계정 열람 요청")
            },
            monologue = lines("@lighthouse_1998. 등대지기의 계정."),
        ),
    )

    // ═════════════════════════════════════════════════════════════════════════
    //  검색 인덱스
    // ═════════════════════════════════════════════════════════════════════════

    private fun docs(vararg ids: DocId): Branch<List<SearchHit>> = Branch.Fixed(ids.map { SearchHit.Doc(it) })

    private fun people(vararg ids: ProfileId): Branch<List<SearchHit>> =
        Branch.Fixed(ids.map { SearchHit.Person(it) })

    val search: Map<AppId, Map<Chip, Branch<List<SearchHit>>>> = mapOf(
        AppId.WEB to mapOf(
            Chip.HAESONG_0419 to docs(DocId.NOTICE, DocId.D_HOE, DocId.D_419, DocId.D_SPAM_SILVER),
            Chip.OH_BYEONGCHEOL to docs(DocId.NOTICE, DocId.D_OH_NEWS),
            Chip.WOLPO_EUP to docs(DocId.D_WOLPO_TOUR, DocId.D_HOE),
            Chip.WOLPO_SUSAN to docs(DocId.D_WOLPOSUSAN),
            Chip.MUJEOK_FOUNDATION to docs(DocId.FOUNDATION, DocId.D_FOUND_JOB),
            Chip.KANG_MINWOO to docs(DocId.WIKI_KANG, DocId.D_DDUK, DocId.D_WRECK),
            Chip.MUJEOK_NOVEL to docs(DocId.RADIO_2005, DocId.SOYUN_NEWS, DocId.D_BOOK_REVIEW),
            Chip.PARANNURI to docs(DocId.D_PARAN_INFO),
            Chip.GAPYEONG_CRASH to docs(DocId.GAPYEONG),
            Chip.DATE_2006_11_03 to docs(DocId.GAPYEONG),
            Chip.SEO_HARIN to docs(DocId.D_HARIN_GALLERY),
            Chip.HAN_SOYUN to docs(DocId.SOYUN_NEWS, DocId.CY_SOYUN),
            Chip.LIGHTHOUSE_KEEPER to docs(DocId.D_LIGHTHOUSE_JOB),
            Chip.BAEKRODO to docs(DocId.BURN_BLOG, DocId.D_BAEKRO_FISH, DocId.D_SCHOLAR),
            Chip.PARK_TAESEONG to Branch.If(
                Cond.ChapterAtLeast(3),
                then = listOf(SearchHit.Doc(DocId.NEWS_PARK), SearchHit.Doc(DocId.D_PARK_AWARD)),
                otherwise = listOf(SearchHit.Doc(DocId.D_PARK_AWARD)),
            ),
            Chip.LIGHTHOUSE_1998 to docs(DocId.D_LH_WEB),
        ),
        AppId.INSTA to mapOf(
            Chip.SEO_HARIN to people(ProfileId.IP_HARIN),
            Chip.KANG_MINWOO to people(ProfileId.IP_KANG, ProfileId.IP_DDUK),
            Chip.MUJEOK_FOUNDATION to people(ProfileId.IP_KANG),
            Chip.OH_BYEONGCHEOL to people(ProfileId.IP_OH),
            Chip.BAEKRODO to people(ProfileId.IP_BAEKRO),
            Chip.LIGHTHOUSE_1998 to people(ProfileId.IP_LH),
            Chip.LIGHTHOUSE_KEEPER to Branch.If(
                Cond.ProxyOff,
                then = listOf(SearchHit.Person(ProfileId.IP_LH)),
                otherwise = emptyList(),
            ),
        ),
        AppId.FACEBOOK to mapOf(
            Chip.OH_BYEONGCHEOL to people(ProfileId.FB_OH),
            Chip.WOLPO_SUSAN to people(ProfileId.FB_OH),
            Chip.KANG_MINWOO to people(ProfileId.FB_KANG),
            Chip.MUJEOK_FOUNDATION to people(ProfileId.FB_KANG),
            Chip.WOLPO_EUP to people(ProfileId.FB_YOUTH),
            Chip.SEO_HARIN to people(ProfileId.FB_HARIN),
            Chip.HAESONG_0419 to people(ProfileId.FB_HAESONG),
        ),
    )

    fun results(app: AppId, chip: Chip, state: GameState): List<SearchHit> =
        search[app]?.get(chip)?.let { state.resolve(it) }.orEmpty()

    // ═════════════════════════════════════════════════════════════════════════
    //  게시물 반응 독백 · 스팸 · 결과 없음
    // ═════════════════════════════════════════════════════════════════════════

    val reactions: Map<Reaction, Lines> = mapOf(
        Reaction.FB_FISH to lines("매달 꼬박꼬박 들어오는 데. 그게 나였다. 정확히는 나를 가둔 값."),
        Reaction.FB_MEMORY to lines("끝까지 남은 식구. 나를 식구라고 불렀다.", "개밥 주듯 문틈으로 밥을 밀어 넣던 그 손이."),
        Reaction.FB_KANG_MEMORY to lines("어쩌면, 아니 분명히.", "이 말버릇… 왜 내 혀끝에서 맴도는 거지?"),
        Reaction.SLANG to lines("폼이 미쳤다. 킹받는다.", "…20년이면 말도 이민을 가는구나."),
        Reaction.POLICE to lines("시체. 월포항.", "그게 오늘 밤 나와 무슨 상관인지, 아직은 모른다."),
        Reaction.HARIN_2008 to lines("다시 시작이라. 이 여자는 정말 몰랐을까."),
        Reaction.HAESONG_GROUP to lines("조용한 0419호. 처음 몇 년은 소리를 질렀다. 그다음엔 따라 읽었다."),
        Reaction.IP_HARIN_SERIES to lines("그 사람은 등대를 싫어한다. 등대지기가 등대를 싫어할 리 없을 텐데."),
        Reaction.IP_HARIN_SUGAR to lines("설탕통을 혼자 연다. 남편은 단것을 끊었다는 뜻이다."),
        Reaction.IP_KANG_BLACK to linesWhen(
            Cond.Deduced(Deduction.BORROWED_HABITS),
            then = listOf("블랙커피. 왼손.", "서하린 일기 속 '돌아온 남자' 그대로다."),
            otherwise = listOf("블랙커피를 왼손으로 들고 있다.", "놈은 왼손잡이인가 보군."),
        ),
        Reaction.DDUK to lines("마라로제. 이 단어만은 끝까지 모르는 채로 살고 싶다."),
        Reaction.MASK to lines("프록시 손상. 이상하게도 이름만 매번 지워져 있다.", "Proxy 1998… 이 필터는 누구를 위해 돌아가고 있지?"),
    )

    /** 52초마다 챕터별 목록에서 하나를 무작위로 띄운다. */
    val spam: Map<Int, List<String>> = mapOf(
        1 to listOf(
            "★축★당첨★ 잃어버린 이름을 돌려드립니다! 1회 상담 무료 TEL 수신거부 0419",
            "[천리안] 고객님의 CUG 휴면 계정이 28년 만에 복구되었습니다",
            "★ 외로운 밤, 등대 채팅방 입장 ★ 지금 3명 대기 중",
        ),
        2 to listOf(
            "[파란은행] 고객님 명의 OTP가 새 기기에 등록되었습니다",
            "★오디오북 《무적》 20주년 무료 듣기★ 작가 본인 낭독!",
            "[알림] 당신의 지문이 저장되었습니다. 확인 >>",
        ),
        3 to listOf(
            "당신 전화기는 당신 편이 아닙니다.",
            "[경찰청] 피의자 위치 확인 협조 요청 — 무시하지 마십시오",
            "오늘도 등대 불 끄고 갑니다.",
        ),
    )

    fun spamFor(chapter: Int): List<String> = spam.getValue(chapter.coerceIn(1, 3))

    // ═════════════════════════════════════════════════════════════════════════
    //  잠금 · 교차검증 쌍
    // ═════════════════════════════════════════════════════════════════════════

    val locks: Map<LockId, LockSpec> = listOf(
        LockSpec(
            id = LockId.WEBHARD,
            title = "오병철 자료실 (구형 웹하드)",
            hint = "법인 등록번호 10자리 (숫자만)",
            code = "4868200119",
            logLine = "오병철 웹하드",
            reward = LockReward.OpenDocument(DocId.SETTLEMENT),
            requires = Cond.Deduced(Deduction.ASYLUM_RENAMED),
            gateLines = listOf(
                "이 영감이 뭘 숨겼는지 아직 모른다.",
                "먼저 서류끼리 맞춰 보자. 냉동창고가 그 요양원이라는 증거부터.",
            ),
        ),
        LockSpec(
            id = LockId.CLOUD,
            title = "하린의 서랍 (비공개 클라우드)",
            hint = "모든 것이 무너진 날 (YYYYMMDD)",
            code = "20061103",
            logLine = "서하린 클라우드",
            reward = LockReward.OpenDocument(DocId.DIARY),
        ),
        LockSpec(
            id = LockId.VAULT,
            title = "VAULT 95 마스터키",
            hint = "첫 원고 등록일 (YYYYMMDD)",
            code = "19980511",
            logLine = "금고95 마스터키",
            reward = LockReward.VaultMasterKey,
            requires = Cond.BinderFound,
            gateLines = listOf("금고95. 아직 이 시스템에 대해 아는 게 없다."),
        ),
        LockSpec(
            id = LockId.LIGHTHOUSE,
            title = "@lighthouse_1998 비공개 계정",
            hint = "네가 20년간 불린 번호이자 네 진짜 생일",
            code = "0419",
            logLine = "@lighthouse_1998",
            reward = LockReward.FinalTruth,
        ),
    ).associateBy { it.id }

    val crossPairs: List<CrossPair> = listOf(
        CrossPair(
            a = PinId.CORP_NOTICE,
            b = PinId.CORP_POWER,
            deduction = Deduction.ASYLUM_RENAMED,
            conclusion = "해송정신요양원 = 월포수산 제3냉동창고. 2009년 폐원은 간판 교체였다. 감금은 한 번도 멈춘 적이 없다.",
        ),
        CrossPair(
            a = PinId.DIARY_BEFORE,
            b = PinId.BODYLOG_COFFEE,
            deduction = Deduction.BORROWED_HABITS,
            conclusion = "왜 내 몸에 원수 강민우의 식습관이 배어 있는가?",
        ),
        CrossPair(
            a = PinId.BURN_RECORD,
            b = PinId.MIRROR_LOG,
            deduction = Deduction.NOT_DOYUN,
            conclusion = "나는 한도윤이 아니다.",
        ),
        CrossPair(
            a = PinId.KANG_BIRTH,
            b = PinId.SUBJECT_NO,
            deduction = Deduction.BIRTHDAY_CELL,
            conclusion = "수감번호 0419 = 강민우의 생일. 놈은 20년 동안 나를 자기 생일로 불렀다.",
        ),
    )

    fun pairFor(pinned: PinId, tapped: PinId): CrossPair? = crossPairs.firstOrNull { it.matches(pinned, tapped) }

    // ═════════════════════════════════════════════════════════════════════════
    //  이야기 트리거 (웹판 story())
    // ═════════════════════════════════════════════════════════════════════════

    val beats: Map<StoryEvent, StoryBeat> = mapOf(
        StoryEvent.DocumentOpened(DocId.NOTICE) to StoryBeat(
            watson = listOf(
                watson("해송. 2009년에 문 닫았다는 그 요양원이지."),
                watson("근데 넌 어제까지 거기 있었어. 이상하지 않냐?"),
                watson("원장 이름이 보이지? 그 영감부터 털어. FaceBook 95."),
            ),
        ),
        StoryEvent.ProfileVisited(ProfileId.FB_OH) to StoryBeat(
            watson = listOf(
                watson("그 영감, 요즘은 수산회사 사장 행세를 한다."),
                watson("서류끼리 맞춰 봐. 문서 속 붉은 밑줄 문장을 누르면 [PIN] 대조 버퍼에 고정할 수 있다."),
            ),
        ),
        StoryEvent.Deduced(Deduction.ASYLUM_RENAMED) to StoryBeat(
            grantChip = Chip.WOLPO_SUSAN,
            watson = listOf(
                watson("간판만 바꿔 단 거다. 감금은 하루도 쉰 적 없어."),
                watson("그 영감 구형 웹하드가 있지? 법인번호로 잠겨 있을 거다."),
                watson("불 끄고 간다."),
            ),
            monologue = listOf("요양원은 문을 닫은 적이 없다. 이름만 바꿨을 뿐."),
        ),
        StoryEvent.DocumentOpened(DocId.SETTLEMENT) to StoryBeat(
            watson = listOf(
                watson("무적문화재단. 강민우. 네 인생을 훔친 놈 이름이다.") {
                    chip(Chip.MUJEOK_FOUNDATION); +". "; chip(Chip.KANG_MINWOO); +". 네 인생을 훔친 놈 이름이다."
                },
                watson("경찰 눈을 피해 내가 마련해 둔 안전가옥, 월포역 앞 파란여관 203호로 들어가라."),
                MessageSpec(
                    text = "환풍구 안에 조사 자료가 있다.",
                    action = MessageAction(Command.GO_CHAPTER_2, "[ 파란여관 203호로 이동 ]"),
                ),
            ),
        ),
        StoryEvent.CoffeeDrunk to StoryBeat(
            watson = listOf(
                watson("좋아. 어쩌면, 아니 분명히 오늘 밤은 길 거다."),
                watson("환풍구. 박태성이라는 놈이 숨겨 둔 게 있다."),
            ),
        ),
        StoryEvent.BinderFound to StoryBeat(
            watson = listOf(
                watson("박태성이라는 회계쟁이가 모은 자료다. 페이지 하나도 빼놓지 말고 읽어."),
                watson("금고95에 들어가면 놈의 비자금이 다 있어. 마스터키는 '첫 원고 등록일'."),
                watson("강민우가 어디 출신인지부터 털어 봐. 웹에서."),
            ),
        ),
        StoryEvent.DocumentOpened(DocId.PARAN_LH) to StoryBeat(
            watson = listOf(watson("등대지기… 1998년의 너지."), watson("그 날짜, 잘 기억해 둬.")),
        ),
        StoryEvent.DocumentOpened(DocId.DIARY) to StoryBeat(
            watson = listOf(watson("그 여자 일기 믿지 마. 강민우 마누라야.")),
        ),
        StoryEvent.Deduced(Deduction.BORROWED_HABITS) to StoryBeat(
            glitchAtLeast = 2,
            watson = listOf(watson("…커피 취향 같은 건 우연이야."), watson("신경 꺼. 불 끄고 간다.")),
            monologue = listOf("오른손. 설탕 두 스푼.", "왜 내 몸에 원수의 식습관이 배어 있지?"),
            checksChapter3 = true,
        ),
        StoryEvent.Deduced(Deduction.BIRTHDAY_CELL) to StoryBeat(
            monologue = listOf(
                "내 번호가 놈의 생일.",
                "20년 동안 놈은 매일 아침 나를 자기 생일로 불렀다.",
                "…그런데 왜 이 숫자가 이렇게 편하지?",
            ),
        ),
        StoryEvent.VaultKeyAccepted to StoryBeat(
            watson = listOf(watson("토큰은 탁자 위에 있다. 네 손으로 직접 눌러.")),
            monologue = listOf("하드웨어 OTP. 203호 탁자 위의 그 열쇠고리."),
        ),
        StoryEvent.DocumentOpened(DocId.VAULT_SUMMARY) to StoryBeat(
            glitchAtLeast = 1,
            watson = listOf(watson("됐다. 그 자료면 놈은 끝이다."), watson("이제 거기서 가만히 있어.")),
            checksChapter3 = true,
        ),
        StoryEvent.Deduced(Deduction.NOT_DOYUN) to StoryBeat(
            glitchAtLeast = 3,
            watson = listOf(watson("…거울은 보지 말라고 했지."), watson("필터는 건드리지 마. 네 머리가 못 버틴다.")),
            monologue = listOf(
                "선천적 왼손잡이. 좌측 귀 뒤 6cm 화상.",
                "나는 오른손잡이다. 귀 뒤는 매끈하다.",
                "나는… 한도윤이 아니다.",
                "그렇다면 이 전화기가 지우고 있던 이름은 누구의 것이지? 설정. Proxy 1998.",
            ),
        ),
    )

    // ═════════════════════════════════════════════════════════════════════════
    //  챕터 · 프록시 붕괴 · 엔딩 대본
    // ═════════════════════════════════════════════════════════════════════════

    val chapters: Map<Int, ChapterScript> = mapOf(
        1 to ChapterScript(
            card = ChapterCard(1, "열려 있던 문", "전남 고흥군 월포읍 · 월포수산 제3냉동창고 앞"),
            battery = 31,
            minutesAdvance = 0,
            landing = Landing.Home,
            grantChip = Chip.HAESONG_0419,
            monologue = listOf(
                "문이 열려 있었다. 20년 만에.",
                "손에 쥔 건 낡은 전화기 한 대. 처음 보는 운영체제, ParanOS 98.",
                "목덜미의 환자복 라벨. '해송 0419'. 이름 대신 불리던 번호다.",
                "나는 한도윤이다. 강민우에게 인생을 도둑맞은 남자.",
            ),
            monologueQueued = true,
            watson = listOf(
                MessageSpec("ParanOS 98 보안 메신저 · Proxy 1998 경유 연결됨", Sender.SYSTEM),
                watson("문은 내가 열었다."),
                watson("이름은 묻지 마. WATSON이라고 불러."),
                watson("그 전화기, 내가 손봐 둔 거다. 검색은 단서 칩으로만 해. 키보드 쓰면 추적당한다."),
                watson("라벨부터 시작해. 해송 0419. 웹에서."),
                watson("오늘은 여기까지. 불 끄고 간다."),
            ),
        ),
        2 to ChapterScript(
            card = ChapterCard(2, "몸이 먼저 기억한다", "월포읍 파란여관 203호"),
            battery = 18,
            minutesAdvance = 18,
            landing = Landing.App(AppId.ROOM),
            monologue = listOf("장기 휴업 중인 여관. 열쇠는 문틀 위에 있었다.", "곰팡이 냄새. 그래도 감옥보다는 넓다."),
            monologueQueued = true,
            watson = listOf(watson("도착했나. 목 좀 축여. 탁자에 믹스커피 있다.")),
        ),
        3 to ChapterScript(
            card = ChapterCard(3, "거울 속의 타인", "파란여관 203호 — 포위망 완성"),
            battery = 9,
            minutesAdvance = 6,
            landing = Landing.Doc(AppId.WEB, DocId.NEWS_PARK),
            grantChip = Chip.PARK_TAESEONG,
            markRead = DocId.NEWS_PARK,
            receiveLog = "속보 — 박태성 변사체 / 이사장 체포영장",
            monologue = NEWS_PARK_THOUGHTS,
            monologueQueued = false,
            watson = listOf(
                watson("속보 봤냐. 놈이 포위됐다."),
                watson("넌 거기 가만히 있어. 지금은 거기가 제일 안전해."),
                watson("그리고 거울은 보지 마. 20년 만의 얼굴은 독이다."),
            ),
        ),
    )

    val proxyCollapse = ProxyCollapseScript(
        restoredDocs = listOf(DocId.SETTLEMENT, DocId.BINDER, DocId.VAULT_SUMMARY),
        monologue = listOf(
            "■■■가 하나씩 붉게 번진다.",
            "실명: 강민우 본인 (760419-1******).",
            "…아니야. 아닐 거다. 바인더부터 다시 열어 보자.",
        ),
        watson = listOf(
            watson("결국 열었네."),
            watson("그럼 마지막 문도 열어 봐. 비밀번호는 네가 제일 잘 알 거다."),
            watson("@lighthouse_1998") { chip(Chip.LIGHTHOUSE_1998) },
        ),
    )

    val ending = EndingScript(
        revelation = listOf(
            "전기충격이 내게 남의 기억을 심어준 게 아니었어…",
            "스무 해 동안 어둠 속에서 내 귀에 흘러나오던 그 목소리, 등대 불길 속에서 울던 그 문장들은…",
            "2005년에 내가 한도윤의 원고를 훔쳐서 라디오에서 읽었던 내 목소리였어.",
            "내가 외웠던 거짓말이 내 진짜 기억을 다 집어삼킨 거다.",
        ),
        flood = listOf(
            watson("이제야 다 읽었네, 민우 형."),
            watson("솔직히 말할게. 네 이름으로 살아온 20년, 단 하루도 지옥 같지 않은 날이 없었어."),
            watson("네 아내는 매일 밤 내 귀 뒤 흉터를 보며 치를 떨었고, 박태성 그 인간은 내 치열과 필적이 가짜라는 걸 끝내 찾아냈지."),
            watson("난 완벽하지 못해서 사흘 전 밤에 결국 내 손에 피까지 묻혔어."),
            watson("어제 월포항에서 박태성 시신이 떠올랐고, 경찰이 '이사장 강민우'를 살인 및 184억 횡령 혐의로 포위해 오더군."),
            watson("그래서 생각했지. 이 무거운 이름, 이제 진짜 주인한테 돌려주자고."),
            watson("네가 지금 앉아 있는 파란여관 203호, 거기가 사흘 전에 내가 박태성을 죽인 방이야."),
            watson("넌 오늘 밤 내 말을 믿고 환풍구에서 박태성의 감사 바인더를 꺼내 읽으면서 모든 페이지에 네 지문을 남겼고,"),
            watson("아까 탁자 위 하드웨어 OTP 토큰을 눌러 비자금 서버 접속 기록까지 네 손으로 찍었어."),
            watson("과학수사대가 들이닥쳐서 그 방의 지문과 커피잔 DNA, 그리고 네 오른쪽 아래 뽑힌 사랑니 자리를 국과수 원부랑 대조하면 누구 이름이 나올까?"),
            watson("성형도 안 했고 주민등록 지문도 그대로인 진짜배기 '강민우(760419)', 바로 너야."),
            MessageSpec(text = "", sender = Sender.POLICE_LOG),
            MessageSpec("[경찰청 중대범죄수사과] 피의자 강민우 은닉 하드웨어 OTP 활성화 및 서버 접속 감지", Sender.ALERT),
            MessageSpec("[발신지 특정] 전남 고흥군 월포읍 해안로 파란여관 203호 — 형사기동대 진입 완료", Sender.ALERT),
            watson("경찰한테 가서 외쳐 봐. \"나는 강민우가 아니라 한도윤이다! 지난 20년간 감옥에 갇혀 있었다!\"라고."),
            watson("20년 전의 내가 똑같이 외쳤을 때, 세상은 날 정신병자로 몰았고 내 동생 소윤이는 옥상에서 뛰어내렸어. 이제 네가 겪을 차례야."),
            watson("오늘도 등대 불 끄고 갑니다."),
        ),
        lighthouseHeadline = "게시물 2 · 팔로워 0 · 팔로잉 1 (@0419)",
        lighthouseFigures = listOf(
            Picture.RADIO to "2005.04 · MBS '새벽 세 시' 낭독 부스",
            Picture.CCTV to "CCTV-03 · 제3냉동창고 · 0419호",
        ),
        sameFace = "같은 얼굴",
        raidBang = "쾅—!",
        raidShout = "\"경찰이다! 강민우, 움직이지 마!\"",
        raidAftermath = "(바닥에 떨어진 화면 위로, 수갑이 채워지는 오른손의 그림자가 겹친다.)",
        lastWords = listOf(
            "내 지문, 내 치아, 내 이름…",
            "세상의 모든 과학이 나를 범인으로 가리키고 있는데, 내 머릿속에는 내가 훔친 남자의 슬픔밖에 남아 있지 않다.",
            "어쩌면, 아니 분명히…",
            "나는 스무 해 전에 이미 그 등대에서 불타 죽었어야 했다.",
        ),
    )

    /** 엔딩 메신저에 끼어드는 '경찰청 전송 로그' 덤프. 최근 9건만 보인다. */
    fun policeLogDump(log: List<LogEntry>): String = buildString {
        append("[Proxy 1998 → 경찰청 전송 로그 ").append(log.size).append("건]")
        log.takeLast(9).forEach { append('\n').append('[').append(it.time).append("] ").append(it.kind.label).append(": ").append(it.text) }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  파란여관 203호 · 파란누리 게시판
    // ═════════════════════════════════════════════════════════════════════════

    val room = RoomScript(
        coffeeAgain = listOf("식은 커피. 설탕 두 스푼의 단맛이 혀에 남아 있다."),
        otpAlreadyPressed = listOf("버튼은 이미 눌렀다. 작은 LED가 아직 깜빡인다."),
        otpIdle = listOf("열쇠고리만 한 OTP 토큰. 버튼이 하나 있다. 아직 누를 이유가 없다."),
        otpCode = "482 019",
        mirrorNotYet = listOf("거울은… 아직이다.", "20년 만의 얼굴을 볼 자신이 없다."),
        doorCalm = listOf("문밖은 조용하다. 너무 조용하다."),
        doorSiege = listOf("복도 끝에서 무전기 잡음이 들린 것 같다.", "…기분 탓이다. 기분 탓이어야 한다."),
        windowCalm = listOf("월포항 불빛. 20년 만에 보는 바깥 야경."),
        windowSiege = listOf("월포항 쪽에서 붉고 푸른 불빛이 번쩍인다.", "저기서 어제 시체가 떠올랐다고 했지."),
        bed = listOf("스프링이 꺼진 침대. 20년 만의 진짜 침대인데, 눕고 싶지 않다."),
        records = listOf(
            DocId.BODYLOG to "[LOG] 상태 로그 #1",
            DocId.BINDER to "[FILE] 감사 바인더",
            DocId.MIRRORLOG to "[LOG] 상태 로그 #2",
        ),
    )

    val paranBoards: Map<ParanBoard, List<DocId>> = mapOf(
        ParanBoard.WRITE to listOf(DocId.PARAN_LH, DocId.PARAN_MW),
        ParanBoard.HUMOR to listOf(DocId.PARAN_JOKE, DocId.PARAN_IMF),
        ParanBoard.NOTICE to listOf(DocId.PARAN_NOTICE),
    )

    // ═════════════════════════════════════════════════════════════════════════
    //  사고정리(?) 힌트
    // ═════════════════════════════════════════════════════════════════════════

    val hints: List<HintRule> = listOf(
        HintRule(1, "목덜미 라벨, '해송 0419'. 웹을 열고 아래 칩을 눌러 검색하자.", Cond.Read(DocId.NOTICE)),
        HintRule(1, "공고문에 원장 이름이 있었다. 오병철. FaceBook 95에서 찾아보자.", Cond.Visited(ProfileId.FB_OH)),
        HintRule(1, "그 영감의 최근 게시물. 계약서 사진이 걸린다.", Cond.Read(DocId.POWER_CONTRACT)),
        HintRule(1, "공고문의 법인번호를 눌러 [PIN] 버퍼에 고정하고, 계약서의 법인번호를 눌러 대조하자.", Cond.Deduced(Deduction.ASYLUM_RENAMED)),
        HintRule(1, "오병철 자료실. 법인번호 10자리, 숫자만.", Cond.Unlocked(LockId.WEBHARD)),
        HintRule(1, "WATSON이 말한 파란여관. 메신저를 확인하자."),

        HintRule(2, "203호 탁자 위 믹스커피. 목부터 축이자.", Cond.CoffeeDrunk),
        HintRule(2, "WATSON이 말한 환풍구.", Cond.BinderFound),
        HintRule(2, "강민우. 웹에서 놈의 이력을 털어 보자.", Cond.Read(DocId.WIKI_KANG)),
        HintRule(2, "파란누리. PC통신 시절의 그 동호회. 홈 화면 앱에서 go paran.", Cond.Read(DocId.PARAN_LH)),
        HintRule(2, "강민우가 불탔다는 그 사고. 날짜를 알아야 한다.", Cond.Read(DocId.GAPYEONG)),
        HintRule(2, "서하린의 InstaPixel. 비공개 클라우드의 '모든 것이 무너진 날'은 아마 그 사고일.", Cond.Unlocked(LockId.CLOUD)),
        HintRule(2, "서하린 일기 속 '사고 전 강민우'. 내 상태 로그 #1과 나란히 놓아 보자.", Cond.Deduced(Deduction.BORROWED_HABITS)),
        HintRule(2, "금고95 마스터키, 첫 원고 등록일. 파란누리 게시판에 날짜가 있었다.", Cond.Unlocked(LockId.VAULT)),
        HintRule(2, "OTP 토큰. 203호 탁자 위.", Cond.OtpPressed),
        HintRule(2, "숨을 고르자. 뭔가 다가오고 있다."),

        HintRule(3, "등대 화재. 한도윤의 화상 기록이 어딘가 남아 있을 것이다. '백로도'를 웹에서 검색하자.", Cond.Read(DocId.BURN_BLOG)),
        HintRule(3, "거울. 20년 동안 피해 온 얼굴. 203호.", Cond.MirrorChecked),
        HintRule(3, "화상 기록과 거울 속 내 몸. 두 문장을 대조하자.", Cond.Deduced(Deduction.NOT_DOYUN)),
        HintRule(3, "설정. Proxy 1998. 이 필터가 무엇을 지우고 있었는지.", Cond.ProxyOff),
        HintRule(3, "복원된 박태성의 바인더를 다시 열어 보자. 메신저도.", Cond.Holding(Chip.LIGHTHOUSE_1998)),
        HintRule(3, "@lighthouse_1998. InstaPixel에서 검색. 마지막 문."),
    )

    fun hintFor(state: GameState): String {
        val chapter = state.chapter.coerceIn(1, 3)
        return hints.first { rule ->
            rule.chapter == chapter && (rule.doneWhen == null || !state.meets(rule.doneWhen))
        }.text
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  짧은 독백 · 로그 문장 · 홈 화면 문구
    // ═════════════════════════════════════════════════════════════════════════

    object Narration {
        const val CONTACT_WATSON = "WATSON"
        const val CONTACT_LIGHTHOUSE = "등대지기"

        fun chipAlreadyHeld(label: String) = "'$label'. 이미 쥔 단서다."
        fun searchElsewhere(label: String) = "'$label'. 검색은 웹·InstaPixel·FaceBook 95에서 할 수 있다."

        const val PICKER_EMPTY = "아직 쥔 단서가 없다. 문서 속 밑줄 단어를 눌러 칩을 모으자."
        const val PIN_FIXED = "고정했다. 이제 이 문장과 이어지거나 어긋나는 문장을 다른 문서에서 찾자."
        val crossMismatch = listOf("두 문장은 서로 아무 말도 하지 않는다.", "억지로 이어 붙일 수는 없다. 증거는 증거끼리.")
        const val CROSS_REPEAT = "이미 대조한 문장이다."

        val keypadWrong = listOf("틀렸다.")
        fun keypadWrongWithHint(hint: String) = listOf("틀렸다. 다시 생각하자.", "힌트: $hint")

        const val MODEM_DIAL = "삐—이이— 치이익. 28.8k 모뎀의 비명."
        const val PARAN_ENTER = "파란누리. 1998년이 그대로 얼어 있다."

        val spamCalm = listOf("스팸. 20년이 지나도 이것만은 변하지 않았다.")
        val spamSiege = listOf("…스팸이라기엔 너무 정확하다.")

        val noResults = listOf(
            "아무것도 없다. 세상은 생각보다 많은 걸 잊는다.",
            "검색 결과 없음. 이 단어는 여기서 쓸모가 없다.",
            "No Results Found. 다른 앱에서 찾아보자.",
        )

        const val DEMO_SKIPPED = "— 시연용 점프: 이전 대화 생략 —"
        const val DEMO_BEFORE_ENDING = "시연용 점프: 엔딩 직전. @lighthouse_1998 프로필을 열고 0419를 입력하자."

        const val HOME_PLACE_WAREHOUSE = "월포수산 제3냉동창고 앞"
        const val HOME_PLACE_INN = "월포읍 파란여관 203호"
        const val TICKER_PROXY_ON = "오늘의 운세: 남의 이름을 조심하세요 · Proxy 1998 경유 중"
        const val TICKER_PROXY_OFF = "PROXY 1998 해제됨 · 모든 이름이 원래 자리로 돌아왔습니다"

        /** 프록시 마스킹 자리에 찍히는 글자 */
        const val REDACTION_BLOCKS = "■■■"
        const val REDACTION_LABEL = "프록시 손상"
    }

    object LogLines {
        fun search(app: AppId, chip: Chip) = "[${app.title}] ${chip.label}"
        const val COFFEE_CONTACT = "커피잔 (타액 잔류)"
        const val OTP_AUTH = "하드웨어 OTP 토큰 작동 · 비자금 서버 접속"
        const val PROXY_KILLED = "Proxy 1998 강제 종료"
    }

    fun contactName(state: GameState): String =
        if (state.contactRevealed) Narration.CONTACT_LIGHTHOUSE else Narration.CONTACT_WATSON

    fun homePlace(state: GameState): String =
        if (state.chapter == 1) Narration.HOME_PLACE_WAREHOUSE else Narration.HOME_PLACE_INN

    fun homeTicker(state: GameState): String =
        if (state.proxyOff) Narration.TICKER_PROXY_OFF else Narration.TICKER_PROXY_ON

    // ═════════════════════════════════════════════════════════════════════════
    //  시연용 점프 (설정 앱)
    // ═════════════════════════════════════════════════════════════════════════

    private val demoChipsChapter1 = listOf(
        Chip.HAESONG_0419, Chip.OH_BYEONGCHEOL, Chip.WOLPO_EUP, Chip.WOLPO_SUSAN, Chip.MUJEOK_FOUNDATION, Chip.KANG_MINWOO,
    )
    private val demoChipsChapter2 = listOf(
        Chip.PARANNURI, Chip.MUJEOK_NOVEL, Chip.GAPYEONG_CRASH, Chip.SEO_HARIN, Chip.DATE_2006_11_03,
        Chip.BAEKRODO, Chip.LIGHTHOUSE_KEEPER, Chip.DATE_1998_05_11, Chip.MW419, Chip.VAULT95,
    )

    /**
     * 점프 직전 상태. CHAPTER_2 / CHAPTER_3 은 이 상태에서 챕터 진입 연출(goCh2 / goCh3)을 이어서 돌리고,
     * BEFORE_ENDING 은 이 상태 그대로 InstaPixel 검색 결과 화면을 연다.
     */
    fun demoState(jump: DemoJump): GameState {
        val origin = GameState(started = true)
        val chapterOne = origin.copy(
            chips = demoChipsChapter1,
            readDocs = setOf(DocId.NOTICE, DocId.POWER_CONTRACT, DocId.SETTLEMENT),
            visitedProfiles = setOf(ProfileId.FB_OH),
            deductions = setOf(Deduction.ASYLUM_RENAMED),
            unlocked = setOf(LockId.WEBHARD),
            firedBeats = setOf(
                StoryEvent.DocumentOpened(DocId.NOTICE).key,
                StoryEvent.ProfileVisited(ProfileId.FB_OH).key,
                StoryEvent.DocumentOpened(DocId.SETTLEMENT).key,
            ),
            messages = listOf(ChatMessage(Sender.SYSTEM, Narration.DEMO_SKIPPED, origin.clock)),
        )
        if (jump == DemoJump.CHAPTER_2) return chapterOne

        val chapterTwo = chapterOne.copy(
            chapter = 2,
            chips = chapterOne.chips + demoChipsChapter2,
            readDocs = chapterOne.readDocs + setOf(
                DocId.BODYLOG, DocId.BINDER, DocId.WIKI_KANG, DocId.PARAN_LH,
                DocId.GAPYEONG, DocId.DIARY, DocId.VAULT_SUMMARY,
            ),
            completedCommands = setOf(Command.GO_CHAPTER_2),
            coffeeAt = "00:01",
            binderFound = true,
            deductions = chapterOne.deductions + Deduction.BORROWED_HABITS,
            unlocked = chapterOne.unlocked + setOf(LockId.CLOUD, LockId.VAULT),
            otpPressed = true,
            chapter3Scheduled = true,
            firedBeats = chapterOne.firedBeats + setOf(
                StoryEvent.CoffeeDrunk.key,
                StoryEvent.BinderFound.key,
                StoryEvent.DocumentOpened(DocId.PARAN_LH).key,
                StoryEvent.DocumentOpened(DocId.DIARY).key,
                StoryEvent.DocumentOpened(DocId.VAULT_SUMMARY).key,
            ),
            glitch = 2,
            minutes = 50,
        )
        if (jump == DemoJump.CHAPTER_3) return chapterTwo

        val beforeEnding = chapterTwo.copy(
            chapter = 3,
            battery = 9,
            chips = chapterTwo.chips + listOf(Chip.PARK_TAESEONG, Chip.LIGHTHOUSE_1998),
            readDocs = chapterTwo.readDocs + setOf(DocId.NEWS_PARK, DocId.BURN_BLOG, DocId.MIRRORLOG),
            mirrorAt = "00:31",
            deductions = chapterTwo.deductions + Deduction.NOT_DOYUN,
            proxyOff = true,
            glitch = 3,
            minutes = 78,
        )
        val at = beforeEnding.clock
        return beforeEnding.copy(
            log = listOf(
                LogEntry(at, LogKind.SEARCH, "[ParanNet 검색] 백로도"),
                LogEntry(at, LogKind.CONTACT, LogLines.COFFEE_CONTACT),
                LogEntry(at, LogKind.AUTH, "하드웨어 OTP 토큰 작동"),
                LogEntry(at, LogKind.SYSTEM, LogLines.PROXY_KILLED),
            ),
        )
    }
}
