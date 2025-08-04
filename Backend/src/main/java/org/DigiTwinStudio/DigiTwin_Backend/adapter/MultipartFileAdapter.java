package org.DigiTwinStudio.DigiTwin_Backend.adapter;

import com.mongodb.lang.NonNull;
import lombok.RequiredArgsConstructor;

import org.DigiTwinStudio.DigiTwin_Backend.domain.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Adapts an UploadedFile entity to the MultipartFile interface.
 */
@RequiredArgsConstructor
public class MultipartFileAdapter implements MultipartFile {

    private final UploadedFile uploadedFile;

    @NonNull
    @Override
    public String getName() {
        return uploadedFile.getFilename();
    }

    @Override
    public String getOriginalFilename() {
        return uploadedFile.getFilename();
    }

    @Override
    public String getContentType() {
        return uploadedFile.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public long getSize() {
        return 0; // or uploadedFile.getSize() if you store it
    }

    @NonNull
    @Override
    public byte[] getBytes() {
        return new byte[0]; // no actual content needed here
    }

    @NonNull
    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    /**
     * Not supported in this adapter.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void transferTo(@NonNull java.io.File dest) {
        throw new UnsupportedOperationException("Not supported");
    }
}
