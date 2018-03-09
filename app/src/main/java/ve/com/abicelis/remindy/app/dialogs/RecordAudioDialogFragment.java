package ve.com.abicelis.remindy.app.dialogs;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transitionseverywhere.TransitionManager;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import ve.com.abicelis.remindy.R;
import ve.com.abicelis.remindy.app.views.VisualizerView;
import ve.com.abicelis.remindy.model.attachment.AudioAttachment;
import ve.com.abicelis.remindy.util.FileUtil;
import ve.com.abicelis.remindy.util.PermissionUtil;
import ve.com.abicelis.remindy.util.SnackbarUtil;



public class RecordAudioDialogFragment extends DialogFragment implements View.OnClickListener {


    //CONST
    private static final String TAG = RecordAudioDialogFragment.class.getSimpleName();
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String AUDIO_FILE_EXTENSION = ".3gp";
    private static final int REPEAT_INTERVAL_VIZ = 50;
    private static final int REPEAT_INTERVAL_TIME = 1000;
    private static final int STATE_IDLE = 0;
    private static final int STATE_RECORDING = 1;
    private static final int STATE_PLAYING = 2;
    private static String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //DATA
    private int mState = STATE_IDLE;
    private AudioAttachment mAudioAttachment;
    private String mAudioFilePath = null;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private Handler mVizHandler = null;                // Handler for updating the visualizer
    private Handler mTextHandler = null;                // Handler for updating the elapsed text
    private long startTime = 0L;
    private RecordAudioDialogFinishListener mListener;

    //UI
    private RelativeLayout mContainer;
    private FloatingActionButton mFab;
    private TextView mTapToStartRecording;
    private TextView mRecordTime;
    private VisualizerView mVisualizer;
    private ImageView mRecIcon;
    private ImageView mPlayIcon;


    public RecordAudioDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static RecordAudioDialogFragment newInstance(AudioAttachment audioAttachment) {
        RecordAudioDialogFragment frag = new RecordAudioDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("audioAttachment", audioAttachment);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        int height = getResources().getDimensionPixelSize(R.dimen.dialog_record_audio_height);
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_record_audio_width);

        getDialog().getWindow().setLayout(width, height);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setCancelable(false);

        mAudioAttachment = (AudioAttachment) getArguments().getSerializable("audioAttachment");

        View dialogView =  inflater.inflate(R.layout.dialog_record_audio, container);

        File audioAttachmentDir = FileUtil.getAudioAttachmentDir(getActivity());
        try {
            FileUtil.createDirIfNotExists(audioAttachmentDir);
        }catch (IOException| SecurityException e) {
            Toast.makeText(getActivity(), "Problem!", Toast.LENGTH_SHORT).show();
        }

        if (mAudioAttachment.getAudioFilename() == null || mAudioAttachment.getAudioFilename().isEmpty()) {
            mAudioAttachment.setAudioFilename(UUID.randomUUID().toString() + AUDIO_FILE_EXTENSION);
        }
        mAudioFilePath = new File(audioAttachmentDir, "/" + mAudioAttachment.getAudioFilename()).getAbsolutePath();

        mContainer = (RelativeLayout) dialogView.findViewById(R.id.dialog_record_audio_container);
        mFab = (FloatingActionButton) dialogView.findViewById(R.id.dialog_record_audio_fab);
        mFab.setOnClickListener(this);
        mTapToStartRecording = (TextView) dialogView.findViewById(R.id.dialog_record_audio_tap_to_start_recording);

        mRecordTime = (TextView) dialogView.findViewById(R.id.dialog_record_audio_time);
        mVisualizer = (VisualizerView) dialogView.findViewById(R.id.dialog_record_audio_visualizer);
        mVisualizer.setLineColor(ContextCompat.getColor(getContext(), R.color.primary));

        mRecIcon = (ImageView) dialogView.findViewById(R.id.dialog_record_audio_rec_icon);
        mPlayIcon = (ImageView) dialogView.findViewById(R.id.dialog_record_audio_play_icon);

        // create the Handlers
        mVizHandler = new Handler();
        mTextHandler = new Handler();

