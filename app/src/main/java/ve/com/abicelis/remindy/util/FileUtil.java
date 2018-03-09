package ve.com.abicelis.remindy.util;

import android.app.Activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import ve.com.abicelis.remindy.R;
import ve.com.abicelis.remindy.model.attachment.Attachment;
import ve.com.abicelis.remindy.model.attachment.AudioAttachment;
import ve.com.abicelis.remindy.model.attachment.ImageAttachment;


public class FileUtil {

    public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
        FileChannel fromChannel = null;
        FileChannel toChannel = null;
        try {
            fromChannel = fromFile.getChannel();
            toChannel = toFile.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try {
                if (fromChannel != null) {
                    fromChannel.close();
                }
            } finally {
                if (toChannel != null) {
                    toChannel.close();
                }
            }
        }
    }

    public static File getAudioAttachmentDir(Activity activity) {
        return new File(activity.getExternalFilesDir(null), activity.getResources().getString(R.string.subdirectory_attachments_audio));
    }

    public static File getImageAttachmentDir(Activity activity) {
        return new File(activity.getExternalFilesDir(null), activity.getResources().getString(R.string.subdirectory_attachments_image));
    }

    public static void createDirIfNotExists(File directory) throws IOException, SecurityException {
        if (directory.mkdirs()) {
            File nomedia = new File(directory, ".nomedia");
            nomedia.createNewFile();
        }
    }


    /**
     * Creates an empty file at the specified directory, with the given name if it doesn't already exist
     */
    public static File createNewFileIfNotExistsInDir(File directory, String fileName) throws IOException {
        File file = new File(directory, fileName);
        file.createNewFile();
        return file;
    }


    /**
     * Deletes the images and audio files from a list of attachments
     */
    public static void deleteAttachmentFiles(Activity activity, List<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            switch (attachment.getType()) {
                case AUDIO:
                    String audioFilename = ((AudioAttachment) attachment).getAudioFilename();
                    deleteAudioAttachment(activity, audioFilename);
                    break;

                case IMAGE:
                    String imageFilename = ((ImageAttachment) attachment).getImageFilename();
                    deleteImageAttachment(activity, imageFilename);
                    break;
            }
        }
    }

    public static void deleteAudioAttachment(Activity activity, String filename) {
        if (filename != null && !filename.isEmpty()) { //Delete file
            File file = new File(FileUtil.getAudioAttachmentDir(activity), filename);
            file.delete();
        }
    }

    public static void deleteImageAttachment(Activity activity, String filename) {
        if (filename != null && !filename.isEmpty()) { //Delete file
            File file = new File(FileUtil.getImageAttachmentDir(activity), filename);
            file.delete();
        }
    }


//    public static File createTempImageFileInDir(File directory, String fileExtension) throws IOException, SecurityException {
//        if(fileExtension.toCharArray()[0] != '.')
//            fileExtension = "." + fileExtension;
//
//        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String fileName = "TEMP_" + UUID.randomUUID().toString() + "_";
//        File file = File.createTempFile(fileName, fileExtension, directory);
//        return file;
//    }

}