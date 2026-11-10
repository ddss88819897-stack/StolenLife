package kr.paranos.stolenlife.ui.components

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kr.paranos.stolenlife.data.AppId
import kr.paranos.stolenlife.data.Picture
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * 1-bit 디더링 그림 공장. 웹판 PAINT · IPAINT · roomURL() · dither() 를 그대로 옮겼다.
 *
 * 밑그림은 흑백 계조(그라데이션 포함)로 그리지만, 화면에 나가는 것은 Bayer 4×4 로 두 색만 남긴
 * 결과물이다. 그래서 화면에는 그라데이션이 한 픽셀도 나가지 않는다. 표시할 때는
 * FilterQuality.None 으로 키워서 도트가 그대로 보이게 한다.
 *
 * 그림은 키마다 한 번만 만들고 캐시한다. 무작위 노이즈는 고정 시드라 매번 같은 그림이 나온다.
 */
object PixelArt {

    private val cache = HashMap<String, ImageBitmap>()

    fun picture(picture: Picture): ImageBitmap = cache.getOrPut("pic:${picture.name}") {
        dither(picture.width, picture.height, picture.ink.toInt(), picture.ground.toInt()) { sketch ->
            photoPainter(picture)(sketch)
        }
    }

    fun icon(app: AppId): ImageBitmap = cache.getOrPut("icon:${app.name}") {
        dither(ICON_SIZE, ICON_SIZE, ICON_INK, ICON_GROUND) { sketch ->
            sketch.fill(sketch.gray(12))
            sketch.rect(0f, 0f, 64f, 64f)
            iconPainter(app)(sketch)
        }
    }

    /** 203호 방 그림: 디더링하지 않은 저해상도 컬러 도트(120×160 을 2배로 그림). */
    fun room(): ImageBitmap = cache.getOrPut("room") { paintRoom().asImageBitmap() }

    /** 상태바 등대 스프라이트 8×12 */
    fun beacon(): ImageBitmap = cache.getOrPut("beacon") {
        sprite(LIGHT_SPRITE, mapOf('y' to 0xFFC9A96E.toInt(), 'w' to 0xFFAEB8B1.toInt(), 'r' to 0xFF8C2D2D.toInt(), 'k' to 0xFF1A1D1C.toInt()))
            .asImageBitmap()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  디더링 · 스프라이트
    // ═════════════════════════════════════════════════════════════════════════

    private val BAYER = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

    private fun dither(width: Int, height: Int, ink: Int, ground: Int, paint: (Sketch) -> Unit): ImageBitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLACK)
        paint(Sketch(canvas, width.toFloat(), height.toFloat()))
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (y in 0 until height) for (x in 0 until width) {
            val c = pixels[y * width + x]
            val luminance = (((c shr 16) and 0xFF) * 0.3f + ((c shr 8) and 0xFF) * 0.59f + (c and 0xFF) * 0.11f) / 255f
            pixels[y * width + x] = if (luminance > (BAYER[(y % 4) * 4 + x % 4] + 0.5f) / 16f) ink else ground
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap.asImageBitmap()
    }