        return dialogView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.dialog_record_audio_fab:
                switch (mState) {
                    case STATE_IDLE:
                        handleStartRecording();
                        break;

                    case STATE_RECORDING:
                        mState = STATE_PLAYING;
                        stopRecording();
                        startPlaying();
                        break;

                    case STATE_PLAYING:
                        stopPlaying();
                }
                break;
        }
    }

    private void handleStartRecording() {

        // Check for the recording permissions. If not granted yet, request permission.
        String[] nonGrantedPermissions = PermissionUtil.checkIfPermissionsAreGranted(getActivity(), permissions);

        if(nonGrantedPermissions == null) {
            mState = STATE_RECORDING;
            startRecording();
        } else
            requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                for (int result : grantResults) {
                    if(result != PackageManager.PERMISSION_GRANTED) {
                        SnackbarUtil.showSnackbar(mContainer, SnackbarUtil.SnackbarType.ERROR, R.string.dialog_record_audio_snackbar_error_no_permissions, SnackbarUtil.SnackbarDuration.LONG, null);
                        return;
                    }
                }

                //Permissions granted
                mState = STATE_RECORDING;
                startRecording();
                break;
        }
    }

    private void startRecording() {

        TransitionManager.beginDelayedTransition(mContainer);

        mFab.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.icon_stop_fab_mini));
        mFab.setSize(FloatingActionButton.SIZE_MINI);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mFab.getLayoutParams();
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);    //Same as lp.removeRule(...) only this call is supported from API 17 :(
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
        mFab.setLayoutParams(lp);

        mTapToStartRecording.setVisibility(View.GONE);
        mRecordTime.setVisibility(View.VISIBLE);
        mVisualizer.setVisibility(View.VISIBLE);
        mRecIcon.setVisibility(View.VISIBLE);


        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mAudioFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
        mVizHandler.post(updateVisualizer);
        startTime = SystemClock.uptimeMillis();
        mTextHandler.postDelayed(updateTime, REPEAT_INTERVAL_TIME);
    }


    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    private void startPlaying() {
        //Reset time
        startTime = SystemClock.uptimeMillis();

        TransitionManager.beginDelayedTransition(mContainer);
        mFab.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.icon_check_fab_mini));
        mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.fab_accept_green)));
        mRecIcon.setVisibility(View.INVISIBLE);
        mPlayIcon.setVisibility(View.VISIBLE);

        mPlayer = new MediaPlayer();
        try {


            mPlayer.setDataSource(mAudioFilePath);
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    handleDismissDialog();

                }
            });
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void handleDismissDialog() {
        if(mListener != null)
            mListener.onFinishedRecording(mAudioAttachment);
        dismiss();
    }

    private void stopPlaying() {
        if(mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        handleDismissDialog();
    }


    // updates the visualizer every 50 milliseconds
    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (mState == STATE_RECORDING) // if we are already recording
            {
                // get the current amplitude
                int x = mRecorder.getMaxAmplitude();
                mVisualizer.addAmplitude(x); // update the VisualizeView
                mVisualizer.invalidate(); // refresh the VisualizerView

                // update in 40 milliseconds
                mVizHandler.postDelayed(this, REPEAT_INTERVAL_VIZ);
            }
        }
    };

    // updates the visualizer every 50 milliseconds
    Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            if (mState == STATE_RECORDING || mState == STATE_PLAYING) // if we are recording or playing
            {
                long elapsedMillis = SystemClock.uptimeMillis() - startTime;

                int secs = (int) (elapsedMillis / 1000);
                int mins = secs / 60;
                secs = secs % 60;
                if (mRecordTime != null)
                    mRecordTime.setText("00:" + String.format(Locale.getDefault(), "%02d", mins) + ":" + String.format(Locale.getDefault(), "%02d", secs));
                mTextHandler.postDelayed(this, REPEAT_INTERVAL_TIME);
            }
        }
    };





    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }


    public void setListener(RecordAudioDialogFinishListener listener) {
        mListener = listener;
    }


    public interface RecordAudioDialogFinishListener {
        void onFinishedRecording(AudioAttachment audioAttachment);
    }
}
