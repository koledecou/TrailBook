package com.trailbook.kole.fragments;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateNoteFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "note_id";
    private static final String ARG_PARAM2 = "parent_id";
    
    private static final int CAMERA_PIC_REQUEST = 1;
    private static final int GALLERY_PIC_REQUEST = 2;
    private static final String NOTE_ID = "NOTE_ID";
    private static final String PARENT_SEGMENT_ID = "PARENT_SEGMENT_ID";
    private static final String TEXT =  "TEXT";
    private static final String TEMP_IMAGE_FILE_URI = "TEMP_IMAGE_URI";

    private String mNoteId;
    private EditText mEditTextContent;
    private CreateNoteFragmentListener mListener;
    private ImageView mImageView;
    private Button mOkButton;
    private Button mCancelButton;
    private String mParentSegmentId;
    private String mImageFileName;
    private Uri mLastPictureUri;

    public static CreateNoteFragment newInstance(String noteId, String parentSegmentId) {
        CreateNoteFragment fragment = new CreateNoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, noteId);
        args.putString(ARG_PARAM2, parentSegmentId);
        fragment.setArguments(args);
        return fragment;
    }
    public CreateNoteFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mNoteId = getArguments().getString(ARG_PARAM1);
            mParentSegmentId = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(NOTE_ID, mNoteId);
        outState.putString(PARENT_SEGMENT_ID, mParentSegmentId);
        if (mEditTextContent.getText() != null)
            outState.putString(TEXT, mEditTextContent.getText().toString());
        if (mLastPictureUri != null) {
            outState.putString(TEMP_IMAGE_FILE_URI, mLastPictureUri.toString());
            Log.d(Constants.TRAILBOOK_TAG, "saving uri:" + mLastPictureUri.toString());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_note, container, false);
        mEditTextContent = (EditText) view.findViewById(R.id.cn_note_content);
        mImageView = (ImageView) view.findViewById(R.id.cn_image);
        mOkButton = (Button)view.findViewById(R.id.cn_b_ok);
        mOkButton.setOnClickListener(this);
        mCancelButton = (Button)view.findViewById(R.id.cn_b_cancel);
        mCancelButton.setOnClickListener(this);

        restoreInstance(savedInstanceState);
        showSoftKeyboard();
        return view;
    }

    private void restoreInstance(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, "restoring note state");
        if (savedInstanceState != null) {
            mNoteId = savedInstanceState.getString(NOTE_ID);
            mParentSegmentId = savedInstanceState.getString(PARENT_SEGMENT_ID);
            String text = savedInstanceState.getString(TEXT);
            mEditTextContent.setText(text);
            String tempImageFileUri = savedInstanceState.getString(TEMP_IMAGE_FILE_URI);
            if (tempImageFileUri != null) {
                Log.d(Constants.TRAILBOOK_TAG, "getting saved Uri:" + tempImageFileUri);
                mLastPictureUri = Uri.parse(tempImageFileUri);
            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onNoteCreateCanceled();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (CreateNoteFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Toast.makeText(getActivity(), "taking picture", Toast.LENGTH_SHORT).show();
        if (id == R.id.action_bar_take_picture) {
            addPicture();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addPicture() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        String fileName = "image_" + timeStamp + ".jpg";
        mLastPictureUri = TrailbookFileUtilities.getOutputMediaFileUri(fileName); // create a file to save the image
        Log.d(Constants.TRAILBOOK_TAG, "picture uri: " + mLastPictureUri);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mLastPictureUri); // set the image file name
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri imageUri = null;
        if (requestCode == CAMERA_PIC_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                try {
                    Log.d(Constants.TRAILBOOK_TAG, "picture uri after capture:" + mLastPictureUri);
                    if (data == null && mLastPictureUri != null) {
                        imageUri = mLastPictureUri;
                    } else {
                        imageUri = data.getData();
                    }
                } catch (Exception e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Image capture failed.", e);
                    Toast.makeText(getActivity(), "Image Capture Failed", Toast.LENGTH_LONG).show();
                    return;
                }

                //TODO: refactor with Picasso
                Bitmap bitmap;
                try {
                    String fileNameFullBitmap = imageUri.getPath();
                    ExifInterface exif=new ExifInterface(fileNameFullBitmap);
                    exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                    bitmap = BitmapFactory.decodeFile(fileNameFullBitmap);
                    bitmap = TrailbookFileUtilities.scaleBitmapToWidth(bitmap, Constants.IMAGE_CAPTURE_WIDTH);
                    bitmap = TrailbookFileUtilities.getRotatedBitmap(bitmap, exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                    File tempFile = new File(imageUri.getPath());
                    FileUtils.forceDelete(tempFile);
                } catch (Exception e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Image capture failed.", e);
                    Toast.makeText(getActivity(), "Image Capture Failed - can't create bitmap", Toast.LENGTH_LONG).show();
                    return;
                }

                mImageFileName = getImageFileName();
                TrailbookFileUtilities.saveImageForPathSegment(getActivity(), bitmap, mParentSegmentId, mImageFileName);
                mImageView.setImageBitmap(bitmap);
            } else if (resultCode == getActivity().RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        } else if (requestCode == GALLERY_PIC_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                Uri chosenImageUri = data.getData();

                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),
                            chosenImageUri);
                    bitmap = TrailbookFileUtilities.scaleBitmapToWidth(bitmap, 480);
                    mImageFileName = getImageFileName();
                    TrailbookFileUtilities.saveImageForPathSegment(getActivity(), bitmap, mParentSegmentId, mImageFileName);
                    mImageView.setImageBitmap(bitmap);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private String getImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return timeStamp + ".jpg";
    }

    @Override
    public void onClick(View view) {
        hideSoftKeyboard();
        if (view.getId() == R.id.cn_b_ok) {
            Note newNote = new Note(TrailbookPathUtilities.getNewNoteId(), mParentSegmentId);
            newNote.setImageFileName(mImageFileName);
            newNote.setNoteContent(mEditTextContent.getText().toString());
            mListener.onNoteCreated(newNote);
        } else if (view.getId() == R.id.cn_b_cancel) {
            //TODO: should delete any outstanding image files
            mListener.onNoteCreateCanceled();
        }
    }



    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditTextContent.getWindowToken(), 0);
    }

    private void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditTextContent, 0);
    }

    public interface CreateNoteFragmentListener {
        public void onNoteCreated(Note newNote);
        public void onNoteCreateCanceled();
    }

}
