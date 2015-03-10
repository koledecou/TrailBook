package com.trailbook.kole.fragments.point_attached_object_create;

import android.app.Activity;
import android.app.Fragment;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Attachment;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.ImageUtil;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.state_objects.PathManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by kole on 11/18/2014.
 */
public abstract class CreatePointAttachedObjectFragment extends Fragment implements View.OnClickListener {
    protected static final String ARG_PARAM1 = "note_id";
    private static final String NOTE_ID = "NOTE_ID";
    private static final String TEMP_IMAGE_FILE_URI = "TEMP_IMAGE_URI";
    private static final String IMAGE_FILE_NAME = "IMAGE_FILE_NAME";

    private static final int CAMERA_PIC_REQUEST = 1;
    private static final int GALLERY_PIC_REQUEST = 2;
    private ArrayList<String> mImageFileNames;
    private int mCurrentImageIndex;
    private Uri mLastPictureUri;
    private CreatePointObjectListener mListener;
    private View mView;
    private ImageView mImageView;
    private ImageView mNextArrowView;
    private ImageView mPreviousArrowView;
    private Button mDeleteButton;
    protected String mNoteId;

    public CreatePointAttachedObjectFragment() {
        // Required empty public constructor
        mImageFileNames = new ArrayList<String>();
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
        if (mLastPictureUri != null) {
            outState.putString(TEMP_IMAGE_FILE_URI, mLastPictureUri.toString());
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "saving uri:" + mLastPictureUri.toString());
        }
        if (mImageFileNames != null) {
            outState.putStringArrayList(IMAGE_FILE_NAME, mImageFileNames);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "saving image file names:" + mImageFileNames);
        }
    }

    protected void restoreInstance(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() +"restoring note state");
        if (savedInstanceState != null) {
            mNoteId = savedInstanceState.getString(NOTE_ID);

            String tempImageFileUri = savedInstanceState.getString(TEMP_IMAGE_FILE_URI);
            if (tempImageFileUri != null) {
                Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": getting saved Uri:" + tempImageFileUri);
                mLastPictureUri = Uri.parse(tempImageFileUri);
            }

            mImageFileNames = savedInstanceState.getStringArrayList(IMAGE_FILE_NAME);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": got saved image " + mImageFileNames);
            restoreImageView();
        }
    }

    public View inflateView(LayoutInflater inflater, ViewGroup container, int layoutId) {
        mView = inflater.inflate(layoutId, container, false);
        return mView;
    }

    public void createViewObjects() {
        mImageView = (ImageView) mView.findViewById(R.id.cn_image);

        Button okButton = (Button) mView.findViewById(R.id.cn_b_ok);
        okButton.setOnClickListener(this);

        Button cancelButton = (Button) mView.findViewById(R.id.cn_b_cancel);
        cancelButton.setOnClickListener(this);

        mDeleteButton = (Button)mView.findViewById(R.id.cn_b_delete);
        mDeleteButton.setOnClickListener(this);

        mNextArrowView = (ImageView)mView.findViewById(R.id.swipe_right);
        if (mNextArrowView!= null)
            mNextArrowView.setOnClickListener(this);

        mPreviousArrowView = (ImageView)mView.findViewById(R.id.swipe_left);
        if (mPreviousArrowView != null)
            mPreviousArrowView.setOnClickListener(this);

        showOrHideSliderArrows(mImageFileNames);
        showOrHideImageContainer(mImageFileNames);
    }


    public void populateValues(PointAttachedObject pao) {
        if (pao != null) {
            mImageFileNames = pao.getAttachment().getImageFileNames();
            restoreImageView();
        } else {
            //if it's an existing object then you can delete it.
            if (PathManager.getInstance().getPointAttachedObject(mNoteId) != null)
                ApplicationUtils.disableButton(mDeleteButton);
        }

    }

    private void restoreImageView() {
        if (mImageFileNames != null && mImageFileNames.size()>0) {
            File imageFileDir = TrailbookFileUtilities.getInternalImageFileDir();
            Uri imageToDisplay = Uri.parse(imageFileDir + File.separator + mImageFileNames.get(mImageFileNames.size()-1));
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "CreateNoteFragment: image uri is:" + mLastPictureUri);
            mImageView.setImageURI(imageToDisplay);
            showOrHideSliderArrows(mImageFileNames);
            showOrHideImageContainer(mImageFileNames);
        }
    }

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
        if (id == R.id.action_bar_take_picture) {
            addPicture();
            return true;
        } else if (id == R.id.action_bar_gallery) {
            addPictureFromGallery();
            return true;
        } else if (id == R.id.action_bar_cut_image) {
            deleteCurrentImage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentImage() {
        if (mImageFileNames != null && mImageFileNames.size()>0) {
            mImageFileNames.remove(mCurrentImageIndex);
        }
        showOrHideSliderArrows(mImageFileNames);
        showOrHideImageContainer(mImageFileNames);
        mCurrentImageIndex=0;
        loadCurrentImage();
    }

    public void addPictureFromGallery() {
        Intent intent = getGalleryIntent();
        startActivityForResult(intent, GALLERY_PIC_REQUEST);
    }

    private Intent getGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        return intent;
    }

    public void addPicture() {
        Intent cameraIntent = getCameraIntent();
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    private Intent getCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String fileName = getTemporaryFilenameFromTimestamp();
        mLastPictureUri = TrailbookFileUtilities.getOutputMediaFileUri(fileName); // create a file to save the image
        Log.d(Constants.TRAILBOOK_TAG, "picture uri: " + mLastPictureUri);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mLastPictureUri); // set the image file name
        return cameraIntent;
    }

    private String getTemporaryFilenameFromTimestamp() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "image_" + timeStamp + ".jpg";
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": temp file name:" + fileName);
        return fileName;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mImageFileNames == null)
            mImageFileNames = new ArrayList<String>();

        Uri imageUri = null;
        if (requestCode == CAMERA_PIC_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                try {
                    Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "picture uri after capture:" + mLastPictureUri);
                    if (mLastPictureUri != null) {
                        imageUri = mLastPictureUri;
                    } else if (data != null) {
                        imageUri = data.getData();
                    }
                } catch (Exception e) {
                    Log.e(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "Image capture failed.", e);
                    Toast.makeText(getActivity(), getClass().getSimpleName() + "Image Capture Failed", Toast.LENGTH_LONG).show();
                    return;
                }

                Bitmap bitmap;
                try {
                    //todo: refactor
                    String fileNameFullBitmap = imageUri.getPath();
                    Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": fileNameFullBitmap: "+ fileNameFullBitmap);
                    bitmap = ImageUtil.rotateBitmapFromCamera(fileNameFullBitmap, Constants.IMAGE_CAPTURE_WIDTH);
                    File tempFile = new File(imageUri.getPath());
                    FileUtils.forceDelete(tempFile);

                    String newImageFileName = getImageFileName();
                    mImageFileNames.add(newImageFileName);
                    TrailbookFileUtilities.saveImage(getActivity(), bitmap, newImageFileName);
                    mImageView.setImageBitmap(bitmap);
                    mImageView.invalidate();
                } catch (Exception e) {
                    Log.e(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "Image capture failed.", e);
                    Toast.makeText(getActivity(), "Image Capture Failed - can't create bitmap", Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (resultCode == getActivity().RESULT_CANCELED) {
                // User cancelled the image capture
                Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": canceled");
            } else {
                // Image capture failed
                Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": failed");
            }
        } else if (requestCode == GALLERY_PIC_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                Uri chosenImageUri = data.getData();

                Bitmap bitmap = null;
                try {
                    bitmap = ImageUtil.getRotatedBitmapFromGallery(getActivity(), chosenImageUri, Constants.IMAGE_CAPTURE_WIDTH);
                    String newImageFileName = getImageFileName();
                    mImageFileNames.add(newImageFileName);
                    TrailbookFileUtilities.saveImage(getActivity(), bitmap, newImageFileName);
                    mImageView.setImageBitmap(bitmap);
                    mImageView.invalidate();
                } catch (Exception e) {
                    Log.e(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": exception getting gallery picture", e);
                }
            }
        }
        mCurrentImageIndex = mImageFileNames.size()-1;
        loadCurrentImage();
    }

    private String getImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return timeStamp + ".jpg";
    }

    @Override
    public void onClick(View view) {
        hideSoftKeyboard();
        if (view.getId() == R.id.cn_b_ok) {
            Attachment a = createAttachment();
            if (mImageFileNames != null && mImageFileNames.size()>0)
                a.addImageFiles(mImageFileNames);
            mListener.onPointObjectCreated(mNoteId, a);
        } else if (view.getId() == R.id.cn_b_cancel) {
            mListener.onPointObjectCreateCanceled();
        } else if (view.getId() == R.id.cn_b_delete) {
            mListener.onPaoDeleted(mNoteId);
        } else if (view.getId() == R.id.swipe_right) {
            incrementCurrentImageIndex();
            loadCurrentImage();
        } else if (view.getId() == R.id.swipe_left) {
            decrementCurrentImageIndex();
            loadCurrentImage();
        }
    }

    private void loadCurrentImage() {
        if (mImageFileNames != null && mImageFileNames.size() > 0) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": loading image :" + mImageFileNames.get(mCurrentImageIndex));
            mImageView.setVisibility(View.VISIBLE);
            Picasso.with(getActivity()).load(TrailbookFileUtilities.getInternalImageFile(mImageFileNames.get(mCurrentImageIndex))).into(mImageView);
        } else {
            mImageView.setVisibility(View.INVISIBLE);
        }
        showOrHideSliderArrows(mImageFileNames);
        showOrHideImageContainer(mImageFileNames);
    }

    private void showOrHideImageContainer(ArrayList<String> imageFileNames) {
        View imageContainer = mView.findViewById(R.id.cn_image_container);
        if (imageFileNames == null || imageFileNames.size() < 1) {
            imageContainer.setVisibility(View.GONE);
        } else {
            imageContainer.setVisibility(View.VISIBLE);
        }
    }

    private void showOrHideSliderArrows(ArrayList<String> imageFileNames) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": ImageFileNames: " + imageFileNames);

        if (imageFileNames == null || imageFileNames.size() < 2) {
            mNextArrowView.setVisibility(View.INVISIBLE);
            mPreviousArrowView.setVisibility(View.INVISIBLE);
        } else {
            mNextArrowView.setVisibility(View.VISIBLE);
            mPreviousArrowView.setVisibility(View.VISIBLE);
        }
    }

    private void decrementCurrentImageIndex() {
        mCurrentImageIndex--;
        if (mCurrentImageIndex < 0) {
            mCurrentImageIndex = mImageFileNames.size()-1;
        }
    }

    private void incrementCurrentImageIndex() {
        mCurrentImageIndex++;
        if (mCurrentImageIndex >= mImageFileNames.size()) {
            mCurrentImageIndex = 0;
        }
    }
    protected abstract Attachment createAttachment();
    protected abstract void hideSoftKeyboard();
    protected abstract void showSoftKeyboard();
}
