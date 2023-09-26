package egovframework.kevinlab.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;

public class ImageUtils {

	public enum Format {
		JPG, JPEG, PNG, GIF;
	}

	public static final int RATIO = 0;
    public static final int SAME = -1;

	public static ByteBuffer createThumbnail(File file, int width, int height, Format format) throws IOException {
		return createThumbnail(file, width, height, format.name());
	}

	public static ByteBuffer createThumbnail(File file, int width, int height, String format) throws IOException {

        Image srcImg = null;

		if (StringUtils.equals(Format.PNG.name(), format) || StringUtils.equals(Format.GIF.name(), format)) {
            srcImg = ImageIO.read(file);
		} else {
            srcImg = new ImageIcon(file.toURL()).getImage();
		}

        int srcWidth = srcImg.getWidth(null);
        int srcHeight = srcImg.getHeight(null);

        int destWidth = -1, destHeight = -1;

        if (width == SAME) {
            destWidth = srcWidth;
        } else if (width > 0) {
            destWidth = width;
        }

        if (height == SAME) {
            destHeight = srcHeight;
        } else if (height > 0) {
            destHeight = height;
        }

        if (width == RATIO && height == RATIO) {
            destWidth = srcWidth;
            destHeight = srcHeight;
        } else if (width == RATIO) {
            double ratio = ((double)destHeight) / ((double)srcHeight);
            destWidth = (int)((double)srcWidth * ratio);
        } else if (height == RATIO) {
            double ratio = ((double)destWidth) / ((double)srcWidth);
            destHeight = (int)((double)srcHeight * ratio);
        }

        Image imgTarget = srcImg.getScaledInstance(destWidth, destHeight, Image.SCALE_SMOOTH);
        int pixels[] = new int[destWidth * destHeight];
        PixelGrabber pg = new PixelGrabber(imgTarget, 0, 0, destWidth, destHeight, pixels, 0, destWidth);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage());
        }

        BufferedImage destImg = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
        destImg.setRGB(0, 0, destWidth, destHeight, pixels, 0, destWidth);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(destImg, format, os);

		os.flush();

		ByteBuffer buffer = ByteBuffer.wrap(os.toByteArray());
		return buffer;
    }
}