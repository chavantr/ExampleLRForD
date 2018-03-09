package ve.com.abicelis.remindy.app.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.theartofdev.edmodo.cropper.CropImage;
import com.transitionseverywhere.Rotate;
import com.transitionseverywhere.TransitionManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import ve.com.abicelis.remindy.R;
import ve.com.abicelis.remindy.app.dialogs.SelectImageSourceDialogFragment;
import ve.com.abicelis.remindy.enums.ImageSourceType;
import ve.com.abicelis.remindy.enums.TapTargetSequenceType;
import ve.com.abicelis.remindy.model.attachment.ImageAttachment;
import ve.com.abicelis.remindy.util.FileUtil;
import ve.com.abicelis.remindy.util.ImageUtil;
import ve.com.abicelis.remindy.util.PermissionUtil;
import ve.com.abicelis.remindy.util.SharedPreferenceUtil;
import ve.com.abicelis.remindy.util.SnackbarUtil;
import ve.com.abicelis.remindy.util.TapTargetSequenceUtil;



public class EditImageAttachmentActivity extends AppCompatActivity implements View.OnClickListener{


    //CONSTS
    private static final int REQUEST_TAKE_PICTURE_PERMISSION = 239;
    private static String [] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static final String TAG = EditImageAttachmentActivity.class.getSimpleName();
    public static final String IMAGE_FILE_EXTENSION = ".jpg";
    private static final int IMAGE_COMPRESSION_PERCENTAGE = 30;
    private static final int THUMBNAIL_COMPRESSION_PERCENTAGE = 60;
    private static final int REQUEST_IMAGE_CAPTURE = 123;
    private static final int REQUEST_PICK_IMAGE_GALLERY = 124;


    public static final String IMAGE_ATTACHMENT_EXTRA = "IMAGE_ATTACHMENT_EXTRA";
    public static final String HOLDER_POSITION_EXTRA = "HOLDER_POSITION_EXTRA";
    public static final String EDITING_ATTACHMENT_EXTRA = "EDITING_ATTACHMENT_EXTRA";
    public static final int EDIT_IMAGE_ATTACHMENT_REQUEST_CODE = 82;

    //DATA
    private int mRotation;
    private boolean mEditingExistingImageAttachment;
    private ImageAttachment mImageAttachment;
    private int mHolderPosition;
    private Bitmap mImageBackup;

    //UI
    private RelativeLayout mContainer;
    private ImageView mImage;
    private FloatingActionButton mCrop;
    private FloatingActionButton mRotate;
    private FloatingActionButton mCamera;
    private Button mOk;
    private Button mCancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image_attachment);

        mContainer = (RelativeLayout) findViewById(R.id.activity_edit_image_attachment_container);
        mImage = (ImageView) findViewById(R.id.activity_edit_image_attachment_image);
        mCrop = (FloatingActionButton) findViewById(R.id.activity_edit_image_attachment_crop);
        mRotate = (FloatingActionButton) findViewById(R.id.activity_edit_image_attachment_rotate);
        mCamera = (FloatingActionButton) findViewById(R.id.activity_edit_image_attachment_camera);
        mOk = (Button) findViewById(R.id.activity_edit_image_attachment_ok);
        mCancel = (Button) findViewById(R.id.activity_edit_image_attachment_cancel);

        //If screen was rotated, for example
        if (savedInstanceState != null) {
            //Load values from savedInstanceState
            mImageAttachment = (ImageAttachment) savedInstanceState.getSerializable(IMAGE_ATTACHMENT_EXTRA);
            mHolderPosition = savedInstanceState.getInt(HOLDER_POSITION_EXTRA);
            mEditingExistingImageAttachment = savedInstanceState.getBoolean(EDITING_ATTACHMENT_EXTRA);

            //Get jpeg from SD Card
            mImageBackup = ImageUtil.getBitmap(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()));
            if(mImageBackup == null) {  //If jpeg was deleted from device, use thumbnail
                mImageBackup = ImageUtil.getBitmap(mImageAttachment.getThumbnail());
                saveThumbnailAsImageFile(mImageBackup);
            }

            mImage.setImageBitmap(mImageBackup);
            showTapTargetSequence();
        } else {

            //Coming from another activity: Check if intent has required extras
            if(getIntent().hasExtra(HOLDER_POSITION_EXTRA) && getIntent().hasExtra(IMAGE_ATTACHMENT_EXTRA)) {
                mHolderPosition = getIntent().getIntExtra(HOLDER_POSITION_EXTRA, -1);
                mImageAttachment = (ImageAttachment) getIntent().getSerializableExtra(IMAGE_ATTACHMENT_EXTRA);

                if (mImageAttachment.getImageFilename() != null && !mImageAttachment.getImageFilename().isEmpty()) {
                    mImageBackup = ImageUtil.getBitmap(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()));
                    if(mImageBackup == null) {  //IF image was deleted from device
                        mImageBackup = ImageUtil.getBitmap(mImageAttachment.getThumbnail());
                        saveThumbnailAsImageFile(mImageBackup);
                    }

                    mEditingExistingImageAttachment = true;
                    mImage.setImageBitmap(mImageBackup);
                    showTapTargetSequence();
                } else {
                    mImageAttachment = new ImageAttachment();
                    mImageAttachment.setImageFilename(UUID.randomUUID().toString() + IMAGE_FILE_EXTENSION);
                    handleShowCameraGalleryDialog();
                }
            } else {
                BaseTransientBottomBar.BaseCallback<Snackbar> callback = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                };
                Log.e(TAG, "Missing HOLDER_POSITION_EXTRA and/or IMAGE_ATTACHMENT_EXTRA parameters in EditImageAttachmentActivity.");
                SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, callback);
                finish();
            }