    private fun sprite(rows: List<String>, palette: Map<Char, Int>): Bitmap {
        val bitmap = Bitmap.createBitmap(rows[0].length, rows.size, Bitmap.Config.ARGB_8888)
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch -> palette[ch]?.let { bitmap.setPixel(x, y, it) } }
        }
        return bitmap
    }

    private const val ICON_SIZE = 64
    private val ICON_INK = 0xFFC9D3C9.toInt()
    private val ICON_GROUND = 0xFF0F1312.toInt()

    private val LIGHT_SPRITE = listOf(
        "...yy...", "..yyyy..", "..kkkk..", "...ww...", "..wrrw..", "..wwww..",
        "..wrrw..", "..wwww..", ".wwrrww.", ".wwwwww.", "wwwwwwww", "kkkkkkkk",
    )

    private val FACE = listOf(
        ".....hhhhhh.....", "...hhhhhhhhhh...", "..hhhhhhhhhhhh..", "..hhsssssssshh..",
        "..hssssssssssh..", ".dsskksssskkssd.", ".dssssssssssssd.", ".dssssssdssssssd",
        "..dssssssssssd..", "..dssskkkksssd..", "...dssssssssd...", "....dssssssd....",
        ".....dssssd.....", "....kksssskk....", "..kkkkkkkkkkkk..", ".kkkkkkkkkkkkkk.",
    )
    private val FACE_TONE = mapOf('h' to 45, 's' to 205, 'd' to 120, 'k' to 25)

    // ═════════════════════════════════════════════════════════════════════════
    //  캔버스 도우미 (웹 CanvasRenderingContext2D 흉내)
    // ═════════════════════════════════════════════════════════════════════════

    private class Sketch(val c: Canvas, val w: Float, val h: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        fun gray(v: Number): Int {
            val g = v.toInt().coerceIn(0, 255)
            return android.graphics.Color.rgb(g, g, g)
        }

        fun rgba(r: Int, g: Int, b: Int, a: Float): Int = android.graphics.Color.argb((a * 255).roundToInt(), r, g, b)

        private fun reset(style: Paint.Style) {
            p.reset()
            p.isAntiAlias = true
            p.style = style
        }

        fun fill(color: Int) {
            reset(Paint.Style.FILL)
            p.color = color
        }

        /** createLinearGradient(x0, y0, x1, y1) 에 회색 두 개 */
        fun fillLinear(x0: Float, y0: Float, x1: Float, y1: Float, a: Int, b: Int) {
            reset(Paint.Style.FILL)
            p.shader = LinearGradient(x0, y0, x1, y1, gray(a), gray(b), Shader.TileMode.CLAMP)
        }

        /** grad(x, y0, y1, a, b): 세로 그라데이션 */
        fun fillVertical(y0: Float, y1: Float, a: Int, b: Int) = fillLinear(0f, y0, 0f, y1, a, b)

        fun fillRadial(cx: Float, cy: Float, radius: Float, colors: IntArray, stops: FloatArray? = null) {
            reset(Paint.Style.FILL)
            p.shader = RadialGradient(cx, cy, radius, colors, stops, Shader.TileMode.CLAMP)
        }

        fun stroke(color: Int, width: Float) {
            reset(Paint.Style.STROKE)
            p.color = color
            p.strokeWidth = width
        }

        fun rect(x: Float, y: Float, width: Float, height: Float) {
            c.drawRect(minOf(x, x + width), minOf(y, y + height), maxOf(x, x + width), maxOf(y, y + height), p)
        }

        fun full() = rect(0f, 0f, w, h)

        fun circle(cx: Float, cy: Float, r: Float) = c.drawCircle(cx, cy, r, p)

        fun oval(cx: Float, cy: Float, rx: Float, ry: Float, rotationRad: Float = 0f) {
            if (rotationRad == 0f) {
                c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, p)
            } else {
                c.save()
                c.rotate(Math.toDegrees(rotationRad.toDouble()).toFloat(), cx, cy)
                c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, p)
                c.restore()
            }
        }

        /** arc(cx, cy, r, from, to): 라디안, 시계 방향 */
        fun arc(cx: Float, cy: Float, r: Float, fromRad: Double, toRad: Double) {
            val start = Math.toDegrees(fromRad).toFloat()
            val sweep = Math.toDegrees(toRad - fromRad).toFloat()
            c.drawArc(cx - r, cy - r, cx + r, cy + r, start, sweep, false, p)
        }

        fun poly(vararg xy: Float) {
            val path = Path()
            path.moveTo(xy[0], xy[1])
            var i = 2
            while (i < xy.size) {
                path.lineTo(xy[i], xy[i + 1])
                i += 2
            }
            path.close()
            c.drawPath(path, p)
        }

        fun line(x0: Float, y0: Float, x1: Float, y1: Float) = c.drawLine(x0, y0, x1, y1, p)

        /** rrect(): 아이콘 밑그림의 둥근 사각형. 디더링 전 단계에서만 쓴다. */
        fun roundRect(x: Float, y: Float, width: Float, height: Float, r: Float) =
            c.drawRoundRect(RectF(x, y, x + width, y + height), r, r, p)

        fun text(value: String, x: Float, y: Float, size: Float, typeface: Typeface, align: Paint.Align = Paint.Align.LEFT) {
            p.textSize = size
            p.typeface = typeface
            p.textAlign = align
            c.drawText(value, x, y, p)
        }

        /** 웹판 face(): 16×16 얼굴 스프라이트. 크게 찍을 때는 흐리게. */
        fun face(cx: Float, cy: Float, scale: Float) {
            c.save()
            FACE.forEachIndexed { y, row ->
                row.forEachIndexed { i, ch ->
                    val tone = FACE_TONE[ch] ?: return@forEachIndexed
                    fill(gray(tone))
                    if (scale >= 3) p.maskFilter = BlurMaskFilter(scale * 0.45f, BlurMaskFilter.Blur.NORMAL)
                    rect(cx + (i - 8) * scale, cy + (y - 8) * scale, scale, scale)
                }
            }
            p.maskFilter = null
            c.restore()
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  사진 (웹판 PAINT)
    // ═════════════════════════════════════════════════════════════════════════

    private fun photoPainter(picture: Picture): (Sketch) -> Unit = when (picture) {
        Picture.WALL -> ::paintWall
        Picture.CONTRACT -> ::paintContract
        Picture.FISH -> ::paintFish
        Picture.LIGHT -> ::paintLight
        Picture.COFFEE -> ::paintCoffee
        Picture.KANG -> ::paintKang
        Picture.KANGCUP -> ::paintKangCup
        Picture.PODIUM -> ::paintPodium
        Picture.RADIO -> ::paintRadio
        Picture.CCTV -> ::paintCctv
        Picture.SCAN -> ::paintScan
        Picture.GROUP -> ::paintGroup
        Picture.FOOD -> ::paintFood
        Picture.ISLAND -> ::paintIsland
        Picture.HARBOR -> ::paintHarbor
    }

    private fun paintWall(s: Sketch) = with(s) {
        fillVertical(0f, h, 10, 70); full()
        fill(rgba(255, 255, 255, 0.18f)); poly(w * .62f, h * .36f, -w * .2f, h * .12f, -w * .2f, h * .5f)
        fill(gray(150)); poly(w * .6f, h * .38f, w * .66f, h * .38f, w * .69f, h * .62f, w * .57f, h * .62f)
        fill(gray(240)); rect(w * .6f, h * .34f, w * .06f, h * .04f)
        fill(gray(35)); rect(0f, h * .62f, w, h)
        fill(gray(55)); for (i in 0 until 9) rect(0f, h * (.66f + i * .04f), w, 1f)
    }

    private fun paintContract(s: Sketch) = with(s) {
        fill(gray(25)); full()
        c.save()
        c.translate(w / 2, h / 2)
        c.rotate(Math.toDegrees(-0.07).toFloat())
        fill(gray(232)); rect(-w * .36f, -h * .44f, w * .72f, h * .88f)
        fill(gray(60)); rect(-w * .28f, -h * .36f, w * .56f, h * .07f)
        fill(gray(125)); for (i in 0 until 8) rect(-w * .28f, -h * .22f + i * h * .075f, w * (if (i % 3 != 0) .42f else .56f), h * .028f)
        stroke(gray(80), 2f); circle(w * .18f, h * .3f, h * .08f)
        c.restore()
    }

    private fun paintFish(s: Sketch) = with(s) {
        fillVertical(0f, h * .55f, 210, 120); rect(0f, 0f, w, h * .55f)
        fillVertical(h * .55f, h, 90, 30); rect(0f, h * .55f, w, h)
        fill(gray(20)); circle(w * .4f, h * .3f, h * .09f); rect(w * .32f, h * .39f, w * .16f, h * .42f)
        fill(gray(235)); oval(w * .62f, h * .5f, w * .14f, h * .06f); poly(w * .76f, h * .5f, w * .85f, h * .42f, w * .85f, h * .58f)
    }

    private fun paintLight(s: Sketch) = with(s) {
        fillVertical(0f, h, 25, 100); full()
        fill(rgba(255, 255, 255, 0.4f)); poly(w * .5f, h * .27f, w * 1.1f, h * .08f, w * 1.1f, h * .44f)
        fill(gray(215)); poly(w * .44f, h * .3f, w * .56f, h * .3f, w * .6f, h * .85f, w * .4f, h * .85f)
        fill(gray(70)); rect(w * .41f, h * .48f, w * .18f, h * .06f); rect(w * .4f, h * .66f, w * .2f, h * .06f)
        fill(gray(250)); rect(w * .46f, h * .21f, w * .08f, h * .09f)
        fill(gray(45)); rect(0f, h * .85f, w, h * .15f)
    }

    private fun paintCoffee(s: Sketch) = with(s) {
        fillVertical(0f, h, 125, 55); full()
        listOf(.32f to false, .68f to true).forEach { (cx, black) ->
            fill(gray(232)); oval(w * cx, h * .56f, w * .13f, h * .17f)
            fill(gray(if (black) 35 else 160)); oval(w * cx, h * .46f, w * .1f, h * .06f)
        }
        fill(gray(215)); rect(w * .1f, h * .78f, w * .1f, h * .035f); rect(w * .12f, h * .84f, w * .1f, h * .035f)
    }

    private fun paintKang(s: Sketch) = with(s) {
        fillVertical(0f, h, 170, 90); full()
        fill(gray(28)); oval(w * .5f, h * 1.02f, w * .42f, h * .32f)
        fill(gray(235)); poly(w * .46f, h * .72f, w * .54f, h * .72f, w * .5f, h * .86f)
        face(w * .5f, h * .44f, max(2, (h / 28).roundToInt()).toFloat())
    }

    private fun paintKangCup(s: Sketch) = with(s) {
        paintKang(s)
        fill(gray(240)); rect(w * .66f, h * .62f, w * .14f, h * .16f)
        fill(gray(20)); rect(w * .67f, h * .63f, w * .12f, h * .03f)
        fill(gray(200)); rect(w * .62f, h * .74f, w * .2f, h * .1f)
    }

    private fun paintPodium(s: Sketch) = with(s) {
        fillVertical(0f, h, 60, 150); full()
        fill(gray(220)); rect(w * .1f, h * .08f, w * .8f, h * .14f)
        fill(gray(60)); rect(w * .18f, h * .12f, w * .64f, h * .05f)
        face(w * .5f, h * .46f, max(2, (h / 30).roundToInt()).toFloat())
        fill(gray(30)); rect(w * .34f, h * .66f, w * .32f, h * .34f)
        fill(gray(180)); rect(w * .3f, h * .7f, w * .4f, h * .3f)
    }

    private fun paintRadio(s: Sketch) = with(s) {
        fill(gray(18)); full()
        fill(gray(240)); rect(w * .66f, h * .07f, w * .26f, h * .12f)
        fill(gray(20)); rect(w * .69f, h * .1f, w * .2f, h * .06f)
        face(w * .4f, h * .46f, max(2, (h / 28).roundToInt()).toFloat())
        stroke(gray(160), max(2f, h / 40)); arc(w * .4f, h * .44f, h * .25f, PI * 1.08, PI * 1.92)
        fill(gray(130)); rect(w * .66f, h * .45f, w * .12f, h * .12f); rect(w * .71f, h * .57f, w * .02f, h * .43f)
        fill(gray(255)); rect(w * .43f, h * .5f, max(1f, h / 60), h * .08f)
    }

    private fun paintCctv(s: Sketch) = with(s) {
        val noise = Random(419)
        for (y in 0 until h.toInt()) for (i in 0 until w.toInt()) {
            fill(gray(30 + noise.nextFloat() * 50)); rect(i.toFloat(), y.toFloat(), 1f, 1f)
        }
        fill(gray(80)); rect(0f, h * .78f, w, h * .22f)
        fill(gray(150)); rect(w * .38f, h * .55f, w * .24f, h * .3f); rect(w * .3f, h * .3f, w * .08f, h * .3f); rect(w * .62f, h * .3f, w * .08f, h * .3f)
        face(w * .5f, h * .42f, max(1, (h / 34).roundToInt()).toFloat())
        fill(gray(255)); text("CAM03 0419", 3f, h / 9 + 2, max(6f, h / 10), Typeface.MONOSPACE)
    }

    private fun paintScan(s: Sketch) = with(s) {
        fill(gray(40)); full()
        fill(gray(222)); rect(w * .1f, h * .06f, w * .8f, h * .9f)
        fill(gray(90)); for (i in 0 until 10) rect(w * .16f, h * (.14f + i * .07f), w * (if (i % 4 != 0) .5f else .66f), h * .02f)
        stroke(gray(60), 1.5f); oval(w * .72f, h * .66f, w * .07f, h * .1f)
        fill(gray(30)); rect(w * .66f, h * .58f, w * .05f, h * .04f)
    }

    private fun paintGroup(s: Sketch) = with(s) {
        fillVertical(0f, h, 200, 120); full()
        fill(gray(150)); rect(w * .05f, h * .1f, w * .9f, h * .45f)
        fill(gray(90)); for (i in 0 until 6) rect(w * (.1f + i * .14f), h * .18f, w * .08f, h * .12f)
        for (i in 0 until 5) {
            fill(gray(25)); circle(w * (.18f + i * .16f), h * .56f, h * .07f); rect(w * (.13f + i * .16f), h * .62f, w * .1f, h * .38f)
        }
    }

    private fun paintFood(s: Sketch) = with(s) {
        fill(gray(70)); full()
        fill(gray(230)); oval(w * .5f, h * .55f, w * .38f, h * .3f)
        fill(gray(110)); for (i in 0 until 9) oval(w * (.3f + (i % 3) * .2f), h * (.45f + floor(i / 3f) * .1f), w * .06f, h * .03f, i.toFloat())
    }

    private fun paintIsland(s: Sketch) = with(s) {
        fillVertical(0f, h * .6f, 230, 160); full()
        fill(gray(250)); circle(w * .75f, h * .25f, h * .1f)
        fillVertical(h * .6f, h, 120, 60); rect(0f, h * .6f, w, h)
        fill(gray(40)); c.drawArc(w * .35f - w * .25f, h * .6f - h * .1f, w * .35f + w * .25f, h * .6f + h * .1f, 180f, 180f, false, p)
        fill(gray(200)); rect(w * .38f, h * .38f, w * .03f, h * .14f)
    }

    private fun paintHarbor(s: Sketch) = with(s) {
        fillVertical(0f, h, 15, 50); full()
        fill(gray(230)); for (i in 0 until 14) rect((i * 37) % w, h * .55f + (i * 13) % (h * .1f), 1f, 1f)
        fill(gray(70)); rect(0f, h * .65f, w, h * .35f)
        fill(gray(25)); rect(w * .5f, h * .4f, w * .4f, h * .26f)
        fill(gray(120)); rect(w * .56f, h * .46f, w * .28f, h * .03f)
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  홈 아이콘 64×64 (웹판 IPAINT)
    // ═════════════════════════════════════════════════════════════════════════

    private fun iconPainter(app: AppId): (Sketch) -> Unit = when (app) {
        AppId.MESSENGER -> ::iconMessenger
        AppId.WEB -> ::iconWeb
        AppId.INSTA -> ::iconInsta
        AppId.FACEBOOK -> ::iconFacebook
        AppId.PARAN -> ::iconParan
        AppId.VAULT -> ::iconVault
        AppId.ROOM -> ::iconRoom
        AppId.SETTINGS -> ::iconSettings
    }

    private val MONO_BOLD: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    private val SERIF_BOLD: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)

    private fun iconMessenger(s: Sketch) = with(s) {
        fillVertical(10f, 46f, 228, 118); roundRect(8f, 10f, 48f, 34f, 7f); poly(18f, 43f, 13f, 55f, 29f, 43f)
        fill(gray(28)); listOf(Triple(16f, 19f, 32f), Triple(16f, 26f, 26f), Triple(16f, 33f, 18f)).forEach { (a, b, width) -> rect(a, b, width, 3f) }
        stroke(gray(255), 1f); roundRect(9.5f, 11.5f, 45f, 31f, 6f)
    }

    private fun iconWeb(s: Sketch) = with(s) {
        fillRadial(28f, 27f, 26f, intArrayOf(gray(250), gray(70))); circle(32f, 32f, 23f)
        stroke(gray(18), 1.6f); circle(32f, 32f, 23f)
        listOf(9f, 16f).forEach { rx -> oval(32f, 32f, rx, 23f) }
        line(32f, 9f, 32f, 55f)
        listOf(-12f, 0f, 12f).forEach { dy ->
            val hw = sqrt(23f * 23f - dy * dy)
            line(32f - hw, 32f + dy, 32f + hw, 32f + dy)
        }
    }

    private fun iconInsta(s: Sketch) = with(s) {
        fillVertical(16f, 52f, 212, 108); roundRect(7f, 18f, 50f, 34f, 5f); rect(16f, 13f, 14f, 7f)
        fill(gray(22)); circle(32f, 35f, 13f)
        fillRadial(30f, 33f, 10f, intArrayOf(gray(245), gray(120), gray(18)), floatArrayOf(0f, .35f, 1f)); circle(32f, 35f, 9.5f)
        fill(gray(250)); rect(46f, 22f, 6f, 4f)
        stroke(gray(255), 1f); roundRect(8.5f, 19.5f, 47f, 31f, 4f)
    }

    private fun iconFacebook(s: Sketch) = with(s) {
        fillVertical(8f, 56f, 150, 52); roundRect(9f, 8f, 46f, 48f, 6f)
        fill(gray(246)); text("f", 30f, 50f, 40f, SERIF_BOLD, Paint.Align.CENTER); text("95", 46f, 52f, 10f, MONO_BOLD, Paint.Align.CENTER)
        stroke(gray(205), 1f); roundRect(10.5f, 9.5f, 43f, 45f, 5f)
    }

    private fun iconParan(s: Sketch) = with(s) {
        fillVertical(8f, 46f, 218, 128); roundRect(7f, 7f, 50f, 38f, 4f)
        fill(gray(16)); rect(12f, 12f, 40f, 28f)
        fill(gray(228)); text(">go", 15f, 22f, 9f, MONO_BOLD); rect(15f, 27f, 22f, 2f); rect(15f, 32f, 14f, 2f); rect(31f, 32f, 5f, 2f)
        fill(gray(150)); rect(27f, 45f, 10f, 5f)
        fillVertical(50f, 58f, 205, 88); rect(12f, 50f, 40f, 7f)
        fill(gray(55)); for (i in 0 until 9) rect(14f + i * 4, 52f, 2f, 1f)
    }

    private fun iconVault(s: Sketch) = with(s) {
        fillVertical(6f, 58f, 205, 68); roundRect(8f, 7f, 48f, 48f, 3f)
        fill(gray(38)); rect(12f, 11f, 40f, 40f)
        fillVertical(12f, 50f, 172, 78); rect(14f, 13f, 36f, 36f)
        fillRadial(30.5f, 29.5f, 11f, intArrayOf(gray(246), gray(88))); circle(32f, 31f, 10f)
        stroke(gray(18), 1.2f)
        for (i in 0 until 12) {
            val a = i * PI / 6
            line(32f + cos(a).toFloat() * 11, 31f + sin(a).toFloat() * 11, 32f + cos(a).toFloat() * 13.5f, 31f + sin(a).toFloat() * 13.5f)
        }
        fill(gray(22)); rect(31f, 22f, 2f, 7f)
        fill(gray(232)); rect(44f, 26f, 3f, 12f)
        fill(gray(28)); rect(12f, 55f, 7f, 3f); rect(45f, 55f, 7f, 3f)
    }

    private fun iconRoom(s: Sketch) = with(s) {
        fill(gray(28)); rect(14f, 4f, 36f, 56f)
        fillLinear(16f, 0f, 48f, 0f, 178, 92); rect(17f, 7f, 30f, 51f)
        stroke(gray(58), 1.2f); rect(21f, 23f, 22f, 13f); rect(21f, 40f, 22f, 14f)
        fill(gray(238)); rect(25f, 11f, 14f, 8f)
        fill(gray(18)); text("203", 32f, 17.5f, 7f, MONO_BOLD, Paint.Align.CENTER)
        fill(gray(250)); circle(42f, 36f, 2f)
        fill(gray(240)); rect(17f, 58f, 30f, 2f)
    }

    private fun iconSettings(s: Sketch) = with(s) {
        val cx = 32f
        val cy = 32f
        val n = 10
        val gear = Path()
        for (i in 0 until n * 2) {
            val a = i * PI / n
            val r = if (i % 2 != 0) 18f else 24f
            val a2 = a + PI / n * .5
            val x1 = cx + cos(a).toFloat() * r
            val y1 = cy + sin(a).toFloat() * r
            if (i == 0) gear.moveTo(x1, y1) else gear.lineTo(x1, y1)
            gear.lineTo(cx + cos(a2).toFloat() * r, cy + sin(a2).toFloat() * r)
        }
        gear.close()
        fillRadial(29f, 28f, 26f, intArrayOf(gray(246), gray(82))); c.drawPath(gear, p)
        stroke(gray(18), 1.2f); c.drawPath(gear, p)
        fill(gray(14)); circle(cx, cy, 8f)
        stroke(gray(205), 1.2f); circle(cx, cy, 12f)
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  203호 (웹판 roomURL)
    // ═════════════════════════════════════════════════════════════════════════

    private fun paintRoom(): Bitmap {
        val width = 120
        val height = 160
        val scale = 2
        val bitmap = Bitmap.createBitmap(width * scale, height * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale.toFloat(), scale.toFloat())
        val paint = Paint()
        fun r(color: Long, a: Number, b: Number, cw: Number, ch: Number) {
            paint.color = color.toInt()
            canvas.drawRect(a.toFloat(), b.toFloat(), a.toFloat() + cw.toFloat(), b.toFloat() + ch.toFloat(), paint)
        }
        r(0xFF18202B, 0, 0, width, 104); for (i in 0 until width step 6) r(0xFF1C2632, i, 0, 3, 104)
        r(0xFF2B1F16, 0, 104, width, 56); for (y in 108 until height step 8) r(0xFF211810, 0, y, width, 1)
        r(0xFF3A3A3A, 9, 16, 36, 28); r(0xFF0A1430, 11, 18, 32, 24)
        listOf(14 to 34, 20 to 30, 27 to 36, 33 to 31, 38 to 35, 17 to 38, 30 to 39).forEach { (a, b) -> r(0xFFCFAE62, a, b, 1, 1) }
        r(0xFF3A3A3A, 26, 18, 1, 24)
        r(0xFF4A4F55, 87, 9, 24, 14); for (i in 0 until 5) r(0xFF202428, 89, 11 + (i * 2.4).roundToInt(), 20, 1)
        r(0xFF6D5A3A, 7, 50, 22, 34); r(0xFF7F97A3, 9, 52, 18, 30); r(0xFFA9C0CA, 11, 54, 3, 12)
        r(0xFF3A2416, 91, 46, 26, 60); r(0xFF4A3020, 93, 48, 22, 56); r(0xFF3A2416, 96, 52, 16, 20); r(0xFF3A2416, 96, 78, 16, 22); r(0xFFD9B44A, 111, 78, 2, 3)
        r(0xFF5B4636, 2, 110, 52, 30); r(0xFFC9C1B2, 3, 106, 50, 22); r(0xFFE4DDD0, 5, 104, 16, 8); r(0xFF9A9284, 3, 124, 50, 4)
        r(0xFF5A3A22, 58, 104, 36, 5); r(0xFF3E2716, 60, 109, 3, 24); r(0xFF3E2716, 89, 109, 3, 24)
        r(0xFFEEEEEE, 63, 97, 7, 7); r(0xFFCFCFCF, 70, 99, 2, 3); r(0xFF6B4A2A, 64, 97, 5, 1); r(0xFFD8C8A0, 72, 102, 4, 2)
        r(0xFF111111, 79, 100, 8, 4); r(0xFFB4483C, 85, 101, 1, 1); r(0xFF7A7A7A, 80, 101, 3, 2)
        // 백열등 한 점의 빛 번짐. 원본 픽셀 아트의 일부로, 아래에서 노이즈와 함께 도트에 구워진다.
        paint.shader = RadialGradient(
            76f, 70f, 85f,
            intArrayOf(0x38FFDC96, 0x80000000.toInt()),
            floatArrayOf(4f / 85f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val noise = Random(203)
        for (i in pixels.indices) {
            val c = pixels[i]
            val n = ((noise.nextFloat() - .5f) * 14).roundToInt()
            val red = (((c shr 16) and 0xFF) + n).coerceIn(0, 255)
            val green = (((c shr 8) and 0xFF) + n).coerceIn(0, 255)
            val blue = ((c and 0xFF) + n).coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }
}
