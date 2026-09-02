package com.fitnesslife.gym.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.fitnesslife.gym.model.User;
import com.fitnesslife.gym.repository.UserRepository;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class QrCodeService {

    private final UserRepository userRepository;

    private static final String QR_BASE_DIR = "qrcode";
    private static final int QR_WIDTH = 500;
    private static final int QR_HEIGHT = 500;

    public String generateAndSaveQRCode(User user) throws WriterException, IOException {
        String qrText = user.getIdentification().toString();

        String userDir = QR_BASE_DIR + File.separator + user.getIdentification();
        Path userDirPath = Paths.get(userDir);

        if (!Files.exists(userDirPath)) {
            Files.createDirectories(userDirPath);
        }

        String qrFileName = user.getIdentification() + "_qr.png";
        String qrFilePath = userDir + File.separator + qrFileName;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        File qrFile = new File(qrFilePath);
        ImageIO.write(qrImage, "png", qrFile);

        user.setQrCodePath(qrFilePath);
        userRepository.save(user);

        return qrFilePath;
    }

    public BufferedImage getQRCodeImage(User user) throws IOException {
        if (user.getQrCodePath() == null || user.getQrCodePath().isEmpty()) {
            return null;
        }

        File qrFile = new File(user.getQrCodePath());
        if (!qrFile.exists()) {
            return null;
        }

        return ImageIO.read(qrFile);
    }

    public boolean qrCodeExists(User user) {
        if (user.getQrCodePath() == null || user.getQrCodePath().isEmpty()) {
            return false;
        }

        File qrFile = new File(user.getQrCodePath());
        return qrFile.exists();
    }

    public String regenerateQRCode(User user) throws WriterException, IOException {
        if (user.getQrCodePath() != null && !user.getQrCodePath().isEmpty()) {
            File oldQrFile = new File(user.getQrCodePath());
            if (oldQrFile.exists()) {
                oldQrFile.delete();
            }
        }

        return generateAndSaveQRCode(user);
    }
}