//            mImageAttachment = new ImageAttachment(new byte[0], "09ce7135-86d6-4d93-bcc5-1fbff5651d0f.jpg");
//            mImageBackup = ImageUtil.getBitmap(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()));
//            mEditingExistingImageAttachment = true;
//            mImage.setImageBitmap(mImageBackup);

        }


        mRotation = 0;
        mCrop.setOnClickListener(this);
        mRotate.setOnClickListener(this);
        mCamera.setOnClickListener(this);
        mOk.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    private void showTapTargetSequence() {
        TapTargetSequenceUtil.showTapTargetSequenceFor(this, TapTargetSequenceType.EDIT_IMAGE_ATTACHMENT_ACTIVITY);
    }



    private void saveThumbnailAsImageFile(Bitmap thumbnail) {
        File imageFile = new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename());
        try {
            ImageUtil.saveBitmapAsJpeg(imageFile, thumbnail, IMAGE_COMPRESSION_PERCENTAGE);
            SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.activity_edit_image_attachment_snackbar_error_image_deleted_from_device, SnackbarUtil.SnackbarDuration.LONG, null);

        }catch (IOException e) {
            BaseTransientBottomBar.BaseCallback<Snackbar> callback = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    super.onDismissed(transientBottomBar, event);
                    setResult(RESULT_CANCELED);
                    finish();
                }
            };
            SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, callback);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        applyPendingRotation();
        outState.putSerializable(IMAGE_ATTACHMENT_EXTRA, mImageAttachment);
        outState.putInt(HOLDER_POSITION_EXTRA, mHolderPosition);
        outState.putBoolean(EDITING_ATTACHMENT_EXTRA, mEditingExistingImageAttachment);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.activity_edit_image_attachment_crop:
                applyPendingRotation();
                File imageFile = new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename());
                Uri imageUri = FileProvider.getUriForFile(this, "ve.com.abicelis.remindy.fileprovider", imageFile);
                CropImage.activity(imageUri).setAllowFlipping(false).setAllowRotation(false).start(this);
                break;

            case R.id.activity_edit_image_attachment_rotate:
                mRotation += 90;

                TransitionManager.beginDelayedTransition(mContainer, new Rotate());
                mImage.setRotation(mRotation);
                break;

            case R.id.activity_edit_image_attachment_camera:
                handleShowCameraGalleryDialog();
                break;

            case R.id.activity_edit_image_attachment_ok:
                applyPendingRotation();
                updateImageAttachmentThumbnail();

                Intent returnData = new Intent();
                returnData.putExtra(HOLDER_POSITION_EXTRA, mHolderPosition);
                returnData.putExtra(IMAGE_ATTACHMENT_EXTRA, mImageAttachment);
                setResult(RESULT_OK, returnData);
                finish();
                break;

            case R.id.activity_edit_image_attachment_cancel:
                if(mEditingExistingImageAttachment)
                    restoreImageFromBackup();
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    private void handleShowCameraGalleryDialog() {
            FragmentManager fm = this.getSupportFragmentManager();

        SelectImageSourceDialogFragment dialog = SelectImageSourceDialogFragment.newInstance();
            dialog.setListener(new SelectImageSourceDialogFragment.SelectImageSourceSelectedListener() {
                @Override
                public void onSourceSelected(ImageSourceType imageSourceType) {
                    switch (imageSourceType) {
                        case CAMERA:
                            applyPendingRotation();
                            handleImageCapture();
                            break;
                        case GALLERY:
                            applyPendingRotation();
                            handlePickImageUsingGallery();
                            break;
                        case NONE:
                            if(!mEditingExistingImageAttachment) {
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                            break;
                    }
                }
            });
            dialog.show(fm, "SelectImageSourceDialogFragment");

    }


    private void applyPendingRotation() {
        if(mRotation != 0) {
            //Normalize rotation
            mRotation = mRotation % 360;
            if (mRotation < 0) mRotation += 360;

            //Get the bitmap
            File imageFile = new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename());
            Bitmap imageBitmap = ImageUtil.getBitmap(imageFile);

            //Rotate it
            Matrix matrix = new Matrix();
            matrix.postRotate(mRotation);
            imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);

            try {
                ImageUtil.saveBitmapAsJpeg(imageFile, imageBitmap, IMAGE_COMPRESSION_PERCENTAGE);
            }catch (IOException e) {
                SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.activity_edit_image_attachment_snackbar_error_could_not_rotate, SnackbarUtil.SnackbarDuration.LONG, null);
            }

            //Reload ImageView
            mImage.setImageBitmap(imageBitmap);
            mImage.setRotation(0);

            //Reset Rotation
            mRotation = 0;
        }
    }

    private void restoreImageFromBackup() {
        File imageFile = new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename());
        try {
            ImageUtil.saveBitmapAsJpeg(imageFile, mImageBackup, IMAGE_COMPRESSION_PERCENTAGE);
        }catch (IOException e) {
            SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.activity_edit_image_attachment_snackbar_error_could_not_rotate, SnackbarUtil.SnackbarDuration.LONG, null);
        }
    }



    private void handleImageCapture() {

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        String[] nonGrantedPermissions = PermissionUtil.checkIfPermissionsAreGranted(this, permissions);

        if(nonGrantedPermissions == null)
            dispatchTakePictureIntent();
        else
            ActivityCompat.requestPermissions(this, nonGrantedPermissions, REQUEST_TAKE_PICTURE_PERMISSION);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_TAKE_PICTURE_PERMISSION:
                for (int result : grantResults) {
                    if(result != PackageManager.PERMISSION_GRANTED) {
                        BaseTransientBottomBar.BaseCallback<Snackbar> callback = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                super.onDismissed(transientBottomBar, event);
                                handleImageCapture();
                            }
                        };
                        SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.NOTICE, R.string.activity_edit_image_attachment_snackbar_error_no_permissions, SnackbarUtil.SnackbarDuration.SHORT, callback);
                        return;
                    }
                }

                //Permissions granted
                dispatchTakePictureIntent();
                break;
        }
    }






    private void dispatchTakePictureIntent() {

        File imageAttachmentDir = FileUtil.getImageAttachmentDir(this);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Create a dir if it doesn't exist
        try {
            FileUtil.createDirIfNotExists(imageAttachmentDir);
        } catch (IOException ex) {
            Log.e(TAG, "Error while creating the image directory");
            SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, null);
        }

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {

            //If file doesn't exist, create an empty file where the photo will be stored
            File imageAttachmentFile;
            //if(!mEditingExistingImageAttachment) {
            try {
                imageAttachmentFile = FileUtil.createNewFileIfNotExistsInDir(imageAttachmentDir, mImageAttachment.getImageFilename());
            } catch (IOException ex) {
                Log.e(TAG, "Error while creating the image");
                SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, null);
                imageAttachmentFile = null;
            }
            //} else {
            //    imageAttachmentFile = new File(imageAttachmentDir, mImageAttachment.getImageFilename());
            //}

            Uri imageUri;
            if(imageAttachmentFile != null) {
                try {
                    imageUri = FileProvider.getUriForFile(this, "ve.com.abicelis.remindy.fileprovider", imageAttachmentFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                    //HACK: Before starting the camera activity on pre-lollipop devices, make sure to grant permissions to all packages that need it
                    List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        this.grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "There was a problem with the image");
                    SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, null);
                }

            } else {
                Log.e(TAG, "There was a problem loading or creating the file for the image");
                SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, null);
            }
        } else {
            Log.e(TAG, getResources().getString(R.string.activity_edit_image_attachment_snackbar_error_no_camera_installed));
            SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.activity_edit_image_attachment_snackbar_error_no_camera_installed, SnackbarUtil.SnackbarDuration.LONG, null);
        }
    }

    private void handlePickImageUsingGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, REQUEST_PICK_IMAGE_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            checkExifAndFixImageRotation();
            Bitmap newImage = ImageUtil.getBitmap(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()));
            mImage.setImageBitmap(newImage);
            showTapTargetSequence();

        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                try {
                    Bitmap newImage = ImageUtil.getBitmap(result.getUri(), this);
                    mImage.setImageBitmap(newImage);
                    ImageUtil.saveBitmapAsJpeg(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()), newImage, IMAGE_COMPRESSION_PERCENTAGE);
                }catch (IOException e) {
                    SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.activity_edit_image_attachment_snackbar_error_loading_cropped_image, SnackbarUtil.SnackbarDuration.LONG, null);
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.e(TAG, getResources().getString(R.string.activity_edit_image_attachment_snackbar_error_loading_cropped_image) + result.getError().toString());
                SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.activity_edit_image_attachment_snackbar_error_loading_cropped_image, SnackbarUtil.SnackbarDuration.LONG, null);
            }
        } else if (requestCode == REQUEST_PICK_IMAGE_GALLERY && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            try {
                Bitmap newImage = ImageUtil.getBitmap(imageUri, this);
                mImage.setImageBitmap(newImage);
                ImageUtil.saveBitmapAsJpeg(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()), newImage, IMAGE_COMPRESSION_PERCENTAGE);
                showTapTargetSequence();

            }catch (IOException e) {
                SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.activity_edit_image_attachment_snackbar_error_loading_gallery_image, SnackbarUtil.SnackbarDuration.LONG, null);
            }
            mImage.setImageURI(imageUri);
        }

    }

    private void checkExifAndFixImageRotation() {
        try {
            ExifInterface ei = new ExifInterface(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()).getAbsolutePath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    mRotation = 90;
                    applyPendingRotation();
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    mRotation = 180;
                    applyPendingRotation();
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    mRotation = 270;
                    applyPendingRotation();
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                    //Good!

                default:
                    break;
            }
            //ei.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }catch (IOException e) {
            /*Do nothing*/
        }
    }


    private void updateImageAttachmentThumbnail() {
        try {
            Bitmap thumbnail = ImageUtil.getBitmap(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()));
            thumbnail = ImageUtil.scaleBitmap(thumbnail, 480);
            byte[] thumbnailBytes = ImageUtil.toCompressedByteArray(thumbnail, THUMBNAIL_COMPRESSION_PERCENTAGE);
            mImageAttachment.setThumbnail(thumbnailBytes);
        } catch (Exception e) {
            Log.e(TAG, "There was a problem updating the thumbnail");
            SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, null);        }
    }

}
