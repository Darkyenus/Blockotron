import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBTruetype.*;

/**
 *
 */
public class FontPacker {

	private static ByteBuffer loadFont (File file) {
		try {
			if (!file.isFile()) throw new RuntimeException("File not found: " + file.getAbsolutePath());
			final ByteBuffer data = ByteBuffer.allocateDirect((int)file.length());
			final FileInputStream in = new FileInputStream(file);
			while (true) {
				final int read = in.read();
				if (read == -1) break;
				data.put((byte)read);
			}
			in.close();
			data.flip();
			return data;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void packFont (File font, File outDir, int sizePx) {
		final STBTTFontinfo fontInfo = STBTTFontinfo.malloc();
		final ByteBuffer fontByteData = loadFont(font);
		if (stbtt_InitFont(fontInfo, fontByteData, stbtt_GetFontOffsetForIndex(fontByteData, 0)) == 0)
			throw new RuntimeException("Init failed");

		final FontMetrics fontMetrics = FontMetrics.getFontMetrics(fontInfo, sizePx);
		System.out.println(fontMetrics);

		final Array<GlyphData> glyphs = new Array<>();

		for (int codePoint = 0; codePoint < 0xFFFF; codePoint++) {
			final int glyphIndex = stbtt_FindGlyphIndex(fontInfo, codePoint);
			if (glyphIndex == 0 && codePoint != 0) continue;

			glyphs.add(GlyphData.create(fontInfo, codePoint, glyphIndex, sizePx));
		}

		// Guess best texture size
		final int pageWidth, pageHeight;
		{
			int approxPixels = sizePx * sizePx * glyphs.size;
			int side = MathUtils.nextPowerOfTwo((int)Math.sqrt(approxPixels));
			pageWidth = side;
			int height = side;
			while (pageWidth * (height >> 1) >= approxPixels) {
				height = height >> 1;
			}

			pageHeight = height;
		}
		System.out.println("Rendering " + glyphs.size + " glyphs to pages " + pageWidth + " x " + pageHeight);

		final PixmapPacker packer = new PixmapPacker(pageWidth, pageHeight, Pixmap.Format.Alpha, 1, false);
		packer.setPackToTexture(false);

		for (GlyphData glyph : glyphs) {
			final Pixmap pixmap = glyph.createPixmap();
			if (pixmap == null) continue;
			final String name = new String(Character.toChars(glyph.codePoint));
			final Rectangle pack = packer.pack(name, pixmap);
			glyph.page = packer.getPageIndex(name);
			glyph.packed = pack;
		}

		final String fontName;

		{
			final String fileName = font.getName();
			int separator = fileName.lastIndexOf('.');
			if (separator == -1) separator = fileName.length();
			fontName = fileName.substring(0, separator);
		}

        //outputFNT(outDir, fontName, sizePx, fontMetrics, packer, glyphs);
        outputBlockofont(outDir, fontName, sizePx, fontMetrics, packer, glyphs);

		System.out.println("Done");
	}

    private static void outputFNT(File outDir, String fontName, int sizePx, FontMetrics fontMetrics, PixmapPacker packer, Array<GlyphData> glyphs) {
        final StringBuilder fnt = new StringBuilder();
        fnt.append("info face=\"").append(fontName).append("\" size=").append(sizePx)
                .append(" bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=1 padding=1,1,1,1 spacing=1,1 outline=0");
        fnt.append('\n');
        fnt.append("common lineHeight=").append(sizePx).append(" base=").append(sizePx - fontMetrics.descent).append(" scaleW=")
                .append(packer.getPageWidth()).append(" scaleH=").append(packer.getPageHeight()).append(" pages=").append(packer.getPages().size)
                .append(" packed=0 alphaChnl=0 redChnl=4 greenChnl=4 blueChnl=4");
        fnt.append('\n');

        int pageNo = 0;
        for (PixmapPacker.Page page : packer.getPages()) {
            final File file = new File(outDir, fontName + pageNo + ".png");
            PixmapIO.writePNG(new FileHandle(file), page.getPixmap());
            fnt.append("page id=").append(pageNo).append(" file=\"").append(file.getName()).append("\"");
            fnt.append('\n');
            pageNo++;
        }

        for (GlyphData glyph : glyphs) {
            fnt.append("char");
            fnt.append(" id=").append(glyph.codePoint);
            if (glyph.packed == null) {
                fnt.append(" x=0 y=0 width=0 height=0");
            } else {
                fnt.append(" x=").append(MathUtils.round(glyph.packed.x));
                fnt.append(" y=").append(MathUtils.round(glyph.packed.y));
                fnt.append(" width=").append(MathUtils.round(glyph.packed.width));
                fnt.append(" height=").append(MathUtils.round(glyph.packed.height));
            }
            fnt.append(" xoffset=").append(glyph.xOff);
            fnt.append(" yoffset=").append(fontMetrics.ascent + glyph.yOff);
            fnt.append(" xadvance=").append(glyph.advanceWidth);
            fnt.append(" page=").append(glyph.page);
            fnt.append(" chnl=15");

            fnt.append('\n');
        }

        new FileHandle(new File(outDir, fontName + ".fnt")).writeString(fnt.toString(), false, "UTF-8");
    }

    /**
     * <code>
     * u-byte pages;
     * [pages]{
     *     UTF pagePath; //Relative to fontFile
     * };
     * float lineHeight;
     * float ascent; //Real ascent, not just to the top of glyph
     * float descent; //From baseline down
     *
     * int amountOfGlyphs;
     * [amountOfGlyphs] {
     *     int codePoint;
     *     u-byte page;
     *     unsigned short pageX, pageY, pageWidth, pageHeight; //Coordinates on page
     *     unsigned short offsetX, offsetY, xAdvance;
     * };
     * </code>
     */
    private static void outputBlockofont(File outDir, String fontName, int sizePx, FontMetrics fontMetrics, PixmapPacker packer, Array<GlyphData> glyphs){
        try(final DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(outDir, fontName+".blockofont")))){
            {
                int pageNo = 0;
                final Array<PixmapPacker.Page> pages = packer.getPages();

                out.writeByte(pages.size);

                for (PixmapPacker.Page page : pages) {
                    final File file = new File(outDir, fontName + pageNo + ".png");
                    PixmapIO.writePNG(new FileHandle(file), page.getPixmap());
                    out.writeUTF(file.getName());
                    pageNo++;
                }
            }

            out.writeShort(fontMetrics.ascent + fontMetrics.descent + fontMetrics.lineGap);
            out.writeShort(fontMetrics.ascent);
            out.writeShort(fontMetrics.descent);

            out.writeInt(glyphs.size);
            for (GlyphData glyph : glyphs) {
                out.writeInt(glyph.codePoint);
                out.writeByte(glyph.page);

                if(glyph.packed == null){
                    out.writeShort(0);
                    out.writeShort(0);
                    out.writeShort(0);
                    out.writeShort(0);
                }else{
                    out.writeShort((int) glyph.packed.x);
                    out.writeShort((int) glyph.packed.y);
                    out.writeShort((int) glyph.packed.width);
                    out.writeShort((int) glyph.packed.height);
                }

                out.writeShort(glyph.xOff);
                out.writeShort(glyph.yOff);
                out.writeShort(glyph.advanceWidth);
            }

        } catch (Exception e){
            throw new RuntimeException("Failed to write blockofont", e);
        }
    }

