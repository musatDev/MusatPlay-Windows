package dev.musat.musatplay;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class AppImages {
    private AppImages() {}

    public static BufferedImage icon() {
        try (var stream = AppImages.class.getResourceAsStream("/icon.png")) {
            if (stream == null) throw new IOException("Missing app icon");
            return ImageIO.read(stream);
        } catch (IOException e) { throw new IllegalStateException("Cannot load app icon", e); }
    }

    public static BufferedImage scaled(BufferedImage source, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return image;
    }

    public static void writeWindowsIcon(Path path) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(scaled(icon(), 256, 256), "png", png);
        byte[] bytes = png.toByteArray();
        ByteBuffer result = ByteBuffer.allocate(22 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        result.putShort((short) 0).putShort((short) 1).putShort((short) 1);
        result.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
        result.putShort((short) 1).putShort((short) 32).putInt(bytes.length).putInt(22).put(bytes);
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.write(path, result.array());
    }
}
