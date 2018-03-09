package ve.com.abicelis.remindy.app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

import ve.com.abicelis.remindy.R;
import ve.com.abicelis.remindy.model.attachment.ImageAttachment;
import ve.com.abicelis.remindy.util.FileUtil;
import ve.com.abicelis.remindy.util.ImageUtil;
import ve.com.abicelis.remindy.util.SnackbarUtil;



public class ViewImageAttachmentActivity extends AppCompatActivity implements View.OnClickListener{


    //CONSTS
    public static final String TAG = ViewImageAttachmentActivity.class.getSimpleName();
    public static final String IMAGE_ATTACHMENT_EXTRA = "IMAGE_ATTACHMENT_EXTRA";
    public static final String HOLDER_POSITION_EXTRA = "HOLDER_POSITION_EXTRA";
    public static final int VIEW_IMAGE_ATTACHMENT_REQUEST_CODE = 83;

    //DATA
    private ImageAttachment mImageAttachment;
    private int mHolderPosition;

    //UI
    private RelativeLayout mContainer;
    private Toolbar mToolbar;
    private PhotoView mImage;
    private FloatingActionButton mEdit;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Enable Lollipop Material Design transitions
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        setContentView(R.layout.activity_view_image_attachment);

        mContainer = (RelativeLayout) findViewById(R.id.activity_view_image_attachment_container);
        mToolbar = (Toolbar) findViewById(R.id.activity_view_image_attachment_toolbar);
        mImage = (PhotoView) findViewById(R.id.activity_view_image_attachment_image);
        mEdit = (FloatingActionButton) findViewById(R.id.activity_view_image_attachment_edit);


        if (savedInstanceState != null) {   //Rotated screen!
            mImageAttachment = (ImageAttachment) savedInstanceState.getSerializable(IMAGE_ATTACHMENT_EXTRA);
            mHolderPosition = savedInstanceState.getInt(HOLDER_POSITION_EXTRA);

            mImage.setImageBitmap(ImageUtil.getBitmap(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename())));
        } else {

            if(getIntent().hasExtra(HOLDER_POSITION_EXTRA) && getIntent().hasExtra(IMAGE_ATTACHMENT_EXTRA)) {
                mHolderPosition = getIntent().getIntExtra(HOLDER_POSITION_EXTRA, -1);
                mImageAttachment = (ImageAttachment) getIntent().getSerializableExtra(IMAGE_ATTACHMENT_EXTRA);

                if (mImageAttachment.getImageFilename() != null && !mImageAttachment.getImageFilename().isEmpty()) {
                    Bitmap imageBitmap = ImageUtil.getBitmap(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename()));
                    if(imageBitmap == null) {
                        SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.activity_edit_image_attachment_snackbar_error_image_deleted_from_device, SnackbarUtil.SnackbarDuration.LONG, null);
                        mImage.setImageBitmap(ImageUtil.getBitmap(mImageAttachment.getThumbnail()));
                    } else
                        mImage.setImageBitmap(imageBitmap);
                } else {
                    BaseTransientBottomBar.BaseCallback<Snackbar> callback = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    };
                    Log.e(TAG, "Invalid image filename value in HOLDER_POSITION_EXTRA in ViewImageAttachmentActivity.");
                    SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, callback);
                    finish();
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
                Log.e(TAG, "Missing HOLDER_POSITION_EXTRA and/or IMAGE_ATTACHMENT_EXTRA parameters in ViewImageAttachmentActivity.");
                SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.error_unexpected, SnackbarUtil.SnackbarDuration.LONG, callback);
                finish();
            }
        }

        setUpToolbar();
        mEdit.setOnClickListener(this);
    }


    private void setUpToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.activity_view_image_attachment_toolbar);
        mToolbar.setTitle(getResources().getString( R.string.activity_view_image_attachment_title));
        mToolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.icon_back_material));

        //Set toolbar as actionbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(IMAGE_ATTACHMENT_EXTRA, mImageAttachment);
        outState.putInt(HOLDER_POSITION_EXTRA, mHolderPosition);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.activity_view_image_attachment_edit:
                launchImageEditAttachmentActivity();
                break;
        }
    }


    private void launchImageEditAttachmentActivity() {
        Intent goToEditImageAttachmentActivity = new Intent(this, EditImageAttachmentActivity.class);
        goToEditImageAttachmentActivity.putExtra(EditImageAttachmentActivity.IMAGE_ATTACHMENT_EXTRA, mImageAttachment);
        goToEditImageAttachmentActivity.putExtra(EditImageAttachmentActivity.HOLDER_POSITION_EXTRA, mHolderPosition);
        this.startActivityForResult(goToEditImageAttachmentActivity, EditImageAttachmentActivity.EDIT_IMAGE_ATTACHMENT_REQUEST_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(requestCode == EditImageAttachmentActivity.EDIT_IMAGE_ATTACHMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            int position = data.getIntExtra(EditImageAttachmentActivity.HOLDER_POSITION_EXTRA, -1);
            ImageAttachment imageAttachment = (ImageAttachment) data.getSerializableExtra(EditImageAttachmentActivity.IMAGE_ATTACHMENT_EXTRA);

            mHolderPosition = position;
            mImageAttachment = imageAttachment;
            mImage.setImageBitmap(ImageUtil.getBitmap(new File(FileUtil.getImageAttachmentDir(this), mImageAttachment.getImageFilename())));

        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                handleBackPressed();
                return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleBackPressed();
    }

    private void handleBackPressed() {
        Intent returnData = new Intent();
        returnData.putExtra(HOLDER_POSITION_EXTRA, mHolderPosition);
        returnData.putExtra(IMAGE_ATTACHMENT_EXTRA, mImageAttachment);
        setResult(RESULT_OK, returnData);
        finish();
        //supportFinishAfterTransition();
    }


}