	public static void main (String[] args) {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		packFont(new File("resources raw/press-start-2p/PressStart2P.ttf"), new File("resources/font"), 8);
		System.exit(0);
	}

	public static final class FontMetrics {
		public int ascent, descent, lineGap;

		public static FontMetrics getFontMetrics (STBTTFontinfo fontInfo, int sizePx) {
			final IntBuffer ascent = MemoryUtil.memAllocInt(1);
			final IntBuffer descent = MemoryUtil.memAllocInt(1);
			final IntBuffer lineGap = MemoryUtil.memAllocInt(1);
			stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);

			final float scale = stbtt_ScaleForPixelHeight(fontInfo, sizePx);

			final FontMetrics result = new FontMetrics();
			result.ascent = (int)(ascent.get(0) * scale);
			result.descent = (int)(descent.get(0) * scale);
			result.lineGap = (int)(lineGap.get(0) * scale);

			return result;
		}

		@Override
		public String toString () {
			return "FontMetrics{" + "ascent=" + ascent + ", descent=" + descent + ", lineGap=" + lineGap + '}';
		}
	}

	public static final class GlyphData {
		public int codePoint;
		public int advanceWidth, leftSideBearing;
		public int xOff, yOff, width, height;
		public ByteBuffer bitmap;

		public int page = 0;
		public Rectangle packed;

		private static final IntBuffer advanceWidthBuf = MemoryUtil.memAllocInt(1);
		private static final IntBuffer leftSideBearingBuf = MemoryUtil.memAllocInt(1);

		private static final IntBuffer widthBuf = MemoryUtil.memAllocInt(1);
		private static final IntBuffer heightBuf = MemoryUtil.memAllocInt(1);
		private static final IntBuffer xOffBuf = MemoryUtil.memAllocInt(1);
		private static final IntBuffer yOffBuf = MemoryUtil.memAllocInt(1);

		public static GlyphData create (STBTTFontinfo fontInfo, int codePoint, int glyphIndex, int sizePx) {
			final GlyphData result = new GlyphData();
			result.codePoint = codePoint;

			stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidthBuf, leftSideBearingBuf);
			final float scale = stbtt_ScaleForPixelHeight(fontInfo, sizePx);
			result.advanceWidth = (int)(advanceWidthBuf.get(0) * scale);
			result.leftSideBearing = (int)(leftSideBearingBuf.get(0) * scale);

			if (stbtt_IsGlyphEmpty(fontInfo, glyphIndex) != 0) {
				result.bitmap = null;
			} else {
				result.bitmap = stbtt_GetGlyphBitmap(fontInfo, 0f, scale, glyphIndex, widthBuf, heightBuf, xOffBuf, yOffBuf);
				result.xOff = xOffBuf.get(0);
				result.yOff = yOffBuf.get(0);
				result.width = widthBuf.get(0);
				result.height = heightBuf.get(0);
			}

			return result;
		}

		public Pixmap createPixmap () {
			if (bitmap == null) return null;
			final Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.Alpha);
			final ByteBuffer pixels = pixmap.getPixels();
			pixels.put(bitmap);
			pixels.flip();
			return pixmap;
		}
	}
}
