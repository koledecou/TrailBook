package com.trailbook.kole.fragments.point_attached_object_create;

import com.trailbook.kole.data.Attachment;

/**
 * Created by kole on 9/29/2014.
 */
public interface CreatePointObjectListener {
    public void onPointObjectCreated(String paoId, Attachment newAttachment);
    public void onPointObjectCreateCanceled();
    public void onPaoDeleted(String noteId);
}
