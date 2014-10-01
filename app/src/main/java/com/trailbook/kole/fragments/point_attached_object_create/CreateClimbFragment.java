package com.trailbook.kole.fragments.point_attached_object_create;

import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
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
import android.widget.Spinner;
import android.widget.Toast;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Climb;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Grade;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.state_objects.PathManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateClimbFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_PARAM1 = "id";

    private static final int CAMERA_PIC_REQUEST = 1;
    private static final int GALLERY_PIC_REQUEST = 2;
    private static final String ID = "ID";
    private static final String TEMP_IMAGE_FILE_URI = "TEMP_IMAGE_URI";
    private static final String IMAGE_FILE_NAME = "IMAGE_FILE_NAME";
    private static final String CLIMB_NAME = "CLIMB_NAME";
    private static final String GRADE = "GRADE";
    private static final String GRADE_SYSTEM_POSITION = "GRADE_SYSTEM_POSITION";
    private static final String TYPE_POSITION = "TYPE_POSITION";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String RACK_DESCRIPTION = "RACK_DESCRIPTION";

    private String mPAOId;
    private CreatePointObjectListener mListener;
    private ImageView mImageView;
    private Button mOkButton;
    private Button mCancelButton;
    private Button mDeleteButton;

    private Spinner mSpinnerType;
    private EditText mEditTextName;
    private EditText mEditTextDescription;
    private EditText mEditTextGrade;
    private EditText mEditTextRackDescription;
    private Spinner mSpinnerGradingSystem;

    private String mImageFileName;
    private Uri mLastPictureUri;

    public static CreateClimbFragment newInstance(String paoId) {
        CreateClimbFragment fragment = new CreateClimbFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, paoId);
        fragment.setArguments(args);
        return fragment;
    }
    public CreateClimbFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mPAOId = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ID, mPAOId);
        if (mLastPictureUri != null) {
            outState.putString(TEMP_IMAGE_FILE_URI, mLastPictureUri.toString());
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "saving uri:" + mLastPictureUri.toString());
        }
        if (mImageFileName != null) {
            outState.putString(IMAGE_FILE_NAME, mImageFileName);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "saving image file name:" + mImageFileName);
        }
        if (mEditTextGrade != null) {
            outState.putString(GRADE, mEditTextGrade.getText().toString());
        }
        if (mEditTextDescription != null) {
            outState.putString(DESCRIPTION, mEditTextDescription.getText().toString());
        }
        if (mEditTextName != null) {
            outState.putString(CLIMB_NAME, mEditTextName.getText().toString());
        }
        if (mEditTextRackDescription != null) {
            outState.putString(RACK_DESCRIPTION, mEditTextRackDescription.getText().toString());
        }
        if (mSpinnerGradingSystem != null) {
            outState.putInt(GRADE_SYSTEM_POSITION, mSpinnerGradingSystem.getSelectedItemPosition());
        }
        if (mSpinnerType != null) {
            outState.putInt(TYPE_POSITION, mSpinnerType.getSelectedItemPosition());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_climb, container, false);

        mImageView = (ImageView) view.findViewById(R.id.cc_image);

        mOkButton = (Button)view.findViewById(R.id.cc_b_ok);
        mOkButton.setOnClickListener(this);

        mCancelButton = (Button)view.findViewById(R.id.cc_b_cancel);
        mCancelButton.setOnClickListener(this);

        mDeleteButton = (Button)view.findViewById(R.id.cc_b_delete);
        mDeleteButton.setOnClickListener(this);

        mSpinnerGradingSystem = (Spinner)view.findViewById(R.id.cc_spinner_grade_system);
        mSpinnerType = (Spinner)view.findViewById(R.id.cc_spinner_type);

        mEditTextName = (EditText)view.findViewById(R.id.cc_et_name);
        mEditTextDescription = (EditText)view.findViewById(R.id.cc_et_description);
        mEditTextGrade = (EditText)view.findViewById(R.id.cc_et_grade);
        mEditTextRackDescription = (EditText)view.findViewById(R.id.cc_et_rack);

        if (savedInstanceState == null) {
            populateValuesFromClimb(PathManager.getInstance().getPointAttachedObject(mPAOId));
        } else {
            restoreInstance(savedInstanceState);
        }

        showSoftKeyboard();
        return view;
    }

    private void populateValuesFromClimb(PointAttachedObject paoClimb) {
        if (paoClimb != null) {
            Climb climb = (Climb)paoClimb.getAttachment();
            mEditTextName.setText(climb.name);
            mImageFileName = paoClimb.getAttachment().getImageFileName();
            mEditTextGrade.setText(climb.grade.grade);
            mEditTextDescription.setText(climb.description);
            mEditTextRackDescription.setText(climb.rackDescription);
            //todo: set spinners

            restoreImageView();
        } else {
            //if it's an existing object then you can delete it.
            if (PathManager.getInstance().getPointAttachedObject(mPAOId) != null)
                ApplicationUtils.disableButton(mDeleteButton);
        }
    }

    private void restoreInstance(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, "restoring note state");
        if (savedInstanceState != null) {
            mPAOId = savedInstanceState.getString(ID);

            mEditTextGrade.setText(savedInstanceState.getString(GRADE));
            mEditTextName.setText(savedInstanceState.getString(CLIMB_NAME));
            mEditTextDescription.setText(savedInstanceState.getString(DESCRIPTION));
            mEditTextRackDescription.setText(savedInstanceState.getString(RACK_DESCRIPTION));
            //todo: restore spinners

            String tempImageFileUri = savedInstanceState.getString(TEMP_IMAGE_FILE_URI);
            if (tempImageFileUri != null) {
                Log.d(Constants.TRAILBOOK_TAG, "getting saved Uri:" + tempImageFileUri);
                mLastPictureUri = Uri.parse(tempImageFileUri);
            }

            mImageFileName = savedInstanceState.getString(IMAGE_FILE_NAME);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": got saved image " + mImageFileName);
            restoreImageView();
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
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");

            startActivityForResult(intent, GALLERY_PIC_REQUEST);
    }

    public void addPicture() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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
                TrailbookFileUtilities.saveImage(getActivity(), bitmap, mImageFileName);
                mImageView.setImageBitmap(bitmap);
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
                    //todo: refactor
                    String fileNameFullBitmap = chosenImageUri.getPath();
                    ExifInterface exif=new ExifInterface(fileNameFullBitmap);
                    exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                    bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),
                            chosenImageUri);
                    bitmap = TrailbookFileUtilities.scaleBitmapToWidth(bitmap, Constants.IMAGE_CAPTURE_WIDTH);
                    mImageFileName = getImageFileName();
                    TrailbookFileUtilities.saveImage(getActivity(), bitmap, mImageFileName);
                    bitmap = TrailbookFileUtilities.getRotatedBitmap(bitmap, exif.getAttribute(ExifInterface.TAG_ORIENTATION));
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
        if (view.getId() == R.id.cc_b_ok) {
            Climb newClimb = new Climb();
            newClimb.addImageFile(mImageFileName);
            newClimb.setName(mEditTextName.getText().toString());
            newClimb.setDescription(mEditTextDescription.getText().toString());
            Grade grade = new Grade();
            grade.grade=mEditTextGrade.getText().toString();
            grade.gradingSystem = mSpinnerGradingSystem.getSelectedItem().toString();
            newClimb.setGrade(grade);
            newClimb.setName(mEditTextName.getText().toString());
            newClimb.setRackDescription(mEditTextRackDescription.getText().toString());
            newClimb.setClimbType(mSpinnerType.getSelectedItem().toString());
            mListener.onPointObjectCreated(mPAOId, newClimb);
        } else if (view.getId() == R.id.cc_b_cancel) {
            mListener.onPointObjectCreateCanceled();
        } else if (view.getId() == R.id.cc_b_delete) {
            mListener.onPaoDeleted(mPAOId);
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditTextName.getWindowToken(), 0);
    }

    private void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditTextName, 0);
    }
}
