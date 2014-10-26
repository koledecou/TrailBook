package com.trailbook.kole.fragments.point_attached_object_create;

import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
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
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.ImageUtil;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.state_objects.PathManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateNoteFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_PARAM1 = "note_id";
    
    private static final int CAMERA_PIC_REQUEST = 1;
    private static final int GALLERY_PIC_REQUEST = 2;
    private static final String NOTE_ID = "NOTE_ID";
    private static final String TEXT =  "TEXT";
    private static final String TEMP_IMAGE_FILE_URI = "TEMP_IMAGE_URI";
    private static final String IMAGE_FILE_NAME = "IMAGE_FILE_NAME";

    private String mNoteId;
    private EditText mEditTextContent;
    private CreatePointObjectListener mListener;
    private ImageView mImageView;
    private Button mOkButton;
    private Button mCancelButton;
    private Button mDeleteButton;

    private String mImageFileName;
    private Uri mLastPictureUri;

    public static CreateNoteFragment newInstance(String noteId) {
        CreateNoteFragment fragment = new CreateNoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, noteId);
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
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(NOTE_ID, mNoteId);
//        outState.putString(PARENT_SEGMENT_ID, mParentPathId);
        if (mEditTextContent.getText() != null)
            outState.putString(TEXT, mEditTextContent.getText().toString());
        if (mLastPictureUri != null) {
            outState.putString(TEMP_IMAGE_FILE_URI, mLastPictureUri.toString());
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "saving uri:" + mLastPictureUri.toString());
        }
        if (mImageFileName != null) {
            outState.putString(IMAGE_FILE_NAME, mImageFileName);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "saving image file name:" + mImageFileName);
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

        mDeleteButton = (Button)view.findViewById(R.id.cn_b_delete);
        mDeleteButton.setOnClickListener(this);

        if (savedInstanceState == null) {
            populateValuesFromNote(PathManager.getInstance().getPointAttachedObject(mNoteId));
        } else {
            restoreInstance(savedInstanceState);
        }

        showSoftKeyboard();
        return view;
    }

    private void populateValuesFromNote(PointAttachedObject paoNote) {
        if (paoNote != null) {
            Note note = (Note)paoNote.getAttachment();
            String noteContent = note.getNoteContent();
            mEditTextContent.setText(noteContent);
            mImageFileName = paoNote.getAttachment().getImageFileName();
            restoreImageView();
        } else {
            //if it's an existing object then you can delete it.
            if (PathManager.getInstance().getPointAttachedObject(mNoteId) != null)
                ApplicationUtils.disableButton(mDeleteButton);
        }
    }

    private void restoreInstance(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, "restoring note state");
        if (savedInstanceState != null) {
            mNoteId = savedInstanceState.getString(NOTE_ID);
//            mParentPathId = savedInstanceState.getString(PARENT_SEGMENT_ID);
            String text = savedInstanceState.getString(TEXT);
            mEditTextContent.setText(text);
            String tempImageFileUri = savedInstanceState.getString(TEMP_IMAGE_FILE_URI);
            if (tempImageFileUri != null) {
                Log.d(Constants.TRAILBOOK_TAG, "getting saved Uri:" + tempImageFileUri);
                mLastPictureUri = Uri.parse(tempImageFileUri);
            }

            mImageFileName = savedInstanceState.getString(IMAGE_FILE_NAME);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": got saved image " + mImageFileName);
            restoreImageView();
/*            try {
                Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": trying last picture uri");
                mImageView.setImageURI(mLastPictureUri);
            } catch (Exception e) {
                Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": probably deleted.  get from image file name");
                restoreImageView();
            }*/
        }
    }

    private void restoreImageView() {
        if (mImageFileName != null && mImageFileName.length()>0) {
            File imageFileDir = TrailbookFileUtilities.getInternalImageFileDir();
            mLastPictureUri = Uri.parse(imageFileDir + File.separator + mImageFileName);
            Log.d(Constants.TRAILBOOK_TAG, "CreateNoteFragment: image uri is:" + mLastPictureUri);
            mImageView.setImageURI(mLastPictureUri);
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onPointObjectCreateCanceled();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (CreatePointObjectListener) activity;
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
        } else if (id == R.id.action_bar_gallery) {
            addPictureFromGallery();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addPictureFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        startActivityForResult(intent, GALLERY_PIC_REQUEST);
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

                Bitmap bitmap;
                try {
                    //todo: refactor
                    String fileNameFullBitmap = imageUri.getPath();
                    bitmap = ImageUtil.rotateBitmapFromCamera(fileNameFullBitmap, Constants.IMAGE_CAPTURE_WIDTH);
                    File tempFile = new File(imageUri.getPath());
                    FileUtils.forceDelete(tempFile);

                    mImageFileName = getImageFileName();
                    TrailbookFileUtilities.saveImage(getActivity(), bitmap, mImageFileName);
                    mImageView.setImageBitmap(bitmap);
                    mImageView.invalidate();
                } catch (Exception e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Image capture failed.", e);
                    Toast.makeText(getActivity(), "Image Capture Failed - can't create bitmap", Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (resultCode == getActivity().RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed
            }
        } else if (requestCode == GALLERY_PIC_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                Uri chosenImageUri = data.getData();

                Bitmap bitmap = null;
                try {
                    bitmap = ImageUtil.getRotatedBitmapFromGallery(getActivity(), chosenImageUri, Constants.IMAGE_CAPTURE_WIDTH);
                    mImageFileName = getImageFileName();
                    TrailbookFileUtilities.saveImage(getActivity(), bitmap, mImageFileName);
                    mImageView.setImageBitmap(bitmap);
                    mImageView.invalidate();
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
            Note newNote = new Note();
            newNote.setImageFileName(mImageFileName);
            newNote.setNoteContent(mEditTextContent.getText().toString());
            mListener.onPointObjectCreated(mNoteId, newNote);
        } else if (view.getId() == R.id.cn_b_cancel) {
            mListener.onPointObjectCreateCanceled();
        } else if (view.getId() == R.id.cn_b_delete) {
            mListener.onPaoDeleted(mNoteId);
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
}
