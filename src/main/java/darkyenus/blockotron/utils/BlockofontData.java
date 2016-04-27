package darkyenus.blockotron.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import java.io.DataInputStream;

/**
 * Special binary format for fast serialization of fonts
 */
public class BlockofontData extends BitmapFont.BitmapFontData {

    public int lineSpacing = 2;

    public BlockofontData(FileHandle fontFile) {
        load(fontFile, false);
    }

    /**
     * Format specification:
     *
     * <code>
     * u-byte pages;
     * [pages]{
     *     UTF pagePath; //Relative to fontFile
     * };
     * short lineHeight;
     * short ascent; //Real ascent, not just to the top of glyph
     * short descent; //From baseline down
     *
     * int amountOfGlyphs;
     * [amountOfGlyphs] {
     *     int codePoint;
     *     u-byte page;
     *     u-short pageX, pageY, pageWidth, pageHeight; //Coordinates on page
     *     short offsetX, offsetY, xAdvance;
     * };
     * </code>
     *
     * @param fontFile file to load
     * @param flip must be false
     */
    @Override
    public void load(FileHandle fontFile, boolean flip) {
        if(flip)throw new RuntimeException("Flipping is forbidden");

        this.flipped = false;
        this.fontFile = fontFile;
        this.padTop = 0;
        this.padBottom = 0;
        this.padLeft = 0;
        this.padRight = 0;
        this.scaleX = 1f;
        this.scaleY = 1f;

        try(final DataInputStream in = new DataInputStream(fontFile.read(512))){
            this.imagePaths = new String[in.readUnsignedByte()];
            for (int i = 0; i < imagePaths.length; i++) {
                imagePaths[i] = fontFile.sibling(in.readUTF()).path();
            }
            this.lineHeight = in.readShort();
            this.down = -lineHeight-lineSpacing;
            final short ascent = in.readShort();
            this.capHeight = ascent;
            this.ascent = 0f;
            this.descent = in.readShort();

            final int amountOfGlyphs = in.readInt();
            for (int i = 0; i < amountOfGlyphs; i++) {
                final int codePoint = in.readInt();
                final int page = in.readUnsignedByte();

                final int pX = in.readUnsignedShort();
                final int pY = in.readUnsignedShort();
                final int pWidth = in.readUnsignedShort();
                final int pHeight = in.readUnsignedShort();

                final int offsetX = in.readShort();
                final int offsetY = in.readShort();
                final int xAdvance = in.readShort();

                final BitmapFont.Glyph glyph = new BitmapFont.Glyph();
                glyph.id = codePoint;
                glyph.page = page;
                glyph.srcX = pX;
                glyph.srcY = pY;
                glyph.width = pWidth;
                glyph.height = pHeight;

                glyph.xoffset = offsetX;
                glyph.yoffset = - ascent - offsetY - pHeight;
                glyph.xadvance = xAdvance;

                setGlyph(codePoint, glyph);

                if (codePoint == 0){
                    missingGlyph = glyph;
                } else if (codePoint == ' '){
                    spaceWidth = xAdvance;
                } else if(codePoint == 'x'){
                    xHeight = -offsetY;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load font "+fontFile, e);
        }
    }
}
